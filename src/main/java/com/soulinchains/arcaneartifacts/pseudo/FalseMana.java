package com.soulinchains.arcaneartifacts.pseudo;

import com.mojang.serialization.Codec;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * False mana: the Pseudomage's counterfeit of real mana. It is not Ars Nouveau mana at all (spells
 * cannot use it); counterfeit accessories raise its maximum ({@link #MAX} attribute), it refills by
 * itself, and firearms load rounds with it before they touch cartridges.
 */
public final class FalseMana {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, ArcaneArtifacts.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ArcaneArtifacts.MODID);

    /** Maximum false mana; 0 for anyone who wears no counterfeit. */
    public static final DeferredHolder<Attribute, Attribute> MAX = ATTRIBUTES.register("false_mana",
            () -> new RangedAttribute("attribute.arcane_artifacts.false_mana", 0.0, 0.0, 100000.0).setSyncable(true));
    public static final Supplier<AttachmentType<Float>> CURRENT = ATTACHMENTS.register("false_mana",
            () -> AttachmentType.builder(() -> 0f).serialize(Codec.FLOAT).build());

    /** One division of the bar: a round costs exactly one division. */
    public static final int DIVISION = 10;

    private static float clientCurrent;

    private FalseMana() {
    }

    public static float max(Player player) {
        return player.getAttributes().hasAttribute(MAX) ? (float) player.getAttributeValue(MAX) : 0f;
    }

    public static float get(Player player) {
        return player.level().isClientSide ? clientCurrent : Math.min(player.getData(CURRENT), max(player));
    }

    /** Takes {@code amount} if the player has that much; returns whether it did. */
    public static boolean spend(Player player, float amount) {
        float current = get(player);
        if (current + 1.0E-4f < amount) {
            return false;
        }
        set(player, current - amount);
        return true;
    }

    public static void set(Player player, float value) {
        float clamped = Math.max(0f, Math.min(value, max(player)));
        player.setData(CURRENT, clamped);
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncPayload(clamped));
        }
    }

    public record SyncPayload(float current) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(ArcaneArtifacts.id("false_mana"));
        public static final StreamCodec<ByteBuf, SyncPayload> STREAM_CODEC = ByteBufCodecs.FLOAT.map(SyncPayload::new, SyncPayload::current);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void handleSync(SyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientCurrent = payload.current());
    }

    @EventBusSubscriber(modid = ArcaneArtifacts.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void attributes(EntityAttributeModificationEvent event) {
            event.add(EntityType.PLAYER, MAX);
        }
    }

    @EventBusSubscriber(modid = ArcaneArtifacts.MODID)
    public static final class GameEvents {
        private GameEvents() {
        }

        /** Refills a little every second and keeps the client's bar in sync. */
        @SubscribeEvent
        public static void onPlayerTick(PlayerTickEvent.Post event) {
            Player player = event.getEntity();
            if (player.level().isClientSide || player.tickCount % 20 != 0) {
                return;
            }
            float max = max(player);
            float current = player.getData(CURRENT);
            float next = max <= 0 ? 0 : Math.min(max, current + Config.FALSE_MANA_REGEN.get().floatValue());
            if (next != current || player.tickCount % 100 == 0) {
                set(player, next);
            }
        }
    }
}
