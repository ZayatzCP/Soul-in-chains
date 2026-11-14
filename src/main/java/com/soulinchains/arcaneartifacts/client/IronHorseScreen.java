package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.mount.IronHorseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The Iron Horse's boiler: riveted plate, the coal bunker slot, a firebox glow and the fuel gauge. */
public class IronHorseScreen extends AbstractContainerScreen<IronHorseMenu> {
    public IronHorseScreen(IronHorseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF2F3236);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF8E9399);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, 0xFFB4B8BE);
        for (int[] r : new int[][]{{6, 6}, {imageWidth - 9, 6}, {6, 74}, {imageWidth - 9, 74}}) {
            g.fill(x + r[0], y + r[1], x + r[0] + 3, y + r[1] + 3, 0xFFDDE0E4);   // rivets
        }
        // The firebox: glows as bright as the boiler is full.
        float fuel = menu.fuelFraction();
        int fx = x + 52, fy = y + 20;
        g.fill(fx - 2, fy - 2, fx + 74, fy + 52, 0xFFC8963A);
        g.fill(fx, fy, fx + 72, fy + 50, 0xFF2A1A10);
        int glow = (int) (fuel * 44);
        if (glow > 0) {
            g.fill(fx + 4, fy + 48 - glow, fx + 20, fy + 48, 0xFFE05A10);
            g.fill(fx + 6, fy + 48 - glow * 3 / 4, fx + 18, fy + 48, 0xFFFF9A20);
        }
        int sx = x + IronHorseMenu.SLOT_X, sy = y + IronHorseMenu.SLOT_Y;
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF3E4248);
        g.fill(sx, sy, sx + 16, sy + 16, 0xFF5C6066);
        // The gauge on the right.
        int gx = x + 104, gy = y + 22;
        g.fill(gx, gy, gx + 14, gy + 46, 0xFF3E4248);
        int level = (int) (fuel * 42);
        g.fill(gx + 2, gy + 44 - level, gx + 12, gy + 44, 0xFFFFB030);
        for (int i = 1; i < menu.slots.size(); i++) {
            var slot = menu.slots.get(i);
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF6E7378);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF9DA2A8);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 7, 0x2F3236, false);
        g.drawString(font, Component.translatable("arcane_artifacts.iron_horse.gauge", menu.fuelSeconds()), 122, 40, 0x2F3236, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY + 2, 0x2F3236, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
