package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModAttachments;
import com.soulinchains.arcaneartifacts.research.ResearchService;
import com.soulinchains.arcaneartifacts.research.ResearchState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Ranks of the Shackled class. Rank 1 (Novice) comes with the "Shackling" research; rank 2 (Overseer) with the
 * Awakening ritual; rank 3 (Chainlord) later, for defeating the Jailer. Creative players with research bypass count as
 * rank 3. The rank limits the servants and opens the stronger glyphs.
 */
public final class Necro {
    public static final ResourceLocation SHACKLING = ArcaneArtifacts.id("necro/shackling");
    public static final int NOVICE = 1;
    public static final int OVERSEER = 2;
    public static final int CHAINLORD = 3;

    private Necro() {
    }

    public static int rank(Player player) {
        if (ResearchService.bypasses(player)) {
            return CHAINLORD;
        }
        int earned = player.getData(ModAttachments.NECRO_RANK);
        if (earned >= NOVICE) {
            return earned;
        }
        return ResearchService.state(player, SHACKLING).atLeast(ResearchState.UNLOCKED) ? NOVICE : 0;
    }

    public static void setRank(Player player, int rank) {
        player.setData(ModAttachments.NECRO_RANK, Math.max(0, Math.min(CHAINLORD, rank)));
    }

    public static int servantLimit(Player player) {
        int rank = rank(player);
        if (rank <= 0) {
            return 0;
        }
        List<? extends Integer> limits = Config.SERVANT_LIMITS.get();
        int base = limits.isEmpty() ? 0 : limits.get(Math.min(rank, limits.size()) - 1);
        int bonus = (ShackledSet.wearsCrown(player) ? 1 : 0) + (Corruption.stage(player) >= 2 ? 1 : 0);
        return Math.min(8, base + bonus);
    }

    /** Checks the rank; tells the player what is missing when it is too low. */
    public static boolean requireRank(Player player, int rank) {
        if (rank(player) >= rank) {
            return true;
        }
        player.displayClientMessage(Component.translatable("arcane_artifacts.necro.rank_needed",
                Component.translatable("arcane_artifacts.necro.rank." + rank)), true);
        return false;
    }
}
