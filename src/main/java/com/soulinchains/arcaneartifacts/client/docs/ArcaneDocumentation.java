package com.soulinchains.arcaneartifacts.client.docs;

import com.hollingsworth.arsnouveau.api.documentation.DocCategory;
import com.hollingsworth.arsnouveau.api.documentation.DocClientUtils;
import com.hollingsworth.arsnouveau.api.documentation.ReloadDocumentationEvent;
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
import com.soulinchains.arcaneartifacts.research.ClientResearch;
import com.soulinchains.arcaneartifacts.research.PlayerResearch;
import com.soulinchains.arcaneartifacts.research.ResearchDefinition;
import com.soulinchains.arcaneartifacts.research.ResearchManager;
import com.soulinchains.arcaneartifacts.research.ResearchService;
import com.soulinchains.arcaneartifacts.research.ResearchState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Changes to the Ars Nouveau book. The addon's own chapters live in the Researcher's Secret Notebook
 * ({@link com.soulinchains.arcaneartifacts.client.notebook.ArcaneNotebookScreen}), not here.
 * <p>
 * The addon's glyphs keep their entries in the Ars glyph index, but while a glyph's research is not
 * formulated its entry is replaced by an unreadable "???" page; the book is rebuilt when the set of
 * locked glyphs changes. Unlocked glyphs get extra pages ({@code arcane_artifacts.glyph_doc.<glyph>}).
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, value = Dist.CLIENT)
public final class ArcaneDocumentation {
    private static Set<ResourceLocation> lockedAtBuild = Set.of();

    private ArcaneDocumentation() {
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

        link(missile, mark, shrapnel, pursuit);
        link(parasitism, mark);
        link(shrapnel, missile, pursuit);
        link(pursuit, missile, shrapnel);
    }

    /** Glyphs whose research is not formulated yet, according to the client's synced research data. */
    public static Set<ResourceLocation> lockedGlyphs() {
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
        Component research = researchFor(glyphId);
        DocEntry mystery = new DocEntry(glyphId, icon, Component.literal("???"));
        mystery.addPage(TextEntry.create(Component.translatable("arcane_artifacts.book.locked_glyph", research),
                Component.literal("???"), icon));
        DocumentationRegistry.registerEntry(glyphCategory, mystery);
    }

    /** Items whose research is not formulated yet (shown as "???" in the Researcher's Notebook). */
    public static Set<ResourceLocation> lockedItems() {
        PlayerResearch data = ClientResearch.progress();
        Set<ResourceLocation> locked = new HashSet<>();
        for (ResourceLocation item : ResearchManager.gatedItems()) {
            boolean unlocked = false;
            for (Map.Entry<ResourceLocation, ResearchDefinition> e : ResearchManager.all().entrySet()) {
                if (e.getValue().unlocksItems().contains(item) && ResearchService.state(data, e.getKey()).atLeast(ResearchState.UNLOCKED)) {
                    unlocked = true;
                    break;
                }
            }
            if (!unlocked) {
                locked.add(item);
            }
        }
        return locked;
    }

    /** The research that unlocks an item, or "???" while the player has not discovered it. */
    public static Component researchForItem(ResourceLocation itemId) {
        PlayerResearch data = ClientResearch.progress();
        for (Map.Entry<ResourceLocation, ResearchDefinition> e : ResearchManager.all().entrySet()) {
            if (e.getValue().unlocksItems().contains(itemId) && ResearchService.state(data, e.getKey()) != ResearchState.UNKNOWN) {
                return Component.translatable(ResearchDefinition.langPrefix(e.getKey()) + ".title");
            }
        }
        return Component.literal("???");
    }

    /** The research that unlocks a glyph, or "???" while the player has not discovered it. */
    public static Component researchFor(ResourceLocation glyphId) {
        PlayerResearch data = ClientResearch.progress();
        for (Map.Entry<ResourceLocation, ResearchDefinition> e : ResearchManager.all().entrySet()) {
            if (e.getValue().unlocks().contains(glyphId) && ResearchService.state(data, e.getKey()) != ResearchState.UNKNOWN) {
                return Component.translatable(ResearchDefinition.langPrefix(e.getKey()) + ".title");
            }
        }
        return Component.literal("???");
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
                || DocumentationRegistry.getEntry(MethodArcaneMissile.INSTANCE.getRegistryName()) == null) {
            return;
        }
        if (!lockedGlyphs().equals(lockedAtBuild)) {
            Documentation.initOnWorldReload();
        }
    }
}
