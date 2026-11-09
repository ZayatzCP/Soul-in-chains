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
    /** Wreath of Manipulation: spells cost no mana and the wearer takes no damage. Mirrors the rush, not curable. */
    public static final DeferredHolder<MobEffect, MobEffect> BATTLE_RUSH = REGISTER.register("battle_rush",
            () -> new ArcaneStatusEffect(MobEffectCategory.BENEFICIAL, 0xFF5A2B, false));
    /** Wreath of Manipulation cooldown: no new Battle Rush until it ends. Not curable. */
    public static final DeferredHolder<MobEffect, MobEffect> RUSH_EXHAUSTION = REGISTER.register("rush_exhaustion",
            () -> new ArcaneStatusEffect(MobEffectCategory.NEUTRAL, 0x6B5A4E, false));

    /** Counterspell: the creature cannot cast Ars Nouveau spells. Can be cured with milk. */
    public static final DeferredHolder<MobEffect, MobEffect> SILENCED = REGISTER.register("silenced",
            () -> new ArcaneStatusEffect(MobEffectCategory.HARMFUL, 0x3A3A55, true));

    /** Satiety glyph: hunger and saturation stay full. */
    public static final DeferredHolder<MobEffect, MobEffect> WELL_FED = REGISTER.register("well_fed",
            com.soulinchains.arcaneartifacts.effect.WellFedEffect::new);

    /** Pity (glyph, gilded spider eye, after a Battle Rush): every spell is weakened like two Dampen per level. */
    public static final DeferredHolder<MobEffect, MobEffect> PITY = REGISTER.register("pity",
            () -> new ArcaneStatusEffect(MobEffectCategory.HARMFUL, 0x8C9A5B, true));
    /** BONK! The creature cannot move, jump or turn. Not curable and never extended. */
    public static final DeferredHolder<MobEffect, MobEffect> BONK = REGISTER.register("bonk",
            com.soulinchains.arcaneartifacts.effect.BonkEffect::new);

    private ModEffects() {
    }
}
