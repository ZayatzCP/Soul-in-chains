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
import net.minecraft.network.chat.Style;
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
        entries.add(itemEntry(font, ModItems.ROSE_GOLD_NUGGET.get(), 1));
        entries.add(itemEntry(font, ModItems.ROSE_GOLD_BLOCK.get(), 1));
        Set<ResourceLocation> lockedItems = ArcaneDocumentation.lockedItems();
        entries.add(gatedItemEntry(font, ModItems.WREATH_OF_MANIPULATION.get(), 2, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.FOCUSING_BOOTS.get(), 2, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.FOCUSING_GLOVES.get(), 2, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.RING_OF_REVERSAL.get(), 2, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.MANTLE_OF_REFLECTION.get(), 2, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.SPELL_STAFF.get(), 2, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.CRYSTAL_BALL.get(), 2, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.BOTTOMLESS_BELT.get(), 2, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.AMULET_OF_OPPOSITES.get(), 2, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.RING_OF_CALM.get(), 2, lockedItems));
        entries.add(itemEntry(font, ModItems.HOP_SLIPPERS.get(), 1));
        entries.add(gatedItemEntry(font, ModItems.MIRROR_SHIELD.get(), 1, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.FOCUSING_SHIELD.get(), 2, lockedItems));
        entries.add(itemEntry(font, ModItems.ENCHANTED_POISONOUS_POTATO.get(), 1));
        entries.add(itemEntry(font, ModItems.GILDED_PUFFERFISH.get(), 1));
        entries.add(itemEntry(font, ModItems.GILDED_FERMENTED_SPIDER_EYE.get(), 1));
        entries.add(itemEntry(font, ModItems.ALCHEMICAL_DISTILLER.get(), 2));
        entries.add(itemEntry(font, ModItems.MUDZYA_HIVE.get(), 1));
        entries.add(textEntry(font, Component.translatable("arcane_artifacts.page.bottle_of_source"),
                new ItemStack(ModItems.BOTTLE_OF_SOURCE.get()), "bottle_of_source", 1, null));
        entries.add(textEntry(font, Component.translatable("arcane_artifacts.page.mudzya"),
                new ItemStack(ModItems.MUDZYA_SPAWN_EGG.get()), "mudzya", 1, null));
        entries.add(textEntry(font, Component.translatable("arcane_artifacts.page.mushroom_caves"),
                new ItemStack(ModItems.MUSHROOM_MOSS.get()), "mushroom_caves", 2, null));
        entries.add(textEntry(font, Component.translatable("arcane_artifacts.page.mushrooms"),
                new ItemStack(ModItems.MUSHROOMS.get(com.soulinchains.arcaneartifacts.mushroom.ArcaneMushroom.BLUE).get()), "mushrooms", 2, null));
        entries.add(textEntry(font, Component.translatable("arcane_artifacts.page.gribushka"),
                new ItemStack(ModItems.GRIBUSHKA_SPAWN_EGG.get()), "gribushka", 2, null));
        entries.add(gatedItemEntry(font, ModItems.OATH_SCROLL.get(), 2, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.HEART_CHARM.get(), 1, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.DIAMOND_CHARM.get(), 1, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.CLUB_CHARM.get(), 1, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.SPADE_CHARM.get(), 1, lockedItems));

        List<Paragraph> effects = new ArrayList<>();
        effects.add(new Paragraph(Component.translatable("arcane_artifacts.page1.status_effects"), ArcaneNotebookScreen.INK_COLOR));
        for (String effect : new String[]{"battle_rush", "rush_exhaustion", "silenced", "arcane_mark", "parasite_host", "parasitic_bond", "parasitic_recovery", "well_fed", "pity", "bonk", "magic_absorption", "fear", "duel_focus"}) {
            effects.add(new Paragraph(Component.translatable("effect.arcane_artifacts." + effect).withStyle(ChatFormatting.BOLD), ArcaneNotebookScreen.TITLE_COLOR));
            effects.add(new Paragraph(Component.translatable("arcane_artifacts.effect_doc." + effect), ArcaneNotebookScreen.INK_COLOR));
        }
        entries.add(new NotebookEntry(Component.translatable("arcane_artifacts.page.status_effects"),
                new ItemStack(ModItems.WREATH_OF_MANIPULATION.get()), paginate(font, Component.translatable("arcane_artifacts.page.status_effects"), effects)));

        entries.add(textEntry(font, Component.translatable("arcane_artifacts.page.enchantments"),
                new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK), "enchantments", 2, null));

        Set<ResourceLocation> locked = ArcaneDocumentation.lockedGlyphs();
        for (AbstractSpellPart part : ArcaneGlyphs.ALL) {
            entries.add(glyphEntry(font, part, locked.contains(part.getRegistryName())));
        }
        return entries;
    }

    /** The Druid's Herbarium: the druid is still being written; the book says what is coming. */
    static List<NotebookEntry> buildDruid(Font font) {
        List<NotebookEntry> entries = new ArrayList<>();
        entries.add(textEntry(font, Component.translatable("arcane_artifacts.page.druid"),
                new ItemStack(ModItems.DRUID_HERBARIUM.get()), "druid", 2, null));
        entries.add(itemEntry(font, ModItems.DRUID_HERBARIUM.get(), 1));
        return entries;
    }

    /** The False Mage's Guide: everything of the Pseudomage, gated by the Pseudomage researches. */
    static List<NotebookEntry> buildPseudomage(Font font) {
        List<NotebookEntry> entries = new ArrayList<>();
        entries.add(textEntry(font, Component.translatable("arcane_artifacts.page.pseudomage"),
                new ItemStack(ModItems.FALSE_MAGE_GUIDE.get()), "pseudomage", 3, null));
        entries.add(textEntry(font, Component.translatable("arcane_artifacts.page.false_mana"),
                new ItemStack(ModItems.COUNTERFEIT_RING.get()), "false_mana", 2, null));
        entries.add(itemEntry(font, ModItems.FALSE_MAGE_GUIDE.get(), 1));
        Set<ResourceLocation> lockedItems = ArcaneDocumentation.lockedItems();
        entries.add(gatedItemEntry(font, ModItems.REVOLVER.get(), 2, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.SAWN_OFF.get(), 1, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.MAGIC_CARTRIDGE.get(), 1, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.BANDOLIER.get(), 1, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.COUNTERFEIT_RING.get(), 1, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.COUNTERFEIT_PENDANT.get(), 1, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.MANA_GOGGLES.get(), 1, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.PONCHO.get(), 2, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.SOMBRERO.get(), 1, lockedItems));
        entries.add(textEntry(font, Component.translatable("arcane_artifacts.page.amber"),
                new ItemStack(ModItems.AMBER.get()), "amber", 1, null));
        entries.add(gatedItemEntry(font, ModItems.MANA_TANK.get(), 2, lockedItems));
        entries.add(gatedItemEntry(font, ModItems.MANA_PIPE.get(), 1, lockedItems));
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

    /** Like {@link #itemEntry}, but an unreadable "???" page until the item's research is formulated. */
    private static NotebookEntry gatedItemEntry(Font font, ItemLike item, int pages, Set<ResourceLocation> locked) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item.asItem());
        if (!locked.contains(id)) {
            return itemEntry(font, item, pages);
        }
        Component mystery = Component.literal("???");
        List<Paragraph> text = List.of(new Paragraph(
                Component.translatable("arcane_artifacts.book.locked_item", ArcaneDocumentation.researchForItem(id)), ArcaneNotebookScreen.LORE_COLOR));
        return new NotebookEntry(mystery, new ItemStack(net.minecraft.world.item.Items.PAPER), paginate(font, mystery, text));
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
                new Paragraph(Component.translatable("arcane_artifacts.glyph_doc." + id.getPath()), ArcaneNotebookScreen.INK_COLOR),
                new Paragraph(Component.translatable("arcane_artifacts.glyph_joke." + id.getPath()).withStyle(ChatFormatting.ITALIC), ArcaneNotebookScreen.LORE_COLOR));
        paragraphs = new ArrayList<>(paragraphs);
        paragraphs.addAll(augmentParagraphs(part));
        List<NotebookPage> pages = paginate(font, glyph.getHoverName(), paragraphs);
        pages.add(new NotebookPage.Recipe(ArcaneArtifacts.id(id.getPath())));
        return new NotebookEntry(glyph.getHoverName(), glyph, pages);
    }

    /** "Augments:" followed by every augment the glyph accepts and what it does to this glyph. */
    private static List<Paragraph> augmentParagraphs(AbstractSpellPart part) {
        List<Paragraph> result = new ArrayList<>();
        var augments = new ArrayList<>(part.compatibleAugments);
        if (augments.isEmpty()) {
            return result;
        }
        augments.sort(java.util.Comparator.comparing(a -> a.getRegistryName().toString()));
        result.add(new Paragraph(Component.translatable("arcane_artifacts.notebook.augments").withStyle(ChatFormatting.BOLD), ArcaneNotebookScreen.TITLE_COLOR));
        for (var augment : augments) {
            String key = "ars_nouveau.augment_desc." + part.getRegistryName().getPath() + "_" + augment.getRegistryName().getPath();
            Component desc = net.minecraft.client.resources.language.I18n.exists(key)
                    ? Component.translatable(key) : augment.getBookDescLang();
            result.add(new Paragraph(Component.literal("• ").append(Component.translatable(augment.getLocalizationKey()).withStyle(ChatFormatting.BOLD))
                    .append(" — ").append(desc), ArcaneNotebookScreen.INK_COLOR));
        }
        return result;
    }

    /** Wraps the paragraphs to the page width (word by word, justified) and splits them into pages; the first page carries the title. */
    private static List<NotebookPage> paginate(Font font, Component title, List<Paragraph> paragraphs) {
        List<NotebookPage.Line> lines = new ArrayList<>();
        for (Paragraph paragraph : paragraphs) {
            Style style = paragraph.text().getStyle();
            for (String block : paragraph.text().getString().split("\n", -1)) {
                wrap(font, block, style, paragraph.color(), lines);
            }
            lines.add(NotebookPage.Line.EMPTY);
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).text().isEmpty()) {
            lines.remove(lines.size() - 1);
        }

        int perPage = ArcaneNotebookScreen.PAGE_HEIGHT / ArcaneNotebookScreen.LINE_HEIGHT;
        List<NotebookPage> pages = new ArrayList<>();
        int index = 0;
        boolean first = true;
        do {
            int capacity = first ? perPage - ArcaneNotebookScreen.TITLE_LINES : perPage;
            // Do not start a page with an empty line.
            while (!first && index < lines.size() && lines.get(index).text().isEmpty()) {
                index++;
            }
            int end = Math.min(lines.size(), index + capacity);
            pages.add(new NotebookPage.Text(first ? title : null, new ArrayList<>(lines.subList(index, end))));
            index = end;
            first = false;
        } while (index < lines.size());
        return pages;
    }

    /** Greedy word wrap; every line but the last of the block is justified. */
    private static void wrap(Font font, String block, Style style, int color, List<NotebookPage.Line> out) {
        if (block.isBlank()) {
            out.add(NotebookPage.Line.EMPTY);
            return;
        }
        String[] words = block.trim().split(" +");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && font.width(Component.literal(candidate).withStyle(style)) > ArcaneNotebookScreen.PAGE_WIDTH) {
                out.add(new NotebookPage.Line(current.toString(), style, color, true));
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
            // A single word longer than the page is cut so it never overflows.
            while (font.width(Component.literal(current.toString()).withStyle(style)) > ArcaneNotebookScreen.PAGE_WIDTH) {
                String head = font.plainSubstrByWidth(current.toString(), ArcaneNotebookScreen.PAGE_WIDTH);
                if (head.isEmpty()) break;
                out.add(new NotebookPage.Line(head, style, color, false));
                current = new StringBuilder(current.substring(head.length()));
            }
        }
        if (!current.isEmpty()) {
            out.add(new NotebookPage.Line(current.toString(), style, color, false));
        }
    }
}
