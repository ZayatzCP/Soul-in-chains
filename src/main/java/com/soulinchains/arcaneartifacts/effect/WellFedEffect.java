package com.soulinchains.arcaneartifacts.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

/** Well Fed (Satiety glyph): keeps the player's hunger and saturation topped up while it lasts. */
public class WellFedEffect extends MobEffect {
    public WellFedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE8A23A);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            FoodData food = player.getFoodData();
            food.setFoodLevel(20);
            food.setSaturation(Math.max(food.getSaturationLevel(), 5.0f + amplifier * 2.5f));
        }
        return true;
    }
}
