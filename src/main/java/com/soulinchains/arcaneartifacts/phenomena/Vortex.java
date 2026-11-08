package com.soulinchains.arcaneartifacts.phenomena;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A swirling vortex.
 * <ul>
 *     <li>{@link Kind#BLACK_HOLE}: pulls every entity (creatures, items, projectiles) toward its centre,
 *     harder the closer they are, and deals magic damage twice a second to creatures caught in the core.</li>
 *     <li>{@link Kind#TORNADO}: spins creatures and items around its axis and lifts them up; what it
 *     carries falls down when it ends.</li>
 * </ul>
 * The caster is never affected, nor are spectators and creative players. Blocks are never broken.
 */
public final class Vortex implements Phenomena.Phenomenon, Phenomena.Located {
    public enum Kind { BLACK_HOLE, TORNADO }

    private final ServerLevel level;
    private final UUID owner;
    private final Kind kind;
    private final Vec3 center;
    private final double radius;
    private final float damage;
    private final double strength;
    private int ticksLeft;
    private int age;

    public Vortex(ServerLevel level, UUID owner, Kind kind, Vec3 center, double radius, int durationTicks, float damage, double strength) {
        this.level = level;
        this.owner = owner;
        this.kind = kind;
        this.center = center;
        this.radius = radius;
        this.ticksLeft = durationTicks;
        this.damage = damage;
        this.strength = strength;
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
        return center;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public boolean tick() {
        if (ticksLeft-- <= 0) {
            return false;
        }
        age++;
        AABB area = new AABB(center, center).inflate(radius, kind == Kind.TORNADO ? radius * 1.5 : radius, radius);
        @Nullable Entity caster = level.getEntity(owner);
        for (Entity entity : level.getEntities((Entity) null, area, e -> e.isAlive() && !e.getUUID().equals(owner) && !e.isSpectator()
                && !(e instanceof Player p && p.getAbilities().instabuild))) {
            if (kind == Kind.BLACK_HOLE) {
                pull(entity, caster);
            } else {
                spin(entity);
            }
        }
        particles();
        return true;
    }

    private void pull(Entity entity, @Nullable Entity caster) {
        Vec3 toCenter = center.subtract(entity.position().add(0, entity.getBbHeight() / 2, 0));
        double distance = toCenter.length();
        if (distance > radius) {
            return;
        }
        double force = strength * (0.35 + 0.65 * (1.0 - distance / radius));
        Vec3 motion = distance > 0.2 ? toCenter.normalize().scale(force) : Vec3.ZERO;
        entity.setDeltaMovement(entity.getDeltaMovement().scale(0.6).add(motion));
        entity.hurtMarked = true;
        entity.fallDistance = 0;
        if (distance < 1.6 && age % 10 == 0 && entity instanceof LivingEntity living && damage > 0) {
            DamageSource source = caster != null ? level.damageSources().indirectMagic(caster, caster) : level.damageSources().magic();
            living.hurt(source, damage);
        }
    }

    private void spin(Entity entity) {
        Vec3 offset = entity.position().subtract(center);
        Vec3 flat = new Vec3(offset.x, 0, offset.z);
        double distance = flat.length();
        if (distance > radius) {
            return;
        }
        Vec3 tangent = distance > 0.1 ? new Vec3(-flat.z, 0, flat.x).normalize() : Vec3.ZERO;
        Vec3 inward = distance > 0.1 ? flat.normalize().scale(-0.08) : Vec3.ZERO;
        double lift = entity.getY() < center.y + radius * 1.4 ? 0.12 * strength + 0.05 : -0.02;
        Vec3 motion = tangent.scale(0.45 * strength).add(inward).add(0, lift, 0);
        entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5).add(motion));
        entity.hurtMarked = true;
    }

    private void particles() {
        if (age % 2 != 0) {
            return;
        }
        double angle = age * 0.35;
        if (kind == Kind.BLACK_HOLE) {
            level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 6, 0.25, 0.25, 0.25, 0.01);
            for (int i = 0; i < 6; i++) {
                double a = angle + i * Math.PI / 3;
                double r = radius * 0.6;
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x + Math.cos(a) * r, center.y + Math.sin(a * 0.5) * 0.6,
                        center.z + Math.sin(a) * r, 1, 0, 0, 0, 0.02);
            }
        } else {
            for (int i = 0; i < 10; i++) {
                double h = (i / 10.0) * radius * 2;
                double a = angle + i * 0.7;
                double r = 0.5 + h * 0.35;
                level.sendParticles(ParticleTypes.CLOUD, center.x + Math.cos(a) * r, center.y + h, center.z + Math.sin(a) * r, 1, 0, 0, 0, 0.0);
            }
        }
    }
}
