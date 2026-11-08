package com.soulinchains.arcaneartifacts.phenomena;

import com.hollingsworth.arsnouveau.api.particle.ParticleEmitter;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData;
import com.mojang.math.Transformation;
import com.soulinchains.arcaneartifacts.fx.Fx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Anvil Drop: a (display) anvil falls from the sky onto a point and smashes everything under it, like a
 * mace slam. Nothing real is placed: no block, no item, no griefing. The anvil is drawn with a block
 * display entity that is removed on impact.
 */
public final class AnvilDrop implements Phenomena.Phenomenon, Phenomena.Located {
    public static final String TAG = "arcane_artifacts.anvil_drop";
    private static final double START_HEIGHT = 14.0;

    private final ServerLevel level;
    private final UUID owner;
    private final Vec3 target;
    private final float damage;
    private final double radius;
    private final float size;
    private final TimelineEntryData impact;
    private final ParticleEmitter trail;
    private final @Nullable Display.BlockDisplay display;
    private double y;
    private double speed;
    private int ticks;

    public AnvilDrop(ServerLevel level, UUID owner, Vec3 target, float damage, double radius, float size,
                     TimelineEntryData trailStyle, TimelineEntryData impactStyle) {
        this.level = level;
        this.owner = owner;
        this.target = target;
        this.damage = damage;
        this.radius = radius;
        this.size = size;
        this.impact = impactStyle;
        this.y = target.y + START_HEIGHT;
        this.trail = Fx.follow(trailStyle, () -> new Vec3(target.x, this.y + size / 2, target.z));
        this.display = spawnDisplay();
    }

    private @Nullable Display.BlockDisplay spawnDisplay() {
        Display.BlockDisplay entity = EntityType.BLOCK_DISPLAY.create(level);
        if (entity == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        tag.put("block_state", NbtUtils.writeBlockState(Blocks.ANVIL.defaultBlockState()));
        Transformation transformation = new Transformation(new Vector3f(-size / 2, 0, -size / 2), null, new Vector3f(size, size, size), null);
        tag.put("transformation", Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, transformation).getOrThrow());
        tag.putInt("teleport_duration", 1);
        entity.load(tag);
        entity.addTag(TAG);
        entity.setPos(target.x, y, target.z);
        level.addFreshEntity(entity);
        return entity;
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
        return target;
    }

    @Override
    public boolean tick() {
        ticks++;
        speed = Math.min(2.5, speed + 0.09);
        y = Math.max(target.y, y - speed);
        if (display != null) {
            display.setPos(target.x, y, target.z);
        }
        trail.tick(level);
        if (ticks % 5 == 0) {
            Fx.ring(level, impact, target, Math.max(1.0, radius), Math.max(10, (int) (radius * 3)));
        }
        if (y <= target.y || ticks > 80) {
            land();
            return false;
        }
        return true;
    }

    @Override
    public void cancelled() {
        if (display != null) {
            display.discard();
        }
    }

    private void land() {
        if (display != null) {
            display.discard();
        }
        Entity caster = level.getEntity(owner);
        DamageSource source = caster != null ? level.damageSources().anvil(caster) : level.damageSources().anvil(null);
        double reach = Math.max(1.2, radius);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(target, target).inflate(reach, 2.0, reach),
                e -> e.isAlive() && !e.getUUID().equals(owner))) {
            double distance = Math.hypot(living.getX() - target.x, living.getZ() - target.z);
            if (distance > reach) {
                continue;
            }
            // Directly under the anvil: full damage; caught by the shockwave: half, like a mace slam.
            float dealt = distance <= 1.2 ? damage : damage * 0.5f;
            living.hurt(source, dealt);
            Vec3 push = living.position().subtract(target).multiply(1, 0, 1);
            push = push.lengthSqr() > 1.0E-4 ? push.normalize().scale(0.6) : Vec3.ZERO;
            living.push(push.x, 0.45, push.z);
            living.hurtMarked = true;
        }
        Fx.area(level, impact, target, reach);
        level.playSound(null, target.x, target.y, target.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5f, size > 1.5f ? 0.5f : 0.8f);
        level.playSound(null, target.x, target.y, target.z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.0f, 0.9f);
    }
}
