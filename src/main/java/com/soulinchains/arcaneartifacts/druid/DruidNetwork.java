package com.soulinchains.arcaneartifacts.druid;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The circle key of the Staff of the Five Circles. */
public final class DruidNetwork {
    private DruidNetwork() {
    }

    public record SwitchCirclePayload() implements CustomPacketPayload {
        public static final Type<SwitchCirclePayload> TYPE = new Type<>(ArcaneArtifacts.id("druid_circle"));
        public static final StreamCodec<ByteBuf, SwitchCirclePayload> STREAM_CODEC = StreamCodec.unit(new SwitchCirclePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void handleSwitch(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DruidStaffItem.switchCircle(player);
            }
        });
    }
}
