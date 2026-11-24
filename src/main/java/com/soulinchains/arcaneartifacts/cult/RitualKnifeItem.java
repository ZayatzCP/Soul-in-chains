package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Witness's Ritual Knife (Confessor): lights the seal for the Pairing, starts the rite, strikes the seal in the
 * inscription windows and closes the rite. A poor weapon on purpose.
 */
public class RitualKnifeItem extends SwordItem {
    public RitualKnifeItem() {
        super(Tiers.IRON, new Properties().rarity(Rarity.UNCOMMON).attributes(SwordItem.createAttributes(Tiers.IRON, 0, -1.8F)));
    }

    /** With an inlay material in the other hand: set it into the handle (one at a time). */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
                                                                      net.minecraft.world.InteractionHand hand) {
        ItemStack knife = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == net.minecraft.world.InteractionHand.MAIN_HAND
                ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND);
        KnifeInlay inlay = KnifeInlay.forMaterial(other);
        if (inlay == KnifeInlay.NONE) {
            return net.minecraft.world.InteractionResultHolder.pass(knife);
        }
        if (!level.isClientSide) {
            knife.set(com.soulinchains.arcaneartifacts.registry.ModComponents.KNIFE_INLAY, inlay.ordinal());
            other.consume(1, player);
            player.displayClientMessage(Component.translatable("arcane_artifacts.knife_inlay.set", inlay.displayName()), true);
            level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.SMITHING_TABLE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(knife, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (int i = 1; i <= 3; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".desc." + i).withStyle(ChatFormatting.GRAY));
        }
        KnifeInlay inlay = KnifeInlay.of(stack);
        tooltip.add(Component.translatable("arcane_artifacts.knife_inlay.current", inlay.displayName()).withStyle(ChatFormatting.DARK_RED));
    }
}
