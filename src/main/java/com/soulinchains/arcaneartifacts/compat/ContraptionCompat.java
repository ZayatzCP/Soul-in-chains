package com.soulinchains.arcaneartifacts.compat;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Create's moving structures (contraptions on bearings, pistons, pulleys, minecarts, trains), without a compile
 * dependency on Create. A shackled contraption is kept "stalled" - the same state Create uses when a drill or harvester
 * gets stuck - so its bearing, piston, minecart or train stops until the shackle ends. A contraption riding a minecart
 * can be pulled by a chain (the chain pulls the minecart).
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class ContraptionCompat {
    private static final String STALLED_UNTIL = ArcaneArtifacts.MODID + ":stalled_until";
    private static boolean resolved;
    @Nullable
    private static Class<?> contraptionEntity;
    @Nullable
    private static Class<?> carriageEntity;
    @Nullable
    private static EntityDataAccessor<Boolean> stalledData;
    @Nullable
    private static Field contraptionField;
    @Nullable
    private static Field contraptionStalled;

    private ContraptionCompat() {
    }

    @SuppressWarnings("unchecked")
    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!ModList.get().isLoaded("create")) {
            return;
        }
        try {
            contraptionEntity = Class.forName("com.simibubi.create.content.contraptions.AbstractContraptionEntity");
            carriageEntity = Class.forName("com.simibubi.create.content.trains.entity.CarriageContraptionEntity");
            Field stalled = contraptionEntity.getDeclaredField("STALLED");
            stalled.setAccessible(true);
            stalledData = (EntityDataAccessor<Boolean>) stalled.get(null);
            contraptionField = contraptionEntity.getDeclaredField("contraption");
            contraptionField.setAccessible(true);
            contraptionStalled = Class.forName("com.simibubi.create.content.contraptions.Contraption").getDeclaredField("stalled");
            contraptionStalled.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            ArcaneArtifacts.LOGGER.warn("Create is installed but its contraptions could not be reached; chains will ignore them", e);
            contraptionEntity = null;
        }
    }

    /** A Create moving structure (any kind). */
    public static boolean isContraption(Entity entity) {
        resolve();
        return contraptionEntity != null && contraptionEntity.isInstance(entity);
    }

    /** A train carriage: far too heavy to pull, but it can be shackled. */
    public static boolean isTrain(Entity entity) {
        resolve();
        return carriageEntity != null && carriageEntity.isInstance(entity);
    }

    /** What a chain moves when it hooks this contraption: the minecart it rides, or null when it cannot be pulled. */
    @Nullable
    public static Entity pullable(Entity contraption) {
        if (!isContraption(contraption) || isTrain(contraption)) {
            return null;
        }
        Entity vehicle = contraption.getVehicle();
        return vehicle != null && !isContraption(vehicle) ? vehicle : null;
    }

    /** Keeps the contraption stalled for this many ticks. */
    public static void freeze(Entity contraption, int ticks) {
        if (!isContraption(contraption)) {
            return;
        }
        long until = contraption.level().getGameTime() + ticks;
        contraption.getPersistentData().putLong(STALLED_UNTIL, Math.max(until, contraption.getPersistentData().getLong(STALLED_UNTIL)));
        stall(contraption);
    }

    public static boolean isFrozen(Entity contraption) {
        return contraption.getPersistentData().getLong(STALLED_UNTIL) > contraption.level().getGameTime();
    }

    private static void stall(Entity contraption) {
        try {
            if (stalledData != null) {
                contraption.getEntityData().set(stalledData, true);
            }
            Object structure = contraptionField == null ? null : contraptionField.get(contraption);
            if (structure != null && contraptionStalled != null) {
                contraptionStalled.setBoolean(structure, true);
            }
            Entity vehicle = contraption.getVehicle();
            if (vehicle != null && !isContraption(vehicle)) {
                vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0, 1, 0));
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // A future Create version may move these fields: the shackle then simply does nothing.
        }
    }

    /** Create clears the stall every tick from its actors; a frozen contraption is stalled again right after. */
    @SubscribeEvent
    public static void afterTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide && entity.getPersistentData().contains(STALLED_UNTIL)) {
            if (isFrozen(entity)) {
                stall(entity);
            } else {
                entity.getPersistentData().remove(STALLED_UNTIL);
            }
        }
    }
}
