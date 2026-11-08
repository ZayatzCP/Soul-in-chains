package com.soulinchains.arcaneartifacts.belt;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Menu type and the "open my belt" packet (sent by the belt key). */
public final class BeltRegistry {
    public static final DeferredHolder<MenuType<?>, MenuType<BeltMenu>> MENU =
            ModBlocks.MENUS.register("bottomless_belt", () -> new MenuType<>(BeltMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /** Client to server: open the worn belt. */
    public record OpenBeltPayload() implements CustomPacketPayload {
        public static final Type<OpenBeltPayload> TYPE = new Type<>(ArcaneArtifacts.id("open_belt"));
        public static final StreamCodec<ByteBuf, OpenBeltPayload> STREAM_CODEC = StreamCodec.unit(new OpenBeltPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void handleOpen(net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BeltHandler.openMenu(player);
            }
        });
    }

    /** Forces class loading so the menu type is registered with the other menus. */
    public static void init() {
    }

    private BeltRegistry() {
    }
}
