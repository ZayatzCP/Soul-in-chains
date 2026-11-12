package com.soulinchains.arcaneartifacts.spell;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.soulinchains.arcaneartifacts.phenomena.Phenomena;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The aura a Sensitive Aura projectile leaves where it lands. On landing it splashes the spell's effects
 * on every creature in the radius (like a splash potion), then stays on the spot and keeps washing over
 * whoever is inside (like a lingering potion), each creature at most every 2 seconds. Radius 2, +2 per
 * AOE, never more than 6; Extend Time makes it last longer.
 */
public final class AuraZone implements Phenomena.Phenomenon, Phenomena.Located {
    private static final int PERIOD = 20;
    private static final int PER_ENTITY_COOLDOWN = 40;

    private final ServerLevel level;
    private final @Nullable UUID owner;
    private final Vec3 center;
    private final double radius;
    private final SpellResolver resolver;
    private final Map<UUID, Integer> lastHit = new HashMap<>();
    private int ticks;
    private final int duration;

    public AuraZone(ServerLevel level, @Nullable UUID owner, Vec3 center, double radius, int duration, SpellResolver resolver) {
        this.level = level;
        this.owner = owner;
        this.center = center;
        this.radius = radius;
        this.duration = duration;
        this.resolver = resolver;
    }

    public static double radiusFor(int aoe) {
        return Math.min(6.0, 2.0 + 2.0 * Math.max(0, aoe));
    }

    public static int durationFor(int extendTime) {
        return 100 + 60 * Math.max(0, extendTime);
    }

    @Override
    public ServerLevel level() {
        return level;
    }

    @Override
    public UUID owner() {
        return owner == null ? new UUID(0, 0) : owner;
    }

    @Override
    public Vec3 center() {
        return center;
    }

    @Override
    public boolean tick() {
        if (ticks % 5 == 0) {
            for (int i = 0; i < 12; i++) {
                double a = (ticks * 0.15) + i * Math.PI * 2 / 12;
                level.sendParticles(ParticleTypes.WITCH, center.x + Math.cos(a) * radius, center.y + 0.15, center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
            }
            level.sendParticles(ParticleTypes.EFFECT, center.x, center.y + 0.3, center.z, 4, radius * 0.5, 0.2, radius * 0.5, 0);
        }
        if (ticks % PERIOD == 0) {
            pulse();
        }
        return ++ticks < duration;
    }

    private void pulse() {
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, 2.0, radius),
                e -> e.isAlive() && !e.isSpectator() && e.position().distanceToSqr(center) <= radius * radius + 4
                        && (owner == null || !e.getUUID().equals(owner)))) {
            Integer last = lastHit.get(living.getUUID());
            if (last != null && ticks - last < PER_ENTITY_COOLDOWN) {
                continue;
            }
            lastHit.put(living.getUUID(), ticks);
            AuraHandler.resolveCopy(level, resolver, new EntityHitResult(living));
        }
    }
}
