package com.soulinchains.arcaneartifacts.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/** Relationship checks shared by targeting, reflection and parasitism. */
public final class Allies {
    private Allies() {
    }

    /** Returns the UUID of whoever ultimately controls {@code entity} (owner of a pet/summon, or itself). */
    public static @Nullable UUID controllerId(@Nullable Entity entity) {
        if (entity == null) return null;
        if (entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return ownable.getOwnerUUID();
        }
        return entity.getUUID();
    }

    /**
     * True if {@code a} and {@code b} are on the same side: the same entity, scoreboard team allies
     * (including mod-provided {@code isAlliedTo} overrides), or one controls the other / both share an owner.
     */
    public static boolean areAllied(@Nullable Entity a, @Nullable Entity b) {
        if (a == null || b == null) return false;
        if (a == b) return true;
        if (a.isAlliedTo(b) || b.isAlliedTo(a)) return true;
        if (com.soulinchains.arcaneartifacts.team.TeamHandler.sworn(a, b)) return true;
        return Objects.equals(controllerId(a), controllerId(b));
    }

    /** Whether {@code shooter} may attack {@code target} at all (allies, creative players, PvP rules). */
    public static boolean isValidHostileTarget(@Nullable Entity shooter, LivingEntity target) {
        if (!target.isAlive() || target.isSpectator() || target.isInvulnerable()) return false;
        if (shooter == null) return true;
        if (areAllied(shooter, target)) return false;
        if (target instanceof Player targetPlayer) {
            if (targetPlayer.isCreative()) return false;
            if (shooter instanceof Player shooterPlayer && !shooterPlayer.canHarmPlayer(targetPlayer)) return false;
            if (shooter.getServer() != null && shooter instanceof Player && !shooter.getServer().isPvpAllowed()) return false;
        }
        return true;
    }

    public static @Nullable Entity owner(Projectile projectile) {
        return projectile.getOwner();
    }
}
