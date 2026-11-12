package com.soulinchains.arcaneartifacts.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/** Biome keys. Kept free of registries so the worldgen mixin can load this class at any time. */
public final class ModBiomes {
    /** Light-blue-to-purple caves coated with mushroom moss, home of the Gribushkas. */
    public static final ResourceKey<Biome> MUSHROOM_CAVES = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("arcane_artifacts", "mushroom_caves"));

    private ModBiomes() {
    }
}
