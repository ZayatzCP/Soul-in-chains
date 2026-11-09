package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;

/** BONK! on the local player: the mouse cannot turn the head (sensitivity -1/3 makes the turn exactly zero). */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, value = Dist.CLIENT)
public final class BonkClient {
    private BonkClient() {
    }

    @SubscribeEvent
    public static void onTurn(CalculatePlayerTurnEvent event) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.hasEffect(ModEffects.BONK)) {
            event.setMouseSensitivity(-1.0 / 3.0);
            event.setCinematicCameraEnabled(false);
        }
    }
}
