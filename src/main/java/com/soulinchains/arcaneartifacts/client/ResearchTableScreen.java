package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.block.ResearchTableMenu;
import com.soulinchains.arcaneartifacts.research.ClientResearch;
import com.soulinchains.arcaneartifacts.research.NoteCategory;
import com.soulinchains.arcaneartifacts.research.PlayerResearch;
import com.soulinchains.arcaneartifacts.research.ResearchDefinition;
import com.soulinchains.arcaneartifacts.research.ResearchManager;
import com.soulinchains.arcaneartifacts.research.ResearchService;
import com.soulinchains.arcaneartifacts.research.ResearchState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Research Table interface drawn as an open book (left page: the sample, its observed properties and
 * the researches it belongs to; right page: the selected research, its experiments, the principle and
 * the notes). The client only displays: Observe and Formulate are menu button clicks validated by the
 * server.
 * <p>
 * Texture: {@code textures/gui/research_table.png} (512x256, the book occupies 352x236 at 0,0). It can be
 * replaced by a hand-painted one of the same layout.
 */
public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final ResourceLocation TEXTURE = ArcaneArtifacts.id("textures/gui/research_table.png");
    private static final int INK = 0x3B2A1A;
    private static final int FADED = 0x8A7458;
    private static final int RIGHT_X = 186;
    private static final int PAGE_WIDTH = 150;

    private int selected;
    private Item lastSample;
    private PageButton observe, formulate, previous, next;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 352;
        imageHeight = 236;
    }

    @Override
    protected void init() {
        super.init();
        observe = addRenderableWidget(new PageButton(leftPos + 50, topPos + 88, 80, 12,
                Component.translatable("arcane_artifacts.research_table.observe"),
                () -> click(ResearchTableMenu.BUTTON_OBSERVE)));
        formulate = addRenderableWidget(new PageButton(leftPos + RIGHT_X + 25, topPos + 200, 100, 12,
                Component.translatable("arcane_artifacts.research_table.formulate"),
                () -> click(ResearchTableMenu.BUTTON_FORMULATE + selected)));
        previous = addRenderableWidget(new PageButton(leftPos + RIGHT_X, topPos + 8, 12, 12, Component.literal("<"), () -> move(-1)));
        next = addRenderableWidget(new PageButton(leftPos + RIGHT_X + PAGE_WIDTH - 12, topPos + 8, 12, 12, Component.literal(">"), () -> move(1)));
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private void move(int delta) {
        int size = ResearchTableMenu.researchOrder().size();
        if (size > 0) {
            selected = Math.floorMod(selected + delta, size);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack sample = menu.sample();
        Item current = sample.isEmpty() ? null : sample.getItem();
        if (current != lastSample) {
            lastSample = current;
            List<ResourceLocation> relevant = ResearchService.researchesFor(ClientResearch.progress(), sample);
            if (!relevant.isEmpty()) {
                selected = Math.max(0, ResearchTableMenu.researchOrder().indexOf(relevant.get(0)));
            }
        }
        List<ResourceLocation> order = ResearchTableMenu.researchOrder();
        ResourceLocation id = selected < order.size() ? order.get(selected) : null;
        observe.active = !sample.isEmpty();
        formulate.visible = id != null && stateOf(ClientResearch.progress(), id) == ResearchState.PRINCIPLE;
        formulate.active = id != null && ResearchService.canFormulate(ClientResearch.progress(), id);
        previous.visible = next.visible = order.size() > 1;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 512, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        PlayerResearch data = ClientResearch.progress();
        renderLeftPage(graphics, data);
        renderRightPage(graphics, data);
    }

    // ------------------------------------------------------------------ left page

    private void renderLeftPage(GuiGraphics g, PlayerResearch data) {
        g.drawString(font, Component.translatable("block.arcane_artifacts.research_table"), 16, 10, INK, false);
        ItemStack sample = menu.sample();
        int y = 104;
        if (sample.isEmpty()) {
            y = paragraph(g, Component.translatable("arcane_artifacts.research_table.place_sample"), 12, y, 156, FADED, 4);
            return;
        }
        g.drawString(font, sample.getHoverName(), 12, y, INK, false);
        y += 11;
        List<ResourceLocation> relevant = ResearchService.researchesFor(data, sample);
        if (relevant.isEmpty()) {
            paragraph(g, Component.translatable("arcane_artifacts.research_table.nothing"), 12, y, 156, FADED, 4);
            return;
        }
        Set<String> properties = new LinkedHashSet<>();
        boolean anyObserved = false;
        for (ResourceLocation id : relevant) {
            if (stateOf(data, id).atLeast(ResearchState.INVESTIGATING)) {
                anyObserved = true;
                properties.addAll(ResearchManager.get(id).properties());
            }
        }
        if (!anyObserved) {
            y = paragraph(g, Component.translatable("arcane_artifacts.research_table.unobserved"), 12, y, 156, FADED, 2);
        } else {
            for (String property : properties) {
                if (y > 130) break;
                g.drawString(font, Component.literal("• ").append(Component.translatable("arcane_artifacts.research_property." + property)), 12, y, INK, false);
                y += 9;
            }
        }
        for (ResourceLocation id : relevant) {
            if (y > 130) break;
            ResearchState state = stateOf(data, id);
            g.drawString(font, researchTitle(id, state).copy().withStyle(ChatFormatting.ITALIC), 12, y, stateColor(state), false);
            y += 9;
        }
    }

    // ------------------------------------------------------------------ right page

    private void renderRightPage(GuiGraphics g, PlayerResearch data) {
        List<ResourceLocation> order = ResearchTableMenu.researchOrder();
        int x = RIGHT_X;
        if (order.isEmpty()) {
            paragraph(g, Component.translatable("arcane_artifacts.research_table.no_research"), x, 30, PAGE_WIDTH, FADED, 4);
            return;
        }
        selected = Math.min(selected, order.size() - 1);
        ResourceLocation id = order.get(selected);
        ResearchDefinition def = ResearchManager.get(id);
        ResearchState state = stateOf(data, id);
        String prefix = ResearchDefinition.langPrefix(id);

        Component title = researchTitle(id, state);
        g.drawString(font, title, x + PAGE_WIDTH / 2 - font.width(title) / 2, 10, INK, false);
        g.renderItem(new ItemStack(def.icon()), x + PAGE_WIDTH / 2 - 8, 22);
        Component stateLine = Component.translatable(state.translationKey())
                .append(Component.literal("  " + (selected + 1) + "/" + order.size()));
        g.drawString(font, stateLine, x + PAGE_WIDTH / 2 - font.width(stateLine) / 2, 41, stateColor(state), false);

        int y = 53;
        if (state == ResearchState.UNKNOWN) {
            y = paragraph(g, Component.translatable("arcane_artifacts.research_table.unknown"), x, y, PAGE_WIDTH, FADED, 3);
            for (ResourceLocation required : def.requires()) {
                if (y > 180) break;
                ResearchState requiredState = stateOf(data, required);
                int color = requiredState.atLeast(ResearchState.PRINCIPLE) ? 0x2E6B3A : FADED;
                y = paragraph(g, Component.translatable("arcane_artifacts.research_table.needs_research",
                        researchTitle(required, requiredState)), x, y, PAGE_WIDTH, color, 2);
            }
            var knows = ResearchService.glyphKnowledge(minecraft == null ? null : minecraft.player);
            for (ResourceLocation glyph : def.requiresGlyphs()) {
                if (y > 180) break;
                int color = knows.test(glyph) ? 0x2E6B3A : FADED;
                y = paragraph(g, Component.translatable("arcane_artifacts.research_table.needs_glyph", glyphName(glyph)), x, y, PAGE_WIDTH, color, 2);
            }
        } else {
            y = paragraph(g, Component.translatable(prefix + ".description"), x, y, PAGE_WIDTH, INK, 3) + 1;
            if (!def.unlocks().isEmpty() || !def.unlocksItems().isEmpty()) {
                y = paragraph(g, unlocksLine(def), x, y, PAGE_WIDTH, 0xA87A1C, 2) + 1;
            }
            if (state == ResearchState.DISCOVERED) {
                y = paragraph(g, Component.translatable("arcane_artifacts.research_table.observe_hint"), x, y, PAGE_WIDTH, FADED, 3);
            } else {
                PlayerResearch.Entry entry = data.entry(id);
                for (ResearchDefinition.ExperimentDefinition experiment : def.experiments()) {
                    if (y > 170) break;
                    boolean done = entry != null && entry.done().contains(experiment.id());
                    MutableComponent line = Component.literal(done ? "✔ " : "○ ")
                            .append(Component.translatable(prefix + ".experiment." + experiment.id()));
                    y = paragraph(g, line, x, y, PAGE_WIDTH, done ? 0x2E6B3A : INK, 2);
                }
                if (state.atLeast(ResearchState.PRINCIPLE) && y < 170) {
                    y = paragraph(g, Component.translatable(prefix + ".principle").withStyle(ChatFormatting.ITALIC), x, y + 2, PAGE_WIDTH, 0x5B1A8E, 3);
                }
            }
            if (state == ResearchState.PRINCIPLE && !def.notesRequired().isEmpty() && y < 186) {
                MutableComponent need = Component.translatable("arcane_artifacts.research_table.requires");
                for (Map.Entry<NoteCategory, Integer> req : def.notesRequired().entrySet()) {
                    need.append(" ").append(Component.literal(data.notes(req.getKey()) + "/" + req.getValue() + " ")
                            .append(Component.translatable(req.getKey().translationKey())).withColor(req.getKey().color));
                }
                paragraph(g, need, x, Math.max(y, 178), PAGE_WIDTH, INK, 2);
            }
        }

        // Notes the player holds, per category.
        int nx = x;
        for (NoteCategory category : NoteCategory.values()) {
            g.fill(nx, 219, nx + 6, 225, 0xFF000000 | category.color);
            String count = String.valueOf(data.notes(category));
            g.drawString(font, count, nx + 8, 218, INK, false);
            nx += 37;
        }
    }

    // ------------------------------------------------------------------ helpers

    private ResearchState stateOf(PlayerResearch data, ResourceLocation id) {
        return ResearchService.state(data, id, ResearchService.glyphKnowledge(minecraft == null ? null : minecraft.player));
    }

    private static Component glyphName(ResourceLocation id) {
        var part = com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.getSpellPart(id);
        return part == null ? Component.literal(id.toString()) : Component.translatable(part.getLocalizationKey());
    }

    /** "Unlocks: Arcane Missile, Pursuit, ..." for glyphs and items of a research. */
    private static Component unlocksLine(ResearchDefinition def) {
        MutableComponent line = Component.translatable("arcane_artifacts.research_table.unlocks");
        boolean first = true;
        for (ResourceLocation glyph : def.unlocks()) {
            line.append(first ? " " : ", ").append(glyphName(glyph));
            first = false;
        }
        for (ResourceLocation item : def.unlocksItems()) {
            Item value = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(item);
            line.append(first ? " " : ", ").append(value.getDescription());
            first = false;
        }
        return line;
    }

    private Component researchTitle(ResourceLocation id, ResearchState state) {
        return state == ResearchState.UNKNOWN ? Component.literal("???") : Component.translatable(ResearchDefinition.langPrefix(id) + ".title");
    }

    private static int stateColor(ResearchState state) {
        return switch (state) {
            case UNKNOWN -> 0x9A8C78;
            case DISCOVERED -> 0x6B4E2A;
            case INVESTIGATING -> 0x1E6E9C;
            case PRINCIPLE -> 0x8A2BD2;
            case UNLOCKED -> 0xA87A1C;
            case RECORDED -> 0x2E6B3A;
        };
    }

    /** Draws wrapped text and returns the next free y. */
    private int paragraph(GuiGraphics g, Component text, int x, int y, int width, int color, int maxLines) {
        List<FormattedCharSequence> lines = new ArrayList<>(font.split(text, width));
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            g.drawString(font, lines.get(i), x, y, color, false);
            y += 9;
        }
        return y;
    }

    /** Text-only button in ink, underlined on hover, faded when inactive. */
    private static final class PageButton extends AbstractButton {
        private final Runnable action;

        PageButton(int x, int y, int width, int height, Component message, Runnable action) {
            super(x, y, width, height, message);
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            int color = !active ? FADED : isHoveredOrFocused() ? 0x8A2BD2 : INK;
            g.fill(getX(), getY(), getX() + width, getY() + height, active && isHoveredOrFocused() ? 0x30B44CF0 : 0x18000000);
            int tx = getX() + width / 2 - font.width(getMessage()) / 2;
            g.drawString(font, getMessage(), tx, getY() + (height - 8) / 2, color, false);
            if (active && isHoveredOrFocused()) {
                g.fill(tx, getY() + height - 2, tx + font.width(getMessage()), getY() + height - 1, 0xFF000000 | color);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
