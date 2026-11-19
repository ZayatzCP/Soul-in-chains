package com.soulinchains.arcaneartifacts.necro;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * State of a Phylactery Amulet: its look (purely cosmetic, chosen by the player), the place it is bound to and the
 * overworld time when it can save its wearer again.
 */
public record Phylactery(int style, Optional<GlobalPos> bound, long readyAt) {
    public static final int STYLES = 5;
    public static final Phylactery EMPTY = new Phylactery(0, Optional.empty(), 0L);
    public static final Codec<Phylactery> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("style", 0).forGetter(Phylactery::style),
            GlobalPos.CODEC.optionalFieldOf("bound").forGetter(Phylactery::bound),
            Codec.LONG.optionalFieldOf("ready_at", 0L).forGetter(Phylactery::readyAt)
    ).apply(i, Phylactery::new));
    public static final StreamCodec<ByteBuf, Phylactery> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Phylactery::style,
            ByteBufCodecs.optional(GlobalPos.STREAM_CODEC), Phylactery::bound,
            ByteBufCodecs.VAR_LONG, Phylactery::readyAt,
            Phylactery::new);

    public Phylactery withStyle(int newStyle) {
        return new Phylactery(Math.floorMod(newStyle, STYLES), bound, readyAt);
    }

    public Phylactery boundTo(GlobalPos pos) {
        return new Phylactery(style, Optional.of(pos), readyAt);
    }

    public Phylactery usedUntil(long time) {
        return new Phylactery(style, bound, time);
    }
}
