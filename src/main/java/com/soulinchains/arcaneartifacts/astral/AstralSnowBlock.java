package com.soulinchains.arcaneartifacts.astral;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * The snow of oblivion: drifts on the white plain of the Astral. It is not weather and never melts. Dug away, now and then
 * it gives up something that belonged to someone, from some other world (loot table {@code blocks/astral_snow}).
 */
public class AstralSnowBlock extends SnowLayerBlock {
    public AstralSnowBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).replaceable().forceSolidOff().strength(0.2F)
                .sound(SoundType.SNOW).isViewBlocking((s, l, p) -> s.getValue(LAYERS) >= 8));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return false;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // the snow of oblivion does not melt
    }
}
