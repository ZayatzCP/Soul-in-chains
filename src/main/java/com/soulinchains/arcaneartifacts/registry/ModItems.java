package com.soulinchains.arcaneartifacts.registry;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.accessory.FocusingBootsItem;
import com.soulinchains.arcaneartifacts.accessory.FocusingGlovesItem;
import com.soulinchains.arcaneartifacts.accessory.MantleOfReflectionItem;
import com.soulinchains.arcaneartifacts.accessory.ResearcherNotebookItem;
import com.soulinchains.arcaneartifacts.accessory.RingOfReversalItem;
import com.soulinchains.arcaneartifacts.accessory.WreathOfManipulationItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(ArcaneArtifacts.MODID);

    public static final DeferredItem<BlockItem> RESEARCH_TABLE = REGISTER.registerSimpleBlockItem(ModBlocks.RESEARCH_TABLE);

    public static final DeferredItem<Item> OLD_PARCHMENT = REGISTER.register("old_parchment", () -> new Item(new Item.Properties()));
    public static final DeferredItem<ResearcherNotebookItem> RESEARCHER_NOTEBOOK = REGISTER.register("researcher_notebook", () -> new ResearcherNotebookItem());

    public static final DeferredItem<Item> ROSE_GOLD_INGOT = REGISTER.register("rose_gold_ingot", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROSE_GOLD_NUGGET = REGISTER.register("rose_gold_nugget", () -> new Item(new Item.Properties()));
    public static final DeferredItem<BlockItem> ROSE_GOLD_BLOCK = REGISTER.registerSimpleBlockItem(ModBlocks.ROSE_GOLD_BLOCK);

    public static final DeferredItem<WreathOfManipulationItem> WREATH_OF_MANIPULATION = REGISTER.register("wreath_of_manipulation", WreathOfManipulationItem::new);
    public static final DeferredItem<FocusingBootsItem> FOCUSING_BOOTS = REGISTER.register("focusing_boots", FocusingBootsItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.accessory.MysteryHatItem> MYSTERY_HAT =
            REGISTER.register("mystery_hat", com.soulinchains.arcaneartifacts.accessory.MysteryHatItem::new);
    public static final DeferredItem<FocusingGlovesItem> FOCUSING_GLOVES = REGISTER.register("focusing_gloves", FocusingGlovesItem::new);
    public static final DeferredItem<RingOfReversalItem> RING_OF_REVERSAL = REGISTER.register("ring_of_reversal", RingOfReversalItem::new);
    public static final DeferredItem<MantleOfReflectionItem> MANTLE_OF_REFLECTION = REGISTER.register("mantle_of_reflection", MantleOfReflectionItem::new);

    public static final DeferredItem<com.soulinchains.arcaneartifacts.item.SpellStaffItem> SPELL_STAFF =
            REGISTER.register("spell_staff", com.soulinchains.arcaneartifacts.item.SpellStaffItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.SoulLanternItem> SOUL_LANTERN =
            REGISTER.register("soul_lantern", com.soulinchains.arcaneartifacts.necro.SoulLanternItem::new);
    public static final DeferredItem<BlockItem> RITUAL_SEAL = REGISTER.registerSimpleBlockItem(ModBlocks.RITUAL_SEAL);
    public static final DeferredItem<BlockItem> RITUAL_ANCHOR = REGISTER.registerSimpleBlockItem(ModBlocks.RITUAL_ANCHOR);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.cult.RitualKnifeItem> RITUAL_KNIFE = REGISTER.register("ritual_knife", com.soulinchains.arcaneartifacts.cult.RitualKnifeItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.cult.OathShieldItem> OATH_SHIELD = REGISTER.register("oath_shield", com.soulinchains.arcaneartifacts.cult.OathShieldItem::new);
    public static final DeferredItem<BlockItem> RITUAL_CENSER = REGISTER.registerSimpleBlockItem(ModBlocks.RITUAL_CENSER);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.cult.SealHammerItem> SEAL_HAMMER = REGISTER.register("seal_hammer", com.soulinchains.arcaneartifacts.cult.SealHammerItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.cult.FormulaBookItem> FORMULA_BOOK = REGISTER.register("formula_book", com.soulinchains.arcaneartifacts.cult.FormulaBookItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.cult.CultGearItem> SEAL_KEEPER_HELM = REGISTER.register("seal_keeper_helm",
            () -> new com.soulinchains.arcaneartifacts.cult.CultGearItem(com.soulinchains.arcaneartifacts.cult.CultGearItem.Piece.HELM));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.cult.CultGearItem> OUTER_CIRCLE_CUIRASS = REGISTER.register("outer_circle_cuirass",
            () -> new com.soulinchains.arcaneartifacts.cult.CultGearItem(com.soulinchains.arcaneartifacts.cult.CultGearItem.Piece.CUIRASS));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.cult.CultGearItem> ANCHOR_BELT = REGISTER.register("anchor_belt",
            () -> new com.soulinchains.arcaneartifacts.cult.CultGearItem(com.soulinchains.arcaneartifacts.cult.CultGearItem.Piece.ANCHOR_BELT));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.cult.CultGearItem> SEAL_GAUNTLETS = REGISTER.register("seal_gauntlets",
            () -> new com.soulinchains.arcaneartifacts.cult.CultGearItem(com.soulinchains.arcaneartifacts.cult.CultGearItem.Piece.GAUNTLETS));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.cult.CultGearItem> SENTRY_BOOTS = REGISTER.register("sentry_boots",
            () -> new com.soulinchains.arcaneartifacts.cult.CultGearItem(com.soulinchains.arcaneartifacts.cult.CultGearItem.Piece.SENTRY_BOOTS));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.cult.CultGearItem> FACE_OF_DOGMA = REGISTER.register("face_of_dogma",
            () -> new com.soulinchains.arcaneartifacts.cult.CultGearItem(com.soulinchains.arcaneartifacts.cult.CultGearItem.Piece.FACE));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.cult.CultGearItem> COMMUNION_MANTLE = REGISTER.register("communion_mantle",
            () -> new com.soulinchains.arcaneartifacts.cult.CultGearItem(com.soulinchains.arcaneartifacts.cult.CultGearItem.Piece.MANTLE));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.cult.CultGearItem> BOOK_OF_ARREARS = REGISTER.register("book_of_arrears",
            () -> new com.soulinchains.arcaneartifacts.cult.CultGearItem(com.soulinchains.arcaneartifacts.cult.CultGearItem.Piece.ARREARS));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.cult.CultGearItem> INSCRIBER_HANDS = REGISTER.register("inscriber_hands",
            () -> new com.soulinchains.arcaneartifacts.cult.CultGearItem(com.soulinchains.arcaneartifacts.cult.CultGearItem.Piece.INSCRIBER_HANDS));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.cult.CultGearItem> QUIET_SHOES = REGISTER.register("quiet_shoes",
            () -> new com.soulinchains.arcaneartifacts.cult.CultGearItem(com.soulinchains.arcaneartifacts.cult.CultGearItem.Piece.QUIET_SHOES));
    public static final DeferredItem<Item> RITUAL_ASH = REGISTER.registerSimpleItem("ritual_ash");
    public static final DeferredItem<Item> BONE_SHARD = REGISTER.registerSimpleItem("bone_shard");
    public static final DeferredItem<Item> VOW_DUST = REGISTER.registerSimpleItem("vow_dust", new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> ASH_WITNESS_SPAWN_EGG = REGISTER.register("ash_witness_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.ASH_WITNESS, 0x929292, 0xF0E7DE, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> RED_ACOLYTE_SPAWN_EGG = REGISTER.register("red_acolyte_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.RED_ACOLYTE, 0xB9463D, 0x171416, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> BONE_GATEKEEPER_SPAWN_EGG = REGISTER.register("bone_gatekeeper_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.BONE_GATEKEEPER, 0xF0E7DE, 0x6F2927, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> PSALMIST_SPAWN_EGG = REGISTER.register("psalmist_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.PSALMIST, 0xD6B2A5, 0x171416, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> VOTIVE_BEAST_SPAWN_EGG = REGISTER.register("votive_beast_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.VOTIVE_BEAST, 0xF0E7DE, 0xB9463D, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> TORN_ACOLYTE_SPAWN_EGG = REGISTER.register("torn_acolyte_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.TORN_ACOLYTE, 0x171416, 0xB9463D, new Item.Properties()));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.SoulFlailItem> SOUL_FLAIL =
            REGISTER.register("soul_flail", com.soulinchains.arcaneartifacts.necro.SoulFlailItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.ChainSickleItem> CHAIN_SICKLE =
            REGISTER.register("chain_sickle", com.soulinchains.arcaneartifacts.necro.ChainSickleItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.WardenRodItem> WARDEN_ROD =
            REGISTER.register("warden_rod", com.soulinchains.arcaneartifacts.necro.WardenRodItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.ShackledGearItem> SHACKLE_GLOVES =
            REGISTER.register("shackle_gloves", () -> new com.soulinchains.arcaneartifacts.necro.ShackledGearItem(com.soulinchains.arcaneartifacts.necro.ShackledGearItem.Piece.GLOVES));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.ShackledGearItem> SHROUD =
            REGISTER.register("shroud", () -> new com.soulinchains.arcaneartifacts.necro.ShackledGearItem(com.soulinchains.arcaneartifacts.necro.ShackledGearItem.Piece.SHROUD));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.ShackledGearItem> BONE_CROWN =
            REGISTER.register("bone_crown", () -> new com.soulinchains.arcaneartifacts.necro.ShackledGearItem(com.soulinchains.arcaneartifacts.necro.ShackledGearItem.Piece.CROWN));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.PhylacteryAmuletItem> PHYLACTERY_AMULET =
            REGISTER.register("phylactery_amulet", com.soulinchains.arcaneartifacts.necro.PhylacteryAmuletItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.ShackledGearItem> SHACKLED_BOOTS =
            REGISTER.register("shackled_boots", () -> new com.soulinchains.arcaneartifacts.necro.ShackledGearItem(com.soulinchains.arcaneartifacts.necro.ShackledGearItem.Piece.BOOTS));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.GravediggerShovelItem> GRAVEDIGGER_SHOVEL =
            REGISTER.register("gravedigger_shovel", com.soulinchains.arcaneartifacts.necro.GravediggerShovelItem::new);
    public static final DeferredItem<BlockItem> GRAVE = REGISTER.registerSimpleBlockItem(ModBlocks.GRAVE);
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> RESTLESS_SPAWN_EGG = REGISTER.register("restless_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.RESTLESS, 0x9FE8FF, 0x2A6B78, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> SOUL_EATER_SPAWN_EGG = REGISTER.register("soul_eater_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.SOUL_EATER, 0x14101A, 0x8E3FD0, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> MOURNER_SPAWN_EGG = REGISTER.register("mourner_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.MOURNER, 0x4A5060, 0xC8D8E8, new Item.Properties()));
    public static final DeferredItem<BlockItem> SHACKLE_STONE = REGISTER.registerSimpleBlockItem(ModBlocks.SHACKLE_STONE);
    public static final DeferredItem<BlockItem> CHAIN_PILLAR = REGISTER.registerSimpleBlockItem(ModBlocks.CHAIN_PILLAR);
    public static final DeferredItem<BlockItem> EMPTY_THRONE = REGISTER.registerSimpleBlockItem(ModBlocks.EMPTY_THRONE);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.ScytheOfTheReaperItem> SCYTHE_OF_THE_REAPER =
            REGISTER.register("scythe_of_the_reaper", com.soulinchains.arcaneartifacts.necro.ScytheOfTheReaperItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.KingsCrownItem> KINGS_CROWN = REGISTER.register("kings_crown", com.soulinchains.arcaneartifacts.necro.KingsCrownItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.JailerNotesItem> JAILER_NOTES = REGISTER.register("jailer_notes", com.soulinchains.arcaneartifacts.necro.JailerNotesItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.RelicItem> TOOTH_NECKLACE = REGISTER.register("tooth_necklace",
            () -> new com.soulinchains.arcaneartifacts.necro.RelicItem(com.soulinchains.arcaneartifacts.necro.Relics.Kind.TOOTH_NECKLACE));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.RelicItem> EXECUTIONER_RING = REGISTER.register("executioner_ring",
            () -> new com.soulinchains.arcaneartifacts.necro.RelicItem(com.soulinchains.arcaneartifacts.necro.Relics.Kind.EXECUTIONER_RING));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.RelicItem> JAILER_EYE = REGISTER.register("jailer_eye",
            () -> new com.soulinchains.arcaneartifacts.necro.RelicItem(com.soulinchains.arcaneartifacts.necro.Relics.Kind.JAILER_EYE));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.RelicItem> CAGED_HEART = REGISTER.register("caged_heart",
            () -> new com.soulinchains.arcaneartifacts.necro.RelicItem(com.soulinchains.arcaneartifacts.necro.Relics.Kind.CAGED_HEART));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> IMPRISONED_KING_SPAWN_EGG = REGISTER.register("imprisoned_king_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.IMPRISONED_KING, 0x1A1620, 0xD4A93A, new Item.Properties()));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.ShackleKeyItem> SHACKLE_KEY =
            REGISTER.register("shackle_key", com.soulinchains.arcaneartifacts.necro.ShackleKeyItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.ExecutionerAxeItem> EXECUTIONER_AXE =
            REGISTER.register("executioner_axe", com.soulinchains.arcaneartifacts.necro.ExecutionerAxeItem::new);
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> CHAIN_GHOST_SPAWN_EGG = REGISTER.register("chain_ghost_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.CHAIN_GHOST, 0x9AA3B5, 0x2A6B78, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> JAILER_SPAWN_EGG = REGISTER.register("jailer_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.JAILER, 0x1A1620, 0x6E7280, new Item.Properties()));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.RitualDaggerItem> RITUAL_DAGGER =
            REGISTER.register("ritual_dagger", com.soulinchains.arcaneartifacts.necro.RitualDaggerItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.necro.SoulVesselItem> SOUL_VESSEL =
            REGISTER.register("soul_vessel", com.soulinchains.arcaneartifacts.necro.SoulVesselItem::new);
    public static final DeferredItem<BlockItem> CONSECRATED_SOIL = REGISTER.registerSimpleBlockItem(ModBlocks.CONSECRATED_SOIL);
    public static final DeferredItem<BlockItem> DESECRATED_SOIL = REGISTER.registerSimpleBlockItem(ModBlocks.DESECRATED_SOIL);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.belt.BottomlessBeltItem> BOTTOMLESS_BELT =
            REGISTER.register("bottomless_belt", com.soulinchains.arcaneartifacts.belt.BottomlessBeltItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.accessory.AmuletOfOppositesItem> AMULET_OF_OPPOSITES =
            REGISTER.register("amulet_of_opposites", com.soulinchains.arcaneartifacts.accessory.AmuletOfOppositesItem::new);

    public static final DeferredItem<com.soulinchains.arcaneartifacts.team.OathScrollItem> OATH_SCROLL =
            REGISTER.register("oath_scroll", com.soulinchains.arcaneartifacts.team.OathScrollItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.team.SuitCharmItem> HEART_CHARM =
            REGISTER.register("heart_charm", () -> new com.soulinchains.arcaneartifacts.team.SuitCharmItem(com.soulinchains.arcaneartifacts.team.Suit.HEARTS));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.team.SuitCharmItem> DIAMOND_CHARM =
            REGISTER.register("diamond_charm", () -> new com.soulinchains.arcaneartifacts.team.SuitCharmItem(com.soulinchains.arcaneartifacts.team.Suit.DIAMONDS));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.team.SuitCharmItem> CLUB_CHARM =
            REGISTER.register("club_charm", () -> new com.soulinchains.arcaneartifacts.team.SuitCharmItem(com.soulinchains.arcaneartifacts.team.Suit.CLUBS));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.team.SuitCharmItem> SPADE_CHARM =
            REGISTER.register("spade_charm", () -> new com.soulinchains.arcaneartifacts.team.SuitCharmItem(com.soulinchains.arcaneartifacts.team.Suit.SPADES));

    public static final DeferredItem<Item> ENCHANTED_POISONOUS_POTATO = REGISTER.register("enchanted_poisonous_potato",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).food(com.soulinchains.arcaneartifacts.item.ArcaneFoods.ENCHANTED_POISONOUS_POTATO)
                    .component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    public static final DeferredItem<Item> GILDED_PUFFERFISH = REGISTER.register("gilded_pufferfish",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).food(com.soulinchains.arcaneartifacts.item.ArcaneFoods.GILDED_PUFFERFISH)));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.item.GildedSpiderEyeItem> GILDED_FERMENTED_SPIDER_EYE =
            REGISTER.register("gilded_fermented_spider_eye", com.soulinchains.arcaneartifacts.item.GildedSpiderEyeItem::new);

    public static final DeferredItem<com.soulinchains.arcaneartifacts.accessory.RingOfCalmItem> RING_OF_CALM =
            REGISTER.register("ring_of_calm", com.soulinchains.arcaneartifacts.accessory.RingOfCalmItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.accessory.HopSlippersItem> HOP_SLIPPERS =
            REGISTER.register("hop_slippers", com.soulinchains.arcaneartifacts.accessory.HopSlippersItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.shield.MirrorShieldItem> MIRROR_SHIELD =
            REGISTER.register("mirror_shield", com.soulinchains.arcaneartifacts.shield.MirrorShieldItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.shield.FocusingShieldItem> FOCUSING_SHIELD =
            REGISTER.register("focusing_shield", com.soulinchains.arcaneartifacts.shield.FocusingShieldItem::new);

    public static final DeferredItem<com.soulinchains.arcaneartifacts.item.CrystalBallItem> CRYSTAL_BALL =
            REGISTER.register("crystal_ball", com.soulinchains.arcaneartifacts.item.CrystalBallItem::new);

    // ------------------------------------------------------------------ batch 6
    public static final DeferredItem<com.soulinchains.arcaneartifacts.item.BottleOfSourceItem> BOTTLE_OF_SOURCE =
            REGISTER.register("bottle_of_source", com.soulinchains.arcaneartifacts.item.BottleOfSourceItem::new);
    public static final DeferredItem<BlockItem> MUSHROOM_MOSS = REGISTER.registerSimpleBlockItem(ModBlocks.MUSHROOM_MOSS);
    public static final DeferredItem<BlockItem> BLUE_MUSHROOM_BLOCK = REGISTER.registerSimpleBlockItem(ModBlocks.BLUE_MUSHROOM_BLOCK);
    public static final DeferredItem<BlockItem> SPOTTED_BLUE_MUSHROOM_BLOCK = REGISTER.registerSimpleBlockItem(ModBlocks.SPOTTED_BLUE_MUSHROOM_BLOCK);
    public static final DeferredItem<BlockItem> GLOWING_BLUE_MUSHROOM_BLOCK = REGISTER.registerSimpleBlockItem(ModBlocks.GLOWING_BLUE_MUSHROOM_BLOCK);
    public static final DeferredItem<BlockItem> BLUE_MUSHROOM_GILLS = REGISTER.registerSimpleBlockItem(ModBlocks.BLUE_MUSHROOM_GILLS);
    public static final DeferredItem<BlockItem> SPORE_TUFT = REGISTER.registerSimpleBlockItem(ModBlocks.SPORE_TUFT);
    public static final DeferredItem<BlockItem> GLOWSPROUT = REGISTER.registerSimpleBlockItem(ModBlocks.GLOWSPROUT);
    public static final DeferredItem<BlockItem> HANGING_HYPHAE = REGISTER.registerSimpleBlockItem(ModBlocks.HANGING_HYPHAE);
    public static final DeferredItem<BlockItem> SPORE_CLAY = REGISTER.registerSimpleBlockItem(ModBlocks.SPORE_CLAY);
    public static final DeferredItem<BlockItem> RAW_SPORE_BRICKS = REGISTER.registerSimpleBlockItem(ModBlocks.RAW_SPORE_BRICKS);
    public static final DeferredItem<BlockItem> SPORE_BRICKS = REGISTER.registerSimpleBlockItem(ModBlocks.SPORE_BRICKS);
    public static final DeferredItem<BlockItem> SPORE_BRICK_STAIRS = REGISTER.registerSimpleBlockItem(ModBlocks.SPORE_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> SPORE_BRICK_SLAB = REGISTER.registerSimpleBlockItem(ModBlocks.SPORE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> SPORE_BRICK_WALL = REGISTER.registerSimpleBlockItem(ModBlocks.SPORE_BRICK_WALL);
    public static final DeferredItem<BlockItem> SPORE_BRICK_TILES = REGISTER.registerSimpleBlockItem(ModBlocks.SPORE_BRICK_TILES);
    public static final DeferredItem<BlockItem> CHISELED_SPORE_BRICKS = REGISTER.registerSimpleBlockItem(ModBlocks.CHISELED_SPORE_BRICKS);
    public static final DeferredItem<BlockItem> POLISHED_SPORE_CLAY = REGISTER.registerSimpleBlockItem(ModBlocks.POLISHED_SPORE_CLAY);
    public static final DeferredItem<BlockItem> MUDZYA_HIVE = REGISTER.registerSimpleBlockItem(ModBlocks.MUDZYA_HIVE);
    public static final DeferredItem<BlockItem> WILD_MUDZYA_HIVE = REGISTER.registerSimpleBlockItem(ModBlocks.WILD_MUDZYA_HIVE);
    public static final DeferredItem<BlockItem> ALCHEMICAL_DISTILLER = REGISTER.registerSimpleBlockItem(ModBlocks.ALCHEMICAL_DISTILLER);
    public static final java.util.Map<com.soulinchains.arcaneartifacts.mushroom.ArcaneMushroom, DeferredItem<BlockItem>> MUSHROOMS = mushrooms();
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> MUDZYA_SPAWN_EGG = REGISTER.register("mudzya_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.MUDZYA, 0x6B3FA0, 0xE0A8FF, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> ELDER_MYCELINOID_SPAWN_EGG = REGISTER.register("elder_mycelinoid_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.ELDER_MYCELINOID, 0x7FCB3A, 0x3E6A1E, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> GRIBULKA_SPAWN_EGG = REGISTER.register("gribulka_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.GRIBULKA, 0xEFE4CC, 0xD08A40, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> GRIBULKA_HIVE_SPAWN_EGG = REGISTER.register("gribulka_hive_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.GRIBULKA_HIVE, 0xD8D8C8, 0x2E2E2A, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> AMANITOID_SPAWN_EGG = REGISTER.register("amanitoid_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.AMANITOID, 0xC8302A, 0xF2EEE0, new Item.Properties()));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.spore.MushroomShieldItem> MUSHROOM_SHIELD = REGISTER.register("mushroom_shield",
            () -> new com.soulinchains.arcaneartifacts.spore.MushroomShieldItem(new Item.Properties().durability(400)));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.spore.MushroomClubItem> MUSHROOM_CLUB = REGISTER.register("mushroom_club",
            () -> new com.soulinchains.arcaneartifacts.spore.MushroomClubItem(new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> GRIBUSHKA_SPAWN_EGG = REGISTER.register("gribushka_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.GRIBUSHKA, 0x8FD3F0, 0x8A4FC8, new Item.Properties()));

    // ------------------------------------------------------------------ Pseudomage
    public static final DeferredItem<ResearcherNotebookItem> FALSE_MAGE_GUIDE = REGISTER.register("false_mage_guide",
            () -> new ResearcherNotebookItem(ResearcherNotebookItem.Book.PSEUDOMAGE));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.pseudo.MagicGunItem> REVOLVER = REGISTER.register("revolver",
            () -> new com.soulinchains.arcaneartifacts.pseudo.MagicGunItem(6, 1, 0.5F, 0, 6, 50));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.pseudo.MagicGunItem> SAWN_OFF = REGISTER.register("sawn_off",
            () -> new com.soulinchains.arcaneartifacts.pseudo.MagicGunItem(2, 6, 9.0F, 2, 12, 40));
    public static final DeferredItem<Item> MAGIC_CARTRIDGE = REGISTER.register("magic_cartridge", () -> new Item(new Item.Properties().stacksTo(16)));
    /** Only the look of a flying bullet; not obtainable. */
    public static final DeferredItem<Item> BULLET = REGISTER.register("bullet", () -> new Item(new Item.Properties()));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.pseudo.BandolierItem> BANDOLIER = REGISTER.register("bandolier",
            com.soulinchains.arcaneartifacts.pseudo.BandolierItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.pseudo.CounterfeitManaItem> COUNTERFEIT_RING = REGISTER.register("counterfeit_ring",
            () -> new com.soulinchains.arcaneartifacts.pseudo.CounterfeitManaItem(com.soulinchains.arcaneartifacts.pseudo.CounterfeitManaItem.Kind.RING));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.pseudo.CounterfeitManaItem> COUNTERFEIT_PENDANT = REGISTER.register("counterfeit_pendant",
            () -> new com.soulinchains.arcaneartifacts.pseudo.CounterfeitManaItem(com.soulinchains.arcaneartifacts.pseudo.CounterfeitManaItem.Kind.PENDANT));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.pseudo.ManaGogglesItem> MANA_GOGGLES = REGISTER.register("mana_goggles",
            com.soulinchains.arcaneartifacts.pseudo.ManaGogglesItem::new);
    public static final DeferredItem<BlockItem> MANA_TANK = REGISTER.registerSimpleBlockItem(ModBlocks.MANA_TANK);
    public static final DeferredItem<Item> AMBER = REGISTER.register("amber", () -> new Item(new Item.Properties()));
    public static final DeferredItem<BlockItem> AMBER_CLUSTER = REGISTER.registerSimpleBlockItem(ModBlocks.AMBER_CLUSTER);
    public static final DeferredItem<BlockItem> LARGE_AMBER_CLUSTER = REGISTER.registerSimpleBlockItem(ModBlocks.LARGE_AMBER_CLUSTER);
    public static final DeferredItem<BlockItem> AMBER_BLOCK = REGISTER.registerSimpleBlockItem(ModBlocks.AMBER_BLOCK);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.pseudo.PonchoItem> PONCHO = REGISTER.register("poncho",
            com.soulinchains.arcaneartifacts.pseudo.PonchoItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.pseudo.SombreroItem> SOMBRERO = REGISTER.register("sombrero",
            com.soulinchains.arcaneartifacts.pseudo.SombreroItem::new);
    public static final DeferredItem<ResearcherNotebookItem> DRUID_HERBARIUM = REGISTER.register("druid_herbarium",
            () -> new ResearcherNotebookItem(ResearcherNotebookItem.Book.DRUID));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.pseudo.SpurBootsItem> PSEUDOMAGE_BOOTS = REGISTER.register("pseudomage_boots",
            com.soulinchains.arcaneartifacts.pseudo.SpurBootsItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.mount.MountItem> FLYING_BROOM = REGISTER.register("flying_broom",
            () -> new com.soulinchains.arcaneartifacts.mount.MountItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), ModEntities.FLYING_BROOM, 3));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.mount.MountItem> MOSS_ELK_CALL = REGISTER.register("moss_elk_call",
            () -> new com.soulinchains.arcaneartifacts.mount.MountItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), ModEntities.MOSS_ELK, 3));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.mount.MountItem> IRON_HORSE = REGISTER.register("iron_horse",
            () -> new com.soulinchains.arcaneartifacts.mount.MountItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), ModEntities.IRON_HORSE, 3));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.pseudo.PseudomageGlovesItem> PSEUDOMAGE_GLOVES = REGISTER.register("pseudomage_gloves",
            com.soulinchains.arcaneartifacts.pseudo.PseudomageGlovesItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.druid.DruidStaffItem> DRUID_STAFF_ROOTS = REGISTER.register("druid_staff_roots",
            () -> new com.soulinchains.arcaneartifacts.druid.DruidStaffItem(com.soulinchains.arcaneartifacts.druid.DruidStaffItem.Circle.ROOTS));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.druid.DruidStaffItem> DRUID_STAFF_GROVE = REGISTER.register("druid_staff_grove",
            () -> new com.soulinchains.arcaneartifacts.druid.DruidStaffItem(com.soulinchains.arcaneartifacts.druid.DruidStaffItem.Circle.GROVE));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.druid.DruidStaffItem> DRUID_STAFF_HIVE = REGISTER.register("druid_staff_hive",
            () -> new com.soulinchains.arcaneartifacts.druid.DruidStaffItem(com.soulinchains.arcaneartifacts.druid.DruidStaffItem.Circle.HIVE));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.druid.DruidStaffItem> DRUID_STAFF_SAP = REGISTER.register("druid_staff_sap",
            () -> new com.soulinchains.arcaneartifacts.druid.DruidStaffItem(com.soulinchains.arcaneartifacts.druid.DruidStaffItem.Circle.SAP));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.druid.DruidStaffItem> DRUID_STAFF_THORNS = REGISTER.register("druid_staff_thorns",
            () -> new com.soulinchains.arcaneartifacts.druid.DruidStaffItem(com.soulinchains.arcaneartifacts.druid.DruidStaffItem.Circle.THORNS));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.druid.DruidStaffItem> DRUID_STAFF_GRAND = REGISTER.register("druid_staff_grand", () -> new com.soulinchains.arcaneartifacts.druid.DruidStaffItem(null));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.druid.DruidGearItem> DRUID_MANTLE = REGISTER.register("druid_mantle",
            () -> new com.soulinchains.arcaneartifacts.druid.DruidGearItem(com.soulinchains.arcaneartifacts.druid.DruidGearItem.Piece.MANTLE));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.druid.DruidGearItem> DRUID_HAT = REGISTER.register("druid_hat",
            () -> new com.soulinchains.arcaneartifacts.druid.DruidGearItem(com.soulinchains.arcaneartifacts.druid.DruidGearItem.Piece.HAT));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.druid.DruidGearItem> DRUID_GLOVES = REGISTER.register("druid_gloves",
            () -> new com.soulinchains.arcaneartifacts.druid.DruidGearItem(com.soulinchains.arcaneartifacts.druid.DruidGearItem.Piece.GLOVES));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.druid.DruidGearItem> DRUID_BOOTS = REGISTER.register("druid_boots",
            () -> new com.soulinchains.arcaneartifacts.druid.DruidGearItem(com.soulinchains.arcaneartifacts.druid.DruidGearItem.Piece.BOOTS));
    public static final DeferredItem<BlockItem> MANA_PIPE = REGISTER.registerSimpleBlockItem(ModBlocks.MANA_PIPE);
    public static final DeferredItem<BlockItem> MANA_PUMP = REGISTER.registerSimpleBlockItem(ModBlocks.MANA_PUMP);
    public static final DeferredItem<BlockItem> GUNSMITH_BENCH = REGISTER.registerSimpleBlockItem(ModBlocks.GUNSMITH_BENCH);
    public static final java.util.Map<com.soulinchains.arcaneartifacts.pseudo.GunPartItem.Part, DeferredItem<com.soulinchains.arcaneartifacts.pseudo.GunPartItem>> GUN_PARTS = gunParts();

    private static java.util.Map<com.soulinchains.arcaneartifacts.pseudo.GunPartItem.Part, DeferredItem<com.soulinchains.arcaneartifacts.pseudo.GunPartItem>> gunParts() {
        var map = new java.util.EnumMap<com.soulinchains.arcaneartifacts.pseudo.GunPartItem.Part, DeferredItem<com.soulinchains.arcaneartifacts.pseudo.GunPartItem>>(
                com.soulinchains.arcaneartifacts.pseudo.GunPartItem.Part.class);
        for (var part : com.soulinchains.arcaneartifacts.pseudo.GunPartItem.Part.values()) {
            map.put(part, REGISTER.register(part.name().toLowerCase(java.util.Locale.ROOT),
                    () -> new com.soulinchains.arcaneartifacts.pseudo.GunPartItem(part)));
        }
        return map;
    }

    private static java.util.Map<com.soulinchains.arcaneartifacts.mushroom.ArcaneMushroom, DeferredItem<BlockItem>> mushrooms() {
        var map = new java.util.EnumMap<com.soulinchains.arcaneartifacts.mushroom.ArcaneMushroom, DeferredItem<BlockItem>>(
                com.soulinchains.arcaneartifacts.mushroom.ArcaneMushroom.class);
        for (var mushroom : com.soulinchains.arcaneartifacts.mushroom.ArcaneMushroom.values()) {
            map.put(mushroom, REGISTER.register(mushroom.id(),
                    () -> new BlockItem(ModBlocks.MUSHROOMS.get(mushroom).get(), new Item.Properties().food(mushroom.food()))));
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    private ModItems() {
    }
}
