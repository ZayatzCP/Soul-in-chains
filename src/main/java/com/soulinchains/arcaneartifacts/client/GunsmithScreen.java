package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.pseudo.GunsmithMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Farraday's Workshop: a scuffed oak bench, the gun on the left, three labelled part sockets on the right. */
public class GunsmithScreen extends AbstractContainerScreen<GunsmithMenu> {
    private static final String[] LABELS = {"arcane_artifacts.gun_part.slot.barrel", "arcane_artifacts.gun_part.slot.cylinder", "arcane_artifacts.gun_part.slot.stock"};

    public GunsmithScreen(GunsmithMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF3A2410);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF8A5A2E);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, 0xFFC08A52);
        // Wood grain on the bench top.
        for (int i = 0; i < 6; i++) {
            g.fill(x + 6, y + 18 + i * 9, x + imageWidth - 6, y + 19 + i * 9, 0x30603A18);
        }
        // The gun cradle and the brass sockets.
        slot(g, x + GunsmithMenu.GUN_X, y + GunsmithMenu.GUN_Y, 0xFF5C3A1C, 0xFF2A1A0E, 4);
        g.fill(x + GunsmithMenu.GUN_X + 22, y + GunsmithMenu.GUN_Y + 7, x + GunsmithMenu.PART_X[0] - 6, y + GunsmithMenu.GUN_Y + 9, 0xFFC8963A);
        for (int px : GunsmithMenu.PART_X) {
            slot(g, x + px, y + GunsmithMenu.PART_Y, 0xFFC8963A, 0xFF3E2A14, 2);
        }
        for (int i = 1 + 3; i < menu.slots.size(); i++) {
            var slot = menu.slots.get(i);
            slot(g, x + slot.x, y + slot.y, 0xFF7A5230, 0xFFA87A4A, 1);
        }
    }

    private static void slot(GuiGraphics g, int sx, int sy, int rim, int inner, int pad) {
        g.fill(sx - pad, sy - pad, sx + 16 + pad, sy + 16 + pad, rim);
        g.fill(sx, sy, sx + 16, sy + 16, inner);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 7, 0x2A1A0E, false);
        for (int i = 0; i < LABELS.length; i++) {
            Component label = Component.translatable(LABELS[i]);
            int px = GunsmithMenu.PART_X[i] + 8 - font.width(label) / 2;
            g.drawString(font, label, px, GunsmithMenu.PART_Y + 21, 0x3A2410, false);
        }
        if (menu.slots.get(0).getItem().isEmpty()) {
            g.drawString(font, Component.translatable("arcane_artifacts.gunsmith.hint"), 8, 60, 0x5A3A1C, false);
        }
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY + 2, 0x2A1A0E, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
