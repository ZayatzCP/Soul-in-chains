package com.soulinchains.arcaneartifacts.client.docs;

import com.hollingsworth.arsnouveau.api.documentation.DocCategory;
import com.hollingsworth.arsnouveau.api.documentation.DocClientUtils;
import com.hollingsworth.arsnouveau.api.documentation.ReloadDocumentationEvent;
import com.hollingsworth.arsnouveau.api.documentation.builder.DocEntryBuilder;
import com.hollingsworth.arsnouveau.api.documentation.entry.DocEntry;
import com.hollingsworth.arsnouveau.api.documentation.entry.TextEntry;
import com.hollingsworth.arsnouveau.api.registry.DocumentationRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.setup.registry.Documentation;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.glyph.ArcaneGlyphs;
import com.soulinchains.arcaneartifacts.glyph.AugmentPursuit;
import com.soulinchains.arcaneartifacts.glyph.AugmentShrapnel;
import com.soulinchains.arcaneartifacts.glyph.EffectArcaneMark;
import com.soulinchains.arcaneartifacts.glyph.EffectParasitism;
import com.soulinchains.arcaneartifacts.glyph.MethodArcaneMissile;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.research.ClientResearch;
import com.soulinchains.arcaneartifacts.research.PlayerResearch;
import com.soulinchains.arcaneartifacts.research.ResearchDefinition;
import com.soulinchains.arcaneartifacts.research.ResearchManager;
import com.soulinchains.arcaneartifacts.research.ResearchService;
import com.soulinchains.arcaneartifacts.research.ResearchState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Book pages of the addon, built with Ars Nouveau's documentation system.
 * <p>
 * The "Arcane Artifacts" category is not listed in the Ars Nouveau book: it is opened by the
 * Researcher's Secret Notebook (descriptions, the lore of the wizard "???", and recipes shown exactly
 * like in the Worn Notebook). Glyphs keep their entries in the Ars glyph index, but while a glyph's
 * research is not formulated its entry is replaced by an unreadable "???" page; the book is rebuilt
 * when the set of locked glyphs changes.
 * <p>
 * Text lives in the lang files: {@code arcane_artifacts.pageN.<entry>}, {@code arcane_artifacts.lore.<entry>},
 * {@code arcane_artifacts.effect_doc.<effect>} and {@code arcane_artifacts.glyph_doc.<glyph>}.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, value = Dist.CLIENT)
public final class ArcaneDocumentation {
    private static DocCategory category;
    private static Set<ResourceLocation> lockedAtBuild = Set.of();
    private static DocEntry overview, researchGuide, researchTable, oldParchment, notebook, roseGold;
    private static DocEntry wreath, boots, gloves, ring, mantle, effects;

    private ArcaneDocumentation() {
    }

    /** Created lazily: items must be registered before an ItemStack icon can be built. */
    public static DocCategory category() {
        if (category == null) {
            category = new DocCategory(ArcaneArtifacts.id("arcane_artifacts"),
                    new ItemStack(ModItems.RESEARCHER_NOTEBOOK.get()), 250);
        }
        return category;
    }

    @SubscribeEvent
    public static void addEntries(ReloadDocumentationEvent.AddEntries event) {
        DocCategory cat = category();

        overview = Documentation.addPage(new DocEntryBuilder(ArcaneArtifacts.MODID, cat, "overview")
                .withIcon(ModItems.RESEARCHER_NOTEBOOK.get())
                .withSortNum(0)
                .withIntroPage()
                .withLocalizedText()
                .withLocalizedText());
        researchGuide = Documentation.addPage(new DocEntryBuilder(ArcaneArtifacts.MODID, cat, "research_guide")
                .withIcon(ModItems.OLD_PARCHMENT.get())
                .withSortNum(1)
                .withIntroPage()
                .withLocalizedText()
                .withLocalizedText());

        researchTable = Documentation.addPage(itemEntry(cat, ModItems.RESEARCH_TABLE.get(), 2, 2));
        oldParchment = Documentation.addPage(itemEntry(cat, ModItems.OLD_PARCHMENT.get(), 1, 3));
        notebook = Documentation.addPage(itemEntry(cat, ModItems.RESEARCHER_NOTEBOOK.get(), 1, 4));
        roseGold = Documentation.addPage(itemEntry(cat, ModItems.ROSE_GOLD_INGOT.get(), 1, 5));
        wreath = Documentation.addPage(itemEntry(cat, ModItems.WREATH_OF_MANIPULATION.get(), 4, 10));
        boots = Documentation.addPage(itemEntry(cat, ModItems.FOCUSING_BOOTS.get(), 3, 20));
        gloves = Documentation.addPage(itemEntry(cat, ModItems.FOCUSING_GLOVES.get(), 3, 30));
        ring = Documentation.addPage(itemEntry(cat, ModItems.RING_OF_REVERSAL.get(), 2, 40));
        mantle = Documentation.addPage(itemEntry(cat, ModItems.MANTLE_OF_REFLECTION.get(), 2, 50));

        DocEntryBuilder effectsBuilder = new DocEntryBuilder(ArcaneArtifacts.MODID, cat, "status_effects")
                .withIcon(ModItems.WREATH_OF_MANIPULATION.get())
                .withSortNum(60)
                .withIntroPage();
        for (String effect : new String[]{"arcane_mark", "parasite_host", "parasitic_bond", "parasitic_recovery", "arcane_adaptation"}) {
            effectsBuilder.withHeaderPage("arcane_artifacts.effect_doc." + effect, "effect.arcane_artifacts." + effect);
        }
        effects = Documentation.addPage(effectsBuilder);
    }

