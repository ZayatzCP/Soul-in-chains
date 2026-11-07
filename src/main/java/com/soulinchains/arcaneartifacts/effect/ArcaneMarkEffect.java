package com.soulinchains.arcaneartifacts.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/**
 * Arcane Mark status. Besides the regular (synced) effect particles, the server draws a small rotating
 * rune ring above the marked creature, which every nearby player sees.
 */
public class ArcaneMarkEffect extends ArcaneStatusEffect {
    private static final Vector3f COLOR = new Vector3f(0.71f, 0.28f, 0.96f);

    public ArcaneMarkEffect() {
        super(MobEffectCategory.HARMFUL, 0xB548F5, true);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 5 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            double y = entity.getY() + entity.getBbHeight() + 0.45;
            double radius = Math.max(0.35, entity.getBbWidth() * 0.6);
            double phase = (level.getGameTime() % 40) / 40.0 * Math.PI * 2;
            for (int i = 0; i < 4; i++) {
                double a = phase + i * Math.PI / 2;
                level.sendParticles(new DustParticleOptions(COLOR, 0.8f),
                        entity.getX() + Math.cos(a) * radius, y, entity.getZ() + Math.sin(a) * radius, 1, 0, 0, 0, 0);
            }
        }
        return true;
    }
}
