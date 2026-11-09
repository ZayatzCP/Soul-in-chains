package com.soulinchains.arcaneartifacts.item;

import com.hollingsworth.arsnouveau.setup.registry.ModPotions;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

/** Food properties of the gilded treats. */
public final class ArcaneFoods {
    /** Like an enchanted golden apple, but from a potato that should have stayed poisonous. */
    public static final FoodProperties ENCHANTED_POISONOUS_POTATO = new FoodProperties.Builder()
            .nutrition(4).saturationModifier(1.2f).alwaysEdible()
            .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 2400, 2), 1.0f)
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 600, 1), 1.0f)
            .effect(() -> new MobEffectInstance(ModPotions.MANA_REGEN_EFFECT, 2400, 1), 1.0f)
            .effect(() -> new MobEffectInstance(ModPotions.SPELL_DAMAGE_EFFECT, 1200, 0), 1.0f)
            .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 120, 0), 1.0f)
            .build();
    /** Breathe under water and swim like a dolphin; the spikes are still there. */
    public static final FoodProperties GILDED_PUFFERFISH = new FoodProperties.Builder()
            .nutrition(3).saturationModifier(0.8f).alwaysEdible()
            .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 3600, 0), 1.0f)
            .effect(() -> new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 1200, 0), 1.0f)
            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0), 1.0f)
            .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 80, 0), 1.0f)
            .build();
    /** Pity: every spell is weakened like two Dampen. */
    public static final FoodProperties GILDED_FERMENTED_SPIDER_EYE = new FoodProperties.Builder()
            .nutrition(2).saturationModifier(0.4f).alwaysEdible()
            .effect(() -> new MobEffectInstance(ModEffects.PITY, 600, 0), 1.0f)
            .build();

    private ArcaneFoods() {
    }
}
