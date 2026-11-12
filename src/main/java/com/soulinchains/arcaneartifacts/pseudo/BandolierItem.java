package com.soulinchains.arcaneartifacts.pseudo;

import com.soulinchains.arcaneartifacts.registry.ModComponents;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Bandolier (body slot): carries up to {@link #CAPACITY} magic cartridges for the Pseudomage's firearms
 * and a little false mana. Use it in hand to load every cartridge from the inventory into it; crouch +
 * use to take them out again.
 */
public class BandolierItem extends CounterfeitManaItem {
    public static final int CAPACITY = 128;

    public BandolierItem() {
        super(20, 3);
    }

    public static int cartridges(ItemStack stack) {
        return stack.getOrDefault(ModComponents.CARTRIDGES, 0);
    }

    public static void setCartridges(ItemStack stack, int count) {
        stack.set(ModComponents.CARTRIDGES, Math.max(0, Math.min(CAPACITY, count)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bandolier = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(bandolier);
        }
        int inside = cartridges(bandolier);
        if (player.isShiftKeyDown()) {
            if (inside > 0) {
                int taken = Math.min(inside, 64);
                setCartridges(bandolier, inside - taken);
                ItemStack out = new ItemStack(ModItems.MAGIC_CARTRIDGE.get(), taken);
                if (!player.getInventory().add(out)) {
                    player.drop(out, false);
                }
            }
        } else {
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize() && inside < CAPACITY; i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.is(ModItems.MAGIC_CARTRIDGE.get())) {
                    int moved = Math.min(stack.getCount(), CAPACITY - inside);
                    stack.shrink(moved);
                    inside += moved;
                }
            }
            setCartridges(bandolier, inside);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1f, 1.2f);
        player.displayClientMessage(Component.translatable("arcane_artifacts.bandolier.count", cartridges(bandolier), CAPACITY)
                .withStyle(ChatFormatting.GOLD), true);
        return InteractionResultHolder.consume(bandolier);
    }

    @Override
    protected void appendState(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("arcane_artifacts.bandolier.count", cartridges(stack), CAPACITY).withStyle(ChatFormatting.GOLD));
    }
}
