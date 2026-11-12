package com.soulinchains.arcaneartifacts.mushroom;

import com.hollingsworth.arsnouveau.setup.registry.ModPotions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

/**
 * The mushrooms of the Mushroom Caves. Each one is a plantable mushroom and a snack: it restores almost
 * no hunger and gives a short taste of its effect. The colour of the cap hints at the effect. Distilled
 * in the Alchemical Distiller, the same mushroom gives the full-length effect (see the
 * {@code distiller_ingredients} data map).
 */
public enum ArcaneMushroom {
    /** The common mushroom of the caves; bone meal grows it into a huge blue mushroom. */
    BLUE("blue_mushroom", MapColor.COLOR_LIGHT_BLUE, 6, () -> new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0)),
    SOURCECAP("sourcecap", MapColor.COLOR_PURPLE, 3, () -> new MobEffectInstance(ModPotions.MANA_REGEN_EFFECT, 400, 0)),
    BRAWNCAP("brawncap", MapColor.COLOR_RED, 0, () -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0)),
    EMBERCAP("embercap", MapColor.COLOR_ORANGE, 4, () -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0)),
    SWIFTCAP("swiftcap", MapColor.COLOR_YELLOW, 0, () -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0)),
    LEAPCAP("leapcap", MapColor.COLOR_LIGHT_GREEN, 0, () -> new MobEffectInstance(MobEffects.JUMP, 400, 0)),
    MENDCAP("mendcap", MapColor.COLOR_PINK, 0, () -> new MobEffectInstance(MobEffects.REGENERATION, 160, 0)),
    GHOSTCAP("ghostcap", MapColor.SNOW, 2, () -> new MobEffectInstance(MobEffects.INVISIBILITY, 300, 0)),
    TOXCAP("toxcap", MapColor.COLOR_GREEN, 0, () -> new MobEffectInstance(MobEffects.POISON, 160, 0)),
    STONECAP("stonecap", MapColor.STONE, 0, () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0)),
    TIDECAP("tidecap", MapColor.COLOR_CYAN, 0, () -> new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0));

    private final String id;
    private final MapColor mapColor;
    private final int light;
    private final Supplier<MobEffectInstance> effect;

    ArcaneMushroom(String id, MapColor mapColor, int light, Supplier<MobEffectInstance> effect) {
        this.id = id;
        this.mapColor = mapColor;
        this.light = light;
        this.effect = effect;
    }

    public String id() {
        return id;
    }

    public MapColor mapColor() {
        return mapColor;
    }

    public int light() {
        return light;
    }

    /** Only the blue mushroom grows into a huge mushroom. */
    public boolean growsHuge() {
        return this == BLUE;
    }

    /** One point of hunger, eaten quickly, always edible, and a short dose of the effect. */
    public FoodProperties food() {
        return new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).alwaysEdible().fast()
                .effect(effect, 1.0f).build();
    }
}
