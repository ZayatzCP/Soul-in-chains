package com.soulinchains.arcaneartifacts.accessory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Battle Rush state stored on the Wreath of Manipulation (game times), synced to the client for the
 * tooltip and the flower colours. The authoritative state is the pair of effects on the wearer.
 */
public record RushState(long activeUntil, long readyAt) {
    public static final RushState EMPTY = new RushState(0L, 0L);

    public static final Codec<RushState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("active_until").forGetter(RushState::activeUntil),
            Codec.LONG.fieldOf("ready_at").forGetter(RushState::readyAt)
    ).apply(i, RushState::new));

    public static final StreamCodec<ByteBuf, RushState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, RushState::activeUntil,
            ByteBufCodecs.VAR_LONG, RushState::readyAt,
            RushState::new);

    public boolean isActive(long now) {
        return now < activeUntil;
    }

    public boolean isRecovering(long now) {
        return now < readyAt;
    }
}
