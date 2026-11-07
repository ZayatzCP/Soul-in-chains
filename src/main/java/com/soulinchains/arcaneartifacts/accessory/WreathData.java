package com.soulinchains.arcaneartifacts.accessory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.EnumMap;
import java.util.Map;

/**
 * Adaptation state stored on the wreath item stack (data component), so that it is saved with the
 * item, survives death/dimension changes/relogging exactly like the item does, and is synced to every
 * client by Curios' slot sync - which is what drives the flower colours.
 *
 * @param hits         hits counted toward the next adaptation, per category
 * @param adaptedUntil absolute game time (overworld clock) at which an active adaptation ends
 */
public record WreathData(Map<DamageCategory, Integer> hits, Map<DamageCategory, Long> adaptedUntil) {
    public static final WreathData EMPTY = new WreathData(Map.of(), Map.of());

    public static final Codec<WreathData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(DamageCategory.CODEC, Codec.INT).optionalFieldOf("hits", Map.of()).forGetter(WreathData::hits),
            Codec.unboundedMap(DamageCategory.CODEC, Codec.LONG).optionalFieldOf("adapted_until", Map.of()).forGetter(WreathData::adaptedUntil)
    ).apply(i, WreathData::new));

    public static final StreamCodec<ByteBuf, WreathData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public WreathData {
        hits = Map.copyOf(hits);
        adaptedUntil = Map.copyOf(adaptedUntil);
    }

    public int hits(DamageCategory category) {
        return hits.getOrDefault(category, 0);
    }

    public boolean isAdapted(DamageCategory category, long gameTime) {
        return adaptedUntil.getOrDefault(category, Long.MIN_VALUE) > gameTime;
    }

    public long remainingTicks(DamageCategory category, long gameTime) {
        return Math.max(0, adaptedUntil.getOrDefault(category, 0L) - gameTime);
    }

    public WreathData withHits(DamageCategory category, int value) {
        EnumMap<DamageCategory, Integer> map = copyInts(hits);
        if (value <= 0) map.remove(category);
        else map.put(category, value);
        return new WreathData(map, adaptedUntil);
    }

    public WreathData withAdaptation(DamageCategory category, long until) {
        EnumMap<DamageCategory, Long> map = copyLongs(adaptedUntil);
        map.put(category, until);
        return new WreathData(copyInts(hits), map).withHits(category, 0);
    }

    /** Removes adaptations that have run out; their counters were already reset when they were granted. */
    public WreathData expire(long gameTime) {
        if (adaptedUntil.values().stream().noneMatch(t -> t <= gameTime)) {
            return this;
        }
        EnumMap<DamageCategory, Long> map = copyLongs(adaptedUntil);
        map.values().removeIf(t -> t <= gameTime);
        return new WreathData(hits, map);
    }

    private static EnumMap<DamageCategory, Integer> copyInts(Map<DamageCategory, Integer> src) {
        EnumMap<DamageCategory, Integer> map = new EnumMap<>(DamageCategory.class);
        map.putAll(src);
        return map;
    }

    private static EnumMap<DamageCategory, Long> copyLongs(Map<DamageCategory, Long> src) {
        EnumMap<DamageCategory, Long> map = new EnumMap<>(DamageCategory.class);
        map.putAll(src);
        return map;
    }
}
