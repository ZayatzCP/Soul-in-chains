package com.soulinchains.arcaneartifacts.necro;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/** The souls in a Soul Lantern: a count per {@link SoulType}. Immutable, stored as an item component. */
public record SoulStore(List<Integer> counts) {
    public static final SoulStore EMPTY = new SoulStore(List.of());
    public static final Codec<SoulStore> CODEC = Codec.INT.listOf().xmap(SoulStore::new, SoulStore::counts);
    public static final StreamCodec<ByteBuf, SoulStore> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).map(SoulStore::new, SoulStore::counts);

    public int count(SoulType type) {
        return type.ordinal() < counts.size() ? counts.get(type.ordinal()) : 0;
    }

    public int total() {
        int sum = 0;
        for (int c : counts) {
            sum += c;
        }
        return sum;
    }

    public SoulStore with(SoulType type, int delta) {
        List<Integer> next = new ArrayList<>(counts);
        while (next.size() < SoulType.values().length) {
            next.add(0);
        }
        next.set(type.ordinal(), Math.max(0, next.get(type.ordinal()) + delta));
        return new SoulStore(List.copyOf(next));
    }
}
