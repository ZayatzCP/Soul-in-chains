package com.soulinchains.arcaneartifacts.mixin;

import com.mojang.datafixers.util.Pair;
import com.soulinchains.arcaneartifacts.world.ModBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Adds the Mushroom Caves to the vanilla overworld biome layout, next to the Lush and Dripstone Caves.
 * Lush Caves take the most humid underground; the Mushroom Caves take the driest. Only newly generated
 * chunks are affected.
 */
@Mixin(OverworldBiomeBuilder.class)
public abstract class OverworldBiomeBuilderMixin {
    @Inject(method = "addUndergroundBiomes", at = @At("TAIL"))
    private void arcaneArtifacts$addMushroomCaves(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer, CallbackInfo ci) {
        // The built-in (code) registries know only vanilla biomes; ours comes from the mod's data pack.
        // Building those registries (dev-time validation, data generation) must not reference it.
        if (arcaneArtifacts$buildingVanillaRegistries()) {
            return;
        }
        Climate.Parameter full = Climate.Parameter.span(-1.0F, 1.0F);
        consumer.accept(Pair.of(Climate.parameters(
                full,                                   // temperature
                Climate.Parameter.span(-1.0F, -0.6F),   // humidity: the dry end
                full,                                   // continentalness
                full,                                   // erosion
                Climate.Parameter.span(0.2F, 0.9F),     // depth: underground, like the vanilla cave biomes
                full,                                   // weirdness
                0.0F), ModBiomes.MUSHROOM_CAVES));
    }

    private static boolean arcaneArtifacts$buildingVanillaRegistries() {
        return StackWalker.getInstance().walk(frames -> frames.anyMatch(frame ->
                frame.getClassName().startsWith("net.minecraft.data.registries.VanillaRegistries")
                        || frame.getClassName().startsWith("net.minecraft.data.")));
    }
}
