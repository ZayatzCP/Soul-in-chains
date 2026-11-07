package com.soulinchains.arcaneartifacts.registry;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.effect.ArcaneStatusEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(Registries.MOB_EFFECT, ArcaneArtifacts.MODID);

    /** Applied by the Arcane Mark glyph. Can be dispelled (milk / Dispel) like any spell debuff. */
    public static final DeferredHolder<MobEffect, MobEffect> ARCANE_MARK = REGISTER.register("arcane_mark",
            com.soulinchains.arcaneartifacts.effect.ArcaneMarkEffect::new);
    /** Shown on the creature that currently carries a parasitic link. Mirrors the link, not curable. */
    public static final DeferredHolder<MobEffect, MobEffect> PARASITE_HOST = REGISTER.register("parasite_host",
            () -> new ArcaneStatusEffect(MobEffectCategory.HARMFUL, 0x5E0F3A, false));
    /** Shown on the caster while their parasitic link is active. Mirrors the link, not curable. */
    public static final DeferredHolder<MobEffect, MobEffect> PARASITIC_BOND = REGISTER.register("parasitic_bond",
            () -> new ArcaneStatusEffect(MobEffectCategory.BENEFICIAL, 0x2FD6B5, false));
    /** Cooldown indicator for Parasitism. Mirrors the cooldown, not curable. */
    public static final DeferredHolder<MobEffect, MobEffect> PARASITIC_RECOVERY = REGISTER.register("parasitic_recovery",
            () -> new ArcaneStatusEffect(MobEffectCategory.NEUTRAL, 0x6B4E7A, false));
    /** Granted by the wreath's magic adaptation: reduces magic damage (see Config). */
    public static final DeferredHolder<MobEffect, MobEffect> ARCANE_ADAPTATION = REGISTER.register("arcane_adaptation",
            () -> new ArcaneStatusEffect(MobEffectCategory.BENEFICIAL, 0x7A6CFF, true));

    private ModEffects() {
    }
}
