package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.accessory.RushState;
import com.soulinchains.arcaneartifacts.accessory.WreathOfManipulationItem;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Sideways jitter of the Wreath of Manipulation during a Battle Rush: a violent shake for the first two
 * seconds (the moment the mana runs out), then a nervous tremble until the rush ends. In model pixels.
 */
public final class RushShake {
    private static final int BURST_TICKS = 40;

    private RushShake() {
    }

    public static float offset(ItemStack stack, float partialTicks) {
        Level level = ClientAccess.level();
        if (level == null) {
            return 0f;
        }
        RushState state = WreathOfManipulationItem.state(stack);
        long now = level.getGameTime();
        if (!state.isActive(now)) {
            return 0f;
        }
        float time = now + partialTicks;
        float sinceStart = time - state.startedAt();
        float amplitude = sinceStart < BURST_TICKS ? Mth.lerp(sinceStart / BURST_TICKS, 1.6f, 0.4f) : 0.3f;
        return Mth.sin(time * 2.6f) * amplitude + Mth.sin(time * 6.1f) * amplitude * 0.35f;
    }
}
