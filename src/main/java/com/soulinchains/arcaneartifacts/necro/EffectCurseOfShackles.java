package com.soulinchains.arcaneartifacts.necro;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.glyph.AreaEffect;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Curse of Shackles (Chainlord): for 8 seconds (+2 per Extend Time) the target cannot jump, heal or teleport, and deals
 * a quarter less damage.
 */
public class EffectCurseOfShackles extends AreaEffect {
    public static final EffectCurseOfShackles INSTANCE = new EffectCurseOfShackles();
    public static final int BASE_TICKS = 160;
    public static final float DAMAGE_DEALT = 0.75F;

    private EffectCurseOfShackles() {
        super(ArcaneArtifacts.id("glyph_curse_of_shackles"), "Curse of Shackles");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        if (shooter instanceof Player player && !Necro.requireRank(player, Necro.CHAINLORD)) {
            return;
        }
        if (!(target instanceof LivingEntity living) || living == shooter) {
            return;
        }
        int ticks = Pvp.holdTicks(living, Math.max(40, BASE_TICKS + (int) Math.round(stats.getDurationMultiplier() * 40)));
        if (ticks <= 0) {
            return;
        }
        living.addEffect(new MobEffectInstance(ModEffects.CURSED_CHAINS, ticks, 0, false, true, true), shooter);
        living.getPersistentData().putUUID(EffectShackle.SHACKLED_BY, shooter.getUUID());
        level.sendParticles(ParticleTypes.SCULK_SOUL, living.getX(), living.getY() + living.getBbHeight() / 2, living.getZ(), 16, 0.3, 0.5, 0.3, 0.02);
        level.playSound(null, living.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.3F);
    }

    @Override
    public int getDefaultManaCost() {
        return 80;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.THREE;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentExtendTime.INSTANCE, "The curse lasts two seconds longer.");
        map.put(AugmentDurationDown.INSTANCE, "The curse lifts sooner.");
    }

    @Override
    public String getBookDescription() {
        return "Chainlord rank. For 8 seconds the target cannot jump, heal or teleport, and deals a quarter less damage.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.NECROMANCY);
    }
}
