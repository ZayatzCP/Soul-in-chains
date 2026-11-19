package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import com.hollingsworth.arsnouveau.api.event.MaxManaCalcEvent;
import com.soulinchains.arcaneartifacts.Config;
import net.minecraft.world.entity.player.Player;

/**
 * Shackled creatures cannot teleport away (endermen, chorus fruit, ender pearls); Bond splits damage between two
 * creatures; servants hold a share of their master's maximum mana.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class NecroEvents {
    private NecroEvents() {
    }

    @SubscribeEvent
    public static void onEnderTeleport(EntityTeleportEvent.EnderEntity event) {
        cancelIfShackled(event);
    }

    @SubscribeEvent
    public static void onChorus(EntityTeleportEvent.ChorusFruit event) {
        cancelIfShackled(event);
    }

    @SubscribeEvent
    public static void onPearl(EntityTeleportEvent.EnderPearl event) {
        cancelIfShackled(event);
    }

    private static boolean splitting;

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (splitting || event.getAmount() <= 0) {
            return;
        }
        LivingEntity partner = BondLink.partner(event.getEntity());
        if (partner == null) {
            return;
        }
        float half = event.getAmount() / 2.0F;
        event.setAmount(half);
        splitting = true;
        try {
            partner.invulnerableTime = 0;
            partner.hurt(event.getEntity().level().damageSources().magic(), half);
        } finally {
            splitting = false;
        }
    }

    @SubscribeEvent
    public static void onMaxMana(MaxManaCalcEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            float reserve = Math.min(Config.SERVANT_MAX_RESERVE.get().floatValue(), Servant.reserveOf(player.getUUID()));
            if (reserve > 0) {
                event.setReserve(Math.min(1.0F, event.getReserve() + reserve));
            }
        }
    }

    private static void cancelIfShackled(EntityTeleportEvent event) {
        if (event.getEntity() instanceof LivingEntity living && living.hasEffect(ModEffects.SHACKLED)) {
            event.setCanceled(true);
        }
    }
}
