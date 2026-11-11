package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;

/**
 * Aura (projectile augment): while the projectile flies, its effects wash over every creature around
 * it (each creature once). With Sensitive it also touches the blocks under its path. AOE widens the
 * aura. Behaviour lives in {@link com.soulinchains.arcaneartifacts.spell.AuraHandler}.
 */
public class AugmentAura extends AbstractAugment {
    public static final AugmentAura INSTANCE = new AugmentAura();

    private AugmentAura() {
        super(ArcaneArtifacts.id("glyph_aura"), "Aura");
    }

    @Override
    public int getDefaultManaCost() {
        return 60;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @Override
    public String getBookDescription() {
        return "A flying projectile with Aura applies its effects to every creature it passes (once each). With Sensitive it also affects the blocks under its path. AOE makes the aura wider.";
    }
}
