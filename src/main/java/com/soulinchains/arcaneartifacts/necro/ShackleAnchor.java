package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.compat.ContraptionCompat;
import com.soulinchains.arcaneartifacts.compat.ShipCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * The ground end of a Shackle: four chains run from it up to what it holds.
 * <ul>
 *     <li>A creature: kept on the spot (knockback cannot carry it away) while it is Shackled.</li>
 *     <li>A Create contraption: kept stalled (its bearing, piston, minecart or train stops).</li>
 *     <li>A Create Aeronautics ship: held still where the chains caught it, whatever its engines do.</li>
 * </ul>
 * Disappears when the hold ends. Not saved.
 */
public class ShackleAnchor extends Entity {
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(ShackleAnchor.class, EntityDataSerializers.INT);
    /** Where the chains end (a ship point, followed as the ship moves). */
    private static final EntityDataAccessor<Vector3f> TOP = SynchedEntityData.defineId(ShackleAnchor.class, EntityDataSerializers.VECTOR3);
    private int ticksLeft;
    @Nullable
    private ShipCompat.ShipPoint ship;
    /** Where the hooked point of the ship was when the chains caught it: the ship is held there. */
    private Vec3 shipAnchor = Vec3.ZERO;

    public ShackleAnchor(EntityType<? extends ShackleAnchor> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Holds a creature or a contraption. */
    public void hold(Entity target, int ticks) {
        entityData.set(TARGET, target.getId());
        setPos(target.position());
        ticksLeft = ticks;
    }

    /** Anchors a ship: the anchor sits on the ground below the hooked point. */
    public void holdShip(ShipCompat.ShipPoint point, int ticks) {
        ship = point;
        ticksLeft = ticks;
        Vec3 top = point.world();
        shipAnchor = top;
        var hit = level().clip(new ClipContext(top.subtract(0, 1.5, 0), top.subtract(0, 24, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        setPos(hit.getType() == HitResult.Type.MISS ? top.subtract(0, 6, 0) : hit.getLocation());
        entityData.set(TOP, top.toVector3f());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TARGET, -1);
        builder.define(TOP, new Vector3f());
    }

    @Nullable
    public Entity target() {
        int id = entityData.get(TARGET);
        return id < 0 ? null : level().getEntity(id);
    }

    /** The upper end of the chains, or null when nothing is held. */
    @Nullable
    public Vec3 top(float partial) {
        Entity target = target();
        if (target != null) {
            return target.getPosition(partial).add(0, target.getBbHeight() * 0.5, 0);
        }
        Vector3f top = entityData.get(TOP);
        return top.lengthSquared() == 0 ? null : new Vec3(top.x, top.y, top.z);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (ticksLeft-- <= 0) {
            discard();
            return;
        }
        if (ship != null) {
            if (!ship.valid()) {
                discard();
                return;
            }
            ship.hold(shipAnchor);
            entityData.set(TOP, ship.world().toVector3f());
            return;
        }
        Entity target = target();
        if (target == null || !target.isAlive()) {
            discard();
            return;
        }
        if (target instanceof LivingEntity living) {
            if (!living.hasEffect(com.soulinchains.arcaneartifacts.registry.ModEffects.SHACKLED)) {
                discard();
                return;
            }
            Vec3 off = living.position().subtract(position());
            if (off.horizontalDistanceSqr() > 0.25) {
                living.setDeltaMovement(new Vec3(-off.x * 0.5, Math.min(0, living.getDeltaMovement().y), -off.z * 0.5));
                living.hurtMarked = true;
            }
        } else if (!ContraptionCompat.isFrozen(target)) {
            discard();
        } else {
            setPos(target.position());
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
