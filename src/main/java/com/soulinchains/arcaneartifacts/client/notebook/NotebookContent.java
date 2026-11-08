package com.soulinchains.arcaneartifacts.client.notebook;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.client.docs.ArcaneDocumentation;
import com.soulinchains.arcaneartifacts.glyph.ArcaneGlyphs;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The chapters of the Researcher's Secret Notebook. All text comes from the lang files:
 * {@code arcane_artifacts.pageN.<chapter>}, {@code arcane_artifacts.lore.<item>},
 * {@code arcane_artifacts.effect_doc.<effect>}, {@code arcane_artifacts.glyph_doc.<glyph>}.
 * Glyph chapters stay unreadable ("???") until the glyph's research is formulated.
 */
final class NotebookContent {
    private NotebookContent() {
    }

    private record Paragraph(Component text, int color) {
    }

    static List<NotebookEntry> build(Font font) {
        List<NotebookEntry> entries = new ArrayList<>();
        entries.add(textEntry(font, Component.translatable("arcane_artifacts.page.overview"),
                new ItemStack(ModItems.RESEARCHER_NOTEBOOK.get()), "overview", 3, null));
        entries.add(textEntry(font, Component.translatable("arcane_artifacts.page.research_guide"),
                new ItemStack(ModItems.OLD_PARCHMENT.get()), "research_guide", 3, null));

        entries.add(itemEntry(font, ModItems.RESEARCH_TABLE.get(), 2));
        entries.add(itemEntry(font, ModItems.OLD_PARCHMENT.get(), 1));
        entries.add(itemEntry(font, ModItems.RESEARCHER_NOTEBOOK.get(), 1));
        entries.add(itemEntry(font, ModItems.ROSE_GOLD_INGOT.get(), 1));
        entries.add(itemEntry(font, ModItems.WREATH_OF_MANIPULATION.get(), 4));
        entries.add(itemEntry(font, ModItems.FOCUSING_BOOTS.get(), 3));
        entries.add(itemEntry(font, ModItems.FOCUSING_GLOVES.get(), 3));
        entries.add(itemEntry(font, ModItems.RING_OF_REVERSAL.get(), 2));
        entries.add(itemEntry(font, ModItems.MANTLE_OF_REFLECTION.get(), 2));

        List<Paragraph> effects = new ArrayList<>();
        effects.add(new Paragraph(Component.translatable("arcane_artifacts.page1.status_effects"), ArcaneNotebookScreen.INK_COLOR));
        for (String effect : new String[]{"arcane_mark", "parasite_host", "parasitic_bond", "parasitic_recovery", "arcane_adaptation"}) {
            effects.add(new Paragraph(Component.translatable("effect.arcane_artifacts." + effect).withStyle(ChatFormatting.BOLD), ArcaneNotebookScreen.TITLE_COLOR));
            effects.add(new Paragraph(Component.translatable("arcane_artifacts.effect_doc." + effect), ArcaneNotebookScreen.INK_COLOR));
        }
        entries.add(new NotebookEntry(Component.translatable("arcane_artifacts.page.status_effects"),
                new ItemStack(ModItems.WREATH_OF_MANIPULATION.get()), paginate(font, Component.translatable("arcane_artifacts.page.status_effects"), effects)));

        Set<ResourceLocation> locked = ArcaneDocumentation.lockedGlyphs();
        for (AbstractSpellPart part : ArcaneGlyphs.ALL) {
            entries.add(glyphEntry(font, part, locked.contains(part.getRegistryName())));
        }
        return entries;
    }

    private static NotebookEntry textEntry(Font font, Component title, ItemStack icon, String key, int pages, @Nullable ResourceLocation recipe) {
        List<Paragraph> paragraphs = new ArrayList<>();
        for (int i = 1; i <= pages; i++) {
            paragraphs.add(new Paragraph(Component.translatable("arcane_artifacts.page" + i + "." + key), ArcaneNotebookScreen.INK_COLOR));
        }
        List<NotebookPage> result = paginate(font, title, paragraphs);
        if (recipe != null) {
            result.add(new NotebookPage.Recipe(recipe));
        }
        return new NotebookEntry(title, icon, result);
    }

