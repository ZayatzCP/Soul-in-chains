package com.soulinchains.arcaneartifacts.necro;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Ritual Dagger: a quick, light blade. A creature killed with it always leaves its soul; killing a shackled creature is
 * a proper ritual and washes {@link #CORRUPTION_WASH} Corruption away.
 */
public class RitualDaggerItem extends SwordItem {
    public static final float CORRUPTION_WASH = 1.0F;

    public RitualDaggerItem() {
        super(Tiers.IRON, new Properties().rarity(Rarity.UNCOMMON).attributes(SwordItem.createAttributes(Tiers.IRON, 1, -1.6F)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        for (int i = 1; i <= 2; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".desc." + i).withStyle(ChatFormatting.GRAY));
        }
    }
}
