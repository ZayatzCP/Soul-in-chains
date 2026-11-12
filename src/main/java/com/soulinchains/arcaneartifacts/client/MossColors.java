package com.soulinchains.arcaneartifacts.client;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/** Light blue that drifts into purple in slow waves through the cave. */
public final class MossColors {
    private static final int BLUE = 0x8FD3F0;
    private static final int PURPLE = 0xB58AF0;

    private MossColors() {
    }

    public static int at(BlockPos pos) {
        double wave = Math.sin(pos.getX() * 0.11) + Math.sin(pos.getZ() * 0.09 + 1.3) + Math.sin(pos.getY() * 0.21 + 0.7);
        float t = Mth.clamp((float) (wave / 3.0 * 0.5 + 0.5), 0F, 1F);
        int r = (int) Mth.lerp(t, (BLUE >> 16) & 0xFF, (PURPLE >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (BLUE >> 8) & 0xFF, (PURPLE >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, BLUE & 0xFF, PURPLE & 0xFF);
        return (r << 16) | (g << 8) | b;
    }
}
