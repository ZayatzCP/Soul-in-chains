package com.soulinchains.arcaneartifacts.effect;

import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import com.hollingsworth.arsnouveau.api.event.SpellModifierEvent;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.glyph.AugmentHarmless;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;

/** Pity (every spell of the creature is weakened like two Dampen per level) and Harmless spells. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class MagicHandler {
    public static final int PITY_DAMPEN = 2;

    private MagicHandler() {
    }

    @SubscribeEvent
    public static void onSpellModifiers(SpellModifierEvent event) {
        if (event.caster == null) {
            return;
        }
        MobEffectInstance pity = event.caster.getEffect(ModEffects.PITY);
        if (pity != null) {
            event.builder.addAmplification(-PITY_DAMPEN * (pity.getAmplifier() + 1));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSpellDamage(SpellDamageEvent.Pre event) {
        if (isHarmless(event.context)) {
            event.damage = 0;
            event.setCanceled(true);
        }
    }

    /** True when the spell carries the Harmless augment: it deals no damage at all. */
    public static boolean isHarmless(@Nullable SpellContext context) {
        return context != null && context.getSpell().unsafeList().contains(AugmentHarmless.INSTANCE);
    }
}
