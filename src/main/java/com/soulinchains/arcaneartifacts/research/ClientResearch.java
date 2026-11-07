package com.soulinchains.arcaneartifacts.research;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * The client's mirror of the server's research data. Plain data, safe to load on any side;
 * only written by {@link ResearchNetwork} handlers on the client.
 */
public final class ClientResearch {
    private static PlayerResearch progress = new PlayerResearch();
    private static int revision;

    private ClientResearch() {
    }

    static void acceptDefinitions(Map<ResourceLocation, ResearchDefinition> definitions) {
        ResearchManager.replace(definitions);
        revision++;
    }

    static void acceptProgress(PlayerResearch received) {
        progress = received;
        revision++;
    }

    public static PlayerResearch progress() {
        return progress;
    }

    /** Increases on every update, so screens and the book can tell when to refresh. */
    public static int revision() {
        return revision;
    }
}
