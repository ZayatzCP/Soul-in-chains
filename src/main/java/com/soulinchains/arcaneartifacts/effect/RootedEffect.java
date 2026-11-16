package com.soulinchains.arcaneartifacts.effect;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Entangled: vines hold the creature in place, it can neither walk nor jump. Milk cures it. */
public class RootedEffect extends MobEffect {
    public RootedEffect() {
        super(MobEffectCategory.HARMFUL, 0x4F8A2F);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ArcaneArtifacts.id("effect.entangled_speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH, ArcaneArtifacts.id("effect.entangled_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
