package com.soulinchains.arcaneartifacts.client.rite;

import com.soulinchains.arcaneartifacts.rite.ChalkColor;
import com.soulinchains.arcaneartifacts.rite.RiteCatalog;
import com.soulinchains.arcaneartifacts.rite.Sigil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The Treatise of Sigils: a few pages on building a site and the roles, then every sigil drawn on its 3x3 grid (two to a
 * page), then every rite the server knows: its four sigils around the altar, the offerings, the cage, the field it needs.
 */
public class RiteCodexScreen extends Screen {
    private static final int W = 300;
    private static final int H = 210;
    private static final int PAPER = 0xFF1D1C22;
    private static final int EDGE = 0xFF34333B;
    private static final int INK = 0xFFE9E6DF;
    private static final int MUTED = 0xFFA29EA6;
    private static final int GRID = 0xFF2F2E35;
    private static final int TITLE = 0xFFB48CF0;

    private interface Page {
        void draw(GuiGraphics g, int x, int y);
    }

    private final List<Page> pages = new ArrayList<>();
    private int page;
    private Button prev;
    private Button next;

    public RiteCodexScreen() {
        super(Component.translatable("item.arcane_artifacts.sigil_codex"));
    }

    @Override
    protected void init() {
        pages.clear();
        pages.add((g, x, y) -> text(g, x, y, "arcane_artifacts.codex.site"));
        pages.add((g, x, y) -> text(g, x, y, "arcane_artifacts.codex.rite"));
        pages.add((g, x, y) -> text(g, x, y, "arcane_artifacts.codex.roles"));
        Sigil[] sigils = Sigil.values();
        for (int i = 0; i < sigils.length; i += 2) {
            Sigil a = sigils[i];
            Sigil b = i + 1 < sigils.length ? sigils[i + 1] : null;
            pages.add((g, x, y) -> {
                sigil(g, x + 10, y, a);
                if (b != null) {
                    sigil(g, x + W / 2 + 4, y, b);
                }
            });
        }
        List<RiteCatalog.Entry> rites = RiteCatalog.client;
        if (rites.isEmpty()) {
            pages.add((g, x, y) -> text(g, x, y, "arcane_artifacts.codex.no_rites"));
        }
        for (RiteCatalog.Entry rite : rites) {
            pages.add((g, x, y) -> rite(g, x, y, rite));
        }
        page = Math.min(page, pages.size() - 1);
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(left + 8, top + H - 24, 20, 18).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(left + W - 28, top + H - 24, 20, 18).build());
        turn(0);
    }

    private void turn(int delta) {
        page = Math.max(0, Math.min(pages.size() - 1, page + delta));
        prev.active = page > 0;
        next.active = page < pages.size() - 1;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        g.drawCenteredString(font, title, width / 2, top + 6, TITLE);
        pages.get(page).draw(g, left, top + 22);
        g.drawCenteredString(font, Component.literal((page + 1) + " / " + pages.size()), width / 2, top + H - 18, MUTED);
    }

    /** The dimmed world and the page itself, under the buttons. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, EDGE);
        g.fill(left, top, left + W, top + H, PAPER);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int wrapped(GuiGraphics g, Component text, int x, int y, int width, int color) {
        for (FormattedCharSequence line : font.split(text, width)) {
            g.drawString(font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private void text(GuiGraphics g, int x, int y, String key) {
        g.drawString(font, Component.translatable(key + ".title"), x + 10, y, TITLE, false);
        wrapped(g, Component.translatable(key + ".text"), x + 10, y + 14, W - 20, INK);
    }

    private static int argb(ChalkColor color) {
        return 0xFF000000 | color.rgb;
    }

    /** A sigil on its grid; {@code cell} pixels a block. */
    private void grid(GuiGraphics g, int x, int y, Sigil sigil, int cell) {
        int color = argb(sigil.color == ChalkColor.COAL ? ChalkColor.WHITE : sigil.color);
        if (sigil.color == ChalkColor.COAL) {
            color = 0xFF8E8A96;
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                g.fill(x + c * cell, y + r * cell, x + (c + 1) * cell - 1, y + (r + 1) * cell - 1, GRID);
            }
        }
        int t = Math.max(1, cell / 6);
        for (int[] line : sigil.lines) {
            int ax = x + (line[0] % 3) * cell + cell / 2;
            int ay = y + (line[0] / 3) * cell + cell / 2;
            int bx = x + (line[1] % 3) * cell + cell / 2;
            int by = y + (line[1] / 3) * cell + cell / 2;
            g.fill(Math.min(ax, bx) - t, Math.min(ay, by) - t, Math.max(ax, bx) + t, Math.max(ay, by) + t, color);
        }
        for (int[] line : sigil.lines) {
            for (int cellIndex : line) {
                int cx = x + (cellIndex % 3) * cell + cell / 2;
                int cy = y + (cellIndex / 3) * cell + cell / 2;
                g.fill(cx - t * 2, cy - t * 2, cx + t * 2, cy + t * 2, color);
            }
        }
    }

    private void sigil(GuiGraphics g, int x, int y, Sigil sigil) {
        grid(g, x, y, sigil, 16);
        int tx = x + 54;
        g.drawString(font, sigil.displayName(), tx, y, INK, false);
        g.drawString(font, Component.translatable("arcane_artifacts.rite.chalk." + sigil.color.id), tx, y + 11, argb(sigil.color == ChalkColor.COAL ? ChalkColor.BONE : sigil.color), false);
        g.drawString(font, Component.translatable(sigil.modifier ? "arcane_artifacts.codex.modifier" : "arcane_artifacts.codex.base"), tx, y + 22, MUTED, false);
        wrapped(g, Component.translatable("arcane_artifacts.rite.sigil." + sigil.id + ".desc"), x, y + 54, W / 2 - 18, INK);
    }

    private void rite(GuiGraphics g, int x, int y, RiteCatalog.Entry rite) {
        g.drawString(font, Component.translatable("arcane_artifacts.rite.name." + rite.id()), x + 10, y, TITLE, false);
        g.drawString(font, Component.translatable("arcane_artifacts.codex.kind." + rite.kind()), x + 10, y + 11, MUTED, false);
        // the four sigils around the altar
        int cell = 9;
        int size = cell * 3;
        int cx = x + 10 + size + 2;
        int cy = y + 26 + size + 2;
        g.fill(cx + 4, cy + 4, cx + size - 4, cy + size - 4, 0xFF7E5BB8);
        drawSmall(g, rite.main(), cx, cy - size - 2, cell);
        drawSmall(g, rite.south(), cx, cy + size + 2, cell);
        drawSmall(g, rite.west(), cx - size - 2, cy, cell);
        drawSmall(g, rite.east(), cx + size + 2, cy, cell);
        g.drawString(font, Component.translatable("arcane_artifacts.codex.north"), cx + size + 4, cy - size, MUTED, false);
        int tx = x + 10 + size * 3 + 16;
        int ty = y + 26;
        int participants = rite.minField() <= 9 ? 2 : rite.minField() <= 15 ? 3 : 4;
        ty = wrapped(g, Component.translatable("arcane_artifacts.codex.field", rite.minField(), rite.minField(), participants), tx, ty, W - (tx - x) - 10, INK);
        ty = wrapped(g, Component.translatable("arcane_artifacts.codex.sigils", name(rite.main()), name(rite.south()), name(rite.west()), name(rite.east())),
                tx, ty + 2, W - (tx - x) - 10, MUTED);
        ty += 4;
        g.drawString(font, Component.translatable("arcane_artifacts.codex.offering"), tx, ty, INK, false);
        ty += 10;
        ty = item(g, tx, ty, rite.offering(), rite.offeringCount());
        int[] extra = rite.extra();
        for (int i = 0; i + 1 < extra.length; i += 2) {
            ty = item(g, tx, ty, extra[i], extra[i + 1]);
        }
        if (rite.cage() > 0) {
            ty = wrapped(g, Component.translatable("arcane_artifacts.codex.cage", rite.cage()), tx, ty + 2, W - (tx - x) - 10, INK);
        }
        wrapped(g, Component.translatable("arcane_artifacts.codex.result." + rite.id()), x + 10, Math.max(ty + 4, cy + size * 2 + 8),
                W - 20, rite.otherworldly() ? 0xFFD0473B : MUTED);
    }

    private static Component name(String sigil) {
        return Component.translatable("arcane_artifacts.rite.sigil." + sigil);
    }

    private void drawSmall(GuiGraphics g, String id, int x, int y, int cell) {
        Sigil sigil = Sigil.byId(id);
        if (sigil != null) {
            grid(g, x, y, sigil, cell);
        }
    }

    private int item(GuiGraphics g, int x, int y, int itemId, int count) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.byId(itemId), Math.max(1, count));
        g.renderItem(stack, x, y);
        g.drawString(font, Component.literal(count + " × ").append(stack.getHoverName()), x + 18, y + 4, INK, false);
        return y + 18;
    }
}
