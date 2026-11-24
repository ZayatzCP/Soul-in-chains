package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Witness's Ritual Knife (Confessor): lights the seal for the Pairing, starts the rite, strikes the seal in the
 * inscription windows and closes the rite. A poor weapon on purpose.
 */
public class RitualKnifeItem extends SwordItem {
    public RitualKnifeItem() {
        super(Tiers.IRON, new Properties().rarity(Rarity.UNCOMMON).attributes(SwordItem.createAttributes(Tiers.IRON, 0, -1.8F)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (int i = 1; i <= 3; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".desc." + i).withStyle(ChatFormatting.GRAY));
        }
    }
}
