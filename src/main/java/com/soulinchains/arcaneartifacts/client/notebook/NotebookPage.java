package com.soulinchains.arcaneartifacts.client.notebook;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A single page of the notebook (one half of an open spread). */
public sealed interface NotebookPage {
    void render(GuiGraphics graphics, ArcaneNotebookScreen screen, int x, int y, int mouseX, int mouseY);

    /**
     * One wrapped line of text. Justified lines are stretched to the full page width (like a printed
     * book); the last line of a paragraph and lines that would get huge gaps stay left-aligned.
     */
    record Line(String text, Style style, int color, boolean justify) {
        static final Line EMPTY = new Line("", Style.EMPTY, 0, false);

        void render(GuiGraphics graphics, Font font, int x, int y) {
            if (text.isEmpty()) {
                return;
            }
            String[] words = text.split(" ");
            Component whole = Component.literal(text).withStyle(style);
            if (font.width(whole) > ArcaneNotebookScreen.PAGE_WIDTH) {
                ArcaneNotebookScreen.drawFitted(graphics, font, whole, x, y, ArcaneNotebookScreen.PAGE_WIDTH, color, false);
                return;
            }
            if (!justify || words.length < 2) {
                graphics.drawString(font, whole, x, y, color, false);
                return;
            }
            int total = 0;
            int[] widths = new int[words.length];
            for (int i = 0; i < words.length; i++) {
                widths[i] = font.width(Component.literal(words[i]).withStyle(style));
                total += widths[i];
            }
            float gap = (ArcaneNotebookScreen.PAGE_WIDTH - total) / (float) (words.length - 1);
            if (gap > font.width(" ") * 4) {
                graphics.drawString(font, Component.literal(text).withStyle(style), x, y, color, false);
                return;
            }
            float cursor = x;
            for (int i = 0; i < words.length; i++) {
                graphics.drawString(font, Component.literal(words[i]).withStyle(style), Math.round(cursor), y, color, false);
                cursor += widths[i] + gap;
            }
        }
    }

    /** Wrapped text; the first page of an entry also carries the entry title. */
    record Text(@Nullable Component title, List<Line> lines) implements NotebookPage {
        @Override
        public void render(GuiGraphics graphics, ArcaneNotebookScreen screen, int x, int y, int mouseX, int mouseY) {
            int lineY = y;
            if (title != null) {
                screen.drawCentered(graphics, title, x, lineY, ArcaneNotebookScreen.TITLE_COLOR);
                graphics.fill(x + 10, lineY + 11, x + ArcaneNotebookScreen.PAGE_WIDTH - 10, lineY + 12, 0x40_5B2A86);
                lineY += ArcaneNotebookScreen.TITLE_LINES * ArcaneNotebookScreen.LINE_HEIGHT;
            }
            for (Line line : lines) {
                line.render(graphics, screen.font(), x, lineY);
                lineY += ArcaneNotebookScreen.LINE_HEIGHT;
            }
        }
    }

    /** A recipe looked up by id in the client's recipe manager and drawn like the item's crafting station. */
    record Recipe(ResourceLocation recipeId) implements NotebookPage {
        @Override
        public void render(GuiGraphics graphics, ArcaneNotebookScreen screen, int x, int y, int mouseX, int mouseY) {
            RecipeRenderer.render(graphics, screen, recipeId, x, y, mouseX, mouseY);
        }
    }
}
