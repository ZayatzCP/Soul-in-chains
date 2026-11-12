package com.soulinchains.arcaneartifacts.client.notebook;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The Researcher's Secret Notebook: the addon's own book, independent of the Ars Nouveau Worn Notebook.
 * An index of chapters (two pages per spread), each chapter a series of text and recipe pages.
 * <p>
 * Texture {@code textures/gui/notebook.png} (512×256): the open book at 0,0 (336×210); the slot frame at
 * 0,214 (18×18); next / previous / back buttons at 20,214 / 40,214 / 60,214 (18×10, hovered variants 12 px
 * lower); the recipe arrow at 80,214 (22×15) and the down arrow at 104,214 (9×12).
 */
public class ArcaneNotebookScreen extends Screen {
    static final ResourceLocation TEXTURE = ArcaneArtifacts.id("textures/gui/notebook.png");
    static final int TEX_W = 512, TEX_H = 256;
    static final int BOOK_W = 336, BOOK_H = 210;
    static final int PAGE_WIDTH = 132, PAGE_HEIGHT = 160;
    static final int UI_V = 214;
    static final int LINE_HEIGHT = 10, TITLE_LINES = 2;
    static final int INK_COLOR = 0x3B2A1A, TITLE_COLOR = 0x5B2A86, LORE_COLOR = 0x6B4B2A, HINT_COLOR = 0x7A6A55;
    private static final int LEFT_PAGE_X = 22, RIGHT_PAGE_X = 182, PAGE_Y = 18;
    private static final int ROW = 18, INDEX_LEFT_ROWS = 7, INDEX_RIGHT_ROWS = 8;

    private List<NotebookEntry> entries = List.of();
    private @Nullable NotebookEntry current;
    private int spread;
    private int indexSpread;
    private int left, top;
    private @Nullable ItemStack hovered;

    private final com.soulinchains.arcaneartifacts.accessory.ResearcherNotebookItem.Book book;

    public ArcaneNotebookScreen() {
        this(com.soulinchains.arcaneartifacts.accessory.ResearcherNotebookItem.Book.ARCANE);
    }

    public ArcaneNotebookScreen(com.soulinchains.arcaneartifacts.accessory.ResearcherNotebookItem.Book book) {
        super(Component.translatable(switch (book) {
            case ARCANE -> "item.arcane_artifacts.researcher_notebook";
            case PSEUDOMAGE -> "item.arcane_artifacts.false_mage_guide";
            case DRUID -> "item.arcane_artifacts.druid_herbarium";
        }));
        this.book = book;
    }

    @Override
    protected void init() {
        left = (width - BOOK_W) / 2;
        top = (height - BOOK_H) / 2;
        entries = switch (book) {
            case ARCANE -> NotebookContent.build(font);
            case PSEUDOMAGE -> NotebookContent.buildPseudomage(font);
            case DRUID -> NotebookContent.buildDruid(font);
        };
        if (current != null) {
            // Rebuilt after a resize: reopen the same chapter by title.
            NotebookEntry previous = current;
            current = entries.stream().filter(e -> e.title().getString().equals(previous.title().getString())).findFirst().orElse(null);
        }
    }

    Font font() {
        return font;
    }

    void setHovered(ItemStack stack) {
        hovered = stack;
    }

    void blitSlot(GuiGraphics g, int x, int y) {
        g.blit(TEXTURE, x, y, 0, UI_V, 18, 18, TEX_W, TEX_H);
    }

    /** Centered on the page; text wider than the page is scaled down so it never leaves the paper. */
    void drawCentered(GuiGraphics g, Component text, int pageX, int y, int color) {
        drawFitted(g, font, text, pageX, y, PAGE_WIDTH, color, true);
    }

