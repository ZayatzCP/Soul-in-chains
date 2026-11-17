package com.soulinchains.arcaneartifacts.catto;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/** Server -> owner: what Catto's menu shows besides slots (queue, skills, reports...). */
public final class CattoNetwork {
    private CattoNetwork() {
    }

    /** Latest data per Catto entity id, read by the client screen. Plain data, safe on both sides. */
    public static final Map<Integer, CompoundTag> CLIENT_DATA = new HashMap<>();

    public record SyncPayload(int entityId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(ArcaneArtifacts.id("catto_sync"));
        public static final StreamCodec<ByteBuf, SyncPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SyncPayload::entityId,
                ByteBufCodecs.COMPOUND_TAG, SyncPayload::data,
                SyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void handle(SyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CLIENT_DATA.put(payload.entityId(), payload.data()));
    }
}
