package com.soulinchains.arcaneartifacts.belt;

import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Belt of Bottomless Pouches (belt slot): three pouches, each holding any amount of one kind of item. */
public class BottomlessBeltItem extends ArcaneAccessoryItem {
    public BottomlessBeltItem() {
        super(new Item.Properties().rarity(Rarity.EPIC).component(ModComponents.BELT_CONTENTS, BeltContents.EMPTY), 3);
    }

    public static BeltContents contents(ItemStack stack) {
        return stack.getOrDefault(ModComponents.BELT_CONTENTS, BeltContents.EMPTY);
    }

    @Override
    protected void appendState(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BeltContents contents = contents(stack);
        for (int i = 0; i < BeltContents.POUCHES; i++) {
            BeltContents.Pouch pouch = contents.get(i);
            if (pouch.isEmpty()) {
                tooltip.add(Component.translatable("arcane_artifacts.belt.pouch_empty", i + 1).withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltip.add(Component.translatable("arcane_artifacts.belt.pouch", i + 1, pouch.type().getHoverName(),
                        BeltContents.format(pouch.count())).withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
