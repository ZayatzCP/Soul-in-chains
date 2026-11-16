package com.soulinchains.arcaneartifacts.block;

import net.minecraft.world.level.block.AmethystClusterBlock;

/**
 * Amber: an orange crystal lying on desert sand like a flower, or growing on cave walls and ceilings. The
 * small one is the common find; the large one (a cluster of tall prisms) is rare and gives more amber.
 */
public class AmberClusterBlock extends AmethystClusterBlock {
    public AmberClusterBlock(Properties properties) {
        this(5.0F, 3.0F, properties);
    }

    public AmberClusterBlock(float height, float offset, Properties properties) {
        super(height, offset, properties);
    }
}
