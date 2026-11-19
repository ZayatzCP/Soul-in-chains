package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** A Tornado that carries the rest of the spell costs {@link EffectTornado#CARRY_SURCHARGE} more (checked and spent alike). */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class TornadoCost {
    private TornadoCost() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCost(SpellCostCalcEvent event) {
        if (EffectTornado.carries(event.context.getSpell())) {
            event.currentCost += EffectTornado.CARRY_SURCHARGE;
        }
    }
}
