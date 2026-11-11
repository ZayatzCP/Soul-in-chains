package com.soulinchains.arcaneartifacts.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Fear: the creature runs away from whoever frightened it and does nothing of its own will (no
 * target, no attacks, its own path overridden). Players are pushed away. Each level of the effect
 * makes the flight faster.
 */
public class FearEffect extends MobEffect {
    public static final String SOURCE = "arcane_artifacts.fear_source";
    public static final String SOURCE_POS = "arcane_artifacts.fear_pos";

    public FearEffect() {
        super(MobEffectCategory.HARMFUL, 0x3B2A4A);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    /** Where the creature runs from: the frightening entity if it is still around, else where it stood. */
    private static Vec3 threat(LivingEntity entity) {
        var data = entity.getPersistentData();
        if (data.hasUUID(SOURCE) && entity.level() instanceof ServerLevel level) {
            Entity source = level.getEntity(data.getUUID(SOURCE));
            if (source != null && source.isAlive()) {
                return source.position();
            }
        }
        if (data.contains(SOURCE_POS + "_x")) {
            return new Vec3(data.getDouble(SOURCE_POS + "_x"), data.getDouble(SOURCE_POS + "_y"), data.getDouble(SOURCE_POS + "_z"));
        }
        return entity.position().add(entity.getLookAngle());
    }

    public static void setSource(LivingEntity target, Entity source) {
        var data = target.getPersistentData();
        data.putUUID(SOURCE, source.getUUID());
        data.putDouble(SOURCE_POS + "_x", source.getX());
        data.putDouble(SOURCE_POS + "_y", source.getY());
        data.putDouble(SOURCE_POS + "_z", source.getZ());
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return true;
        }
        Vec3 from = threat(entity);
        double speed = 1.25 + 0.35 * amplifier;
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.setAggressive(false);
            if (mob instanceof PathfinderMob pathfinder && (mob.tickCount % 10 == 0 || mob.getNavigation().isDone())) {
                Vec3 away = DefaultRandomPos.getPosAway(pathfinder, 16, 7, from);
                if (away != null) {
                    mob.getNavigation().moveTo(away.x, away.y, away.z, speed);
                }
            } else if (!(mob instanceof PathfinderMob)) {
                push(entity, from, 0.06 * (1 + amplifier));
            }
        } else if (entity instanceof Player) {
            push(entity, from, 0.05 * (1 + amplifier));
        }
        if (entity.tickCount % 10 == 0) {
            level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getEyeY() + 0.3, entity.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
        }
        return true;
    }

    private static void push(LivingEntity entity, Vec3 from, double strength) {
        Vec3 away = entity.position().subtract(from).multiply(1, 0, 1);
        if (away.lengthSqr() < 1.0E-4) {
            return;
        }
        away = away.normalize().scale(strength);
        entity.setDeltaMovement(entity.getDeltaMovement().add(away.x, 0, away.z));
        entity.hurtMarked = true;
    }

    /** Clears the stored source once the fear is over. */
    public static void forget(LivingEntity entity) {
        var data = entity.getPersistentData();
        data.remove(SOURCE);
        data.remove(SOURCE_POS + "_x");
        data.remove(SOURCE_POS + "_y");
        data.remove(SOURCE_POS + "_z");
    }

    public static UUID sourceOf(LivingEntity entity) {
        return entity.getPersistentData().hasUUID(SOURCE) ? entity.getPersistentData().getUUID(SOURCE) : null;
    }
}
