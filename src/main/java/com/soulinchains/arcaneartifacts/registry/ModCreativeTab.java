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
                output.accept(ModItems.ALCHEMICAL_DISTILLER.get());
                output.accept(ModItems.MUDZYA_HIVE.get());
                output.accept(ModItems.WILD_MUDZYA_HIVE.get());
                output.accept(ModItems.BOTTLE_OF_SOURCE.get());
                output.accept(ModItems.MUSHROOM_MOSS.get());
                output.accept(ModItems.BLUE_MUSHROOM_BLOCK.get());
                output.accept(ModItems.SPOTTED_BLUE_MUSHROOM_BLOCK.get());
                output.accept(ModItems.GLOWING_BLUE_MUSHROOM_BLOCK.get());
                output.accept(ModItems.BLUE_MUSHROOM_GILLS.get());
                output.accept(ModItems.CAVE_MYCELIUM.get());
                output.accept(ModItems.SPORE_TUFT.get());
                output.accept(ModItems.GLOWSPROUT.get());
                output.accept(ModItems.HANGING_HYPHAE.get());
                output.accept(ModItems.MYCELIUM_SOIL.get());
                output.accept(ModItems.MYCELIAL_MUD.get());
                output.accept(ModItems.SPORE_CLAY.get());
                ModItems.MUSHROOMS.values().forEach(item -> output.accept(item.get()));
                output.accept(ModItems.MUDZYA_SPAWN_EGG.get());
                output.accept(ModItems.GRIBUSHKA_SPAWN_EGG.get());
                output.accept(ModItems.AMANITOID_SPAWN_EGG.get());
                output.accept(MushroomWood.MUSHROOM_MOSS_CARPET_ITEM.get());
                MushroomWood.SETS.values().forEach(set -> set.items.forEach(item -> output.accept(item.get())));
                output.accept(ModItems.MUSHROOM_SHIELD.get());
                output.accept(ModItems.MUSHROOM_CLUB.get());
                output.accept(ModItems.FALSE_MAGE_GUIDE.get());
                output.accept(ModItems.REVOLVER.get());
                output.accept(ModItems.SAWN_OFF.get());
                output.accept(ModItems.MAGIC_CARTRIDGE.get());
                output.accept(ModItems.BANDOLIER.get());
                output.accept(ModItems.COUNTERFEIT_RING.get());
                output.accept(ModItems.COUNTERFEIT_PENDANT.get());
                output.accept(ModItems.MANA_GOGGLES.get());
                output.accept(ModItems.PONCHO.get());
                output.accept(ModItems.SOMBRERO.get());
                output.accept(ModItems.AMBER.get());
                output.accept(ModItems.AMBER_CLUSTER.get());
                output.accept(ModItems.PSEUDOMAGE_BOOTS.get());
                output.accept(ModItems.DRUID_HERBARIUM.get());
                output.accept(ModItems.DRUID_MANTLE.get());
                output.accept(ModItems.DRUID_HAT.get());
                output.accept(ModItems.DRUID_GLOVES.get());
                output.accept(ModItems.DRUID_BOOTS.get());
                output.accept(ModItems.MANA_TANK.get());
                output.accept(ModItems.MANA_PIPE.get());
                output.accept(ModItems.MANA_PUMP.get());
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
