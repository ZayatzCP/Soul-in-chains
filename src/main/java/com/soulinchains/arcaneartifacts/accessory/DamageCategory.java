package com.soulinchains.arcaneartifacts.accessory;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import com.soulinchains.arcaneartifacts.registry.ModTags;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Damage categories the Wreath of Manipulation can adapt to. Each category has its own hit counter
 * and its own adaptation timer; fire/lava and drowning/suffocation share a granted effect but are
 * still tracked independently, as the design requires.
 */
public enum DamageCategory implements StringRepresentable {
    PHYSICAL("physical", 0, () -> MobEffects.DAMAGE_RESISTANCE),
    FIRE("fire", 1, () -> MobEffects.FIRE_RESISTANCE),
    LAVA("lava", 1, () -> MobEffects.FIRE_RESISTANCE),
    SUFFOCATION("suffocation", 2, () -> MobEffects.WATER_BREATHING),
    DROWNING("drowning", 2, () -> MobEffects.WATER_BREATHING),
    MAGIC("magic", 3, () -> ModEffects.ARCANE_ADAPTATION);

    public static final Codec<DamageCategory> CODEC = StringRepresentable.fromEnum(DamageCategory::values);

    private final String name;
    /** Which tinted flower group of the wreath model represents this category (0..3). */
    public final int flowerGroup;
    private final Supplier<Holder<MobEffect>> granted;

    DamageCategory(String name, int flowerGroup, Supplier<Holder<MobEffect>> granted) {
        this.name = name;
        this.flowerGroup = flowerGroup;
        this.granted = granted;
    }

    public Holder<MobEffect> grantedEffect() {
        return granted.get();
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String translationKey() {
        return "arcane_artifacts.damage_category." + name;
    }

    /**
     * Classifies a damage source. Returns {@code null} for damage the wreath never adapts to
     * (falling, void, starvation, generic kill commands, ...).
     */
    public static @Nullable DamageCategory classify(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FALL)
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.is(ModTags.WREATH_IGNORED)) {
            return null;
        }
        if (source.is(DamageTypes.LAVA)) {
            return LAVA;
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return FIRE;
        }
        if (source.is(DamageTypeTags.IS_DROWNING)) {
            return DROWNING;
        }
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING)) {
            return SUFFOCATION;
        }
        if (source.is(ModTags.MAGIC_DAMAGE)) {
            return MAGIC;
        }
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            // Melee, projectiles, explosions, thorns-less physical sources: everything armor applies to.
            return PHYSICAL;
        }
        return null;
    }
}
