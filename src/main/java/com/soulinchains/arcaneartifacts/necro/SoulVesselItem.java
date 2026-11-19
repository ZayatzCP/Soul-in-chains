package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Soul Vessel: one soul outside the lantern - to keep, trade or give away. Right click an empty vessel: the weakest soul
 * from your lantern goes in (sneak: the strongest). Right click a full one: the soul goes back to your lantern.
 */
public class SoulVesselItem extends Item {
    public SoulVesselItem() {
        super(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    @Nullable
    public static SoulType soul(ItemStack stack) {
        Integer id = stack.get(ModComponents.VESSEL_SOUL);
        return id == null ? null : SoulType.byOrdinal(id);
    }

    public static ItemStack filled(Item item, SoulType type) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.VESSEL_SOUL, type.ordinal());
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        SoulType inside = soul(stack);
        if (inside != null) {
            if (!Souls.store(player, inside)) {
                player.displayClientMessage(Component.translatable("arcane_artifacts.soul_vessel.no_room"), true);
                return InteractionResultHolder.fail(stack);
            }
            stack.shrink(1);
            give(player, new ItemStack(this));
            return InteractionResultHolder.consume(stack);
        }
        SoulType pick = pick(player, player.isShiftKeyDown());
        if (pick == null || !Souls.take(player, pick)) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.necro.no_souls"), true);
            return InteractionResultHolder.fail(stack);
        }
        stack.shrink(1);
        give(player, filled(this, pick));
        level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8F, 0.6F);
        return InteractionResultHolder.consume(stack);
    }

    @Nullable
    private static SoulType pick(Player player, boolean strongest) {
        SoulType best = null;
        for (SoulType type : SoulType.values()) {
            if (Souls.count(player, type) > 0 && (best == null || (strongest ? type.strength > best.strength : type.strength < best.strength))) {
                best = type;
            }
        }
        return best;
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        SoulType type = soul(stack);
        return type == null ? super.getName(stack) : Component.translatable("item.arcane_artifacts.soul_vessel.filled", Component.translatable(type.translationKey()));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return soul(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SoulType type = soul(stack);
        if (type != null) {
            tooltip.add(Component.translatable(type.translationKey()).withColor(type.color));
        }
        tooltip.add(Component.translatable("item.arcane_artifacts.soul_vessel.desc." + (type == null ? 1 : 2)).withStyle(ChatFormatting.GRAY));
    }
}
