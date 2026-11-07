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
                output.accept(ModItems.WREATH_OF_MANIPULATION.get());
                output.accept(ModItems.FOCUSING_BOOTS.get());
                output.accept(ModItems.FOCUSING_GLOVES.get());
                output.accept(ModItems.RING_OF_REVERSAL.get());
                output.accept(ModItems.MANTLE_OF_REFLECTION.get());
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
