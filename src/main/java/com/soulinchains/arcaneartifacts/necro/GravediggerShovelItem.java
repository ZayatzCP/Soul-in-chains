package com.soulinchains.arcaneartifacts.necro;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Gravedigger's Shovel: an iron shovel that also digs graves up (right click a grave). */
public class GravediggerShovelItem extends ShovelItem {
    public GravediggerShovelItem() {
        super(Tiers.IRON, new Properties().rarity(Rarity.UNCOMMON).attributes(ShovelItem.createAttributes(Tiers.IRON, 1.5F, -3.0F)));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof GraveBlock) {
            if (context.getLevel() instanceof ServerLevel level && GraveBlock.dig(level, context.getClickedPos(), context.getPlayer())) {
                if (context.getPlayer() != null) {
                    context.getItemInHand().hurtAndBreak(2, context.getPlayer(), net.minecraft.world.entity.LivingEntity.getSlotForHand(context.getHand()));
                }
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.1").withStyle(ChatFormatting.GRAY));
    }
}
