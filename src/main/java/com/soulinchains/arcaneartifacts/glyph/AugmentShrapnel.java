package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;

/**
 * Projectile modifier: when the projectile hits a creature it bursts into two smaller homing
 * projectiles that deal half damage and seek other nearby targets. Split projectiles never split
 * again. Behaviour lives in {@link com.soulinchains.arcaneartifacts.spell.ShrapnelHandler}.
 */
public class AugmentShrapnel extends AbstractAugment {
    public static final AugmentShrapnel INSTANCE = new AugmentShrapnel();

    private AugmentShrapnel() {
        super(ArcaneArtifacts.id("glyph_shrapnel"), "Shrapnel");
    }

    @Override
    public int getDefaultManaCost() {
        return 40;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @Override
    public String getBookDescription() {
        return "When a projectile augmented with Shrapnel strikes a creature, it splits into two smaller homing projectiles that deal half damage and seek other nearby targets. Split projectiles never split again and do not strike the creature the original projectile hit.";
    }
}
