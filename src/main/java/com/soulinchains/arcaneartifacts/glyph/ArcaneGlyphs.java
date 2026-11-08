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
            AugmentShrapnel.INSTANCE,
            AugmentPursuit.INSTANCE,
            MethodBeam.INSTANCE,
            EffectBlackHole.INSTANCE,
            EffectTornado.INSTANCE,
            EffectWeatherShift.INSTANCE,
            EffectCounterspell.INSTANCE,
            EffectMeteorShower.INSTANCE,
            EffectTimeWarp.INSTANCE
    );

    /** Called from the mod constructor: Ars builds glyph items and per-glyph configs from this. */
    public static void registerGlyphs() {
        for (AbstractSpellPart part : ALL) {
            GlyphRegistry.registerSpell(part);
        }
    }

    /**
     * Lets the plain Ars Projectile form accept Shrapnel (the addon-supported way, through
     * {@link AbstractSpellPart#compatibleAugments}). Pursuit stays exclusive to homing forms, since
     * the plain projectile has no homing to improve.
     */
    public static void postInit() {
        MethodProjectile.INSTANCE.compatibleAugments.add(AugmentShrapnel.INSTANCE);
        MethodProjectile.INSTANCE.augmentDescriptions.put(AugmentShrapnel.INSTANCE,
                Component.translatable("ars_nouveau.augment_desc.glyph_projectile_glyph_shrapnel"));
    }

    private ArcaneGlyphs() {
    }
}
