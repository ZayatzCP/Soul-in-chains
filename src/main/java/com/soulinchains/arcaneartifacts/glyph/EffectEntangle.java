package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
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

/** Entangle (Root Magic): vines hold the target in place, 3 seconds (+1 per Extend Time). AOE catches everything around. */
public class EffectEntangle extends AreaEffect {
    public static final EffectEntangle INSTANCE = new EffectEntangle();
    public static final int BASE_TICKS = 60;

    private EffectEntangle() {
        super(ArcaneArtifacts.id("glyph_entangle"), "Entangle");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        int ticks = Math.max(20, BASE_TICKS + (int) Math.round(stats.getDurationMultiplier() * 20));
        double radius = stats.getAoeMultiplier() > 0 ? 1.5 + stats.getAoeMultiplier() : 0;
        if (target instanceof LivingEntity living && living != shooter) {
            entangle(level, living, ticks, shooter);
        }
        if (radius > 0) {
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius))) {
                if (living != shooter && living != target) {
                    entangle(level, living, ticks, shooter);
                }
            }
        }
    }

    /** Vines grab the creature: Entangled for the given time. */
    public static void entangle(ServerLevel level, LivingEntity living, int ticks, @Nullable LivingEntity source) {
        ticks = com.soulinchains.arcaneartifacts.necro.Pvp.holdTicks(living, ticks);
        if (ticks <= 0) {
            return;
        }
        living.addEffect(new MobEffectInstance(ModEffects.ENTANGLED, ticks, 0, false, true, true), source);
        living.setDeltaMovement(0, Math.min(0, living.getDeltaMovement().y), 0);
        level.sendParticles(ParticleTypes.COMPOSTER, living.getX(), living.getY() + 0.3, living.getZ(), 24, 0.4, 0.4, 0.4, 0.02);
        level.playSound(null, living.blockPosition(), SoundEvents.CAVE_VINES_PLACE, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    @Override
    public int getDefaultManaCost() {
        return 35;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.ONE;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE, AugmentAOE.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentExtendTime.INSTANCE, "The vines hold one second longer.");
        map.put(AugmentDurationDown.INSTANCE, "The vines let go sooner.");
        map.put(AugmentAOE.INSTANCE, "Vines grab every creature around the point.");
    }

    @Override
    public String getBookDescription() {
        return "Vines grab the target and hold it in place for 3 seconds: it can neither walk nor jump.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.ELEMENTAL_EARTH);
    }
}
