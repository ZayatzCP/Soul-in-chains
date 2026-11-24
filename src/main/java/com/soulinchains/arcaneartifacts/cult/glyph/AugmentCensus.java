package com.soulinchains.arcaneartifacts.cult.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;

/** Changes what the Witness Call brings: one more faint witness per Census instead of one strengthened witness. */
public class AugmentCensus extends AbstractAugment {
    public static final AugmentCensus INSTANCE = new AugmentCensus();

    private AugmentCensus() {
        super(ArcaneArtifacts.id("glyph_census"), "Census");
    }

    @Override
    public int getDefaultManaCost() {
        return 65;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @Override
    public String getBookDescription() {
        return "Changes what the Witness Call brings: one more faint witness per Census instead of one strengthened witness.";
    }
}
