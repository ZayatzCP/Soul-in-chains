package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Curse of Shackles: held in place: no walking, no jumping, no healing, no teleporting, a quarter less damage dealt. */
public class CursedChainsEffect extends MobEffect {
    public CursedChainsEffect() {
        super(MobEffectCategory.HARMFUL, 0x3A2A55);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ArcaneArtifacts.id("effect.cursed_chains_speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH, ArcaneArtifacts.id("effect.cursed_chains_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
