package com.soulinchains.arcaneartifacts.research;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/** Something that happened in the world, produced on the server only and matched against experiments. */
public record ResearchEvent(ResourceLocation trigger, Set<ResourceLocation> glyphs, @Nullable EntityType<?> entity,
                            @Nullable Block block, @Nullable Item item, @Nullable Holder<MobEffect> effect,
                            @Nullable Holder<DamageType> damage) {

    public static ResearchEvent of(ResourceLocation trigger) {
        return new ResearchEvent(trigger, Set.of(), null, null, null, null, null);
    }

    public ResearchEvent withDamage(@Nullable Holder<DamageType> value) {
        return new ResearchEvent(trigger, glyphs, entity, block, item, effect, value);
    }

    public ResearchEvent withGlyphs(Set<ResourceLocation> value) {
        return new ResearchEvent(trigger, value, entity, block, item, effect, damage);
    }

    public ResearchEvent withEntity(@Nullable EntityType<?> value) {
        return new ResearchEvent(trigger, glyphs, value, block, item, effect, damage);
    }

    public ResearchEvent withBlock(@Nullable Block value) {
        return new ResearchEvent(trigger, glyphs, entity, value, item, effect, damage);
    }

    public ResearchEvent withItem(@Nullable Item value) {
        return new ResearchEvent(trigger, glyphs, entity, block, value, effect, damage);
    }

    public ResearchEvent withEffect(@Nullable Holder<MobEffect> value) {
        return new ResearchEvent(trigger, glyphs, entity, block, item, value, damage);
    }
}
