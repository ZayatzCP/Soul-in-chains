package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Shackle Key: the Jailer's key. Right click to open every lock on you and your allies within 4 blocks: Shackled,
 * Entangled and the Curse of Shackles fall away. 16 uses. It will also be needed to free a certain king.
 */
public class ShackleKeyItem extends Item {
    public ShackleKeyItem() {
        super(new Properties().durability(16).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int freed = 0;
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(4),
                    e -> e == player || e.isAlliedTo(player) || player.isAlliedTo(e))) {
                boolean any = living.removeEffect(ModEffects.SHACKLED) | living.removeEffect(ModEffects.ENTANGLED) | living.removeEffect(ModEffects.CURSED_CHAINS);
                if (any) {
                    freed++;
                }
            }
            level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 1.4F);
            player.displayClientMessage(Component.translatable("arcane_artifacts.shackle_key.freed", freed), true);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.1").withStyle(ChatFormatting.GRAY));
    }
}
