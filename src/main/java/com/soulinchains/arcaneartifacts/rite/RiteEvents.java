package com.soulinchains.arcaneartifacts.rite;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** A blow on the Reader breaks off the line being read. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class RiteEvents {
    private RiteEvents() {
    }

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (event.getAmount() > 0 && event.getEntity() instanceof Player player && !player.level().isClientSide) {
            RiteHeartBlockEntity.readerHurt(player);
        }
    }
}
