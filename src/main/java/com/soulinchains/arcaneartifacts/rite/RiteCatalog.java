package com.soulinchains.arcaneartifacts.rite;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** The rites the server knows, sent to players for the Treatise of Sigils (on join and after /reload). */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class RiteCatalog {
    private RiteCatalog() {
    }

    /** One rite as the Treatise shows it. Items are given by registry id number; {@code extra} pairs item, count. */
    public record Entry(String id, String kind, String main, String south, String west, String east, int minField, int offering, int offeringCount,
                        int[] extra, int cage, boolean otherworldly) {
    }

    public record Payload(List<Entry> entries) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ArcaneArtifacts.id("rite_catalog"));
        public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    FriendlyByteBuf out = new FriendlyByteBuf(buf);
                    out.writeVarInt(p.entries.size());
                    for (Entry e : p.entries) {
                        out.writeUtf(e.id);
                        out.writeUtf(e.kind);
                        out.writeUtf(e.main);
                        out.writeUtf(e.south);
                        out.writeUtf(e.west);
                        out.writeUtf(e.east);
                        out.writeVarInt(e.minField);
                        out.writeVarInt(e.offering);
                        out.writeVarInt(e.offeringCount);
                        out.writeVarIntArray(e.extra);
                        out.writeVarInt(e.cage);
                        out.writeBoolean(e.otherworldly);
                    }
                },
                buf -> {
                    FriendlyByteBuf in = new FriendlyByteBuf(buf);
                    int n = in.readVarInt();
                    List<Entry> list = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        list.add(new Entry(in.readUtf(), in.readUtf(), in.readUtf(), in.readUtf(), in.readUtf(), in.readUtf(), in.readVarInt(),
                                in.readVarInt(), in.readVarInt(), in.readVarIntArray(64), in.readVarInt(), in.readBoolean()));
                    }
                    return new Payload(list);
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The client's copy. */
    public static List<Entry> client = List.of();

    public static void handle(Payload payload, IPayloadContext context) {
        context.enqueueWork(() -> client = List.copyOf(payload.entries));
    }

    public static Payload build() {
        List<Entry> list = new ArrayList<>();
        for (RiteDefinition r : Rites.all()) {
            int[] extra = new int[r.extraOfferings().size() * 2];
            for (int i = 0; i < r.extraOfferings().size(); i++) {
                extra[i * 2] = BuiltInRegistries.ITEM.getId(r.extraOfferings().get(i).item());
                extra[i * 2 + 1] = r.extraOfferings().get(i).count();
            }
            list.add(new Entry(r.id().getPath(), r.kind().name().toLowerCase(java.util.Locale.ROOT), r.main().id, r.south().id, r.west().id, r.east().id,
                    r.minField(), BuiltInRegistries.ITEM.getId(r.offering().item()), r.offering().count(), extra, r.cageValue(), r.otherworldly()));
        }
        return new Payload(list);
    }

    @SubscribeEvent
    public static void onSync(OnDatapackSyncEvent event) {
        Payload payload = build();
        event.getRelevantPlayers().forEach(p -> PacketDistributor.sendToPlayer(p, payload));
    }

    public static void send(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, build());
    }
}
