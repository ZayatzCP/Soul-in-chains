package com.soulinchains.arcaneartifacts.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * A research loaded from {@code data/<namespace>/arcane_research/<path>.json}. Example:
 * <pre>{@code
 * {
 *   "category": "material",
 *   "icon": "minecraft:ice",
 *   "sort": 10,
 *   "subjects": ["#minecraft:ice", "minecraft:snow_block"],
 *   "properties": ["crystalline", "heat_sensitive"],
 *   "requires": [],
 *   "experiments": [
 *     {"id": "melt_ice", "any_of": [
 *        {"trigger": "arcane_artifacts:spell_effect", "glyph": "ars_nouveau:glyph_ignite", "block": "#minecraft:ice"},
 *        {"trigger": "arcane_artifacts:spell_effect", "glyph": "ars_nouveau:glyph_flare", "block": "#minecraft:ice"}]}
 *   ],
 *   "notes": 1,
 *   "notes_required": {"material": 1},
 *   "unlocks": ["arcane_artifacts:glyph_deep_freeze"]
 * }
 * }</pre>
 * Texts are lang keys derived from the id: {@code arcane_artifacts.research.<namespace>.<path>.title},
 * {@code .description}, {@code .principle}, {@code .experiment.<experiment id>}, and
 * {@code arcane_artifacts.research_property.<property>}.
 *
 * @param subjects      items (or #item tags) that can be observed at the Research Table to start the research
 * @param notes         notes of {@code category} awarded when every experiment is done
 * @param notesRequired notes the player must already hold to formulate the principle (not consumed)
 * @param unlocks       glyph ids whose Scribes Table recipe becomes available once formulated
 */
public record ResearchDefinition(NoteCategory category, Item icon, int sort, List<String> subjects, List<String> properties,
                                 List<ResourceLocation> requires, List<ExperimentDefinition> experiments, int notes,
                                 Map<NoteCategory, Integer> notesRequired, List<ResourceLocation> unlocks) {

    public static final Codec<ResearchDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            NoteCategory.CODEC.fieldOf("category").forGetter(ResearchDefinition::category),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("icon").forGetter(ResearchDefinition::icon),
            Codec.INT.optionalFieldOf("sort", 100).forGetter(ResearchDefinition::sort),
            Codec.STRING.listOf().optionalFieldOf("subjects", List.of()).forGetter(ResearchDefinition::subjects),
            Codec.STRING.listOf().optionalFieldOf("properties", List.of()).forGetter(ResearchDefinition::properties),
            ResourceLocation.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(ResearchDefinition::requires),
            ExperimentDefinition.CODEC.listOf().fieldOf("experiments").forGetter(ResearchDefinition::experiments),
            Codec.INT.optionalFieldOf("notes", 1).forGetter(ResearchDefinition::notes),
            Codec.unboundedMap(NoteCategory.CODEC, Codec.INT).optionalFieldOf("notes_required", Map.of()).forGetter(ResearchDefinition::notesRequired),
            ResourceLocation.CODEC.listOf().optionalFieldOf("unlocks", List.of()).forGetter(ResearchDefinition::unlocks)
    ).apply(i, ResearchDefinition::new));

    public static final StreamCodec<ByteBuf, ResearchDefinition> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public boolean isSubject(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (String subject : subjects) {
            if (ExperimentCondition.matchesItem(subject, stack.getItem())) return true;
        }
        return false;
    }

    public static String langPrefix(ResourceLocation id) {
        return "arcane_artifacts.research." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    public record ExperimentDefinition(String id, List<ExperimentCondition> anyOf) {
        public static final Codec<ExperimentDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(ExperimentDefinition::id),
                ExperimentCondition.CODEC.listOf().fieldOf("any_of").forGetter(ExperimentDefinition::anyOf)
        ).apply(i, ExperimentDefinition::new));

        public boolean matches(ResearchEvent event) {
            for (ExperimentCondition condition : anyOf) {
                if (condition.matches(event)) return true;
            }
            return false;
        }
    }
}
