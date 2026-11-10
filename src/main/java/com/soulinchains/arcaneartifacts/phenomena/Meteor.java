package com.soulinchains.arcaneartifacts.phenomena;

import com.hollingsworth.arsnouveau.api.particle.ParticleEmitter;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData;
import com.soulinchains.arcaneartifacts.fx.Fx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.UUID;

/**
 * One meteor of a Meteor Shower: falls from the sky after a short delay, trailing fire, and bursts on
 * the first block it meets. The burst damages and sets on fire the creatures around it (never the
 * caster) and knocks them away. It never breaks or ignites blocks.
 */
public final class Meteor implements Phenomena.Phenomenon, Phenomena.Located {
    private static final double SPEED = 1.4;

    private final ServerLevel level;
    private final UUID owner;
    private Vec3 position;
    private final Vec3 velocity;
    private final float damage;
    private final double burstRadius;
    private final TimelineEntryData impact;
    private final ParticleEmitter trail;
    private int delay;
    private int life = 80;

    public Meteor(ServerLevel level, UUID owner, Vec3 target, int delay, float damage, double burstRadius, double height,
                  TimelineEntryData trailStyle, TimelineEntryData impactStyle) {
        this.level = level;
        this.owner = owner;
        this.delay = delay;
        this.damage = damage;
        this.burstRadius = burstRadius;
        Vec3 drift = new Vec3(level.random.nextGaussian() * 0.15, -1, level.random.nextGaussian() * 0.15).normalize();
        this.velocity = drift.scale(SPEED);
        this.position = target.subtract(drift.scale(height));
        this.impact = impactStyle;
        this.trail = Fx.follow(trailStyle, () -> this.position);
    }

    @Override
    public ServerLevel level() {
        return level;
    }

    @Override
    public UUID owner() {
        return owner;
    }

    @Override
    public Vec3 center() {
        return position;
    }

    @Override
    public boolean tick() {
        if (delay > 0) {
            delay--;
            return true;
        }
        if (life-- <= 0) {
            burst(position);
            return false;
        }
        Vec3 next = position.add(velocity);
        BlockHitResult hit = level.clip(new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
        trail.tick(level);
        if (hit.getType() != HitResult.Type.MISS || next.y < level.getMinBuildHeight()) {
            burst(hit.getType() != HitResult.Type.MISS ? hit.getLocation() : next);
            return false;
        }
        position = next;
        return true;
    }

    private void burst(Vec3 at) {
        Fx.area(level, impact, at, burstRadius);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.9f + level.random.nextFloat() * 0.2f);
        Entity caster = level.getEntity(owner);
        DamageSource source = caster != null ? level.damageSources().indirectMagic(caster, caster) : level.damageSources().magic();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(burstRadius),
                LivingEntity::isAlive)) {
            double distance = living.position().distanceTo(at);
            if (distance > burstRadius) {
                continue;
            }
            float scaled = (float) (damage * (1.0 - 0.5 * distance / burstRadius));
            if (living != caster) {
                com.soulinchains.arcaneartifacts.effect.MagicHandler.recoil(caster instanceof LivingEntity l ? l : null, living, scaled);
            }
            if (living.hurt(source, scaled)) {
                living.igniteForSeconds(3);
                Vec3 push = living.position().subtract(at).normalize().scale(0.6);
                living.push(push.x, 0.35, push.z);
                living.hurtMarked = true;
            }
        }
    }
}
