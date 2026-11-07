package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;

/**
 * Projectile modifier for homing forms: larger detection radius, sharper turning and a longer
 * lifetime. Never changes damage.
 */
public class AugmentPursuit extends AbstractAugment {
    public static final AugmentPursuit INSTANCE = new AugmentPursuit();

    private AugmentPursuit() {
        super(ArcaneArtifacts.id("glyph_pursuit"), "Pursuit");
    }

    @Override
    public int getDefaultManaCost() {
        return 20;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @Override
    public String getBookDescription() {
        return "Sharpens the senses of homing projectiles: each Pursuit widens the target search radius, makes the projectile turn more sharply and lets it fly longer. Pursuit never increases damage.";
    }
}
