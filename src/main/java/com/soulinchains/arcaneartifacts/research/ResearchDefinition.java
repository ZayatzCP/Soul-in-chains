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
 * @param requiresGlyphs Ars Nouveau glyphs the player must already know before the research is discovered
 * @param unlocks       glyph ids whose Scribes Table recipe becomes available once formulated
 * @param unlocksItems  item ids whose Enchanting Apparatus recipe becomes available once formulated
 */
public record ResearchDefinition(NoteCategory category, Item icon, int sort, List<String> subjects, List<String> properties,
                                 List<ResourceLocation> requires, List<ResourceLocation> requiresGlyphs,
                                 List<ExperimentDefinition> experiments, int notes,
                                 Map<NoteCategory, Integer> notesRequired, List<ResourceLocation> unlocks,
                                 List<ResourceLocation> unlocksItems) {

    public static final Codec<ResearchDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            NoteCategory.CODEC.fieldOf("category").forGetter(ResearchDefinition::category),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("icon").forGetter(ResearchDefinition::icon),
            Codec.INT.optionalFieldOf("sort", 100).forGetter(ResearchDefinition::sort),
            Codec.STRING.listOf().optionalFieldOf("subjects", List.of()).forGetter(ResearchDefinition::subjects),
            Codec.STRING.listOf().optionalFieldOf("properties", List.of()).forGetter(ResearchDefinition::properties),
            ResourceLocation.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(ResearchDefinition::requires),
            ResourceLocation.CODEC.listOf().optionalFieldOf("requires_glyphs", List.of()).forGetter(ResearchDefinition::requiresGlyphs),
            ExperimentDefinition.CODEC.listOf().fieldOf("experiments").forGetter(ResearchDefinition::experiments),
            Codec.INT.optionalFieldOf("notes", 1).forGetter(ResearchDefinition::notes),
            Codec.unboundedMap(NoteCategory.CODEC, Codec.INT).optionalFieldOf("notes_required", Map.of()).forGetter(ResearchDefinition::notesRequired),
            ResourceLocation.CODEC.listOf().optionalFieldOf("unlocks", List.of()).forGetter(ResearchDefinition::unlocks),
            ResourceLocation.CODEC.listOf().optionalFieldOf("unlocks_items", List.of()).forGetter(ResearchDefinition::unlocksItems)
    ).apply(i, ResearchDefinition::new));

    public static final StreamCodec<ByteBuf, ResearchDefinition> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public boolean isSubject(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (String subject : subjects) {
            if (ExperimentCondition.matchesItem(subject, stack.getItem())) return true;
        }
        return false;
    }

    /** Whether the stack is the {@code index}-th subject (0 = the one that starts the research). */
    public boolean isSubject(ItemStack stack, int index) {
        return !stack.isEmpty() && index >= 0 && index < subjects.size() && ExperimentCondition.matchesItem(subjects.get(index), stack.getItem());
    }

    /** Marker stored with the done experiments once the {@code index}-th subject has been observed. */
    public static String observedMarker(int index) {
        return "#observed_" + index;
    }

    /**
     * Experiments come in order and each one after the first needs the next subject observed first:
     * the subject number {@code i} (from 0) is revealed when experiment {@code i - 1} is done, and
     * observing it reveals experiment {@code i}.
     */
    public boolean experimentAvailable(java.util.Set<String> done, int index) {
        if (index == 0) {
            return true;
        }
        if (!done.contains(experiments.get(index - 1).id())) {
            return false;
        }
        return index >= subjects.size() || done.contains(observedMarker(index));
    }

    /** The subject the player must observe next, or -1 if none is waiting. */
    public int pendingSubject(java.util.Set<String> done) {
        for (int i = 1; i < experiments.size() && i < subjects.size(); i++) {
            if (done.contains(experiments.get(i - 1).id()) && !done.contains(observedMarker(i))) {
                return i;
            }
        }
        return -1;
    }

    /** Researches of the Pseudomage live under {@code pseudo/} (e.g. {@code arcane_artifacts:pseudo/firearms}). */
    public static boolean isPseudomage(ResourceLocation id) {
        return id.getPath().startsWith("pseudo/");
    }

    /** The school (Research Table tab) of a research: 0 Arcane Mage, 1 Pseudomage ({@code pseudo/}), 2 Druid ({@code druid/}). */
    public static int school(ResourceLocation id) {
        String path = id.getPath();
        return path.startsWith("pseudo/") ? 1 : path.startsWith("druid/") ? 2 : 0;
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
