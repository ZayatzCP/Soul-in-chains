package com.soulinchains.arcaneartifacts.necro;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The Jailer's Notes: how to build the Throne of Chains, written into the tooltip. Found in crypts and Soul Dungeons. */
public class JailerNotesItem extends Item {
    public static final int LINES = 6;

    public JailerNotesItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (int i = 1; i <= LINES; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".desc." + i).withStyle(i == 1 ? ChatFormatting.DARK_AQUA : ChatFormatting.GRAY));
        }
    }
}
