package com.soulinchains.arcaneartifacts.necro;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

/**
 * Executioner's Axe (Chainlord): a heavy two-hander. In a Chainlord's hands it beheads a creature left with 15% health
 * or less (not bosses), and the soul of an executed creature comes out one step stronger.
 */
public class ExecutionerAxeItem extends AxeItem {
    public static final float EXECUTE_SHARE = 0.15F;
    public static final String EXECUTED_TAG = "arcane_artifacts:executed";

    public ExecutionerAxeItem() {
        super(Tiers.DIAMOND, new Properties().rarity(Rarity.EPIC).attributes(AxeItem.createAttributes(Tiers.DIAMOND, 8.0F, -3.3F)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (attacker instanceof Player player && Necro.rank(player) >= Necro.CHAINLORD && target.isAlive()
                && !target.getType().is(Tags.EntityTypes.BOSSES) && target.getHealth() <= target.getMaxHealth() * Relics.executeShare(player)
                && player.level() instanceof ServerLevel level) {
            target.addTag(EXECUTED_TAG);
            target.hurt(level.damageSources().playerAttack(player), Float.MAX_VALUE);
            if (target.isAlive()) {
                target.removeTag(EXECUTED_TAG);
            }
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.8, target.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 0.5F);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
