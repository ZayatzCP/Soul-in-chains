package com.soulinchains.arcaneartifacts.research;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;

/**
 * Server -> client sync of research data. The client never sends research state: every change is
 * decided on the server (experiments by server-side events, observe/formulate through the
 * Research Table menu buttons), then mirrored here.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ResearchNetwork {
    private ResearchNetwork() {
    }

    /** All research definitions of the server (sent on login and after /reload). */
    public record DefinitionsPayload(Map<ResourceLocation, ResearchDefinition> definitions) implements CustomPacketPayload {
        public static final Type<DefinitionsPayload> TYPE = new Type<>(ArcaneArtifacts.id("research_definitions"));
        public static final StreamCodec<ByteBuf, DefinitionsPayload> STREAM_CODEC =
                ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ResearchDefinition.STREAM_CODEC)
                        .map(DefinitionsPayload::new, p -> new HashMap<>(p.definitions()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The receiving player's own progress. */
    public record ProgressPayload(PlayerResearch progress) implements CustomPacketPayload {
        public static final Type<ProgressPayload> TYPE = new Type<>(ArcaneArtifacts.id("research_progress"));
        public static final StreamCodec<ByteBuf, ProgressPayload> STREAM_CODEC =
                PlayerResearch.STREAM_CODEC.map(ProgressPayload::new, ProgressPayload::progress);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(DefinitionsPayload.TYPE, DefinitionsPayload.STREAM_CODEC,
                (payload, context) -> ClientResearch.acceptDefinitions(payload.definitions()));
        registrar.playToClient(ProgressPayload.TYPE, ProgressPayload.STREAM_CODEC,
                (payload, context) -> ClientResearch.acceptProgress(payload.progress()));
        registrar.playToServer(com.soulinchains.arcaneartifacts.belt.BeltRegistry.OpenBeltPayload.TYPE,
                com.soulinchains.arcaneartifacts.belt.BeltRegistry.OpenBeltPayload.STREAM_CODEC,
                (payload, context) -> com.soulinchains.arcaneartifacts.belt.BeltRegistry.handleOpen(context));
    }

    public static void sendDefinitions(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new DefinitionsPayload(new HashMap<>(ResearchManager.all())));
    }

    public static void sendProgress(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ProgressPayload(ResearchService.data(player).copy()));
    }
}
