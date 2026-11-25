package com.soulinchains.arcaneartifacts.compat;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * Create Aeronautics ships (Sable sub-levels): a chain can hook a point of a ship and pull the ship, or pull its thrower
 * to the ship; a shackle anchors a ship (a strong braking force). Nothing here touches Sable unless it is installed.
 */
public final class ShipCompat {
    private static Boolean loaded;

    private ShipCompat() {
    }

    /** A point on a ship, followed as the ship moves. */
    public interface ShipPoint {
        boolean valid();

        /** Where the point is in the world right now. */
        Vec3 world();

        /** Pulls the ship at this point towards {@code worldDirection} (normalised) with acceleration {@code accel}. */
        void pull(Vec3 worldDirection, double accel);

        /** Brakes the whole ship: a force against its velocity, {@code strength} times its momentum per second. */
        void brake(double strength);

        /** Holds the ship still: this point is drawn back to {@code anchor} and the ship stops turning (overrides its engines). */
        void hold(Vec3 anchor);
    }

    public static boolean loaded() {
        if (loaded == null) {
            loaded = ModList.get() != null && ModList.get().isLoaded("sable");
        }
        return loaded;
    }

    /** Whether the entity stands on (or rides) a Create Aeronautics ship. */
    public static boolean onShip(net.minecraft.world.entity.Entity entity) {
        if (!loaded() || entity.level().isClientSide) {
            return false;
        }
        try {
            return com.soulinchains.arcaneartifacts.compat.sable.SableShips.onShip(entity);
        } catch (LinkageError | RuntimeException e) {
            return false;
        }
    }

    /** The ship whose block is at this point (in ship-plot or world coordinates), or null. */
    @Nullable
    public static ShipPoint at(Level level, Vec3 point) {
        if (!loaded() || level.isClientSide) {
            return null;
        }
        try {
            ShipPoint found = com.soulinchains.arcaneartifacts.compat.sable.SableShips.at(level, point);
            return found == null ? null : new Guarded(found);
        } catch (LinkageError | RuntimeException e) {
            return null;
        }
    }

    /**
     * Every call into Sable after the hook is guarded too: a newer Sable that changed its API, or a ship that went away
     * mid-tick, makes the point invalid instead of crashing the server tick.
     */
    private static final class Guarded implements ShipPoint {
        private final ShipPoint inner;
        private boolean broken;
        private Vec3 last = Vec3.ZERO;

        Guarded(ShipPoint inner) {
            this.inner = inner;
        }

        @Override
        public boolean valid() {
            if (broken) {
                return false;
            }
            try {
                return inner.valid();
            } catch (LinkageError | RuntimeException e) {
                broken = true;
                return false;
            }
        }

        @Override
        public Vec3 world() {
            if (!broken) {
                try {
                    last = inner.world();
                } catch (LinkageError | RuntimeException e) {
                    broken = true;
                }
            }
            return last;
        }

        @Override
        public void pull(Vec3 worldDirection, double accel) {
            if (!broken) {
                try {
                    inner.pull(worldDirection, accel);
                } catch (LinkageError | RuntimeException e) {
                    broken = true;
                }
            }
        }

        @Override
        public void brake(double strength) {
            if (!broken) {
                try {
                    inner.brake(strength);
                } catch (LinkageError | RuntimeException e) {
                    broken = true;
                }
            }
        }

        @Override
        public void hold(Vec3 anchor) {
            if (!broken) {
                try {
                    inner.hold(anchor);
                } catch (LinkageError | RuntimeException e) {
                    broken = true;
                }
            }
        }
    }
}
