package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;

/**
 * Projectile modifier: when the projectile hits a creature it bursts into two smaller homing
 * projectiles that deal half damage and seek other nearby targets. Split projectiles never split
 * again. Behaviour lives in {@link com.soulinchains.arcaneartifacts.spell.SplittingHandler}.
 */
public class AugmentSplitting extends AbstractAugment {
    public static final AugmentSplitting INSTANCE = new AugmentSplitting();

    private AugmentSplitting() {
        super(ArcaneArtifacts.id("glyph_splitting"), "Splitting");
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
        return "When a projectile augmented with Splitting strikes a creature, it splits into two smaller homing projectiles that deal half damage and seek other nearby targets. Split projectiles never split again and do not strike the creature the original projectile hit.";
    }
}
