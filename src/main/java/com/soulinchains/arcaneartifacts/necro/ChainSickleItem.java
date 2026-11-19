package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Chain Sickle (kusarigama): a sickle on a chain. Right click throws the chain and reels the first creature it hooks
 * in to you; sneak + right click hooks a block or creature and reels you to it. A creature killed with it always leaves
 * its soul.
 */
public class ChainSickleItem extends SwordItem {
    public ChainSickleItem() {
        super(Tiers.IRON, new Properties().rarity(Rarity.UNCOMMON).attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.6F)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            level.addFreshEntity(new ChainBolt(level, player, null, player.isShiftKeyDown(), 1.8F));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
            stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
        }
        player.getCooldowns().addCooldown(this, Config.SICKLE_COOLDOWN_TICKS.get());
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        for (int i = 1; i <= 3; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".desc." + i).withStyle(ChatFormatting.GRAY));
        }
    }
}
