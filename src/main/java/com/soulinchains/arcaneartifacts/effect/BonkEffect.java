package com.soulinchains.arcaneartifacts.effect;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;

/**
 * BONK! The creature is stunned: it cannot walk, jump, fly or even turn its head. Not curable, and a
 * running BONK! can never be extended (see {@link Bonk#apply}).
 */
public class BonkEffect extends MobEffect {
    public static final String YAW = "arcane_artifacts.bonk_yaw";
    public static final String PITCH = "arcane_artifacts.bonk_pitch";

    public BonkEffect() {
        super(MobEffectCategory.HARMFUL, 0xC8C8D0);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ArcaneArtifacts.id("bonk_speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH, ArcaneArtifacts.id("bonk_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.FLYING_SPEED, ArcaneArtifacts.id("bonk_flying"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(0, Math.min(0, motion.y), 0);
        var data = entity.getPersistentData();
        if (data.contains(YAW)) {
            float yaw = data.getFloat(YAW);
            float pitch = data.getFloat(PITCH);
            entity.setYRot(yaw);
            entity.setXRot(pitch);
            entity.setYHeadRot(yaw);
            entity.yBodyRot = yaw;
        }
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0);
        }
        if (entity.level() instanceof ServerLevel level && entity.tickCount % 8 == 0) {
            double a = entity.tickCount * 0.4;
            level.sendParticles(ParticleTypes.CRIT, entity.getX() + Math.cos(a) * 0.4, entity.getEyeY() + 0.4,
                    entity.getZ() + Math.sin(a) * 0.4, 2, 0.05, 0.05, 0.05, 0.0);
        }
        return true;
    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
        // Not curable: you have to wait until the stars stop spinning.
    }
}
