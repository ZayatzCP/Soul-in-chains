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
    public static final DeferredItem<FocusingGlovesItem> FOCUSING_GLOVES = REGISTER.register("focusing_gloves", FocusingGlovesItem::new);
    public static final DeferredItem<RingOfReversalItem> RING_OF_REVERSAL = REGISTER.register("ring_of_reversal", RingOfReversalItem::new);
    public static final DeferredItem<MantleOfReflectionItem> MANTLE_OF_REFLECTION = REGISTER.register("mantle_of_reflection", MantleOfReflectionItem::new);

    public static final DeferredItem<com.soulinchains.arcaneartifacts.item.SpellStaffItem> SPELL_STAFF =
            REGISTER.register("spell_staff", com.soulinchains.arcaneartifacts.item.SpellStaffItem::new);
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
    public static final DeferredItem<BlockItem> MYCELIUM_SOIL = REGISTER.registerSimpleBlockItem(ModBlocks.MYCELIUM_SOIL);
    public static final DeferredItem<BlockItem> CAVE_MYCELIUM = REGISTER.registerSimpleBlockItem(ModBlocks.CAVE_MYCELIUM);
    public static final DeferredItem<BlockItem> SPORE_CLAY = REGISTER.registerSimpleBlockItem(ModBlocks.SPORE_CLAY);
    public static final DeferredItem<BlockItem> MYCELIAL_MUD = REGISTER.registerSimpleBlockItem(ModBlocks.MYCELIAL_MUD);
    public static final DeferredItem<BlockItem> MUDZYA_HIVE = REGISTER.registerSimpleBlockItem(ModBlocks.MUDZYA_HIVE);
    public static final DeferredItem<BlockItem> WILD_MUDZYA_HIVE = REGISTER.registerSimpleBlockItem(ModBlocks.WILD_MUDZYA_HIVE);
    public static final DeferredItem<BlockItem> ALCHEMICAL_DISTILLER = REGISTER.registerSimpleBlockItem(ModBlocks.ALCHEMICAL_DISTILLER);
    public static final java.util.Map<com.soulinchains.arcaneartifacts.mushroom.ArcaneMushroom, DeferredItem<BlockItem>> MUSHROOMS = mushrooms();
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> MUDZYA_SPAWN_EGG = REGISTER.register("mudzya_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.MUDZYA, 0x6B3FA0, 0xE0A8FF, new Item.Properties()));
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
    public static final DeferredItem<com.soulinchains.arcaneartifacts.pseudo.PonchoItem> PONCHO = REGISTER.register("poncho",
            com.soulinchains.arcaneartifacts.pseudo.PonchoItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.pseudo.SombreroItem> SOMBRERO = REGISTER.register("sombrero",
            com.soulinchains.arcaneartifacts.pseudo.SombreroItem::new);
    public static final DeferredItem<ResearcherNotebookItem> DRUID_HERBARIUM = REGISTER.register("druid_herbarium",
            () -> new ResearcherNotebookItem(ResearcherNotebookItem.Book.DRUID));
    public static final DeferredItem<com.soulinchains.arcaneartifacts.pseudo.SpurBootsItem> PSEUDOMAGE_BOOTS = REGISTER.register("pseudomage_boots",
            com.soulinchains.arcaneartifacts.pseudo.SpurBootsItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.druid.DruidGearItem> DRUID_MANTLE = REGISTER.register("druid_mantle",
            com.soulinchains.arcaneartifacts.druid.DruidGearItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.druid.DruidGearItem> DRUID_HAT = REGISTER.register("druid_hat",
            com.soulinchains.arcaneartifacts.druid.DruidGearItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.druid.DruidGearItem> DRUID_GLOVES = REGISTER.register("druid_gloves",
            com.soulinchains.arcaneartifacts.druid.DruidGearItem::new);
    public static final DeferredItem<com.soulinchains.arcaneartifacts.druid.DruidGearItem> DRUID_BOOTS = REGISTER.register("druid_boots",
            com.soulinchains.arcaneartifacts.druid.DruidGearItem::new);
    public static final DeferredItem<BlockItem> MANA_PIPE = REGISTER.registerSimpleBlockItem(ModBlocks.MANA_PIPE);
    public static final DeferredItem<BlockItem> MANA_PUMP = REGISTER.registerSimpleBlockItem(ModBlocks.MANA_PUMP);

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
