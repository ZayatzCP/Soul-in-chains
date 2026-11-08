package com.soulinchains.arcaneartifacts.team;

import com.soulinchains.arcaneartifacts.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
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

import java.util.List;
import java.util.UUID;

/**
 * Scroll of the Oath of Aid. The first use founds a company (named after the scroll; rename it on an
 * anvil first). Used with a suit charm in the other hand, it swears the charm to that company forever.
 * Bearers of charms sworn to the same company cannot hurt each other.
 */
public class OathScrollItem extends Item {
    public OathScrollItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack scroll = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (level.isClientSide) {
            return InteractionResultHolder.success(scroll);
        }
        TeamBond bond = TeamBond.of(scroll);
        if (bond == null) {
            String name = scroll.has(DataComponents.CUSTOM_NAME) ? scroll.getHoverName().getString()
                    : Component.translatable("arcane_artifacts.oath.default_name", player.getName()).getString();
            bond = new TeamBond(UUID.randomUUID(), name);
            scroll.set(ModComponents.TEAM_BOND, bond);
            player.displayClientMessage(Component.translatable("arcane_artifacts.oath.founded", name).withStyle(ChatFormatting.GOLD), false);
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.8f);
            if (!(other.getItem() instanceof SuitCharmItem)) {
                return InteractionResultHolder.consume(scroll);
            }
        }
        if (!(other.getItem() instanceof SuitCharmItem)) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.oath.need_charm").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(scroll);
        }
        TeamBond existing = TeamBond.of(other);
        if (existing != null) {
            player.displayClientMessage(Component.translatable(existing.team().equals(bond.team())
                    ? "arcane_artifacts.oath.already_ours" : "arcane_artifacts.oath.already_sworn", existing.name()).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(scroll);
        }
        other.set(ModComponents.TEAM_BOND, bond);
        player.displayClientMessage(Component.translatable("arcane_artifacts.oath.sworn", other.getHoverName(), bond.name()).withStyle(ChatFormatting.GOLD), false);
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1.2f);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.4f, 1.6f);
        return InteractionResultHolder.consume(scroll);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return TeamBond.of(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        TeamBond bond = TeamBond.of(stack);
        if (bond != null) {
            tooltip.add(Component.translatable("arcane_artifacts.oath.company", bond.name()).withStyle(ChatFormatting.GOLD));
        }
        for (int i = 1; i <= 3; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".desc." + i).withStyle(ChatFormatting.GRAY));
        }
    }
}
