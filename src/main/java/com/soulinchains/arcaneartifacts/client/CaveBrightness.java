package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.world.ModBiomes;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The Mushroom Caves glow faintly: on the client the darkness there is lifted a little (like a dimension's
 * ambient light), so the cave can be seen without torches. The real light level is unchanged, so monsters
 * still spawn in the dark parts.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, value = Dist.CLIENT)
public final class CaveBrightness {
    public static final float GLOW = 0.24F;
    private static float current;

    private CaveBrightness() {
    }

    /** How much the darkness is lifted right now (0 outside the Mushroom Caves). */
    public static float boost() {
        return current;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        float target = 0F;
        if (minecraft.player != null && minecraft.level != null
                && minecraft.level.getBiome(minecraft.player.blockPosition()).is(ModBiomes.MUSHROOM_CAVES)) {
            target = GLOW;
        }
        current += (target - current) * 0.05F;
        if (Math.abs(current) < 0.001F) {
            current = 0F;
        }
    }
}
