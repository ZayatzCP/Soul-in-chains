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
                output.accept(ModItems.MYSTERY_HAT.get());
                output.accept(ModItems.FOCUSING_BOOTS.get());
                output.accept(ModItems.FOCUSING_GLOVES.get());
                output.accept(ModItems.RING_OF_REVERSAL.get());
                output.accept(ModItems.MANTLE_OF_REFLECTION.get());
                output.accept(ModItems.SPELL_STAFF.get());
                output.accept(ModItems.FLYING_BROOM.get());
                output.accept(ModItems.CRYSTAL_BALL.get());
                output.accept(ModItems.BOTTOMLESS_BELT.get());
                output.accept(ModItems.SOUL_LANTERN.get());
                output.accept(ModItems.CHAIN_SICKLE.get());
                output.accept(ModItems.SOUL_FLAIL.get());
                output.accept(ModItems.RITUAL_SEAL.get());
                output.accept(ModItems.RITUAL_ANCHOR.get());
                output.accept(ModItems.RITUAL_KNIFE.get());
                output.accept(ModItems.OATH_SHIELD.get());
                output.accept(ModItems.SEAL_HAMMER.get());
                output.accept(ModItems.FORMULA_BOOK.get());
                output.accept(ModItems.RITUAL_CENSER.get());
                output.accept(ModItems.RITUAL_ASH.get());
                output.accept(ModItems.BONE_SHARD.get());
                output.accept(ModItems.VOW_DUST.get());
                output.accept(ModItems.ASH_WITNESS_SPAWN_EGG.get());
                output.accept(ModItems.TORN_ACOLYTE_SPAWN_EGG.get());
                output.accept(ModItems.WARDEN_ROD.get());
                output.accept(ModItems.SHACKLE_GLOVES.get());
                output.accept(ModItems.SHROUD.get());
                output.accept(ModItems.BONE_CROWN.get());
                output.accept(ModItems.PHYLACTERY_AMULET.get());
                output.accept(ModItems.SHACKLED_BOOTS.get());
                output.accept(ModItems.RITUAL_DAGGER.get());
                output.accept(ModItems.GRAVEDIGGER_SHOVEL.get());
                output.accept(ModItems.GRAVE.get());
                output.accept(ModItems.RESTLESS_SPAWN_EGG.get());
                output.accept(ModItems.SOUL_EATER_SPAWN_EGG.get());
                output.accept(ModItems.MOURNER_SPAWN_EGG.get());
                output.accept(ModItems.CHAIN_GHOST_SPAWN_EGG.get());
                output.accept(ModItems.JAILER_SPAWN_EGG.get());
                output.accept(ModItems.IMPRISONED_KING_SPAWN_EGG.get());
                output.accept(ModItems.SHACKLE_STONE.get());
                output.accept(ModItems.CHAIN_PILLAR.get());
                output.accept(ModItems.EMPTY_THRONE.get());
                output.accept(ModItems.JAILER_NOTES.get());
                output.accept(ModItems.SCYTHE_OF_THE_REAPER.get());
                output.accept(ModItems.KINGS_CROWN.get());
                output.accept(ModItems.TOOTH_NECKLACE.get());
                output.accept(ModItems.EXECUTIONER_RING.get());
                output.accept(ModItems.JAILER_EYE.get());
                output.accept(ModItems.CAGED_HEART.get());
                output.accept(ModItems.SHACKLE_KEY.get());
                output.accept(ModItems.EXECUTIONER_AXE.get());
                output.accept(ModItems.SOUL_VESSEL.get());
                output.accept(ModItems.CONSECRATED_SOIL.get());
                output.accept(ModItems.DESECRATED_SOIL.get());
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
                output.accept(ModItems.SPORE_TUFT.get());
                output.accept(ModItems.GLOWSPROUT.get());
                output.accept(ModItems.HANGING_HYPHAE.get());
                output.accept(ModItems.SPORE_CLAY.get());
                for (var brick : java.util.List.of(ModItems.RAW_SPORE_BRICKS, ModItems.SPORE_BRICKS, ModItems.SPORE_BRICK_STAIRS, ModItems.SPORE_BRICK_SLAB,
                        ModItems.SPORE_BRICK_WALL, ModItems.SPORE_BRICK_TILES, ModItems.CHISELED_SPORE_BRICKS, ModItems.POLISHED_SPORE_CLAY)) {
                    output.accept(brick.get());
                }
                ModItems.MUSHROOMS.values().forEach(item -> output.accept(item.get()));
                output.accept(ModItems.MUDZYA_SPAWN_EGG.get());
                output.accept(ModItems.GRIBUSHKA_SPAWN_EGG.get());
                output.accept(ModItems.AMANITOID_SPAWN_EGG.get());
                output.accept(ModItems.ELDER_MYCELINOID_SPAWN_EGG.get());
                output.accept(ModItems.GRIBULKA_SPAWN_EGG.get());
                output.accept(ModItems.GRIBULKA_HIVE_SPAWN_EGG.get());
                output.accept(MushroomWood.MUSHROOM_MOSS_CARPET_ITEM.get());
                MushroomWood.SETS.values().forEach(set -> set.items.forEach(item -> output.accept(item.get())));
                output.accept(ModItems.MUSHROOM_SHIELD.get());
                output.accept(ModItems.MUSHROOM_CLUB.get());
                output.accept(ModItems.FALSE_MAGE_GUIDE.get());
                output.accept(ModItems.IRON_HORSE.get());
                output.accept(ModItems.PSEUDOMAGE_GLOVES.get());
                output.accept(ModItems.GUNSMITH_BENCH.get());
                ModItems.GUN_PARTS.values().forEach(part -> output.accept(part.get()));
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
                output.accept(ModItems.LARGE_AMBER_CLUSTER.get());
                output.accept(ModItems.AMBER_BLOCK.get());
                output.accept(ModItems.PSEUDOMAGE_BOOTS.get());
                output.accept(ModItems.DRUID_HERBARIUM.get());
                output.accept(ModItems.MOSS_ELK_CALL.get());
                output.accept(ModItems.DRUID_STAFF_ROOTS.get());
                output.accept(ModItems.DRUID_STAFF_GROVE.get());
                output.accept(ModItems.DRUID_STAFF_HIVE.get());
                output.accept(ModItems.DRUID_STAFF_SAP.get());
                output.accept(ModItems.DRUID_STAFF_THORNS.get());
                output.accept(ModItems.DRUID_STAFF_GRAND.get());
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
                com.soulinchains.arcaneartifacts.catto.CattoContent.ITEMS.forEach(item -> output.accept(item.get()));
                var tablet = com.hollingsworth.arsnouveau.api.registry.RitualRegistry.getRitualItemMap()
                        .get(com.soulinchains.arcaneartifacts.ArcaneArtifacts.id(com.soulinchains.arcaneartifacts.catto.FamiliarBondRitual.ID));
                if (tablet != null) {
                    output.accept(tablet);
                }
                var awakening = com.hollingsworth.arsnouveau.api.registry.RitualRegistry.getRitualItemMap()
                        .get(com.soulinchains.arcaneartifacts.ArcaneArtifacts.id(com.soulinchains.arcaneartifacts.necro.AwakeningRitual.ID));
                if (awakening != null) {
                    output.accept(awakening);
                }
                var eternal = com.hollingsworth.arsnouveau.api.registry.RitualRegistry.getRitualItemMap()
                        .get(com.soulinchains.arcaneartifacts.ArcaneArtifacts.id(com.soulinchains.arcaneartifacts.necro.EternalBindingRitual.ID));
                if (eternal != null) {
                    output.accept(eternal);
                }
                var conversion = com.hollingsworth.arsnouveau.api.registry.RitualRegistry.getRitualItemMap()
                        .get(com.soulinchains.arcaneartifacts.ArcaneArtifacts.id(com.soulinchains.arcaneartifacts.necro.ConversionRitual.ID));
                if (conversion != null) {
                    output.accept(conversion);
                }
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
