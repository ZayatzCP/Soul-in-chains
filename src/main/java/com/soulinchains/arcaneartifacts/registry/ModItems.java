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
    public static final DeferredItem<ResearcherNotebookItem> RESEARCHER_NOTEBOOK = REGISTER.register("researcher_notebook", ResearcherNotebookItem::new);

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
    public static final DeferredItem<com.soulinchains.arcaneartifacts.accessory.SpellQuiverItem> SPELL_QUIVER =
            REGISTER.register("spell_quiver", com.soulinchains.arcaneartifacts.accessory.SpellQuiverItem::new);

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

    private ModItems() {
    }
}
