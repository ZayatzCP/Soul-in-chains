package com.soulinchains.arcaneartifacts.rite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** The chalk rites known to the server ({@code data/<namespace>/arcane_rites/}). */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class Rites extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile Map<ResourceLocation, RiteDefinition> rites = Map.of();

    public Rites() {
        super(GSON, "arcane_rites");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, RiteDefinition> loaded = new LinkedHashMap<>();
        files.forEach((id, json) -> {
            try {
                loaded.put(id, RiteDefinition.parse(id, json.getAsJsonObject()));
            } catch (RuntimeException e) {
                ArcaneArtifacts.LOGGER.error("Bad chalk rite {}: {}", id, e.getMessage());
            }
        });
        rites = Map.copyOf(loaded);
        ArcaneArtifacts.LOGGER.info("Loaded {} chalk rites", loaded.size());
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new Rites());
    }

    public static Collection<RiteDefinition> all() {
        return rites.values();
    }

    @Nullable
    public static RiteDefinition get(ResourceLocation id) {
        return rites.get(id);
    }

    /** The rite the site's four places call, or null. */
    @Nullable
    public static RiteDefinition calledBy(RiteSite site) {
        for (RiteDefinition rite : rites.values()) {
            if (rite.calledBy(site)) {
                return rite;
            }
        }
        return null;
    }
}
