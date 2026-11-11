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
            EffectTimeWarp.INSTANCE,
            EffectAnvilDrop.INSTANCE,
            EffectSatiety.INSTANCE,
            EffectPity.INSTANCE,
            EffectCupid.INSTANCE,
            AugmentHarmless.INSTANCE,
            AugmentAura.INSTANCE,
            EffectFear.INSTANCE
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
        MethodProjectile.INSTANCE.compatibleAugments.add(AugmentAura.INSTANCE);
        MethodProjectile.INSTANCE.augmentDescriptions.put(AugmentAura.INSTANCE,
                Component.translatable("ars_nouveau.augment_desc.glyph_projectile_glyph_aura"));
        MethodArcaneMissile.INSTANCE.compatibleAugments.add(AugmentAura.INSTANCE);
        MethodArcaneMissile.INSTANCE.augmentDescriptions.put(AugmentAura.INSTANCE,
                Component.translatable("ars_nouveau.augment_desc.glyph_projectile_glyph_aura"));
        MethodProjectile.INSTANCE.augmentDescriptions.put(AugmentShrapnel.INSTANCE,
                Component.translatable("ars_nouveau.augment_desc.glyph_projectile_glyph_shrapnel"));
        // Harmless can follow any effect, from Ars or from any addon.
        for (AbstractSpellPart part : GlyphRegistry.getSpellpartMap().values()) {
            if (part instanceof com.hollingsworth.arsnouveau.api.spell.AbstractEffect effect && effect != EffectCupid.INSTANCE) {
                effect.compatibleAugments.add(AugmentHarmless.INSTANCE);
                effect.augmentDescriptions.putIfAbsent(AugmentHarmless.INSTANCE,
                        Component.translatable("arcane_artifacts.augment_desc.harmless"));
            }
        }
    }

    private ArcaneGlyphs() {
    }
}
