package com.soulinchains.arcaneartifacts.cult;

import com.soulinchains.arcaneartifacts.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** Reading and writing a player's Cultist state. */
public final class Cult {
    private Cult() {
    }

    public static CultData get(Player player) {
        return player.getData(ModAttachments.CULT);
    }

    public static void set(Player player, CultData data) {
        player.setData(ModAttachments.CULT, data);
    }

    public static CultRole role(Player player) {
        return get(player).role();
    }

    /** Whether these two are each other's partners. */
    public static boolean partners(Player a, Player b) {
        return a != b && get(a).partner().map(b.getUUID()::equals).orElse(false)
                && get(b).partner().map(a.getUUID()::equals).orElse(false);
    }

    /** Swears the pair: the Confessor reads, the Warden holds the circle. */
    public static void pair(Player confessor, Player warden) {
        set(confessor, get(confessor).pairedWith(CultRole.CONFESSOR, warden.getUUID()));
        set(warden, get(warden).pairedWith(CultRole.WARDEN, confessor.getUUID()));
        confessor.displayClientMessage(Component.translatable("arcane_artifacts.cult.paired_confessor", warden.getDisplayName()), false);
        warden.displayClientMessage(Component.translatable("arcane_artifacts.cult.paired_warden", confessor.getDisplayName()), false);
    }

    /** Breaks this player's pair; the partner's side is cleared when found (or at their next login). */
    public static void unpair(Player player, Player partnerIfKnown) {
        UUID partner = get(player).partner().orElse(null);
        set(player, get(player).unpaired());
        player.displayClientMessage(Component.translatable("arcane_artifacts.cult.unpaired"), false);
        Player other = partnerIfKnown != null ? partnerIfKnown : partner == null ? null : player.level().getPlayerByUUID(partner);
        if (other != null && get(other).partner().map(player.getUUID()::equals).orElse(false)) {
            set(other, get(other).unpaired());
            other.displayClientMessage(Component.translatable("arcane_artifacts.cult.unpaired"), false);
        }
    }

    /**
     * Clears a stale pair: this player still names {@code partner}, but the partner broke the pair (while this one was
     * away). Returns true when something was cleared.
     */
    public static boolean reconcile(Player self, Player partner) {
        if (get(self).partner().map(partner.getUUID()::equals).orElse(false)
                && !get(partner).partner().map(self.getUUID()::equals).orElse(false)) {
            set(self, get(self).unpaired());
            self.displayClientMessage(Component.translatable("arcane_artifacts.cult.unpaired"), false);
            return true;
        }
        return false;
    }

    public static final int ARCHDEACON_BEST = 95;

    /** Voices of Dogma needs the Archdeacon's fall: a rank III won before 1.37 without it goes back to II. */
    public static boolean migrate(Player player) {
        CultData data = get(player);
        if (data.rank() >= 3 && !data.archdeacon()) {
            set(player, data.withRank(2));
            player.displayClientMessage(Component.translatable("arcane_artifacts.cult.rank_needs_archdeacon"), false);
            return true;
        }
        return false;
    }

    /** The Archdeacon fell with this Cultist in the fight: the mark is kept; a Servant of the Circle with a perfect rite rises. */
    public static boolean reward(Player player) {
        CultData data = get(player).withArchdeacon();
        boolean risen = data.rank() == 2 && data.best() >= ARCHDEACON_BEST;
        if (risen) {
            data = data.withRank(3);
            player.displayClientMessage(Component.translatable("arcane_artifacts.cult.rank_up", Component.translatable("arcane_artifacts.cult.rank.3")), false);
        } else {
            player.displayClientMessage(Component.translatable("arcane_artifacts.cult.archdeacon_fell"), false);
        }
        set(player, data);
        return risen;
    }
}
