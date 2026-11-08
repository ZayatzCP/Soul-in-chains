package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Effects of the Battle Rush started by the Wreath of Manipulation: spells cost nothing (both the
 * mana check and the mana actually spent go through {@link SpellCostCalcEvent}) and incoming damage is
 * cancelled, except damage that bypasses invulnerability (/kill, the void).
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class WreathHandler {
    private WreathHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCost(SpellCostCalcEvent event) {
        if (event.context.getUnwrappedCaster() instanceof LivingEntity caster && caster.hasEffect(ModEffects.BATTLE_RUSH)) {
            event.currentCost = 0;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().hasEffect(ModEffects.BATTLE_RUSH) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }
}
