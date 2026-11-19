package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Bond: a chain between two creatures. While it lasts, damage to either one is split in half between both. The chain
 * snaps when one dies or they are more than {@link #SNAP_DISTANCE} blocks apart. Not saved.
 */
public class BondLink extends Entity {
    public static final double SNAP_DISTANCE = 16.0;
    public static final String TAG = ArcaneArtifacts.MODID + ":bond";
    private static final EntityDataAccessor<Integer> A = SynchedEntityData.defineId(BondLink.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> B = SynchedEntityData.defineId(BondLink.class, EntityDataSerializers.INT);
    private int ticksLeft;

    public BondLink(EntityType<? extends BondLink> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void link(LivingEntity a, LivingEntity b, int ticks) {
        entityData.set(A, a.getId());
        entityData.set(B, b.getId());
        ticksLeft = ticks;
        setPos(a.position());
        long until = a.level().getGameTime() + ticks;
        mark(a, b.getUUID(), until);
        mark(b, a.getUUID(), until);
    }

    private static void mark(LivingEntity who, UUID partner, long until) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("partner", partner);
        tag.putLong("until", until);
        who.getPersistentData().put(TAG, tag);
    }

    /** The creature bonded to this one right now, or null. */
    @Nullable
    public static LivingEntity partner(LivingEntity who) {
        if (!(who.level() instanceof ServerLevel level) || !who.getPersistentData().contains(TAG)) {
            return null;
        }
        CompoundTag tag = who.getPersistentData().getCompound(TAG);
        if (tag.getLong("until") < level.getGameTime() || !tag.hasUUID("partner")) {
            who.getPersistentData().remove(TAG);
            return null;
        }
        Entity partner = level.getEntity(tag.getUUID("partner"));
        if (!(partner instanceof LivingEntity living) || !living.isAlive() || living.distanceTo(who) > SNAP_DISTANCE) {
            who.getPersistentData().remove(TAG);
            return null;
        }
        return living;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(A, -1);
        builder.define(B, -1);
    }

    @Nullable
    public Entity first() {
        return level().getEntity(entityData.get(A));
    }

    @Nullable
    public Entity second() {
        return level().getEntity(entityData.get(B));
    }

    @Override
    public void tick() {
        super.tick();
        Entity a = first();
        if (a != null) {
            setPos(a.position());
        }
        if (level().isClientSide) {
            return;
        }
        Entity b = second();
        if (ticksLeft-- <= 0 || !(a instanceof LivingEntity la) || !la.isAlive() || !(b instanceof LivingEntity lb) || !lb.isAlive()
                || la.distanceTo(lb) > SNAP_DISTANCE) {
            if (a instanceof LivingEntity la2) {
                la2.getPersistentData().remove(TAG);
            }
            if (b instanceof LivingEntity lb2) {
                lb2.getPersistentData().remove(TAG);
            }
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
