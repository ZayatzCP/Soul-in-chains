package com.soulinchains.arcaneartifacts.client.catto;

import com.soulinchains.arcaneartifacts.catto.Catto;
import com.soulinchains.arcaneartifacts.catto.CattoMenu;
import com.soulinchains.arcaneartifacts.catto.CattoNetwork;
import com.soulinchains.arcaneartifacts.catto.CattoSkill;
import com.soulinchains.arcaneartifacts.catto.CattoTask;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * Catto's menu, drawn in the colours of Ars Nouveau's books: equipment and bag on the left, three tabs on the right
 * (queue, skills, reports) with the status line and the command buttons under them.
 */
public class CattoScreen extends AbstractContainerScreen<CattoMenu> {
    // The colours of the player's inventory, with a little violet for Catto.
    private static final int PARCHMENT = 0xFFC6C6C6;
    private static final int PARCHMENT_DARK = 0xFF555555;
    private static final int PURPLE = 0xFF4A2A6A;
    private static final int PURPLE_LIGHT = 0xFF8E62B5;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int INK = 0xFF404040;
    private static final int INK_SOFT = 0xFF606060;
    private static final int RED = 0xFFA8322A;
    private static final int PANEL_X = 180;
    private static final int PANEL_W = 164;
    private static final int CONTENT_Y = 22;
    private static final int ROW_H = 16;
    private static final int ROWS = 7;

    private enum Tab { QUEUE, SKILLS, REPORTS }

    private Tab tab = Tab.QUEUE;
    private CompoundTag parsedFrom;
    private final List<CattoTask> tasks = new ArrayList<>();
    private final List<Catto.Report> reports = new ArrayList<>();
    private CompoundTag data = new CompoundTag();
    private Button pauseButton;
    private Button stayButton;

