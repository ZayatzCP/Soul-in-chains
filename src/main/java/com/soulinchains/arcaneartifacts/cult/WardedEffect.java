package com.soulinchains.arcaneartifacts.cult;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** The Seal of Shelter: 60% less damage and no knockback while it holds. */
public class WardedEffect extends MobEffect {
    public WardedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xB9463D);
        addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, ArcaneArtifacts.id("effect.warded_knockback"), 1.0, AttributeModifier.Operation.ADD_VALUE);
    }
}
