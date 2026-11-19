package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Shackled: chains hold the creature on the spot: it can neither walk, jump nor teleport. */
public class ShackledEffect extends MobEffect {
    public ShackledEffect() {
        super(MobEffectCategory.HARMFUL, 0x6B6F7A);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ArcaneArtifacts.id("effect.shackled_speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH, ArcaneArtifacts.id("effect.shackled_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
