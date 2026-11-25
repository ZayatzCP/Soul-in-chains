package com.soulinchains.arcaneartifacts.client.rite;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.rite.RiteHeartBlockEntity;
import com.soulinchains.arcaneartifacts.rite.RiteNetwork;
import com.soulinchains.arcaneartifacts.rite.RiteRole;
import com.soulinchains.arcaneartifacts.rite.RiteSite;
import com.soulinchains.arcaneartifacts.rite.RiteTier;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.ArrayList;
import java.util.List;

/** The rite HUD, top centre: quality bar with the tier marks, time left, your roles, the reading progress and your current task. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RiteHud {
    private static final int RED = 0xFFD0473B;
    private static final int DARK = 0xFF171416;
    private static final int CHALK = 0xFFF1EEE6;
    private static final int AMETHYST = 0xFFB48CF0;

    private RiteHud() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ArcaneArtifacts.id("chalk_rite"), RiteHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        RiteNetwork.RitePayload s = RiteNetwork.clientState;
        if (s == null || !s.active() || mc.options.hideGui || System.currentTimeMillis() - RiteNetwork.clientReceivedAt > 1500) {
            return;
        }
        int width = 140;
        int x = g.guiWidth() / 2 - width / 2;
        int y = 22;
        g.fill(x - 2, y - 2, x + width + 2, y + 6, DARK);
        g.fill(x, y, x + Math.round(width * s.quality() / 100F), y + 4, RED);
        for (RiteTier t : RiteTier.values()) {
            if (t.from > 0) {
                int notch = x + Math.round(width * t.from / 100F);
                g.fill(notch, y - 2, notch + 1, y + 6, CHALK);
            }
        }
        g.drawCenteredString(mc.font, Component.translatable("arcane_artifacts.rite.hud", s.quality(), RiteTier.of(s.quality()).displayName(),
                s.ticksLeft() / 20), g.guiWidth() / 2, y + 9, CHALK);
        List<Component> roles = new ArrayList<>();
        for (RiteRole r : RiteRole.values()) {
            if ((s.roles() & (1 << r.ordinal())) != 0) {
                roles.add(r.displayName());
            }
        }
        g.drawCenteredString(mc.font, ComponentUtils.formatList(roles, Component.literal(", ")), g.guiWidth() / 2, y + 20, AMETHYST);
        Component task = switch (s.task()) {
            case 1 -> Component.translatable("arcane_artifacts.rite.hud_trace",
                    RiteHeartBlockEntity.placeName(new RiteSite.Place(s.arg() / 17 - 8, s.arg() % 17 - 8)), s.taskLeft() / 20);
            case 2 -> Component.translatable("arcane_artifacts.rite.hud_offer", BuiltInRegistries.ITEM.byId(s.arg()).getDescription(), s.taskLeft() / 20);
            case 3 -> Component.translatable("arcane_artifacts.rite.hud_cage", s.taskLeft() / 20);
            case 4 -> Component.translatable("arcane_artifacts.rite.hud_close", s.taskLeft() / 20);
            case 5 -> Component.translatable("arcane_artifacts.rite.hud_read", s.read());
            default -> null;
        };
        if (task != null && (s.task() == 5 || (System.currentTimeMillis() / 250) % 2 == 0)) {
            g.drawCenteredString(mc.font, task, g.guiWidth() / 2, y + 31, s.task() == 5 ? CHALK : RED);
        }
    }
}
