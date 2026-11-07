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
import com.soulinchains.arcaneartifacts.glyph.AugmentSplitting;
import com.soulinchains.arcaneartifacts.glyph.EffectArcaneMark;
import com.soulinchains.arcaneartifacts.glyph.EffectParasitism;
import com.soulinchains.arcaneartifacts.glyph.MethodArcaneMissile;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Pages of the Ars Nouveau book (Worn Notebook) for this addon.
 * <p>
 * Items, mechanics and status effects get their own sub-category "Arcane Artifacts" under
 * "Items and Equipment". Glyphs are documented by Ars Nouveau itself (description, augments and
 * Scribes Table recipe); this class appends a detailed "how it works" page to each of them and links
 * related entries together.
 * <p>
 * Text lives in the lang files: {@code arcane_artifacts.pageN.<entry>} for entry pages,
 * {@code arcane_artifacts.effect_doc.<effect>} for status effects and
 * {@code arcane_artifacts.glyph_doc.<glyph>} for the extra glyph pages. Long texts are split across
 * pages automatically.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, value = Dist.CLIENT)
public final class ArcaneDocumentation {
    private static DocCategory category;

    private static DocEntry wreath, boots, gloves, ring, mantle, roseGold, effects, overview;

    private ArcaneDocumentation() {
    }

    /** Created lazily: items must be registered before an ItemStack icon can be built. */
    private static DocCategory category() {
        if (category == null) {
            category = new DocCategory(ArcaneArtifacts.id("arcane_artifacts"),
                    new ItemStack(ModItems.WREATH_OF_MANIPULATION.get()), 250);
        }
        if (!DocumentationRegistry.ITEMS_BLOCKS_EQUIPMENT.subCategories().contains(category)) {
            DocumentationRegistry.ITEMS_BLOCKS_EQUIPMENT.addSubCategory(category);
        }
        return category;
    }

    @SubscribeEvent
    public static void addEntries(ReloadDocumentationEvent.AddEntries event) {
        DocCategory cat = category();

        overview = Documentation.addPage(new DocEntryBuilder(ArcaneArtifacts.MODID, cat, "overview")
                .withIcon(ModItems.WREATH_OF_MANIPULATION.get())
                .withSortNum(0)
                .withIntroPage()
                .withLocalizedText()
                .withLocalizedText());

        roseGold = Documentation.addPage(itemEntry(cat, ModItems.ROSE_GOLD_INGOT.get(), 1, 1));
        wreath = Documentation.addPage(itemEntry(cat, ModItems.WREATH_OF_MANIPULATION.get(), 4, 10));
        boots = Documentation.addPage(itemEntry(cat, ModItems.FOCUSING_BOOTS.get(), 3, 20));
        gloves = Documentation.addPage(itemEntry(cat, ModItems.FOCUSING_GLOVES.get(), 3, 30));
        ring = Documentation.addPage(itemEntry(cat, ModItems.RING_OF_REVERSAL.get(), 2, 40));
        mantle = Documentation.addPage(itemEntry(cat, ModItems.MANTLE_OF_REFLECTION.get(), 2, 50));

        DocEntryBuilder effectsBuilder = new DocEntryBuilder(ArcaneArtifacts.MODID, cat, "status_effects")
                .withIcon(EffectArcaneMark.INSTANCE.glyphItem)
                .withSortNum(60)
                .withIntroPage();
        for (String effect : new String[]{"arcane_mark", "parasite_host", "parasitic_bond", "parasitic_recovery", "arcane_adaptation"}) {
            effectsBuilder.withHeaderPage("arcane_artifacts.effect_doc." + effect, "effect.arcane_artifacts." + effect);
        }
        effects = Documentation.addPage(effectsBuilder);
    }

    /** Intro page with the item, {@code textPages - 1} more text pages, then the crafting page(s). */
    private static DocEntryBuilder itemEntry(DocCategory cat, ItemLike item, int textPages, int sort) {
        DocEntryBuilder builder = new DocEntryBuilder(ArcaneArtifacts.MODID, cat, item).withSortNum(sort).withIntroPage();
        for (int i = 1; i < textPages; i++) {
            builder.withLocalizedText();
        }
        return builder.withCraftingPages();
    }

    @SubscribeEvent
    public static void afterEntries(ReloadDocumentationEvent.Post event) {
        for (AbstractSpellPart part : ArcaneGlyphs.ALL) {
            DocEntry glyph = DocumentationRegistry.getEntry(part.getRegistryName());
            if (glyph == null) {
                continue;
            }
            Component body = Component.translatable("arcane_artifacts.glyph_doc." + part.getRegistryName().getPath());
            for (var label : DocClientUtils.splitToFitFullPage(body)) {
                glyph.addPage(TextEntry.create(label));
            }
        }

        DocEntry missile = glyphEntry(MethodArcaneMissile.INSTANCE);
        DocEntry mark = glyphEntry(EffectArcaneMark.INSTANCE);
        DocEntry parasitism = glyphEntry(EffectParasitism.INSTANCE);
        DocEntry splitting = glyphEntry(AugmentSplitting.INSTANCE);
        DocEntry pursuit = glyphEntry(AugmentPursuit.INSTANCE);

        link(overview, roseGold, wreath, boots, gloves, ring, mantle, effects);
        link(roseGold, wreath, boots, gloves, ring, mantle, splitting);
        link(wreath, effects);
        link(gloves, missile, splitting, parasitism);
        link(mantle, missile);
        link(effects, mark, parasitism, wreath);
        link(missile, mark, splitting, pursuit, gloves);
        link(parasitism, mark, effects);
        link(splitting, missile, pursuit);
        link(pursuit, missile, splitting);
    }

    private static DocEntry glyphEntry(AbstractSpellPart part) {
        return DocumentationRegistry.getEntry(part.getRegistryName());
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
}
