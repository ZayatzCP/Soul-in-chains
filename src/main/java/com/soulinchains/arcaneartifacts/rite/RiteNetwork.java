package com.soulinchains.arcaneartifacts.rite;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The state of a running rite, sent to each participant for the HUD. */
public final class RiteNetwork {
    private RiteNetwork() {
    }

    /**
     * {@code task}: 0 none, 1 retrace (arg: place), 2 offering (arg: item id), 3 open the cage, 4 close the rite, 5 read.
     */
    public record RitePayload(boolean active, int quality, int ticksLeft, int roles, int task, int arg, int taskLeft, int read)
            implements CustomPacketPayload {
        public static final RitePayload NONE = new RitePayload(false, 0, 0, 0, 0, 0, 0, 0);
        public static final Type<RitePayload> TYPE = new Type<>(ArcaneArtifacts.id("chalk_rite"));
        public static final StreamCodec<ByteBuf, RitePayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    FriendlyByteBuf out = new FriendlyByteBuf(buf);
                    out.writeBoolean(p.active);
                    out.writeVarInt(p.quality);
                    out.writeVarInt(p.ticksLeft);
                    out.writeVarInt(p.roles);
                    out.writeVarInt(p.task);
                    out.writeVarInt(p.arg);
                    out.writeVarInt(p.taskLeft);
                    out.writeVarInt(p.read);
                },
                buf -> {
                    FriendlyByteBuf in = new FriendlyByteBuf(buf);
                    return new RitePayload(in.readBoolean(), in.readVarInt(), in.readVarInt(), in.readVarInt(), in.readVarInt(), in.readVarInt(),
                            in.readVarInt(), in.readVarInt());
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static RitePayload clientState = RitePayload.NONE;
    public static long clientReceivedAt;

    public static void handle(RitePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            clientState = payload;
            clientReceivedAt = System.currentTimeMillis();
        });
    }
}
