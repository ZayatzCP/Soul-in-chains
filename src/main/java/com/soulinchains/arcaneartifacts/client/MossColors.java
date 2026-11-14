package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.world.MossZones;
import net.minecraft.core.BlockPos;

/** The tint of mushroom moss: the colour zones of {@link MossZones}. */
public final class MossColors {
    private MossColors() {
    }

    public static int at(BlockPos pos) {
        return MossZones.color(pos);
    }
}