    /** Description, the wizard's remark (lore) and the recipe, which always starts on its own page. */
    private static NotebookEntry itemEntry(Font font, ItemLike item, int pages) {
        ItemStack stack = new ItemStack(item);
        String path = BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
        List<Paragraph> paragraphs = new ArrayList<>();
        for (int i = 1; i <= pages; i++) {
            paragraphs.add(new Paragraph(Component.translatable("arcane_artifacts.page" + i + "." + path), ArcaneNotebookScreen.INK_COLOR));
        }
        paragraphs.add(new Paragraph(Component.translatable("arcane_artifacts.lore." + path).withStyle(ChatFormatting.ITALIC), ArcaneNotebookScreen.LORE_COLOR));
        List<NotebookPage> result = paginate(font, stack.getHoverName(), paragraphs);
        result.add(new NotebookPage.Recipe(ArcaneArtifacts.id(path)));
        return new NotebookEntry(stack.getHoverName(), stack, result);
    }

    private static NotebookEntry glyphEntry(Font font, AbstractSpellPart part, boolean locked) {
        ResourceLocation id = part.getRegistryName();
        if (locked) {
            Component mystery = Component.literal("???");
            ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ars_nouveau:blank_glyph")));
            List<Paragraph> text = List.of(new Paragraph(
                    Component.translatable("arcane_artifacts.book.locked_glyph", ArcaneDocumentation.researchFor(id)), ArcaneNotebookScreen.LORE_COLOR));
            return new NotebookEntry(mystery, icon, paginate(font, mystery, text));
        }
        ItemStack glyph = new ItemStack(part.getGlyph());
        List<Paragraph> paragraphs = List.of(
                new Paragraph(part.getBookDescLang(), ArcaneNotebookScreen.INK_COLOR),
                new Paragraph(Component.translatable("arcane_artifacts.glyph_doc." + id.getPath()), ArcaneNotebookScreen.INK_COLOR));
        List<NotebookPage> pages = paginate(font, glyph.getHoverName(), paragraphs);
        pages.add(new NotebookPage.Recipe(ArcaneArtifacts.id(id.getPath())));
        return new NotebookEntry(glyph.getHoverName(), glyph, pages);
    }

    /** Wraps the paragraphs to the page width and splits them into pages; the first page carries the title. */
    private static List<NotebookPage> paginate(Font font, Component title, List<Paragraph> paragraphs) {
        List<NotebookPage.Line> lines = new ArrayList<>();
        for (Paragraph paragraph : paragraphs) {
            for (String block : paragraph.text().getString().split("\n", -1)) {
                Component styled = Component.literal(block).withStyle(paragraph.text().getStyle());
                List<FormattedCharSequence> wrapped = font.split(styled, ArcaneNotebookScreen.PAGE_WIDTH);
                if (wrapped.isEmpty()) {
                    lines.add(new NotebookPage.Line(FormattedCharSequence.EMPTY, paragraph.color()));
                }
                for (FormattedCharSequence line : wrapped) {
                    lines.add(new NotebookPage.Line(line, paragraph.color()));
                }
            }
            lines.add(new NotebookPage.Line(FormattedCharSequence.EMPTY, paragraph.color()));
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).text() == FormattedCharSequence.EMPTY) {
            lines.remove(lines.size() - 1);
        }

        int perPage = ArcaneNotebookScreen.PAGE_HEIGHT / ArcaneNotebookScreen.LINE_HEIGHT;
        List<NotebookPage> pages = new ArrayList<>();
        int index = 0;
        boolean first = true;
        do {
            int capacity = first ? perPage - ArcaneNotebookScreen.TITLE_LINES : perPage;
            // Do not start a page with an empty line.
            while (!first && index < lines.size() && lines.get(index).text() == FormattedCharSequence.EMPTY) {
                index++;
            }
            int end = Math.min(lines.size(), index + capacity);
            pages.add(new NotebookPage.Text(first ? title : null, new ArrayList<>(lines.subList(index, end))));
            index = end;
            first = false;
        } while (index < lines.size());
        return pages;
    }
}
