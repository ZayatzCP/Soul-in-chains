package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.alchemy.AlchemicalDistillerBlockEntity;
import com.soulinchains.arcaneartifacts.alchemy.AlchemicalDistillerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Alchemical Distiller screen: an extended brewing stand. Fuel (nether wart blocks) on the left, four
 * ingredients and a modifier on top, three bottles below, a progress tube and a Source indicator.
 */
public class AlchemicalDistillerScreen extends AbstractContainerScreen<AlchemicalDistillerMenu> {
    public AlchemicalDistillerScreen(AlchemicalDistillerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E1530);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF3A2A55);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, 0xFFBFB2D6);
        // Glass tubes from the ingredients down to the bottles.
        g.fill(x + 50, y + 38, x + 112, y + 42, 0xFF8E7FB0);
        g.fill(x + 79, y + 42, x + 83, y + 66, 0xFF8E7FB0);
        g.fill(x + 63, y + 54, x + 67, y + 68, 0xFF8E7FB0);
        g.fill(x + 107, y + 54, x + 111, y + 68, 0xFF8E7FB0);
        g.fill(x + 63, y + 52, x + 111, y + 56, 0xFF8E7FB0);
        // Brew progress: the liquid runs down the middle tube.
        int progress = menu.progress();
        if (progress > 0) {
            int filled = Math.min(24, progress * 24 / menu.brewTime());
            g.fill(x + 79, y + 42, x + 83, y + 42 + filled, 0xFFB44CF0);
            g.fill(x + 50, y + 38, x + 112, y + 42, 0xFFB44CF0);
        }
        // Fuel charges under the fuel slot.
        for (int i = 0; i < AlchemicalDistillerBlockEntity.CHARGES_PER_FUEL; i++) {
            int color = i < menu.charges() ? 0xFFC0392B : 0xFF6B5A7A;
            g.fill(x + 15 + i * 7, y + 40, x + 20 + i * 7, y + 44, color);
        }
        // Source indicator.
        int source = menu.sourceNearby() ? 0xFF2FD6E0 : 0xFF6B5A7A;
        g.fill(x + 138, y + 44, x + 158, y + 50, source);
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF5B4A78);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF8B7BA8);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0x2A1B3A, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x2A1B3A, false);
        g.drawString(font, Component.translatable("arcane_artifacts.distiller.source"), 132, 52, menu.sourceNearby() ? 0x1E6E7C : 0x6B5A7A, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int x = leftPos, y = topPos;
        if (mouseX >= x + 138 && mouseX < x + 158 && mouseY >= y + 44 && mouseY < y + 60) {
            g.renderTooltip(font, Component.translatable(menu.sourceNearby() ? "arcane_artifacts.distiller.source_ok"
                    : "arcane_artifacts.distiller.source_missing", AlchemicalDistillerBlockEntity.sourceCost()), mouseX, mouseY);
        }
    }
}
