package com.soulinchains.arcaneartifacts.world;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> REGISTER = DeferredRegister.create(Registries.FEATURE, ArcaneArtifacts.MODID);

    public static final DeferredHolder<Feature<?>, MushroomMossCoatFeature> MUSHROOM_MOSS_COAT =
            REGISTER.register("mushroom_moss_coat", () -> new MushroomMossCoatFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, HugeBlueMushroomFeature> HUGE_BLUE_MUSHROOM =
            REGISTER.register("huge_blue_mushroom", () -> new HugeBlueMushroomFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, GribushkaColonyFeature> GRIBUSHKA_COLONY =
            REGISTER.register("gribushka_colony", () -> new GribushkaColonyFeature(NoneFeatureConfiguration.CODEC));

    /** The configured feature grown from a blue mushroom with bone meal. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> HUGE_BLUE_MUSHROOM_KEY =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ArcaneArtifacts.id("huge_blue_mushroom"));

    private ModFeatures() {
    }
}
