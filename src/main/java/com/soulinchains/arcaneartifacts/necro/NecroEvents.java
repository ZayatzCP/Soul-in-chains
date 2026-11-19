package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

/** Shackled creatures cannot teleport away (endermen, chorus fruit, ender pearls). */
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

    private static void cancelIfShackled(EntityTeleportEvent event) {
        if (event.getEntity() instanceof LivingEntity living && living.hasEffect(ModEffects.SHACKLED)) {
            event.setCanceled(true);
        }
    }
}