    /** Intro page, {@code textPages - 1} more pages, a page of the wizard's notes (lore), then the recipe. */
    private static DocEntryBuilder itemEntry(DocCategory cat, ItemLike item, int textPages, int sort) {
        DocEntryBuilder builder = new DocEntryBuilder(ArcaneArtifacts.MODID, cat, item).withSortNum(sort).withIntroPage();
        for (int i = 1; i < textPages; i++) {
            builder.withLocalizedText();
        }
        String path = BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
        builder.withTextPage(Component.translatable("arcane_artifacts.lore." + path).withStyle(ChatFormatting.ITALIC));
        return builder.withCraftingPages();
    }

    @SubscribeEvent
    public static void afterEntries(ReloadDocumentationEvent.Post event) {
        Set<ResourceLocation> locked = lockedGlyphs();
        lockedAtBuild = locked;
        for (AbstractSpellPart part : ArcaneGlyphs.ALL) {
            DocEntry glyph = DocumentationRegistry.getEntry(part.getRegistryName());
            if (glyph == null) {
                continue;
            }
            if (locked.contains(part.getRegistryName())) {
                hide(glyph, part.getRegistryName());
                continue;
            }
            Component body = Component.translatable("arcane_artifacts.glyph_doc." + part.getRegistryName().getPath());
            for (var label : DocClientUtils.splitToFitFullPage(body)) {
                glyph.addPage(TextEntry.create(label));
            }
        }

        DocEntry missile = visibleGlyph(MethodArcaneMissile.INSTANCE, locked);
        DocEntry mark = visibleGlyph(EffectArcaneMark.INSTANCE, locked);
        DocEntry parasitism = visibleGlyph(EffectParasitism.INSTANCE, locked);
        DocEntry shrapnel = visibleGlyph(AugmentShrapnel.INSTANCE, locked);
        DocEntry pursuit = visibleGlyph(AugmentPursuit.INSTANCE, locked);

        link(overview, researchGuide, researchTable, oldParchment, notebook, roseGold, wreath, boots, gloves, ring, mantle, effects);
        link(researchGuide, researchTable, notebook);
        link(researchTable, researchGuide, oldParchment);
        link(oldParchment, notebook);
        link(roseGold, wreath, boots, gloves, ring, mantle, shrapnel);
        link(wreath, effects);
        link(gloves, missile, shrapnel, parasitism);
        link(mantle, missile);
        link(effects, mark, parasitism, wreath);
        link(missile, mark, shrapnel, pursuit, gloves);
        link(parasitism, mark, effects);
        link(shrapnel, missile, pursuit);
        link(pursuit, missile, shrapnel);
    }

    /** Glyphs whose research is not formulated yet, according to the client's synced research data. */
    private static Set<ResourceLocation> lockedGlyphs() {
        PlayerResearch data = ClientResearch.progress();
        Set<ResourceLocation> locked = new HashSet<>();
        for (ResourceLocation glyph : ResearchManager.gatedGlyphs()) {
            boolean unlocked = false;
            for (Map.Entry<ResourceLocation, ResearchDefinition> e : ResearchManager.all().entrySet()) {
                if (e.getValue().unlocks().contains(glyph) && ResearchService.state(data, e.getKey()).atLeast(ResearchState.UNLOCKED)) {
                    unlocked = true;
                    break;
                }
            }
            if (!unlocked) {
                locked.add(glyph);
            }
        }
        return locked;
    }

    /** Replaces a locked glyph's entry by an unreadable page that only names the research to pursue. */
    private static void hide(DocEntry original, ResourceLocation glyphId) {
        DocCategory glyphCategory = DocumentationRegistry.getCategoryForEntry(original);
        if (glyphCategory == null) {
            return;
        }
        ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ars_nouveau:blank_glyph")));
        Component research = Component.literal("???");
        PlayerResearch data = ClientResearch.progress();
        for (Map.Entry<ResourceLocation, ResearchDefinition> e : ResearchManager.all().entrySet()) {
            if (e.getValue().unlocks().contains(glyphId) && ResearchService.state(data, e.getKey()) != ResearchState.UNKNOWN) {
                research = Component.translatable(ResearchDefinition.langPrefix(e.getKey()) + ".title");
                break;
            }
        }
        DocEntry mystery = new DocEntry(glyphId, icon, Component.literal("???"));
        mystery.addPage(TextEntry.create(Component.translatable("arcane_artifacts.book.locked_glyph", research),
                Component.literal("???"), icon));
        DocumentationRegistry.registerEntry(glyphCategory, mystery);
    }

    private static DocEntry visibleGlyph(AbstractSpellPart part, Set<ResourceLocation> locked) {
        return locked.contains(part.getRegistryName()) ? null : DocumentationRegistry.getEntry(part.getRegistryName());
    }

    private static void link(DocEntry from, DocEntry... to) {
        if (from == null) {
            return;
        }
        for (DocEntry target : to) {
            if (target != null && target != from) {
                from.withRelation(target);
            }
        }
    }

    /** Rebuilds the book when a glyph gets unlocked (or research data arrives after the first build). */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.player.tickCount % 20 != 0
                || DocumentationRegistry.getEntry(ArcaneArtifacts.id("overview")) == null) {
            return;
        }
        if (!lockedGlyphs().equals(lockedAtBuild)) {
            Documentation.initOnWorldReload();
        }
    }
}
