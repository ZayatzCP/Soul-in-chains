package com.soulinchains.arcaneartifacts.client.cult;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.cult.CultNetwork;
import com.soulinchains.arcaneartifacts.cult.RitualQuality;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * The rite HUD, top centre, while a rite of yours runs: quality bar with the grade marks, time left, one pip per anchor
 * (its stability, or a cross when broken), and a flashing "INSCRIBE" when the seal opens its window.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CultHud {
    private static final int RED = 0xFFB9463D;
    private static final int DARK = 0xFF171416;
    private static final int BONE = 0xFFF0E7DE;

    private CultHud() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ArcaneArtifacts.id("cult_ritual"), CultHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        CultNetwork.RitualPayload state = CultNetwork.clientState;
        if (state == null || !state.active() || minecraft.options.hideGui || System.currentTimeMillis() - CultNetwork.clientReceivedAt > 1500) {
            return;
        }
        int width = 120;
        int x = g.guiWidth() / 2 - width / 2;
        int y = 22;
        g.fill(x - 2, y - 2, x + width + 2, y + 6, DARK);
        g.fill(x, y, x + Math.round(width * state.quality() / 100F), y + 4, RED);
        for (RitualQuality q : RitualQuality.values()) {
            if (q.from > 0) {
                int notch = x + Math.round(width * q.from / 100F);
                g.fill(notch, y - 2, notch + 1, y + 6, BONE);
            }
        }
        Component grade = RitualQuality.of(state.quality()).displayName();
        Component line = Component.translatable("arcane_artifacts.cult.hud", state.quality(), grade, state.ticksLeft() / 20);
        g.drawCenteredString(minecraft.font, line, g.guiWidth() / 2, y + 9, BONE);
        int pipX = g.guiWidth() / 2 - state.anchors().length * 7;
        for (int stability : state.anchors()) {
            int color = stability < 0 ? 0xFF555555 : stability > 60 ? RED : stability > 25 ? 0xFFD6B2A5 : 0xFF6F2927;
            g.fill(pipX, y + 20, pipX + 12, y + 24, color);
            pipX += 14;
        }
        if (state.window() && (System.currentTimeMillis() / 150) % 2 == 0) {
            g.drawCenteredString(minecraft.font, Component.translatable(state.closing() ? "arcane_artifacts.cult.hud_close" : "arcane_artifacts.cult.hud_window"),
                    g.guiWidth() / 2, y + 28, RED);
        }
        if (state.solo()) {
            g.drawCenteredString(minecraft.font, Component.translatable("arcane_artifacts.cult.hud_solo"), g.guiWidth() / 2, y + 38, 0xFF929292);
        }
    }
}
