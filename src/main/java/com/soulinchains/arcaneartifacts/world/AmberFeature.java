package com.soulinchains.arcaneartifacts.world;

import com.mojang.serialization.Codec;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Scatters a few amber crystals around a point, on whatever solid face is next to air: floor, wall or ceiling. */
public class AmberFeature extends Feature<NoneFeatureConfiguration> {
    private static final Direction[] DIRECTIONS = Direction.values();

    public AmberFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int placed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < 24 && placed < 4; attempt++) {
            pos.setWithOffset(origin, random.nextInt(7) - 3, random.nextInt(5) - 2, random.nextInt(7) - 3);
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }
            Direction direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
            BlockPos support = pos.relative(direction.getOpposite());
            if (!level.getBlockState(support).isFaceSturdy(level, support, direction)) {
                continue;
            }
            BlockState amber = ModBlocks.AMBER_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, direction);
            level.setBlock(pos, amber, 2);
            placed++;
        }
        return placed > 0;
    }
}
