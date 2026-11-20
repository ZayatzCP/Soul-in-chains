package com.soulinchains.arcaneartifacts.catto;

import com.hollingsworth.arsnouveau.api.registry.RitualRegistry;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;

/** Everything Catto brings into the game: the entity, its items, its menu and the bonding ritual. */
public final class CattoContent {
    private CattoContent() {
    }

    public static final DeferredHolder<EntityType<?>, EntityType<Catto>> CATTO = ModEntities.REGISTER.register("catto",
            () -> EntityType.Builder.of(Catto::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.9F).eyeHeight(0.75F).passengerAttachments(0.5125F).clientTrackingRange(10)
                    .build(ArcaneArtifacts.MODID + ":catto"));

    public static final DeferredHolder<MenuType<?>, MenuType<CattoMenu>> MENU = ModBlocks.MENUS.register("catto",
            () -> IMenuTypeExtension.create(CattoMenu::new));

    /** Every Catto item, in creative tab order. */
    public static final List<DeferredItem<? extends Item>> ITEMS = new ArrayList<>();

    public static final DeferredItem<Item> ARCANE_COLLAR = item("arcane_collar",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<CattoItems.BellItem> BELL = item("catto_bell", CattoItems.BellItem::new);
    public static final DeferredItem<CattoItems.WandItem> WAND = item("errand_wand", CattoItems.WandItem::new);
    public static final DeferredItem<CattoItems.ScrollItem> SCROLL = item("errand_scroll", CattoItems.ScrollItem::new);
    public static final DeferredItem<CattoItems.BlueprintItem> BLUEPRINT = item("blueprint_parchment", CattoItems.BlueprintItem::new);
    public static final DeferredItem<CattoItems.PackItem> TRAVELER_PACK = item("traveler_pack", () -> new CattoItems.PackItem(9, Rarity.COMMON));
    public static final DeferredItem<CattoItems.PackItem> EXPEDITION_PACK = item("expedition_pack", () -> new CattoItems.PackItem(18, Rarity.UNCOMMON));
    public static final DeferredItem<CattoItems.PackItem> BOTTOMLESS_PACK = item("bottomless_pack", () -> new CattoItems.PackItem(27, Rarity.RARE));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> SPAWN_EGG = item("catto_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(CATTO, 0xECDAB4, 0x4E2E78, new Item.Properties()));
    // Catto's gear: headwear and armor of three sets.
    public static final DeferredItem<CattoGear.GearItem> ARCANE_HOOD = item("arcane_catto_hood",
            () -> new CattoGear.GearItem(CattoGear.Set.ARCANE, CattoGear.Slot.HEAD, 1, 300, CattoModule.KEEN_NOSE, CattoModule.FIREFLY));
    public static final DeferredItem<CattoGear.GearItem> ARCANE_MANTLE = item("arcane_catto_mantle",
            () -> new CattoGear.GearItem(CattoGear.Set.ARCANE, CattoGear.Slot.ARMOR, 3, 300, CattoModule.SOURCE_FUR));
    public static final DeferredItem<CattoGear.GearItem> SOMBRERITO = item("catto_sombrero",
            () -> new CattoGear.GearItem(CattoGear.Set.PSEUDOMAGE, CattoGear.Slot.HEAD, 1, 100, CattoModule.MAGNET_WHISKER));
    public static final DeferredItem<CattoGear.GearItem> PONCHITO = item("catto_poncho",
            () -> new CattoGear.GearItem(CattoGear.Set.PSEUDOMAGE, CattoGear.Slot.ARMOR, 5, 100, CattoModule.CAT_BOOTS));
    public static final DeferredItem<CattoGear.GearItem> DRUID_WREATH = item("catto_druid_wreath",
            () -> new CattoGear.GearItem(CattoGear.Set.DRUID, CattoGear.Slot.HEAD, 1, 200, CattoModule.GREEN_THUMB));
    public static final DeferredItem<CattoGear.GearItem> DRUID_CLOAK = item("catto_druid_cloak",
            () -> new CattoGear.GearItem(CattoGear.Set.DRUID, CattoGear.Slot.ARMOR, 3, 200, CattoModule.REGROWTH));
    // Catto's tools.
    public static final DeferredItem<CattoGear.HammerItem> HAMMER = item("catto_hammer", CattoGear.HammerItem::new);
    public static final DeferredItem<CattoGear.ScytheItem> SCYTHE = item("catto_scythe", CattoGear.ScytheItem::new);
    public static final DeferredItem<CattoGear.HatchetItem> HATCHET = item("catto_hatchet", CattoGear.HatchetItem::new);
    public static final DeferredItem<CattoGear.TrowelItem> TROWEL = item("catto_trowel", CattoGear.TrowelItem::new);
    public static final DeferredItem<CattoGear.RodItem> ROD = item("catto_rod", CattoGear.RodItem::new);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> CUSHION = item("catto_cushion",
            () -> new net.minecraft.world.item.BlockItem(CattoVillage.CUSHION.get(), new Item.Properties()));

    private static <T extends Item> DeferredItem<T> item(String name, java.util.function.Supplier<T> factory) {
        DeferredItem<T> item = ModItems.REGISTER.register(name, factory);
        ITEMS.add(item);
        return item;
    }

    /** Called from the mod constructor: the ritual must be known before Ars registers its tablet items. */
    public static void init() {
        RitualRegistry.registerRitual(new FamiliarBondRitual());
        RitualRegistry.registerRitual(new com.soulinchains.arcaneartifacts.necro.AwakeningRitual());
        RitualRegistry.registerRitual(new com.soulinchains.arcaneartifacts.necro.EternalBindingRitual());
        RitualRegistry.registerRitual(new com.soulinchains.arcaneartifacts.necro.ConversionRitual());
    }
}
