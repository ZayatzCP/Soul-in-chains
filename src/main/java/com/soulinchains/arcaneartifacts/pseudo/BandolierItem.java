package com.soulinchains.arcaneartifacts.pseudo;

import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Bandolier (body slot): eight pouches of magic cartridges, 16 in each. Use it in hand (or press the belt
 * key while wearing it without a belt) to open it. Firearms reload from it first; the cartridges inside
 * are the Pseudomage's "false mana".
 */
public class BandolierItem extends ArcaneAccessoryItem {
    public static final int POUCHES = 8;
    public static final int STACK = 16;
    public static final int CAPACITY = POUCHES * STACK;

    public BandolierItem() {
        super(new Item.Properties().rarity(Rarity.UNCOMMON).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY), 2);
    }

    public static NonNullList<ItemStack> pouches(ItemStack bandolier) {
        NonNullList<ItemStack> items = NonNullList.withSize(POUCHES, ItemStack.EMPTY);
        bandolier.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    public static void setPouches(ItemStack bandolier, List<ItemStack> items) {
        bandolier.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    public static int cartridges(ItemStack bandolier) {
        int count = 0;
        for (ItemStack stack : pouches(bandolier)) {
            if (stack.is(ModItems.MAGIC_CARTRIDGE.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** Takes up to {@code amount} cartridges out; returns how many it took. */
    public static int take(ItemStack bandolier, int amount) {
        NonNullList<ItemStack> items = pouches(bandolier);
        int taken = 0;
        for (int i = items.size() - 1; i >= 0 && taken < amount; i--) {
            ItemStack stack = items.get(i);
            if (stack.is(ModItems.MAGIC_CARTRIDGE.get())) {
                int take = Math.min(stack.getCount(), amount - taken);
                stack.shrink(take);
                taken += take;
            }
        }
        setPouches(bandolier, items);
        return taken;
    }

    public static void open(ServerPlayer player, ItemStack bandolier) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new BandolierMenu(id, inventory, bandolier),
                Component.translatable("item.arcane_artifacts.bandolier")));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bandolier = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            open(serverPlayer, bandolier);
        }
        return InteractionResultHolder.sidedSuccess(bandolier, level.isClientSide);
    }

    @Override
    protected void appendState(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("arcane_artifacts.bandolier.count", cartridges(stack), CAPACITY).withStyle(ChatFormatting.GOLD));
    }
}