    public CattoScreen(CattoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_X + PANEL_W;
        imageHeight = 222;
        inventoryLabelY = CattoMenu.INVENTORY_Y - 10;
        titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        int px = leftPos + PANEL_X;
        int w = (PANEL_W - 8) / 3;
        addRenderableWidget(Button.builder(Component.translatable("arcane_artifacts.catto.tab.queue"), b -> tab = Tab.QUEUE)
                .bounds(px + 4, topPos + 4, w, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("arcane_artifacts.catto.tab.skills"), b -> tab = Tab.SKILLS)
                .bounds(px + 4 + w, topPos + 4, w, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("arcane_artifacts.catto.tab.reports"), b -> tab = Tab.REPORTS)
                .bounds(px + 4 + 2 * w, topPos + 4, w, 14).build());
        int by = topPos + imageHeight - 36;
        pauseButton = addRenderableWidget(Button.builder(Component.translatable("arcane_artifacts.catto.button.pause"), b -> send(CattoMenu.BUTTON_PAUSE))
                .bounds(px + 4, by, w, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("arcane_artifacts.catto.button.stop"), b -> send(CattoMenu.BUTTON_STOP))
                .bounds(px + 4 + w, by, w, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("arcane_artifacts.catto.button.call"), b -> send(CattoMenu.BUTTON_CALL))
                .bounds(px + 4 + 2 * w, by, w, 16).build());
        stayButton = addRenderableWidget(Button.builder(Component.translatable("arcane_artifacts.catto.button.stay"), b -> send(CattoMenu.BUTTON_STAY))
                .bounds(px + 4, by + 17, w, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("arcane_artifacts.catto.button.clear"), b -> send(CattoMenu.BUTTON_CLEAR))
                .bounds(px + 4 + w, by + 17, w, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("arcane_artifacts.catto.button.give_all"), b -> send(CattoMenu.BUTTON_GIVE_ALL))
                .bounds(px + 4 + 2 * w, by + 17, w, 16).build());
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private void refresh() {
        CompoundTag latest = CattoNetwork.CLIENT_DATA.get(menu.catId);
        if (latest == null || latest == parsedFrom || minecraft == null || minecraft.level == null) {
            return;
        }
        parsedFrom = latest;
        data = latest;
        tasks.clear();
        for (Tag t : latest.getList("Queue", Tag.TAG_COMPOUND)) {
            CattoTask task = CattoTask.load((CompoundTag) t, minecraft.level.registryAccess());
            if (task != null) {
                tasks.add(task);
            }
        }
        reports.clear();
        for (Tag t : latest.getList("Reports", Tag.TAG_COMPOUND)) {
            CompoundTag r = (CompoundTag) t;
            List<String> args = new ArrayList<>();
            for (Tag a : r.getList("Args", Tag.TAG_STRING)) {
                args.add(a.getAsString());
            }
            reports.add(new Catto.Report(r.getString("Key"), args, r.getBoolean("Bad")));
        }
        boolean paused = latest.getBoolean("Paused");
        pauseButton.setMessage(Component.translatable(paused ? "arcane_artifacts.catto.button.resume" : "arcane_artifacts.catto.button.pause"));
        stayButton.setMessage(Component.translatable(latest.getBoolean("Stay") ? "arcane_artifacts.catto.button.follow" : "arcane_artifacts.catto.button.stay"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        refresh();
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (tab == Tab.QUEUE) {
            queueTooltip(g, mouseX, mouseY);
        } else if (tab == Tab.SKILLS) {
            skillTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (menu.getCarried().isEmpty() && hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < Catto.EQUIPMENT_SIZE) {
            String[] keys = {"head", "armor", "bag", "tool", "weapon", "offhand"};
            g.renderTooltip(font, List.of(Component.translatable("arcane_artifacts.catto.slot." + keys[hoveredSlot.index]).getVisualOrderText(),
                    Component.translatable("arcane_artifacts.catto.slot." + keys[hoveredSlot.index] + ".hint").withStyle(ChatFormatting.GRAY).getVisualOrderText()),
                    mouseX, mouseY);
            return;
        }
        super.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        frame(g, x, y, PANEL_X - 2, imageHeight);
        frame(g, x + PANEL_X, y, PANEL_W, imageHeight);
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            slot(g, x + slot.x - 1, y + slot.y - 1);
        }
        // Catto himself between the two equipment columns, like the player in the inventory.
        int ex = x + 26;
        int ey = y + 8;
        g.fill(ex, ey, ex + 52, ey + 54, 0xFF000000);
        g.fill(ex + 1, ey + 1, ex + 51, ey + 53, 0xFF2A2A2A);
        if (menu.cat != null) {
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(g, ex + 1, ey + 1, ex + 51, ey + 53,
                    24, 0.0625F, mouseX, mouseY, menu.cat);
        }
        // Locked bag slots: shown as shadows so the player sees what a bigger bag would give.
        int capacity = data.contains("Capacity") ? data.getInt("Capacity") : Catto.MOUTH_SLOTS;
        for (int i = capacity; i < Catto.BAG_SIZE; i++) {
            int sx = x + 7 + (i % 9) * 18;
            int sy = y + CattoMenu.BAG_Y - 1 + (i / 9) * 18;
            g.fill(sx, sy, sx + 18, sy + 18, 0x30000000);
        }
        int px = x + PANEL_X;
        g.fill(px + 4, y + CONTENT_Y - 2, px + PANEL_W - 4, y + CONTENT_Y - 1, PURPLE);
        switch (tab) {
            case QUEUE -> drawQueue(g, px + 4, y + CONTENT_Y, mouseX, mouseY);
            case SKILLS -> drawSkills(g, px + 4, y + CONTENT_Y);
            case REPORTS -> drawReports(g, px + 4, y + CONTENT_Y);
        }
        drawStatus(g, px + 4, y + imageHeight - 58);
    }

    /** A panel with the raised edges of vanilla containers. */
    private static void frame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, WHITE);
        g.fill(x + 3, y + 3, x + w - 1, y + h - 1, PARCHMENT_DARK);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, PARCHMENT);
    }

