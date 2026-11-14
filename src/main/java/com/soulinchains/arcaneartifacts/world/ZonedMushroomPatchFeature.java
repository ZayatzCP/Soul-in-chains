package com.soulinchains.arcaneartifacts.world;

import com.mojang.serialization.Codec;
import com.soulinchains.arcaneartifacts.mushroom.ArcaneMushroom;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A small patch of cave mushrooms of the kind whose moss zone this is (green moss: Toxcaps, pink: Mendcaps...),
 * with now and then a plain brown or red one among them. Mushrooms stand on solid ground only.
 */
public class ZonedMushroomPatchFeature extends Feature<NoneFeatureConfiguration> {
    private static final int TRIES = 16;

    public ZonedMushroomPatchFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        ArcaneMushroom kind = MossZones.zone(origin);
        BlockState mushroom = ModBlocks.MUSHROOMS.get(kind).get().defaultBlockState();
        int placed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < TRIES; i++) {
            pos.setWithOffset(origin, random.nextInt(9) - 4, random.nextInt(3) - 1, random.nextInt(9) - 4);
            BlockPos below = pos.below();
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                continue;
            }
            BlockState state = random.nextInt(10) == 0
                    ? (random.nextBoolean() ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM).defaultBlockState() : mushroom;
            level.setBlock(pos, state, 2);
            placed++;
        }
        return placed > 0;
    }
}
