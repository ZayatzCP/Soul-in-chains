package com.soulinchains.arcaneartifacts.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;

/**
 * A mob effect that only displays/mirrors state owned by game logic (a parasitic link, a cooldown, a
 * mark). It cannot be cured by milk or totems, so the icon never gets out of sync with the real state;
 * the logic that owns the state removes the effect itself.
 */
public class ArcaneStatusEffect extends MobEffect {
    private final boolean curable;

    public ArcaneStatusEffect(MobEffectCategory category, int color, boolean curable) {
        super(category, color);
        this.curable = curable;
    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
        if (curable) {
            super.fillEffectCures(cures, effectInstance);
        }
    }
}
