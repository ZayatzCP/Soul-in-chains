package com.soulinchains.arcaneartifacts.pseudo;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A part for Farraday's Workshop: fitted into a revolver or a sawn-off, one per slot (barrel, cylinder, stock). */
public class GunPartItem extends Item {
    public enum Slot { BARREL, CYLINDER, STOCK }

    public enum Part {
        /** Spread halved, bullets 40% faster. */
        LONG_BARREL(Slot.BARREL),
        /** Fires 25% more often, spread 50% wider. */
        SHORT_BARREL(Slot.BARREL),
        /** Magazine half as large again. */
        DRUM_CYLINDER(Slot.CYLINDER),
        /** Reloads twice as fast. */
        SPEED_LOADER(Slot.CYLINDER),
        /** 20% of shots use no round. */
        AMBER_STOCK(Slot.STOCK),
        /** 15% of shots fire a second volley. */
        HORSESHOE_STOCK(Slot.STOCK);

        public final Slot slot;

        Part(Slot slot) {
            this.slot = slot;
        }
    }

    private final Part part;

    public GunPartItem(Part part) {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        this.part = part;
    }

    public Part part() {
        return part;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("arcane_artifacts.gun_part.slot." + part.slot.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
