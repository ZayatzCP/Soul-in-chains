package com.soulinchains.arcaneartifacts.research;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Loads research definitions from {@code data/<namespace>/arcane_research/*.json} on the server.
 * Datapacks can add, replace or remove researches; clients receive the result through
 * {@link ResearchNetwork}. Broken files are logged and skipped, never crash the server.
 */
public final class ResearchManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile Map<ResourceLocation, ResearchDefinition> definitions = Map.of();
    private static volatile Set<ResourceLocation> gatedGlyphs = Set.of();

    public ResearchManager() {
        super(GSON, "arcane_research");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, ResearchDefinition> loaded = new LinkedHashMap<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e ->
                ResearchDefinition.CODEC.parse(JsonOps.INSTANCE, e.getValue())
                        .resultOrPartial(err -> ArcaneArtifacts.LOGGER.error("Invalid research {}: {}", e.getKey(), err))
                        .ifPresent(def -> loaded.put(e.getKey(), def)));
        replace(loaded);
        ArcaneArtifacts.LOGGER.info("Loaded {} arcane researches", loaded.size());
    }

    /** Installs a set of definitions (server reload, or the copy received by a client). */
    public static void replace(Map<ResourceLocation, ResearchDefinition> loaded) {
        Map<ResourceLocation, ResearchDefinition> sorted = new LinkedHashMap<>();
        loaded.entrySet().stream()
                .sorted((a, b) -> a.getValue().sort() != b.getValue().sort()
                        ? Integer.compare(a.getValue().sort(), b.getValue().sort())
                        : a.getKey().compareTo(b.getKey()))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        Set<ResourceLocation> gated = new HashSet<>();
        sorted.values().forEach(d -> gated.addAll(d.unlocks()));
        definitions = Collections.unmodifiableMap(sorted);
        gatedGlyphs = Collections.unmodifiableSet(gated);
    }

    /** All researches in display order (sort, then id). */
    public static Map<ResourceLocation, ResearchDefinition> all() {
        return definitions;
    }

    public static ResearchDefinition get(ResourceLocation id) {
        return definitions.get(id);
    }

    /** Glyphs that some research unlocks, i.e. glyphs that are locked until researched. */
    public static Set<ResourceLocation> gatedGlyphs() {
        return gatedGlyphs;
    }
}
