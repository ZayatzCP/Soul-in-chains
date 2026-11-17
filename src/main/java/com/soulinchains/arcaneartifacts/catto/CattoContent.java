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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Everything Catto brings into the game: the entity, its items, its menu and the bonding ritual. */
public final class CattoContent {
    private CattoContent() {
    }

    public static final DeferredHolder<EntityType<?>, EntityType<Catto>> CATTO = ModEntities.REGISTER.register("catto",
            () -> EntityType.Builder.of(Catto::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.7F).eyeHeight(0.35F).passengerAttachments(0.5125F).clientTrackingRange(10)
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
    public static final Map<CattoModule, DeferredItem<CattoItems.ModuleItem>> MODULES = new EnumMap<>(CattoModule.class);

    static {
        for (CattoModule module : CattoModule.values()) {
            MODULES.put(module, item(module.id(), () -> new CattoItems.ModuleItem(module)));
        }
    }

    private static <T extends Item> DeferredItem<T> item(String name, java.util.function.Supplier<T> factory) {
        DeferredItem<T> item = ModItems.REGISTER.register(name, factory);
        ITEMS.add(item);
        return item;
    }

    /** Called from the mod constructor: the ritual must be known before Ars registers its tablet items. */
    public static void init() {
        RitualRegistry.registerRitual(new FamiliarBondRitual());
    }
}
