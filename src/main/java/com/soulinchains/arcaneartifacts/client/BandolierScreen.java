package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.pseudo.BandolierItem;
import com.soulinchains.arcaneartifacts.pseudo.BandolierMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The bandolier: a leather strap with eight brass-rimmed pouches, cartridges counted on top. */
public class BandolierScreen extends AbstractContainerScreen<BandolierMenu> {
    public BandolierScreen(BandolierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF2A1A0E);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF6B3A1E);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, 0xFFC9A36A);
        // The strap with its stitching.
        g.fill(x + 6, y + 22, x + imageWidth - 6, y + 58, 0xFF6B3A1E);
        g.fill(x + 6, y + 24, x + imageWidth - 6, y + 25, 0xFFE8C88A);
        g.fill(x + 6, y + 55, x + imageWidth - 6, y + 56, 0xFFE8C88A);
        for (int i = 0; i < BandolierItem.POUCHES; i++) {
            int px = x + BandolierMenu.POUCH_X[i], py = y + BandolierMenu.POUCH_Y;
            g.fill(px - 2, py - 3, px + 18, py + 19, 0xFFB07A2A);
            g.fill(px - 1, py - 2, px + 17, py + 18, 0xFF3A2010);
            g.fill(px, py - 1, px + 16, py + 1, 0xFF8E5530);
        }
        for (int i = BandolierItem.POUCHES; i < menu.slots.size(); i++) {
            var slot = menu.slots.get(i);
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF8B6A3A);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFFA88A5A);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        int total = 0;
        for (int i = 0; i < BandolierItem.POUCHES; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            total += stack.getCount();
        }
        g.drawString(font, title, 8, 7, 0x2A1A0E, false);
        g.drawString(font, Component.translatable("arcane_artifacts.bandolier.count", total, BandolierItem.CAPACITY), 8, 61, 0x2A1A0E, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY + 2, 0x2A1A0E, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
