package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.accessory.GlovesHandler;
import com.soulinchains.arcaneartifacts.phenomena.Phenomena;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Counterspell: cancels magic.
 * <ul>
 *     <li>On a creature: every spell it has in play is undone - its spell projectiles, lingering spells
 *     and walls vanish, its black holes, tornadoes and meteors stop, the free repeat of its Focusing
 *     Gloves is lost - and it is Silenced for a few seconds (it cannot cast Ars spells).</li>
 *     <li>On a block: every spell in play near that point is undone, whoever cast it.</li>
 * </ul>
 */
public class EffectCounterspell extends AreaEffect {
    public static final EffectCounterspell INSTANCE = new EffectCounterspell();

    private EffectCounterspell() {
        super(ArcaneArtifacts.id("glyph_counterspell"), "Counterspell");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        int undone;
        if (target instanceof LivingEntity caster) {
            undone = counter(level, caster.getUUID(), caster.position());
            int ticks = Math.max(20, Config.COUNTERSPELL_SILENCE_SECONDS.get() * 20 + stats.getDurationInTicks());
            caster.addEffect(new MobEffectInstance(ModEffects.SILENCED, ticks, 0, false, true, true), shooter);
        } else {
            double radius = Math.max(1.0, Config.COUNTERSPELL_RADIUS.get() + 2.0 * stats.getAoeMultiplier());
            undone = counterNear(level, point, radius);
        }
        level.sendParticles(ParticleTypes.SMOKE, point.x, point.y, point.z, 20 + undone * 5, 0.5, 0.5, 0.5, 0.02);
        level.playSound(null, point.x, point.y, point.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    /** Undoes everything cast by {@code owner} (searching spell entities within 64 blocks of {@code around}). */
    public static int counter(ServerLevel level, UUID owner, Vec3 around) {
        int count = 0;
        for (EntityProjectileSpell spell : level.getEntitiesOfClass(EntityProjectileSpell.class, new AABB(around, around).inflate(64),
                e -> e.getOwner() != null && owner.equals(e.getOwner().getUUID()))) {
            spell.discard();
            count++;
        }
        count += Phenomena.cancelOwnedBy(owner);
        GlovesHandler.cancelFor(owner);
        return count;
    }

    /** Undoes every spell in play within {@code radius} of the point. */
    public static int counterNear(ServerLevel level, Vec3 point, double radius) {
        int count = 0;
        for (EntityProjectileSpell spell : level.getEntitiesOfClass(EntityProjectileSpell.class, new AABB(point, point).inflate(radius),
                e -> e.position().distanceTo(point) <= radius)) {
            spell.discard();
            count++;
        }
        return count + Phenomena.cancelNear(level, point, radius);
    }

    @Override
    public int getDefaultManaCost() {
        return 60;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAOE.INSTANCE, AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAOE.INSTANCE, "On a block: cancels spells in a wider area.");
        map.put(AugmentExtendTime.INSTANCE, "On a creature: a longer silence.");
        map.put(AugmentDurationDown.INSTANCE, "On a creature: a shorter silence.");
    }

    @Override
    public String getBookDescription() {
        return "Cancels magic. On a creature, every spell it has in play vanishes and it is silenced for a few seconds. On a block, every spell in play nearby vanishes, whoever cast it.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.ABJURATION);
    }
}
