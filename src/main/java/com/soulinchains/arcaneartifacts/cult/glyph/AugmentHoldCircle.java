package com.soulinchains.arcaneartifacts.cult.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;

/** Cast from inside the circle of a rite you hold, the spell steadies it: every anchor gains 10 stability per Hold (once in 2 s). Fits the Cultist glyphs. */
public class AugmentHoldCircle extends AbstractAugment {
    public static final AugmentHoldCircle INSTANCE = new AugmentHoldCircle();

    private AugmentHoldCircle() {
        super(ArcaneArtifacts.id("glyph_hold_circle"), "Hold the Circle");
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
        return "Cast from inside the circle of a rite you hold, the spell steadies it: every anchor gains 10 stability per Hold (once in 2 s). Fits the Cultist glyphs.";
    }
}
