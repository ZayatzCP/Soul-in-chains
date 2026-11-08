package com.soulinchains.arcaneartifacts.client.notebook;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A single page of the notebook (one half of an open spread). */
public sealed interface NotebookPage {
    void render(GuiGraphics graphics, ArcaneNotebookScreen screen, int x, int y, int mouseX, int mouseY);

    /** One wrapped line of text with its colour. */
    record Line(FormattedCharSequence text, int color) {
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
                graphics.drawString(screen.font(), line.text(), x, lineY, line.color(), false);
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
