package com.soulinchains.arcaneartifacts.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Elder Mycelinoid: the oldest of a village, green with age, a pipe in the corner of its mouth and a hat
 * with a brim wide enough for the whole colony. It stays by the hall, knows the best mushroom prices
 * (every trade is rolled twice) and now and then breathes healing spores on the Mycelinoids around it.
 */
public class ElderMycelinoid extends Gribushka {
    public static final int BLESSING_INTERVAL = 200;
    public static final double BLESSING_RANGE = 8.0;

    public ElderMycelinoid(EntityType<? extends ElderMycelinoid> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected int tradeRolls() {
        return 2;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % BLESSING_INTERVAL == 0 && level() instanceof ServerLevel server) {
            bless(server);
        }
    }

    /** Healing spores for every Mycelinoid around (and the Elder itself). */
    public void bless(ServerLevel server) {
        for (Gribushka friend : server.getEntitiesOfClass(Gribushka.class, getBoundingBox().inflate(BLESSING_RANGE))) {
            friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, true, true));
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, friend.getX(), friend.getY() + 1.0, friend.getZ(), 4, 0.3, 0.3, 0.3, 0.0);
        }
        server.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1.5, getZ(), 30, BLESSING_RANGE / 3, 0.6, BLESSING_RANGE / 3, 0.01);
    }

    @Override
    public float getVoicePitch() {
        return 1.1F + getRandom().nextFloat() * 0.1F;
    }
}
