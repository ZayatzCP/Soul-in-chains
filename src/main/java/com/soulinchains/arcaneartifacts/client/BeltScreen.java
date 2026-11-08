package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.belt.BeltContents;
import com.soulinchains.arcaneartifacts.belt.BeltMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen of the Belt of Bottomless Pouches. Each pouch is a big slot showing its item and amount.
 * Left click: put the held stack in (or take a stack if the cursor is empty). Shift + left click: take
 * a stack into the inventory. Right click: put every matching item from the inventory in. Shift-clicking
 * an inventory slot sends the stack to its pouch.
 */
public class BeltScreen extends AbstractContainerScreen<BeltMenu> {
    private static final int POUCH_SIZE = 26, POUCH_Y = 26;
    private static final int[] POUCH_X = {35, 75, 115};

    public BeltScreen(BeltMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF2B1F3A);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF4A3566);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, 0xFFC9B48A);
        for (int i = 0; i < BeltContents.POUCHES; i++) {
            int px = x + POUCH_X[i], py = y + POUCH_Y;
            boolean hover = isOverPouch(i, mouseX, mouseY);
            g.fill(px - 1, py - 1, px + POUCH_SIZE + 1, py + POUCH_SIZE + 1, hover ? 0xFFB44CF0 : 0xFF5B3E8A);
            g.fill(px, py, px + POUCH_SIZE, py + POUCH_SIZE, 0xFF8B7355);
        }
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF8B7355);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFFA48A66);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        BeltContents contents = menu.contents();
        for (int i = 0; i < BeltContents.POUCHES; i++) {
            BeltContents.Pouch pouch = contents.get(i);
            int px = leftPos + POUCH_X[i] + 5, py = topPos + POUCH_Y + 5;
            if (!pouch.isEmpty()) {
                g.renderItem(pouch.type(), px, py);
                String count = BeltContents.format(pouch.count());
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.drawString(font, count, leftPos + POUCH_X[i] + POUCH_SIZE / 2 - font.width(count) / 2, topPos + POUCH_Y + POUCH_SIZE + 3, 0x3B2A1A, false);
                g.pose().popPose();
            }
        }
        for (int i = 0; i < BeltContents.POUCHES; i++) {
            if (isOverPouch(i, mouseX, mouseY)) {
                BeltContents.Pouch pouch = contents.get(i);
                List<Component> lines = new ArrayList<>();
                if (pouch.isEmpty()) {
                    lines.add(Component.translatable("arcane_artifacts.belt.pouch_empty", i + 1));
                } else {
                    lines.add(pouch.type().getHoverName().copy().withStyle(ChatFormatting.AQUA));
                    lines.add(Component.translatable("arcane_artifacts.belt.amount", pouch.count()).withStyle(ChatFormatting.GRAY));
                }
                lines.add(Component.translatable("arcane_artifacts.belt.help.1").withStyle(ChatFormatting.DARK_GRAY));
                lines.add(Component.translatable("arcane_artifacts.belt.help.2").withStyle(ChatFormatting.DARK_GRAY));
                lines.add(Component.translatable("arcane_artifacts.belt.help.3").withStyle(ChatFormatting.DARK_GRAY));
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        }
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 10, 0x3B2A1A, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x3B2A1A, false);
    }

    private boolean isOverPouch(int i, double mouseX, double mouseY) {
        int px = leftPos + POUCH_X[i], py = topPos + POUCH_Y;
        return mouseX >= px && mouseX < px + POUCH_SIZE && mouseY >= py && mouseY < py + POUCH_SIZE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < BeltContents.POUCHES; i++) {
            if (isOverPouch(i, mouseX, mouseY) && minecraft != null && minecraft.gameMode != null) {
                int action;
                if (button == 1) {
                    action = BeltMenu.PUT_ALL;
                } else if (Screen.hasShiftDown()) {
                    action = BeltMenu.TAKE_TO_INVENTORY;
                } else {
                    action = menu.getCarried().isEmpty() ? BeltMenu.TAKE_TO_CURSOR : BeltMenu.PUT_CARRIED;
                }
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i * 4 + action);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
