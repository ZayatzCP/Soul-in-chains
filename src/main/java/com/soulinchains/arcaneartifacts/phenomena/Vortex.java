package com.soulinchains.arcaneartifacts.phenomena;

import com.hollingsworth.arsnouveau.api.particle.ParticleEmitter;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData;
import com.soulinchains.arcaneartifacts.fx.Fx;
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
 * A swirling vortex ({@link Kind#BLACK_HOLE}): pulls every entity (creatures, items, projectiles) toward its centre,
 * harder the closer they are, and deals magic damage twice a second to creatures caught in the core.
 * The Tornado has its own phenomenon, {@link Tornado}.
 * The caster is never pulled (but is hurt by the core like anyone else), nor are spectators and creative players. Blocks are never broken.
 */
public final class Vortex implements Phenomena.Phenomenon, Phenomena.Located {
    public enum Kind { BLACK_HOLE }

    private final ServerLevel level;
    private final UUID owner;
    private final Kind kind;
    private final Vec3 center;
    private final double radius;
    private final float damage;
    private final double strength;
    private final java.util.List<ParticleEmitter> emitters = new java.util.ArrayList<>();
    private final TimelineEntryData style;
    private final TimelineEntryData impact;
    private int ticksLeft;
    private int age;

    public Vortex(ServerLevel level, UUID owner, Kind kind, Vec3 center, double radius, int durationTicks, float damage, double strength,
                  TimelineEntryData style, TimelineEntryData impact) {
        this.level = level;
        this.owner = owner;
        this.kind = kind;
        this.center = center;
        this.radius = radius;
        this.ticksLeft = durationTicks;
        this.damage = damage;
        this.strength = strength;
        this.style = style;
        this.impact = impact;
        emitters.add(Fx.follow(style, () -> center));
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
            finish();
            return false;
        }
        age++;
        AABB area = new AABB(center, center).inflate(radius);
        @Nullable Entity caster = level.getEntity(owner);
        for (Entity entity : level.getEntities((Entity) null, area, e -> e.isAlive() && !e.getUUID().equals(owner) && !e.isSpectator()
                && !(e instanceof Player p && p.getAbilities().instabuild))) {
            pull(entity, caster);
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
            com.soulinchains.arcaneartifacts.effect.MagicHandler.recoil(caster instanceof LivingEntity l ? l : null, living, damage);
            living.hurt(source, damage);
        }
    }

    /**
     * Drawn with the glyph's Ars particle timeline (restylable in the spell book). The area stays marked
     * with the impact style. The black hole keeps its dark core (ink and reverse portal particles).
     */
    private void particles() {
        if (age % 2 == 0) {
            for (ParticleEmitter emitter : emitters) {
                emitter.tick(level);
            }
        }
        if (age % 10 == 0) {
            Fx.ring(level, impact, center, radius, Math.max(10, (int) (radius * 3)));
        }
        if (kind == Kind.BLACK_HOLE && age % 2 == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, center.x, center.y, center.z, 6, 0.25, 0.25, 0.25, 0.01);
            double angle = age * 0.35;
            for (int i = 0; i < 6; i++) {
                double a = angle + i * Math.PI / 3;
                double r = radius * 0.6;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, center.x + Math.cos(a) * r,
                        center.y + Math.sin(a * 0.5) * 0.6, center.z + Math.sin(a) * r, 1, 0, 0, 0, 0.02);
            }
        }
    }

    /** The collapse: a burst in the impact style, plus the black hole's dark implosion. */
    private void finish() {
        Fx.burst(level, impact, center);
        if (kind == Kind.BLACK_HOLE) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, center.x, center.y, center.z, 40, 0.6, 0.6, 0.6, 0.15);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 60, 0.2, 0.2, 0.2, 0.6);
            level.playSound(null, center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.5f);
        }
    }
}
