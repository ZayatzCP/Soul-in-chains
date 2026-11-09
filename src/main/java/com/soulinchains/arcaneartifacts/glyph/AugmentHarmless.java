package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;

/**
 * Harmless: an augment for any effect that strips all damage from the spell (Ars damage effects and
 * the addon's glyphs alike). Everything else the spell does still happens.
 */
public class AugmentHarmless extends AbstractAugment {
    public static final AugmentHarmless INSTANCE = new AugmentHarmless();

    private AugmentHarmless() {
        super(ArcaneArtifacts.id("glyph_harmless"), "Harmless");
    }

    @Override
    public int getDefaultManaCost() {
        return 5;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.ONE;
    }

    @Override
    public String getBookDescription() {
        return "Removes all damage from the spell. Knockback, effects and everything else still happen.";
    }
}
