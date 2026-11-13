package com.soulinchains.arcaneartifacts.spore;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Mushroom Club: slow and heavy (9 damage, 0.7 attacks per second). Every hit dusts the target with spores
 * like the spore cloud does, without the Speed: spore damage and the Spore Infection.
 */
public class MushroomClubItem extends SwordItem {
    public static final float SPORE_DAMAGE = 2.0F;

    public MushroomClubItem(Properties properties) {
        super(Tiers.IRON, properties.attributes(SwordItem.createAttributes(Tiers.IRON, 6, -3.3F)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        dust(target);
        return result;
    }

    /** The club's spores on {@code target}: the Spore Infection and, a moment later, spore damage. */
    public static void dust(LivingEntity target) {
        if (target.level() instanceof ServerLevel level && !Spores.isMycelial(target)) {
            Spores.infect(target, Spores.INFECTION_TICKS);
            // The spores settle a moment after the blow, so they are not swallowed by its invulnerability.
            SporeDust.settle(target);
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                    16, 0.4, 0.4, 0.4, 0.02);
        }
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.MUSHROOM_STEM);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.1").withStyle(ChatFormatting.GRAY));
    }
}
