package com.soulinchains.arcaneartifacts.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One player's research progress, stored as a serialized player attachment (kept on death, saved
 * with the world) and mirrored to that player's client. Only researches that were started are
 * stored; UNKNOWN / DISCOVERED are derived from prerequisites by {@link ResearchService}.
 */
public final class PlayerResearch {
    public record Entry(ResearchState state, Set<String> done) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResearchState.CODEC.fieldOf("state").forGetter(Entry::state),
                Codec.STRING.listOf().xmap(l -> (Set<String>) new HashSet<>(l), s -> List.copyOf(s)).optionalFieldOf("done", Set.of()).forGetter(Entry::done)
        ).apply(i, Entry::new));
    }

    public static final Codec<PlayerResearch> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Entry.CODEC).optionalFieldOf("entries", Map.of()).forGetter(p -> p.entries),
            Codec.unboundedMap(NoteCategory.CODEC, Codec.INT).optionalFieldOf("notes", Map.of()).forGetter(p -> p.notes)
    ).apply(i, PlayerResearch::new));

    public static final StreamCodec<ByteBuf, PlayerResearch> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private final Map<ResourceLocation, Entry> entries = new HashMap<>();
    private final Map<NoteCategory, Integer> notes = new EnumMap<>(NoteCategory.class);

    public PlayerResearch() {
    }

    private PlayerResearch(Map<ResourceLocation, Entry> entries, Map<NoteCategory, Integer> notes) {
        entries.forEach((k, v) -> this.entries.put(k, new Entry(v.state(), new HashSet<>(v.done()))));
        this.notes.putAll(notes);
    }

    public PlayerResearch copy() {
        return new PlayerResearch(entries, notes);
    }

    /** Stored state, or null if the research was never started. */
    public Entry entry(ResourceLocation id) {
        return entries.get(id);
    }

    public Map<ResourceLocation, Entry> entries() {
        return entries;
    }

    public void setState(ResourceLocation id, ResearchState state) {
        Entry old = entries.get(id);
        entries.put(id, new Entry(state, old == null ? new HashSet<>() : old.done()));
    }

    public boolean markDone(ResourceLocation id, String experiment) {
        Entry entry = entries.get(id);
        return entry != null && entry.done().add(experiment);
    }

    public int notes(NoteCategory category) {
        return notes.getOrDefault(category, 0);
    }

    public void addNotes(NoteCategory category, int amount) {
        notes.merge(category, amount, Integer::sum);
    }
}
