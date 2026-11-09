package com.soulinchains.arcaneartifacts.item;

import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** Gilded Fermented Spider Eye: eaten, or pressed onto a creature, it gives Pity. Also the Pity glyph's catalyst. */
public class GildedSpiderEyeItem extends Item {
    public GildedSpiderEyeItem() {
        super(new Item.Properties().rarity(Rarity.UNCOMMON).food(ArcaneFoods.GILDED_FERMENTED_SPIDER_EYE));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(player.level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        target.addEffect(new MobEffectInstance(ModEffects.PITY, 600, 0), player);
        level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getEyeY(), target.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, target.blockPosition(), SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 1f, 1.4f);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