    private static void slot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, SLOT_DARK);
        g.fill(x + 1, y + 1, x + 18, y + 18, WHITE);
        g.fill(x + 1, y + 1, x + 17, y + 17, SLOT);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, INK, false);
        int capacity = data.contains("Capacity") ? data.getInt("Capacity") : Catto.MOUTH_SLOTS;
        g.drawString(font, Component.translatable("arcane_artifacts.catto.bag_label", capacity), 8, CattoMenu.BAG_Y - 10, INK, false);
        // Name, nature and the numbers that matter, to the right of the second column.
        int ix = 102;
        g.drawString(font, font.plainSubstrByWidth(title.getString(), 70), ix, 8, PURPLE, false);
        String nature = data.contains("Nature") ? Component.translatable("arcane_artifacts.catto.nature."
                + data.getString("Nature").toLowerCase(java.util.Locale.ROOT)).getString() : "";
        g.drawString(font, font.plainSubstrByWidth(nature, 70), ix, 19, INK_SOFT, false);
        g.drawString(font, Component.translatable("arcane_artifacts.catto.info.armor", data.getInt("Armor")), ix, 31, INK, false);
        g.drawString(font, Component.translatable("arcane_artifacts.catto.info.bond", data.getInt("Bond")), ix, 41, INK, false);
        int max = Math.max(1, data.getInt("MaxSource"));
        g.drawString(font, Component.translatable("arcane_artifacts.catto.info.mana", data.getInt("Source"), max), ix, 51, 0xFF6A3A9A, false);
        g.fill(ix, 61, ix + 66, 64, 0xFF555555);
        g.fill(ix, 61, ix + Math.round(66 * Math.min(1.0F, data.getInt("Source") / (float) max)), 64, PURPLE_LIGHT);
        if (data.contains("Set")) {
            g.drawString(font, font.plainSubstrByWidth(Component.translatable("arcane_artifacts.catto.set."
                    + data.getString("Set").toLowerCase(java.util.Locale.ROOT)).getString(), 70), ix, 66, 0xFF2A7A7A, false);
        }
    }

    // ------------------------------------------------------------------ queue

    private void drawQueue(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        if (tasks.isEmpty()) {
            for (FormattedCharSequence line : font.split(Component.translatable("arcane_artifacts.catto.queue.empty"), PANEL_W - 10)) {
                g.drawString(font, line, x + 1, y + 2, INK_SOFT, false);
                y += 10;
            }
            return;
        }
        int cap = data.getInt("QueueCap");
        for (int i = 0; i < Math.min(ROWS, tasks.size()); i++) {
            CattoTask task = tasks.get(i);
            int ry = y + i * ROW_H;
            if (i == 0) {
                g.fill(x, ry, x + PANEL_W - 8, ry + ROW_H, 0x30A070D0);
            }
            g.renderItem(task.icon(), x, ry);
            String text = font.plainSubstrByWidth(task.describe().getString(), PANEL_W - 8 - 18 - 44);
            g.drawString(font, text, x + 18, ry + 4, i == 0 ? PURPLE : INK, false);
            int bx = x + PANEL_W - 8 - 44;
            String[] marks = {"↑", "+", "-", "✕"};
            for (int b = 0; b < 4; b++) {
                int cx = bx + b * 11;
                boolean hover = mouseX >= cx && mouseX < cx + 10 && mouseY >= ry + 3 && mouseY < ry + 13;
                g.fill(cx, ry + 3, cx + 10, ry + 13, hover ? PURPLE_LIGHT : PURPLE);
                g.drawCenteredString(font, marks[b], cx + 5, ry + 4, b == 3 ? 0xFFFFB0A0 : 0xFFFFFFFF);
            }
        }
        String counter = tasks.size() + "/" + cap + (tasks.size() > ROWS ? "  +" + (tasks.size() - ROWS) : "");
        g.drawString(font, counter, x + PANEL_W - 8 - font.width(counter), y + ROWS * ROW_H + 1, INK_SOFT, false);
    }

    private void queueTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos + PANEL_X + 4;
        int y = topPos + CONTENT_Y;
        for (int i = 0; i < Math.min(ROWS, tasks.size()); i++) {
            int ry = y + i * ROW_H;
            if (mouseX >= x && mouseX < x + PANEL_W - 8 - 44 && mouseY >= ry && mouseY < ry + ROW_H) {
                CattoTask task = tasks.get(i);
                List<Component> lines = new ArrayList<>();
                lines.add(task.describe());
                lines.add(Component.translatable("arcane_artifacts.catto.task." + task.type.key() + ".desc").withStyle(ChatFormatting.GRAY));
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == Tab.QUEUE && button == 0) {
            int x = leftPos + PANEL_X + 4;
            int y = topPos + CONTENT_Y;
            int bx = x + PANEL_W - 8 - 44;
            int[] actions = {CattoMenu.ROW_UP, CattoMenu.ROW_MORE, CattoMenu.ROW_LESS, CattoMenu.ROW_REMOVE};
            for (int i = 0; i < Math.min(ROWS, tasks.size()); i++) {
                int ry = y + i * ROW_H;
                for (int b = 0; b < 4; b++) {
                    int cx = bx + b * 11;
                    if (mouseX >= cx && mouseX < cx + 10 && mouseY >= ry + 3 && mouseY < ry + 13) {
                        send(actions[b] * CattoMenu.ROW_STRIDE + i);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ------------------------------------------------------------------ skills

    private void drawSkills(GuiGraphics g, int x, int y) {
        int[] xp = data.getIntArray("Xp");
        for (CattoSkill skill : CattoSkill.values()) {
            int value = skill.ordinal() < xp.length ? xp[skill.ordinal()] : 0;
            int tier = CattoSkill.tier(value);
            String roman = tier == 3 ? "III" : tier == 2 ? "II" : "I";
            g.drawString(font, skill.title().getString() + " " + roman, x + 1, y, PURPLE, false);
            int barX = x + 92;
            int barW = PANEL_W - 8 - 92;
            int lo = tier == 3 ? CattoSkill.TIER_3 : tier == 2 ? CattoSkill.TIER_2 : 0;
            int hi = tier == 3 ? CattoSkill.TIER_3 : tier == 2 ? CattoSkill.TIER_3 : CattoSkill.TIER_2;
            float part = tier == 3 ? 1.0F : (value - lo) / (float) Math.max(1, hi - lo);
            g.fill(barX, y + 2, barX + barW, y + 6, 0xFFB8A27A);
            g.fill(barX, y + 2, barX + Math.round(barW * part), y + 6, PURPLE_LIGHT);
            String perk = font.plainSubstrByWidth(skill.perk(tier).getString(), PANEL_W - 10);
            g.drawString(font, perk, x + 1, y + 9, INK_SOFT, false);
            y += 19;
        }
    }

    private void skillTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos + PANEL_X + 4;
        int y = topPos + CONTENT_Y;
        int[] xp = data.getIntArray("Xp");
        for (CattoSkill skill : CattoSkill.values()) {
            if (mouseX >= x && mouseX < x + PANEL_W - 8 && mouseY >= y && mouseY < y + 20) {
                int value = skill.ordinal() < xp.length ? xp[skill.ordinal()] : 0;
                List<Component> lines = new ArrayList<>();
                lines.add(skill.title().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
                lines.add(Component.translatable("arcane_artifacts.catto.skill.xp", value, CattoSkill.toNext(value)).withStyle(ChatFormatting.GRAY));
                for (int t = 1; t <= 3; t++) {
                    boolean open = CattoSkill.tier(value) >= t;
                    lines.add(Component.literal((t == 3 ? "III" : t == 2 ? "II" : "I") + ": ").append(skill.perk(t))
                            .withStyle(open ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
                }
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
                return;
            }
            y += 19;
        }
    }

    // ------------------------------------------------------------------ reports and status

    private void drawReports(GuiGraphics g, int x, int y) {
        int bottom = y + ROWS * ROW_H + 8;
        if (reports.isEmpty()) {
            g.drawString(font, Component.translatable("arcane_artifacts.catto.reports.empty"), x + 1, y + 2, INK_SOFT, false);
            return;
        }
        for (Catto.Report report : reports) {
            for (FormattedCharSequence line : font.split(report.text(), PANEL_W - 10)) {
                if (y + 9 > bottom) {
                    return;
                }
                g.drawString(font, line, x + 1, y, report.bad() ? RED : INK, false);
                y += 9;
            }
            y += 2;
        }
    }

    private void drawStatus(GuiGraphics g, int x, int y) {
        Catto cat = menu.cat;
        if (cat != null) {
            String status = font.plainSubstrByWidth(cat.status().getString(), PANEL_W - 10);
            g.drawString(font, status, x + 1, y, cat.alert() ? RED : PURPLE, false);
        }
        int source = data.getInt("Source");
        int max = Math.max(1, data.getInt("MaxSource"));
        String line = Component.translatable("arcane_artifacts.catto.status_line", source, max, data.getInt("Bond"), data.getInt("Storages")).getString();
        g.drawString(font, font.plainSubstrByWidth(line, PANEL_W - 10), x + 1, y + 10, INK_SOFT, false);
        int barW = PANEL_W - 10;
        g.fill(x + 1, y + 19, x + 1 + barW, y + 21, 0xFFB8A27A);
        g.fill(x + 1, y + 19, x + 1 + Math.round(barW * Math.min(1.0F, source / (float) max)), y + 21, PURPLE_LIGHT);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return mouseX < left || mouseY < top || mouseX >= left + imageWidth || mouseY >= top + imageHeight;
    }
}
