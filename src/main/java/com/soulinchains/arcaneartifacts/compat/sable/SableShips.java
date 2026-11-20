package com.soulinchains.arcaneartifacts.compat.sable;

import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.compat.ShipCompat;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/** The Sable side of {@link ShipCompat}. Loaded only when Sable is installed. */
public final class SableShips {
    private SableShips() {
    }

    @Nullable
    public static ShipCompat.ShipPoint at(Level level, Vec3 point) {
        // A block hit on a ship is usually reported in the ship's own plot coordinates.
        SubLevel direct = Sable.HELPER.getContaining(level, point);
        if (direct instanceof ServerSubLevel ship && !ship.isRemoved()) {
            return new Point(ship, new Vector3d(point.x, point.y, point.z));
        }
        // Otherwise it is a world position: find the ship around it and look for its block there.
        BoundingBox3d box = new BoundingBox3d(point.x - 0.5, point.y - 0.5, point.z - 0.5, point.x + 0.5, point.y + 0.5, point.z + 0.5);
        for (var access : Sable.HELPER.getAllIntersecting(level, box)) {
            if (access instanceof ServerSubLevel ship && !ship.isRemoved()) {
                Vector3d local = ship.logicalPose().transformPositionInverse(new Vector3d(point.x, point.y, point.z));
                if (!level.getBlockState(BlockPos.containing(local.x, local.y, local.z)).isAir()) {
                    return new Point(ship, local);
                }
            }
        }
        return null;
    }

    /** Looked up in Sable's (vanilla) registry, so the Veil registry wrapper is never touched. */
    @Nullable
    private static dev.ryanhcode.sable.api.physics.force.ForceGroup forceGroup(String path) {
        return ForceGroups.REGISTRY.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("sable", path));
    }

    public static boolean onShip(net.minecraft.world.entity.Entity entity) {
        var box = entity.getBoundingBox().inflate(0.5, 1.0, 0.5);
        BoundingBox3d bounds = new BoundingBox3d(box.minX, box.minY - 1.0, box.minZ, box.maxX, box.maxY, box.maxZ);
        for (var access : Sable.HELPER.getAllIntersecting(entity.level(), bounds)) {
            if (access instanceof ServerSubLevel ship && !ship.isRemoved()) {
                return true;
            }
        }
        return Sable.HELPER.getContaining(entity) != null;
    }

    private record Point(ServerSubLevel ship, Vector3d local) implements ShipCompat.ShipPoint {
        @Override
        public boolean valid() {
            return !ship.isRemoved();
        }

        @Override
        public Vec3 world() {
            Vector3d world = ship.logicalPose().transformPosition(new Vector3d(local));
            return new Vec3(world.x, world.y, world.z);
        }

        @Override
        public void pull(Vec3 worldDirection, double accel) {
            double mass = Math.max(1.0, ship.getMassTracker().getMass());
            double force = Math.min(mass * accel, Config.SHIP_MAX_FORCE.get());
            Vector3d worldForce = new Vector3d(worldDirection.x, worldDirection.y, worldDirection.z).mul(force);
            Vector3d localForce = ship.logicalPose().transformNormalInverse(worldForce);
            var group = forceGroup("propulsion");
            if (group != null) {
                ship.getOrCreateQueuedForceGroup(group).applyAndRecordPointForce(new Vector3d(local), localForce);
            }
        }

        @Override
        public void brake(double strength) {
            RigidBodyHandle handle = RigidBodyHandle.of(ship);
            if (handle == null) {
                return;
            }
            Vector3d velocity = handle.getLinearVelocity(new Vector3d());
            double mass = Math.max(1.0, ship.getMassTracker().getMass());
            Vector3d worldForce = velocity.mul(-mass * strength);
            double cap = Config.SHIP_MAX_FORCE.get() * 3.0;
            if (worldForce.length() > cap) {
                worldForce.normalize(cap);
            }
            Vector3d localForce = ship.logicalPose().transformNormalInverse(worldForce);
            Vector3dc centre = ship.getMassTracker().getCenterOfMass();
            Vector3d at = centre != null ? new Vector3d(centre) : new Vector3d(local);
            var group = forceGroup("drag");
            if (group != null) {
                ship.getOrCreateQueuedForceGroup(group).applyAndRecordPointForce(at, localForce);
            }
        }
    }
}
