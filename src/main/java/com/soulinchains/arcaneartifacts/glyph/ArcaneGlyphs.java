package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ArcaneGlyphs {
    public static final List<AbstractSpellPart> ALL = List.of(
            MethodArcaneMissile.INSTANCE,
            EffectArcaneMark.INSTANCE,
            EffectParasitism.INSTANCE,
            AugmentSplitting.INSTANCE,
            AugmentPursuit.INSTANCE
    );

    /** Called from the mod constructor: Ars builds glyph items and per-glyph configs from this. */
    public static void registerGlyphs() {
        for (AbstractSpellPart part : ALL) {
            GlyphRegistry.registerSpell(part);
        }
    }

    /**
     * Lets the plain Ars Projectile form accept Splitting (the addon-supported way, through
     * {@link AbstractSpellPart#compatibleAugments}). Pursuit stays exclusive to homing forms, since
     * the plain projectile has no homing to improve.
     */
    public static void postInit() {
        MethodProjectile.INSTANCE.compatibleAugments.add(AugmentSplitting.INSTANCE);
        MethodProjectile.INSTANCE.augmentDescriptions.put(AugmentSplitting.INSTANCE,
                Component.translatable("ars_nouveau.augment_desc.glyph_projectile_glyph_splitting"));
    }

    private ArcaneGlyphs() {
    }
}
