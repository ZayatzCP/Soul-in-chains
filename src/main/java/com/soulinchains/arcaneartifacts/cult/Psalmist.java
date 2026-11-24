package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;

/**
 * Psalmist (Abundance): frail, never fights. Every 2 s it mends the summoned within 8 blocks (+4) and gives them Strength
 * I; a summoned creature that dies near a living Psalmist drops its whitelisted materials with +0.25 on the multiplier.
 */
public class Psalmist extends RitualServant {
    public static final double CHOIR_RANGE = 8;
    public static final float LOOT_BONUS = 0.25F;

    public Psalmist(EntityType<? extends Psalmist> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected int defaultLife() {
        return 1800;
    }

    @Override
    protected ParticleOptions trail() {
        return ParticleTypes.NOTE;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void ability(ServerLevel level) {
        if (tickCount % 40 == 0) {
            sing();
        }
    }

    /** Mends and strengthens the summoned around. Returns how many it reached. */
    public int sing() {
        int reached = 0;
        for (RitualServant servant : level().getEntitiesOfClass(RitualServant.class, getBoundingBox().inflate(CHOIR_RANGE), s -> s != this && s.isAlive())) {
            servant.heal(4F);
            servant.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, false, true));
            reached++;
        }
        return reached;
    }
}
