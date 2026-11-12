package com.soulinchains.arcaneartifacts.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jetbrains.annotations.Nullable;

/**
 * What an item adds to a potion in the Alchemical Distiller. Data map
 * {@code arcane_artifacts:distiller_ingredients} (file {@code data/<ns>/data_maps/item/distiller_ingredients.json}),
 * so packs can add or change ingredients.
 */
public record DistillerIngredient(Holder<MobEffect> effect, int duration, int amplifier) {
    public static final Codec<DistillerIngredient> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(DistillerIngredient::effect),
            Codec.INT.optionalFieldOf("duration", 3600).forGetter(DistillerIngredient::duration),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(DistillerIngredient::amplifier)
    ).apply(i, DistillerIngredient::new));

    public static final DataMapType<Item, DistillerIngredient> DATA_MAP = DataMapType
            .builder(ArcaneArtifacts.id("distiller_ingredients"), Registries.ITEM, CODEC)
            .synced(CODEC, false)
            .build();

    @SuppressWarnings("deprecation")
    public static @Nullable DistillerIngredient of(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getItem().builtInRegistryHolder().getData(DATA_MAP);
    }

    public MobEffectInstance instance(double durationScale, int extraAmplifier) {
        int ticks = effect.value().isInstantenous() ? 1 : Math.max(20, (int) Math.round(duration * durationScale));
        return new MobEffectInstance(effect, ticks, Math.max(0, amplifier + extraAmplifier));
    }
}
