package com.soulinchains.arcaneartifacts.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * One way of completing an experiment. Every field except {@code trigger} is an optional filter;
 * filters that are present must all match. Ids may be tags ("#minecraft:ice"); "*" means "any, but present".
 *
 * @param trigger which game event this listens to, see {@link ResearchTriggers}
 * @param glyph   spell part id (e.g. "ars_nouveau:glyph_ignite")
 * @param entity  entity type id or #tag of the affected entity
 * @param block   block id or #tag of the affected block
 * @param item    item id or #tag
 * @param effect  mob effect id
 */
public record ExperimentCondition(ResourceLocation trigger, Optional<ResourceLocation> glyph, Optional<String> entity,
                                  Optional<String> block, Optional<String> item, Optional<ResourceLocation> effect) {
    public static final Codec<ExperimentCondition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("trigger").forGetter(ExperimentCondition::trigger),
            ResourceLocation.CODEC.optionalFieldOf("glyph").forGetter(ExperimentCondition::glyph),
            Codec.STRING.optionalFieldOf("entity").forGetter(ExperimentCondition::entity),
            Codec.STRING.optionalFieldOf("block").forGetter(ExperimentCondition::block),
            Codec.STRING.optionalFieldOf("item").forGetter(ExperimentCondition::item),
            ResourceLocation.CODEC.optionalFieldOf("effect").forGetter(ExperimentCondition::effect)
    ).apply(i, ExperimentCondition::new));

    public boolean matches(ResearchEvent event) {
        if (!trigger.equals(event.trigger())) return false;
        if (glyph.isPresent() && (event.glyphs().isEmpty() || !event.glyphs().contains(glyph.get()))) return false;
        if (entity.isPresent() && (event.entity() == null || !matchesEntity(entity.get(), event.entity()))) return false;
        if (block.isPresent() && (event.block() == null || !matchesBlock(block.get(), event.block()))) return false;
        if (item.isPresent() && (event.item() == null || !matchesItem(item.get(), event.item()))) return false;
        if (effect.isPresent() && (event.effect() == null || !event.effect().is(effect.get()))) return false;
        return true;
    }

    static boolean matchesItem(String filter, Item item) {
        if (filter.equals("*")) return true;
        if (filter.startsWith("#")) {
            return item.builtInRegistryHolder().is(TagKey.create(Registries.ITEM, ResourceLocation.parse(filter.substring(1))));
        }
        return BuiltInRegistries.ITEM.getKey(item).equals(ResourceLocation.parse(filter));
    }

    static boolean matchesBlock(String filter, Block block) {
        if (filter.equals("*")) return true;
        if (filter.startsWith("#")) {
            return block.builtInRegistryHolder().is(TagKey.create(Registries.BLOCK, ResourceLocation.parse(filter.substring(1))));
        }
        return BuiltInRegistries.BLOCK.getKey(block).equals(ResourceLocation.parse(filter));
    }

    static boolean matchesEntity(String filter, EntityType<?> type) {
        if (filter.equals("*")) return true;
        if (filter.startsWith("#")) {
            return type.is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(filter.substring(1))));
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).equals(ResourceLocation.parse(filter));
    }
}
