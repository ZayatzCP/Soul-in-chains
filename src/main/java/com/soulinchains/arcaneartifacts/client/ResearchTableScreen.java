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
    private static final int INK = 0x2A1B10;
    private static final int FADED = 0x6E5A40;
    private static final int RIGHT_X = 186;
    private static final int PAGE_WIDTH = 150;
    private static final int LEFT_WIDTH = 154, LEFT_BOTTOM = 144, RIGHT_BOTTOM = 196;

    private int selected;
    /** Which school is shown: 0 Arcane Mage, 1 Pseudomage, 2 Druid. */
    private int school;
    private static final String[] TABS = {"arcane_artifacts.research_table.tab_arcane", "arcane_artifacts.research_table.tab_pseudo",
            "arcane_artifacts.research_table.tab_druid"};
    private final PageButton[] tabs = new PageButton[3];
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
                () -> {
                    List<ResourceLocation> order = order();
                    if (selected < order.size()) {
                        click(ResearchTableMenu.BUTTON_FORMULATE + ResearchTableMenu.researchOrder().indexOf(order.get(selected)));
                    }
                }));
        for (int i = 0; i < tabs.length; i++) {
            final int tab = i;
            tabs[i] = addRenderableWidget(new PageButton(leftPos + 8 + i * 114, topPos - 14, 110, 13,
                    Component.translatable(TABS[i]), () -> switchTab(tab)));
        }
        previous = addRenderableWidget(new PageButton(leftPos + RIGHT_X, topPos + 8, 12, 12, Component.literal("<"), () -> move(-1)));
        next = addRenderableWidget(new PageButton(leftPos + RIGHT_X + PAGE_WIDTH - 12, topPos + 8, 12, 12, Component.literal(">"), () -> move(1)));
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    /** The researches of the open tab, in display order. */
    private List<ResourceLocation> order() {
        List<ResourceLocation> all = ResearchTableMenu.researchOrder();
        all.removeIf(id -> ResearchDefinition.school(id) != school);
        return all;
    }

    private static Component tabLabel(String key, boolean open) {
        return open ? Component.literal("» ").append(Component.translatable(key)).append(" «") : Component.translatable(key);
    }

    private void switchTab(int tab) {
        if (school != tab) {
            school = tab;
            selected = 0;
        }
    }

    private void move(int delta) {
        int size = order().size();
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
                school = ResearchDefinition.school(relevant.get(0));
                selected = Math.max(0, order().indexOf(relevant.get(0)));
            }
        }
        List<ResourceLocation> order = order();
        // The open tab is marked with arrows.
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setMessage(tabLabel(TABS[i], i == school));
        }
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
            y = paragraph(g, Component.translatable("arcane_artifacts.research_table.place_sample"), 12, y, LEFT_WIDTH, FADED, 4, LEFT_BOTTOM);
            return;
        }
        line(g, sample.getHoverName(), 12, y, LEFT_WIDTH, INK);
        y += 11;
        List<ResourceLocation> relevant = ResearchService.researchesFor(data, sample);
        if (relevant.isEmpty()) {
            paragraph(g, Component.translatable("arcane_artifacts.research_table.nothing"), 12, y, LEFT_WIDTH, FADED, 3, LEFT_BOTTOM);
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
            y = paragraph(g, Component.translatable("arcane_artifacts.research_table.unobserved"), 12, y, LEFT_WIDTH, FADED, 2, LEFT_BOTTOM);
        } else {
            for (String property : properties) {
                if (y + 9 > LEFT_BOTTOM) break;
                line(g, Component.literal("• ").append(Component.translatable("arcane_artifacts.research_property." + property)), 12, y, LEFT_WIDTH, INK);
                y += 9;
            }
        }
        for (ResourceLocation id : relevant) {
            if (y + 9 > LEFT_BOTTOM) break;
            ResearchState state = stateOf(data, id);
            line(g, researchTitle(id, state).copy().withStyle(ChatFormatting.ITALIC), 12, y, LEFT_WIDTH, stateColor(state));
            y += 9;
        }
    }

    // ------------------------------------------------------------------ right page

    private void renderRightPage(GuiGraphics g, PlayerResearch data) {
        List<ResourceLocation> order = order();
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
        centered(g, title, x + 14, 10, PAGE_WIDTH - 28, INK);
        g.renderItem(new ItemStack(def.icon()), x + PAGE_WIDTH / 2 - 8, 22);
        Component stateLine = Component.translatable(state.translationKey())
                .append(Component.literal("  " + (selected + 1) + "/" + order.size()));
        centered(g, stateLine, x, 41, PAGE_WIDTH, stateColor(state));

        int y = 53;
        if (state == ResearchState.UNKNOWN) {
            y = paragraph(g, Component.translatable("arcane_artifacts.research_table.unknown"), x, y, PAGE_WIDTH, FADED, 4);
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
            y = paragraph(g, Component.translatable(prefix + ".description"), x, y, PAGE_WIDTH, INK, 6) + 2;
            if (!def.unlocks().isEmpty() || !def.unlocksItems().isEmpty()) {
                y = paragraph(g, unlocksLine(def), x, y, PAGE_WIDTH, 0x8A5A0C, 3) + 2;
            }
            if (state == ResearchState.DISCOVERED) {
                y = paragraph(g, Component.translatable("arcane_artifacts.research_table.observe_hint"), x, y, PAGE_WIDTH, FADED, 4);
            } else {
                PlayerResearch.Entry entry = data.entry(id);
                java.util.Set<String> doneSet = entry == null ? java.util.Set.of() : entry.done();
                int pending = def.pendingSubject(doneSet);
                for (int index = 0; index < def.experiments().size(); index++) {
                    if (y > 170) break;
                    ResearchDefinition.ExperimentDefinition experiment = def.experiments().get(index);
                    boolean done = doneSet.contains(experiment.id());
                    Component text;
                    int color = done ? 0x2E6B3A : INK;
                    if (done || def.experimentAvailable(doneSet, index)) {
                        text = Component.translatable(prefix + ".experiment." + experiment.id());
                    } else if (index == pending) {
                        // The next sample to study is revealed; the experiment behind it is not yet.
                        text = Component.translatable("arcane_artifacts.research_table.observe_next", subjectName(def.subjects().get(index)));
                        color = 0x1E6E9C;
                    } else {
                        text = Component.translatable("arcane_artifacts.research_table.hidden_experiment");
                        color = FADED;
                    }
                    y = paragraph(g, Component.literal(done ? "✔ " : "○ ").append(text), x, y, PAGE_WIDTH, color, 2);
                }
                if (state.atLeast(ResearchState.PRINCIPLE) && y < 170) {
                    y = paragraph(g, Component.translatable(prefix + ".principle").withStyle(ChatFormatting.ITALIC), x, y + 2, PAGE_WIDTH, 0x5B1A8E, 4);
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

    /** Display name of a research subject ("minecraft:shield" or "#minecraft:arrows"). */
    private static Component subjectName(String subject) {
        if (subject.startsWith("#")) {
            var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse(subject.substring(1)));
            for (var holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                return holder.value().getDescription();
            }
            return Component.literal(subject);
        }
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(subject)).getDescription();
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
            case UNKNOWN -> 0x7A6C58;
            case DISCOVERED -> 0x6B4E2A;
            case INVESTIGATING -> 0x1E6E9C;
            case PRINCIPLE -> 0x8A2BD2;
            case UNLOCKED -> 0x8A5A0C;
            case RECORDED -> 0x2E6B3A;
        };
    }

    /** One line, cut with "..." if it does not fit. */
    private void line(GuiGraphics g, Component text, int x, int y, int width, int color) {
        g.drawString(font, fit(text, width), x, y, color, false);
    }

    private void centered(GuiGraphics g, Component text, int x, int y, int width, int color) {
        FormattedCharSequence fitted = fit(text, width);
        g.drawString(font, fitted, x + (width - font.width(fitted)) / 2, y, color, false);
    }

    private FormattedCharSequence fit(Component text, int width) {
        if (font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        String ellipsis = "...";
        var cut = font.substrByWidth(text, width - font.width(ellipsis));
        return net.minecraft.locale.Language.getInstance().getVisualOrder(
                net.minecraft.network.chat.FormattedText.composite(cut, net.minecraft.network.chat.FormattedText.of(ellipsis)));
    }

    /** Draws wrapped text and returns the next free y. */
    private int paragraph(GuiGraphics g, Component text, int x, int y, int width, int color, int maxLines) {
        return paragraph(g, text, x, y, width, color, maxLines, RIGHT_BOTTOM);
    }

    /** Draws wrapped text (never below {@code bottom}, the last visible line ends with "...") and returns the next free y. */
    private int paragraph(GuiGraphics g, Component text, int x, int y, int width, int color, int maxLines, int bottom) {
        List<FormattedCharSequence> lines = new ArrayList<>(font.split(text, width));
        int fitting = Math.max(0, Math.min(maxLines, (bottom - y) / 9));
        if (lines.size() > fitting && fitting > 0) {
            List<FormattedCharSequence> shown = new ArrayList<>(lines.subList(0, fitting - 1));
            shown.add(fit(Component.literal(lineText(lines.get(fitting - 1)) + " " + lineText(lines.get(Math.min(fitting, lines.size() - 1)))).withStyle(text.getStyle()), width));
            lines = shown;
        }
        for (int i = 0; i < lines.size() && i < fitting; i++) {
            g.drawString(font, lines.get(i), x, y, color, false);
            y += 9;
        }
        return y;
    }

    private static String lineText(FormattedCharSequence sequence) {
        StringBuilder builder = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
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
