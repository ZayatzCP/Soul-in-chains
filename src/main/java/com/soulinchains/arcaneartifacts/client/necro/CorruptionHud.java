package com.soulinchains.arcaneartifacts.client.necro;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.necro.Corruption;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** A thin Corruption bar over the health bar, coloured by stage, shown while the player has any Corruption. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CorruptionHud {
    private static final int[] COLORS = {0xFF8E8EA8, 0xFF8E5BC8, 0xFF6A2FB0, 0xFF3E0E60, 0xFF5ADB5A};

    private CorruptionHud() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ArcaneArtifacts.id("corruption"), CorruptionHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        float value = Corruption.clientValue;
        if (player == null || minecraft.options.hideGui || player.isSpectator() || player.isCreative() || value <= 0) {
            return;
        }
        int width = 81;
        int x = g.guiWidth() / 2 - 91;
        int y = g.guiHeight() - 49 - (player.getArmorValue() > 0 ? 10 : 0) - 4;
        int stage = Corruption.stage(value);
        g.fill(x - 1, y - 1, x + width + 1, y + 3, 0xFF140A1E);
        g.fill(x, y, x + Math.round(width * value / 100F), y + 2, COLORS[stage]);
        for (float mark : new float[]{Corruption.TOUCHED, Corruption.SHACKLED, Corruption.UNCLEAN}) {
            int notch = x + Math.round(width * mark / 100F);
            g.fill(notch, y - 1, notch + 1, y + 3, 0xFFD8C8F0);
        }
        if (minecraft.player.isShiftKeyDown()) {
            Component label = Component.translatable("arcane_artifacts.corruption.hud", (int) value,
                    Component.translatable("arcane_artifacts.corruption.stage." + stage));
            g.drawString(minecraft.font, label, x, y - 10, 0xFFD8C8F0, true);
        }
    }
}
