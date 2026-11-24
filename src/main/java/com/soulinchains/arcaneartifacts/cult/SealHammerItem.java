package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Seal-Wright's Hammer (Warden): 8 damage, slow. On an anchor during the rite it mends +40; a blow on a creature
 * sounds the Call (ordinary hostiles within 8 turn on you, every 10 s); sneak + right click in the air raises the Seal of
 * Shelter on your Confessor.
 */
public class SealHammerItem extends SwordItem {
    public SealHammerItem() {
        super(Tiers.IRON, new Properties().rarity(Rarity.UNCOMMON).attributes(SwordItem.createAttributes(Tiers.IRON, 5, -3.1F)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (attacker instanceof Player warden && !warden.level().isClientSide) {
            CultTools.call(warden);
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            Player confessor = Cult.get(player).partner().map(level::getPlayerByUUID).orElse(null);
            if (CultTools.ward(player, confessor)) {
                player.swing(hand, true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (int i = 1; i <= 3; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".desc." + i).withStyle(ChatFormatting.GRAY));
        }
    }
}
