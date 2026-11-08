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
    private final java.util.List<ParticleEmitter> emitters = new java.util.ArrayList<>();
    private final TimelineEntryData style;
    private final TimelineEntryData impact;
    /** Tornado: progress (0..1 of the height) of each strand of the funnel, and where each strand is now. */
    private final double[] strandProgress = new double[STRANDS];
    private final Vec3[] strandPosition = new Vec3[STRANDS];
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
        if (kind == Kind.TORNADO) {
            // One tall funnel: STRANDS trails spiral from the ground to the top, again and again.
            for (int i = 0; i < STRANDS; i++) {
                final int strand = i;
                strandProgress[i] = i / (double) STRANDS;
                strandPosition[i] = funnelPoint(i, strandProgress[i]);
                emitters.add(Fx.follow(style, () -> strandPosition[strand]));
            }
        } else {
            emitters.add(Fx.follow(style, () -> center));
        }
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
        AABB area = kind == Kind.TORNADO ? new AABB(center, center).inflate(radius, 1, radius).expandTowards(0, radius * 2.6, 0)
                : new AABB(center, center).inflate(radius);
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
        double lift = entity.getY() < center.y + radius * 2.2 ? 0.13 * strength + 0.06 : -0.02;
        Vec3 motion = tangent.scale(0.45 * strength).add(inward).add(0, lift, 0);
        entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5).add(motion));
        entity.hurtMarked = true;
    }

    /**
     * Drawn with the glyph's Ars particle timeline (restylable in the spell book). The area stays marked
     * with the impact style. The black hole keeps its dark core (ink and reverse portal particles).
     */
    private static final int STRANDS = 3;
    private static final int STEPS_PER_TICK = 4;
    private static final double SWEEP_STEPS = 44.0;
    /** The funnel walls lean out at about 75 degrees from the ground: radius grows by cot(75) per block. */
    private static final double WALL_SLOPE = 1.0 / Math.tan(Math.toRadians(75));

    private double funnelHeight() {
        return Math.max(4.0, radius * 2.2);
    }

    /** A point of strand {@code strand} at height fraction {@code t}: two full turns from bottom to top. */
    private Vec3 funnelPoint(int strand, double t) {
        double h = t * funnelHeight();
        double r = 0.5 + h * WALL_SLOPE;
        double angle = strand * (Math.PI * 2 / STRANDS) + t * Math.PI * 4 + age * 0.25;
        return center.add(Math.cos(angle) * r, h, Math.sin(angle) * r);
    }

    private void tornadoParticles() {
        for (int i = 0; i < STRANDS; i++) {
            for (int step = 0; step < STEPS_PER_TICK; step++) {
                strandProgress[i] += 1.0 / SWEEP_STEPS;
                if (strandProgress[i] >= 1.0) {
                    // Back to the ground: a fresh trail, so no line is drawn from the top down.
                    strandProgress[i] -= 1.0;
                    strandPosition[i] = funnelPoint(i, strandProgress[i]);
                    final int strand = i;
                    emitters.set(i, Fx.follow(style, () -> strandPosition[strand]));
                }
                strandPosition[i] = funnelPoint(i, strandProgress[i]);
                emitters.get(i).tick(level);
            }
        }
    }

    private void particles() {
        if (kind == Kind.TORNADO) {
            tornadoParticles();
        } else if (age % 2 == 0) {
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
