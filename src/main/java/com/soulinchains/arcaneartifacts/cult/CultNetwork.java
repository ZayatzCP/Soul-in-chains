package com.soulinchains.arcaneartifacts.cult;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The state of a running rite, sent to both of the pair for the HUD. */
public final class CultNetwork {
    private CultNetwork() {
    }

    public record RitualPayload(boolean active, int quality, int ticksLeft, boolean window, boolean closing, boolean solo, int[] anchors)
            implements CustomPacketPayload {
        public static final Type<RitualPayload> TYPE = new Type<>(ArcaneArtifacts.id("cult_ritual"));
        public static final StreamCodec<ByteBuf, RitualPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    FriendlyByteBuf out = new FriendlyByteBuf(buf);
                    out.writeBoolean(p.active);
                    out.writeVarInt(p.quality);
                    out.writeVarInt(p.ticksLeft);
                    out.writeBoolean(p.window);
                    out.writeBoolean(p.closing);
                    out.writeBoolean(p.solo);
                    out.writeVarIntArray(p.anchors);
                },
                buf -> {
                    FriendlyByteBuf in = new FriendlyByteBuf(buf);
                    return new RitualPayload(in.readBoolean(), in.readVarInt(), in.readVarInt(), in.readBoolean(), in.readBoolean(),
                            in.readBoolean(), in.readVarIntArray(8));
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The client's view of the rite (read by the HUD). */
    public static RitualPayload clientState;
    public static long clientReceivedAt;

    public static void handle(RitualPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            clientState = payload;
            clientReceivedAt = System.currentTimeMillis();
        });
    }
}
