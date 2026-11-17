package com.soulinchains.arcaneartifacts.client.catto;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.catto.Catto;
import com.soulinchains.arcaneartifacts.catto.CattoBrain;
import com.soulinchains.arcaneartifacts.catto.CattoContent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A small plate in the top left corner while Catto works (or while the bell or the Errand Wand is in hand): what Catto
 * is doing, its Source, and an arrow when it is far away.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CattoHud {
    private CattoHud() {
    }

    @Nullable
    private static Catto cached;
    private static long cachedAt = -100;

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ArcaneArtifacts.id("catto"), CattoHud::render);
    }

    @Nullable
    private static Catto find(Player player) {
        long now = player.level().getGameTime();
        if (now - cachedAt >= 20 || cached != null && (!cached.isAlive() || cached.level() != player.level())) {
            cachedAt = now;
            List<Catto> cats = player.level().getEntitiesOfClass(Catto.class, player.getBoundingBox().inflate(96),
                    c -> player.getUUID().equals(c.getOwnerUUID()));
            cached = cats.isEmpty() ? null : cats.get(0);
        }
        return cached;
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        boolean holding = player.getMainHandItem().is(CattoContent.WAND.get()) || player.getMainHandItem().is(CattoContent.BELL.get())
                || player.getOffhandItem().is(CattoContent.WAND.get());
        Catto cat = find(player);
        if (cat == null) {
            return;
        }
        CattoBrain.State state = cat.shownState();
        boolean busy = state != CattoBrain.State.IDLE && state != CattoBrain.State.OFFLINE;
        if (!holding && !busy && !cat.alert()) {
            return;
        }
        var font = minecraft.font;
        Component name = cat.getDisplayName();
        Component status = cat.status();
        int width = Math.max(120, Math.min(220, Math.max(font.width(name), font.width(status)) + 30));
        int x = 4;
        int y = 4;
        g.fill(x, y, x + width, y + 34, 0xA0201030);
        g.fill(x, y, x + width, y + 1, 0xFF9B59D0);
        ItemStack icon = cat.bubble().isEmpty() ? new ItemStack(Items.STRING) : cat.bubble();
        g.renderItem(icon, x + 3, y + 3);
        g.drawString(font, name, x + 22, y + 3, 0xFFE8D0FF, true);
        String line = font.plainSubstrByWidth(status.getString(), width - 26);
        g.drawString(font, line, x + 22, y + 13, cat.alert() ? 0xFFFF8070 : 0xFFDDDDDD, true);
        int barX = x + 22;
        int barW = width - 26;
        int max = Math.max(1, cat.shownMaxSource());
        int filled = Math.round(barW * Math.min(1.0F, cat.shownSource() / (float) max));
        g.fill(barX, y + 25, barX + barW, y + 29, 0xFF2A1A40);
        g.fill(barX, y + 25, barX + filled, y + 29, 0xFFB070FF);
        double dist = cat.distanceTo(player);
        if (dist > 16) {
            double dx = cat.getX() - player.getX();
            double dz = cat.getZ() - player.getZ();
            float angle = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0 - player.getYRot());
            String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
            int index = Math.floorMod(Math.round(angle / 45.0F), 8);
            g.drawString(font, arrows[index] + " " + (int) dist, x + 3, y + 22, 0xFFFFD080, true);
        }
    }
}
