package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The Oath Shield (Warden): confirms the Pairing, lights the anchors and mends them during the rite. A full shield too. */
public class OathShieldItem extends ShieldItem {
    public OathShieldItem() {
        super(new Item.Properties().durability(500).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (int i = 1; i <= 3; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".desc." + i).withStyle(ChatFormatting.GRAY));
        }
    }
}
