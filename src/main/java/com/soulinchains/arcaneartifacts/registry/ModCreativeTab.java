package com.soulinchains.arcaneartifacts.registry;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.glyph.ArcaneGlyphs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArcaneArtifacts.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = REGISTER.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.arcane_artifacts"))
            .icon(() -> ModItems.WREATH_OF_MANIPULATION.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(ModItems.RESEARCH_TABLE.get());
                output.accept(ModItems.RESEARCHER_NOTEBOOK.get());
                output.accept(ModItems.OLD_PARCHMENT.get());
                output.accept(ModItems.ROSE_GOLD_INGOT.get());
                output.accept(ModItems.ROSE_GOLD_NUGGET.get());
                output.accept(ModItems.ROSE_GOLD_BLOCK.get());
                output.accept(ModItems.WREATH_OF_MANIPULATION.get());
                output.accept(ModItems.FOCUSING_BOOTS.get());
                output.accept(ModItems.FOCUSING_GLOVES.get());
                output.accept(ModItems.RING_OF_REVERSAL.get());
                output.accept(ModItems.MANTLE_OF_REFLECTION.get());
                output.accept(ModItems.SPELL_STAFF.get());
                output.accept(ModItems.CRYSTAL_BALL.get());
                output.accept(ModItems.BOTTOMLESS_BELT.get());
                output.accept(ModItems.AMULET_OF_OPPOSITES.get());
                output.accept(ModItems.RING_OF_CALM.get());
                output.accept(ModItems.HOP_SLIPPERS.get());
                output.accept(ModItems.MIRROR_SHIELD.get());
                output.accept(ModItems.FOCUSING_SHIELD.get());
                output.accept(ModItems.ENCHANTED_POISONOUS_POTATO.get());
                output.accept(ModItems.GILDED_PUFFERFISH.get());
                output.accept(ModItems.GILDED_FERMENTED_SPIDER_EYE.get());
                output.accept(ModItems.OATH_SCROLL.get());
                output.accept(ModItems.HEART_CHARM.get());
                output.accept(ModItems.DIAMOND_CHARM.get());
                output.accept(ModItems.CLUB_CHARM.get());
                output.accept(ModItems.SPADE_CHARM.get());
                for (AbstractSpellPart part : ArcaneGlyphs.ALL) {
                    if (part.glyphItem != null) {
                        output.accept(part.glyphItem);
                    }
                }
            })
            .build());

    private ModCreativeTab() {
    }
}
