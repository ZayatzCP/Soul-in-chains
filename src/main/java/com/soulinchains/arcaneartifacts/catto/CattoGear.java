package com.soulinchains.arcaneartifacts.catto;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * Catto's versions of the player's accessory sets: a piece of headwear and a piece of armor each. Every piece adds to
 * Catto's Source pool and gives its effects; head and armor of one set together give the set bonus.
 */
public final class CattoGear {
    private CattoGear() {
    }

    public enum Slot { HEAD, ARMOR }

    public enum Set {
        /** Arcane: a big Source pool; spells from Catto's weapon cost 30% less. */
        ARCANE,
        /** Pseudomage: one spell in five costs nothing and Catto strikes a quarter faster. */
        PSEUDOMAGE,
        /** Druid: half of the harvests give double, and poison does not stick. */
        DRUID;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component bonus() {
            return Component.translatable("arcane_artifacts.catto.set." + key() + ".bonus");
        }
    }

    public static final class GearItem extends Item {
        public final Set set;
        public final Slot slot;
        public final int armor;
        public final int mana;
        public final java.util.Set<CattoModule> effects;

        public GearItem(Set set, Slot slot, int armor, int mana, CattoModule... effects) {
            super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
            this.set = set;
            this.slot = slot;
            this.armor = armor;
            this.mana = mana;
            this.effects = effects.length == 0 ? EnumSet.noneOf(CattoModule.class) : EnumSet.of(effects[0], effects);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("arcane_artifacts.catto.gear.for_catto",
                    Component.translatable("arcane_artifacts.catto.slot." + (slot == Slot.HEAD ? "head" : "armor"))).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (armor > 0) {
                tooltip.add(Component.translatable("arcane_artifacts.catto.gear.armor", armor).withStyle(ChatFormatting.BLUE));
            }
            if (mana > 0) {
                tooltip.add(Component.translatable("arcane_artifacts.catto.gear.mana", mana).withStyle(ChatFormatting.BLUE));
            }
            for (CattoModule effect : effects) {
                tooltip.add(Component.literal("• ").append(Component.translatable("arcane_artifacts.catto.module." + effect.id() + ".desc"))
                        .withStyle(ChatFormatting.GRAY));
            }
            tooltip.add(Component.translatable("arcane_artifacts.catto.gear.set", Component.translatable("arcane_artifacts.catto.set." + set.key()),
                    set.bonus()).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    /** Catto's own tools: they work as iron tools for anyone, and do more in Catto's paws. */
    public static final class HammerItem extends net.minecraft.world.item.PickaxeItem {
        public HammerItem() {
            super(net.minecraft.world.item.Tiers.IRON, new Item.Properties()
                    .attributes(net.minecraft.world.item.PickaxeItem.createAttributes(net.minecraft.world.item.Tiers.IRON, 2.0F, -3.0F)));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("arcane_artifacts.catto.tool.hammer").withStyle(ChatFormatting.GRAY));
        }
    }

    public static final class ScytheItem extends net.minecraft.world.item.HoeItem {
        public ScytheItem() {
            super(net.minecraft.world.item.Tiers.IRON, new Item.Properties()
                    .attributes(net.minecraft.world.item.HoeItem.createAttributes(net.minecraft.world.item.Tiers.IRON, 1.0F, -2.4F)));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("arcane_artifacts.catto.tool.scythe").withStyle(ChatFormatting.GRAY));
        }
    }

    public static final class HatchetItem extends net.minecraft.world.item.AxeItem {
        public HatchetItem() {
            super(net.minecraft.world.item.Tiers.IRON, new Item.Properties()
                    .attributes(net.minecraft.world.item.AxeItem.createAttributes(net.minecraft.world.item.Tiers.IRON, 5.0F, -3.0F)));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("arcane_artifacts.catto.tool.hatchet").withStyle(ChatFormatting.GRAY));
        }
    }

    public static final class TrowelItem extends net.minecraft.world.item.ShovelItem {
        public TrowelItem() {
            super(net.minecraft.world.item.Tiers.IRON, new Item.Properties()
                    .attributes(net.minecraft.world.item.ShovelItem.createAttributes(net.minecraft.world.item.Tiers.IRON, 1.5F, -3.0F)));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("arcane_artifacts.catto.tool.trowel").withStyle(ChatFormatting.GRAY));
        }
    }

    public static final class RodItem extends net.minecraft.world.item.FishingRodItem {
        public RodItem() {
            super(new Item.Properties().durability(128));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("arcane_artifacts.catto.tool.rod").withStyle(ChatFormatting.GRAY));
        }
    }

    static java.util.Set<CattoModule> none() {
        return EnumSet.noneOf(CattoModule.class);
    }
}
