package com.soulinchains.arcaneartifacts.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.belt.BeltRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** The keys of worn accessories: the Belt of Bottomless Pouches (B), the bandolier (G), the amulet (N); all rebindable. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, value = Dist.CLIENT)
public final class BeltKey {
    public static final KeyMapping KEY = new KeyMapping("key.arcane_artifacts.open_belt", StaffContext.NOT_HOLDING,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.arcane_artifacts");

    /**
     * Turns the Staff of the Five Circles to its next circle (B by default, rebindable). It shares B with the belt: the
     * key does this while the staff is in a hand and opens the belt otherwise.
     */
    public static final KeyMapping CIRCLE_KEY = new KeyMapping("key.arcane_artifacts.druid_circle", StaffContext.HOLDING,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.arcane_artifacts");

    /** In game, with or without the Staff of the Five Circles in a hand: the two never clash with each other. */
    public enum StaffContext implements net.neoforged.neoforge.client.settings.IKeyConflictContext {
        HOLDING, NOT_HOLDING;

        @Override
        public boolean isActive() {
            Minecraft minecraft = Minecraft.getInstance();
            if (!KeyConflictContext.IN_GAME.isActive() || minecraft.player == null) {
                return false;
            }
            boolean holding = com.soulinchains.arcaneartifacts.druid.DruidStaffItem.holdsGrandStaff(minecraft.player);
            return this == HOLDING ? holding : !holding;
        }

        @Override
        public boolean conflicts(net.neoforged.neoforge.client.settings.IKeyConflictContext other) {
            return other == this || other == KeyConflictContext.IN_GAME;
        }
    }

    /** Flips the polarity of the worn Amulet of Opposites (N by default, rebindable). */
    public static final KeyMapping AMULET_KEY = new KeyMapping("key.arcane_artifacts.toggle_amulet", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.arcane_artifacts");

    /** Opens the worn bandolier (G by default, rebindable). */
    public static final KeyMapping BANDOLIER_KEY = new KeyMapping("key.arcane_artifacts.open_bandolier", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.arcane_artifacts");

    private BeltKey() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (KEY.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                PacketDistributor.sendToServer(new BeltRegistry.OpenBeltPayload());
            }
        }
        while (CIRCLE_KEY.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null
                    && com.soulinchains.arcaneartifacts.druid.DruidStaffItem.holdsGrandStaff(minecraft.player)) {
                PacketDistributor.sendToServer(new com.soulinchains.arcaneartifacts.druid.DruidNetwork.SwitchCirclePayload());
            }
        }
        while (BANDOLIER_KEY.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                PacketDistributor.sendToServer(new BeltRegistry.OpenBandolierPayload());
            }
        }
        while (AMULET_KEY.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                PacketDistributor.sendToServer(new com.soulinchains.arcaneartifacts.accessory.AmuletOfOppositesItem.TogglePayload());
            }
        }
    }
}
