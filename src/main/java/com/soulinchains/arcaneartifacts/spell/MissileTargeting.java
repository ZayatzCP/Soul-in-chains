package com.soulinchains.arcaneartifacts.spell;

import com.soulinchains.arcaneartifacts.registry.ModEffects;
import com.soulinchains.arcaneartifacts.util.Allies;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

/**
 * Target selection for Arcane Missile and split projectiles.
 * <p>
 * Priority: marked creatures, then creatures hostile to the shooter (monsters or mobs currently
 * targeting the shooter), then any other valid creature; inside a priority class the nearest wins.
 * Allies of the shooter (team, own pets/summons) and the shooter itself are never selected, and a
 * target must be in line of sight so missiles do not try to home through walls.
 */
public final class MissileTargeting {
    private MissileTargeting() {
    }

    public static @Nullable LivingEntity findTarget(Level level, Entity missile, @Nullable Entity shooter, double radius, Set<UUID> excluded) {
        double r2 = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, missile.getBoundingBox().inflate(radius),
                        e -> e != shooter
                                && !excluded.contains(e.getUUID())
                                && !(e instanceof ArmorStand)
                                && e.distanceToSqr(missile) <= r2
                                && Allies.isValidHostileTarget(shooter, e)
                                && hasLineOfSight(level, missile, e))
                .stream()
                .min(Comparator.<LivingEntity>comparingInt(e -> priority(e, shooter))
                        .thenComparingDouble(e -> e.distanceToSqr(missile)))
                .orElse(null);
    }

    private static int priority(LivingEntity e, @Nullable Entity shooter) {
        if (e.hasEffect(ModEffects.ARCANE_MARK)) return 0;
        if (e instanceof Enemy) return 1;
        if (shooter != null && e instanceof Mob mob && mob.getTarget() == shooter) return 1;
        return 2;
    }

    public static boolean hasLineOfSight(Level level, Entity from, Entity to) {
        Vec3 start = from.position().add(0, from.getBbHeight() / 2.0, 0);
        Vec3 end = to.getEyePosition();
        return level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from)).getType() == HitResult.Type.MISS;
    }
}
