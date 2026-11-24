package com.soulinchains.arcaneartifacts.cult.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;

/** Raises the extra roll of ordinary loot from rite-summoned creatures: the Witness Call raises them with +0.25 to the multiplier per Abundance (up to x1.5), Retribution adds +0.1 to each creature it commands. */
public class AugmentRiteAbundance extends AbstractAugment {
    public static final AugmentRiteAbundance INSTANCE = new AugmentRiteAbundance();

    private AugmentRiteAbundance() {
        super(ArcaneArtifacts.id("glyph_rite_abundance"), "Abundance");
    }

    @Override
    public int getDefaultManaCost() {
        return 120;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.THREE;
    }

    @Override
    public String getBookDescription() {
        return "Raises the extra roll of ordinary loot from rite-summoned creatures: the Witness Call raises them with +0.25 to the multiplier per Abundance (up to x1.5), Retribution adds +0.1 to each creature it commands.";
    }
}
