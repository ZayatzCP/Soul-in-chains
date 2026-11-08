package com.soulinchains.arcaneartifacts.accessory;

import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

/**
 * Amulet of Opposites (necklace slot). Right-click it in the hand to flip its polarity (the texture
 * flips too).
 * <ul>
 *     <li>Attraction: dropped items and experience orbs in a wide radius fly to the wearer.</li>
 *     <li>Repulsion: they are pushed away from the wearer, except while the wearer crouches, so the
 *     wearer can still walk up and take them.</li>
 * </ul>
 * Items the wearer has just thrown are left alone (so dropping things still works).
 */
public class AmuletOfOppositesItem extends ArcaneAccessoryItem {
    public AmuletOfOppositesItem() {
        super(new Item.Properties().rarity(Rarity.RARE).component(ModComponents.REPELLING, false), 2);
    }

    public static boolean isRepelling(ItemStack stack) {
        return stack.getOrDefault(ModComponents.REPELLING, false);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean repel = !isRepelling(stack);
        stack.set(ModComponents.REPELLING, repel);
        level.playSound(player, player.blockPosition(), repel ? SoundEvents.AMETHYST_BLOCK_BREAK : SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, repel ? 0.6f : 1.4f);
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(repel ? "arcane_artifacts.amulet.repel" : "arcane_artifacts.amulet.attract")
                    .withStyle(repel ? ChatFormatting.RED : ChatFormatting.AQUA), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        Level level = wearer.level();
        if (level.isClientSide || slotContext.cosmetic() || !(wearer instanceof Player player) || player.isSpectator()) {
            return;
        }
        boolean repel = isRepelling(stack);
        if (repel && player.isShiftKeyDown()) {
            return;
        }
        double radius = repel ? Config.AMULET_REPEL_RADIUS.get() : Config.AMULET_ATTRACT_RADIUS.get();
        Vec3 center = player.position().add(0, 0.75, 0);
        AABB area = player.getBoundingBox().inflate(radius);
        for (Entity entity : level.getEntities(player, area, e -> e instanceof ItemEntity || e instanceof ExperienceOrb)) {
            if (entity instanceof ItemEntity item && (item.getItem().isEmpty()
                    || (item.hasPickUpDelay() && player.getUUID().equals(item.getOwner() == null ? null : item.getOwner().getUUID())))) {
                continue;
            }
            Vec3 offset = center.subtract(entity.position());
            double distance = offset.length();
            if (distance > radius || distance < 0.01) {
                continue;
            }
            Vec3 dir = offset.normalize();
            if (repel) {
                double push = 0.12 * (1.0 - distance / radius) + 0.04;
                entity.setDeltaMovement(entity.getDeltaMovement().add(dir.scale(-push)).add(0, 0.01, 0));
            } else {
                double speed = Math.min(0.6, 0.08 + 0.05 * distance);
                entity.setDeltaMovement(dir.scale(speed));
            }
            entity.hurtMarked = true;
        }
    }

    @Override
    protected void appendState(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean repel = isRepelling(stack);
        tooltip.add(Component.translatable(repel ? "arcane_artifacts.amulet.mode_repel" : "arcane_artifacts.amulet.mode_attract")
                .withStyle(repel ? ChatFormatting.RED : ChatFormatting.AQUA));
    }
}
