package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Soul Lantern (belt): where the Shackled keeps the souls it catches. Soul wisps within a few blocks fly into it by
 * themselves. Holds {@code soul_lantern_capacity} souls.
 */
public class SoulLanternItem extends ArcaneAccessoryItem {
    public SoulLanternItem() {
        super(new Item.Properties().rarity(Rarity.UNCOMMON).component(ModComponents.SOUL_STORE, SoulStore.EMPTY), 2);
    }

    public static SoulStore store(ItemStack stack) {
        return stack.getOrDefault(ModComponents.SOUL_STORE, SoulStore.EMPTY);
    }

    public static int capacity(ItemStack stack) {
        return Config.SOUL_LANTERN_CAPACITY.get();
    }

    @Override
    protected void appendState(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SoulStore store = store(stack);
        tooltip.add(Component.translatable("arcane_artifacts.soul_lantern.count", store.total(), capacity(stack)).withStyle(ChatFormatting.AQUA));
        for (SoulType type : SoulType.values()) {
            int n = store.count(type);
            if (n > 0) {
                tooltip.add(Component.literal("  ").append(Component.translatable(type.translationKey()))
                        .append(" ×" + n).withColor(type.color));
            }
        }
    }
}
