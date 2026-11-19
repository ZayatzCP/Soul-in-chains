package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * PvP rules for the addon's control spells (config section {@code pvp}): shorter holds on players with a short
 * immunity afterwards, weaker pulls and vortices. Mobs are never affected.
 */
public final class Pvp {
    private static final String IMMUNE_UNTIL = ArcaneArtifacts.MODID + ":hold_immune_until";

    private Pvp() {
    }

    /** How long a Shackle or Entangle of {@code ticks} holds this creature; 0 when a player is still immune. */
    public static int holdTicks(LivingEntity target, int ticks) {
        if (!(target instanceof Player player)) {
            return ticks;
        }
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong(IMMUNE_UNTIL) > now) {
            return 0;
        }
        int held = (int) Math.round(ticks * Config.PVP_HOLD_MULTIPLIER.get());
        if (held > 0) {
            player.getPersistentData().putLong(IMMUNE_UNTIL, now + held + Config.PVP_HOLD_IMMUNITY_TICKS.get());
        }
        return held;
    }

    /** Multiplier of a chain pull on this entity (0 = not pulled). */
    public static double pull(Entity target) {
        if (!(target instanceof Player)) {
            return 1.0;
        }
        return Config.PVP_CHAIN_PULLS_PLAYERS.get() ? Config.PVP_PULL_MULTIPLIER.get() : 0.0;
    }

    /** Multiplier of a vortex (Tornado, Black Hole) on this entity. */
    public static double vortex(Entity target) {
        return target instanceof Player ? Config.PVP_VORTEX_MULTIPLIER.get() : 1.0;
    }
}
