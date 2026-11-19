package com.soulinchains.arcaneartifacts.necro;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The ground end of a Shackle: four chains run from it to the creature while it is held. It keeps the creature on the
 * spot (knockback cannot carry it away) and disappears when the hold ends. Not saved.
 */
public class ShackleAnchor extends Entity {
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(ShackleAnchor.class, EntityDataSerializers.INT);
    private int ticksLeft;

    public ShackleAnchor(EntityType<? extends ShackleAnchor> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void hold(LivingEntity target, int ticks) {
        entityData.set(TARGET, target.getId());
        setPos(target.position());
        ticksLeft = ticks;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TARGET, -1);
    }

    @Nullable
    public Entity target() {
        int id = entityData.get(TARGET);
        return id < 0 ? null : level().getEntity(id);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        Entity target = target();
        if (!(target instanceof LivingEntity living) || !living.isAlive() || ticksLeft-- <= 0
                || !living.hasEffect(com.soulinchains.arcaneartifacts.registry.ModEffects.SHACKLED)) {
            discard();
            return;
        }
        Vec3 off = living.position().subtract(position());
        if (off.horizontalDistanceSqr() > 0.25) {
            living.setDeltaMovement(new Vec3(-off.x * 0.5, Math.min(0, living.getDeltaMovement().y), -off.z * 0.5));
            living.hurtMarked = true;
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
