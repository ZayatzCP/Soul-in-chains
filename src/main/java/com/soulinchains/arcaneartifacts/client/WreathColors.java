package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.accessory.RushState;
import com.soulinchains.arcaneartifacts.accessory.WreathOfManipulationItem;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Flower colours of the wreath. Tint index 0 is the vine (untinted); indexes 1-4 are the four flower
 * groups. Ready: soft colours. Battle Rush: bright, pulsing fire colours. Recovering: faded grey.
 */
public final class WreathColors {
    private static final int FADED = 0x8E8A99;
    private static final int[] READY = {0xD9DEE8, 0xF0A35E, 0x7FD6E0, 0xC48CF5};
    private static final int[] RUSH = {0xFFF2B0, 0xFF5A1F, 0xFFB01F, 0xFF2B6E};

    private WreathColors() {
    }

    public static int color(ItemStack stack, int tintIndex) {
        if (tintIndex <= 0 || tintIndex > READY.length) {
            return 0xFFFFFFFF;
        }
        int group = tintIndex - 1;
        RushState state = WreathOfManipulationItem.state(stack);
        Level level = ClientAccess.level();
        long now = level == null ? 0 : level.getGameTime();
        if (state.isActive(now)) {
            float pulse = 0.75f + 0.25f * (float) Math.sin(now / 3.0 + group);
            return 0xFF000000 | lerp(0xFFFFFF, RUSH[group], pulse);
        }
        if (state.isRecovering(now)) {
            return 0xFF000000 | lerp(FADED, READY[group], 0.25f);
        }
        return 0xFF000000 | READY[group];
    }

    private static int lerp(int from, int to, float t) {
        int r = (int) (FastColor.ARGB32.red(from) + (FastColor.ARGB32.red(to) - FastColor.ARGB32.red(from)) * t);
        int g = (int) (FastColor.ARGB32.green(from) + (FastColor.ARGB32.green(to) - FastColor.ARGB32.green(from)) * t);
        int b = (int) (FastColor.ARGB32.blue(from) + (FastColor.ARGB32.blue(to) - FastColor.ARGB32.blue(from)) * t);
        return (r << 16) | (g << 8) | b;
    }
}
