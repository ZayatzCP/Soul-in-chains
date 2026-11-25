package com.soulinchains.arcaneartifacts.rite;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Book of Sigils. On the Heart of the Circle: starts a rite, or closes it when it is time; sneaking - reads out the site
 * (size, sigils, the rite they call, the expected quality). Held in use near the Heart, it is how the Reader reads the
 * formula during the channel.
 */
public class SigilBookItem extends Item {
    public SigilBookItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.arcane_artifacts.sigil_book.lore").withStyle(ChatFormatting.GRAY));
        for (Sigil sigil : Sigil.values()) {
            tooltip.add(Component.literal(" ").append(sigil.displayName()).append(Component.literal(" - "))
                    .append(Component.translatable("arcane_artifacts.rite.chalk." + sigil.color.id)).withStyle(sigil.modifier ? ChatFormatting.DARK_AQUA : ChatFormatting.DARK_PURPLE));
        }
    }

    /** Not used for blocks other than the Heart; kept for symmetry with the chalk. */
    static boolean onHeart(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RiteHeartBlockEntity;
    }
}