    /** Draws one line inside {@code width}; if it is wider, it is scaled down (never cut). */
    static void drawFitted(GuiGraphics g, net.minecraft.client.gui.Font font, Component text, int x, int y, int width, int color, boolean centered) {
        int textWidth = font.width(text);
        if (textWidth <= width) {
            g.drawString(font, text, centered ? x + (width - textWidth) / 2 : x, y, color, false);
            return;
        }
        float scale = width / (float) textWidth;
        g.pose().pushPose();
        g.pose().translate(x, y + 4 * (1 - scale), 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        hovered = null;
        g.blit(TEXTURE, left, top, 0, 0, BOOK_W, BOOK_H, TEX_W, TEX_H);
        if (current == null) {
            renderIndex(g, mouseX, mouseY);
        } else {
            renderEntry(g, mouseX, mouseY);
        }
        if (hasPrevious()) {
            button(g, prevX(), mouseX, mouseY, 40);
        }
        if (hasNext()) {
            button(g, nextX(), mouseX, mouseY, 20);
        }
        if (current != null) {
            button(g, backX(), mouseX, mouseY, 60);
        }
        if (hovered != null && !hovered.isEmpty()) {
            g.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
    }

    private void button(GuiGraphics g, int x, int mouseX, int mouseY, int u) {
        int y = buttonY();
        boolean hover = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 10;
        g.blit(TEXTURE, x, y, u, hover ? UI_V + 12 : UI_V, 18, 10, TEX_W, TEX_H);
    }

    // ------------------------------------------------------------------ index
    private void renderIndex(GuiGraphics g, int mouseX, int mouseY) {
        int lx = left + LEFT_PAGE_X;
        if (indexSpread == 0) {
            drawCentered(g, Component.translatable("arcane_artifacts.notebook.title"), lx, top + PAGE_Y + 2, TITLE_COLOR);
            drawCentered(g, Component.translatable("arcane_artifacts.notebook.subtitle").withStyle(net.minecraft.ChatFormatting.ITALIC),
                    lx, top + PAGE_Y + 14, LORE_COLOR);
            g.fill(lx + 10, top + PAGE_Y + 26, lx + PAGE_WIDTH - 10, top + PAGE_Y + 27, 0x40_5B2A86);
        }
        for (int i = 0; i < perIndexSpread(); i++) {
            int index = indexOffset() + i;
            if (index >= entries.size()) {
                break;
            }
            int[] pos = indexRow(i);
            NotebookEntry entry = entries.get(index);
            boolean hover = mouseX >= pos[0] && mouseX < pos[0] + PAGE_WIDTH && mouseY >= pos[1] && mouseY < pos[1] + ROW;
            g.renderItem(entry.icon(), pos[0], pos[1]);
            Component title = entry.title();
            String text = title.getString();
            if (font.width(text) > PAGE_WIDTH - 20) {
                text = font.plainSubstrByWidth(text, PAGE_WIDTH - 20 - font.width("...")).stripTrailing() + "...";
            }
            g.drawString(font, text, pos[0] + 19, pos[1] + 4, hover ? TITLE_COLOR : INK_COLOR, false);
            if (hover) {
                g.fill(pos[0] + 19, pos[1] + 13, pos[0] + 19 + font.width(text), pos[1] + 14, 0x80_5B2A86);
            }
        }
    }

    private int leftRows() {
        return indexSpread == 0 ? INDEX_LEFT_ROWS : INDEX_LEFT_ROWS + 1;
    }

    private int perIndexSpread() {
        return leftRows() + INDEX_RIGHT_ROWS;
    }

    private int indexOffset() {
        int offset = 0;
        for (int s = 0; s < indexSpread; s++) {
            offset += (s == 0 ? INDEX_LEFT_ROWS : INDEX_LEFT_ROWS + 1) + INDEX_RIGHT_ROWS;
        }
        return offset;
    }

    private int[] indexRow(int i) {
        if (i < leftRows()) {
            int startY = indexSpread == 0 ? PAGE_Y + 32 : PAGE_Y;
            return new int[]{left + LEFT_PAGE_X, top + startY + i * ROW};
        }
        return new int[]{left + RIGHT_PAGE_X, top + PAGE_Y + (i - leftRows()) * ROW};
    }

    // ------------------------------------------------------------------ entry
    private void renderEntry(GuiGraphics g, int mouseX, int mouseY) {
        List<NotebookPage> pages = current.pages();
        int first = spread * 2;
        if (first < pages.size()) {
            pages.get(first).render(g, this, left + LEFT_PAGE_X, top + PAGE_Y, mouseX, mouseY);
        }
        if (first + 1 < pages.size()) {
            pages.get(first + 1).render(g, this, left + RIGHT_PAGE_X, top + PAGE_Y, mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------ navigation
    private int buttonY() {
        return top + BOOK_H - 22;
    }

    private int prevX() {
        return left + 26;
    }

    private int nextX() {
        return left + BOOK_W - 44;
    }

    private int backX() {
        return left + BOOK_W / 2 - 9;
    }

    private boolean hasPrevious() {
        return current != null ? spread > 0 : indexSpread > 0;
    }

    private boolean hasNext() {
        if (current != null) {
            return (spread + 1) * 2 < current.pages().size();
        }
        return indexOffset() + perIndexSpread() < entries.size();
    }

    private void turn(int direction) {
        if (current != null) {
            spread += direction;
        } else {
            indexSpread += direction;
        }
        playPageSound();
    }

    private void back() {
        current = null;
        spread = 0;
        playPageSound();
    }

    private void playPageSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        }
    }

    private boolean over(double mouseX, double mouseY, int x) {
        int y = buttonY();
        return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 10;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && current != null) {
            back();
            return true;
        }
        if (button == 0) {
            if (hasPrevious() && over(mouseX, mouseY, prevX())) {
                turn(-1);
                return true;
            }
            if (hasNext() && over(mouseX, mouseY, nextX())) {
                turn(1);
                return true;
            }
            if (current != null && over(mouseX, mouseY, backX())) {
                back();
                return true;
            }
            if (current == null) {
                for (int i = 0; i < perIndexSpread(); i++) {
                    int index = indexOffset() + i;
                    if (index >= entries.size()) {
                        break;
                    }
                    int[] pos = indexRow(i);
                    if (mouseX >= pos[0] && mouseX < pos[0] + PAGE_WIDTH && mouseY >= pos[1] && mouseY < pos[1] + ROW) {
                        current = entries.get(index);
                        spread = 0;
                        playPageSound();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0 && hasPrevious()) {
            turn(-1);
            return true;
        }
        if (scrollY < 0 && hasNext()) {
            turn(1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && current != null) {
            back();
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) && hasPrevious()) {
            turn(-1);
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) && hasNext()) {
            turn(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
