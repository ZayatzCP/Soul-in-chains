package com.soulinchains.arcaneartifacts.necro.throne;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** A lock of the Imprisoned King's chains: while one stands, the King cannot be hurt. Hard to break, drops nothing. */
public class ChainLockBlock extends Block {
    public ChainLockBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(20.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)
                .lightLevel(state -> 10).pushReaction(PushReaction.BLOCK));
    }
}
