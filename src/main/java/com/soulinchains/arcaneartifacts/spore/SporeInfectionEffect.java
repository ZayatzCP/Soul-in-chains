package com.soulinchains.arcaneartifacts.spore;

import com.soulinchains.arcaneartifacts.effect.ArcaneStatusEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Spore Infection: the creature sneezes every 2.5 seconds and passes the spores on to its fellows. */
public class SporeInfectionEffect extends ArcaneStatusEffect {
    public static final int SNEEZE_INTERVAL = 50;

    public SporeInfectionEffect() {
        super(MobEffectCategory.HARMFUL, 0xA88A4A, true);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % SNEEZE_INTERVAL == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && Spores.canCatch(entity)) {
            Spores.sneeze(entity);
        }
        return true;
    }
}
