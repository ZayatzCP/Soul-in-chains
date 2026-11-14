package com.soulinchains.arcaneartifacts.world;

import com.mojang.serialization.Codec;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A huge blue mushroom: like the huge brown one (a flat cap on a stem) but taller and wider, with a
 * rounder cap. Under a low ceiling it grows as tall as it can (at least 3 blocks of stem) and its cap
 * shrinks with it.
 */
public class HugeBlueMushroomFeature extends Feature<NoneFeatureConfiguration> {
    public HugeBlueMushroomFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        RandomSource random = context.random();
        return grow(context.level(), context.origin(), 7 + random.nextInt(5));
    }

    /** Grows a mushroom whose stem starts at {@code origin}; returns false if there is no room or no soil. */
    public static boolean grow(WorldGenLevel level, BlockPos origin, int wantedHeight) {
        if (!isSoil(level.getBlockState(origin.below()))) {
            return false;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int free = 0;
        while (free <= wantedHeight && isFree(level.getBlockState(pos.setWithOffset(origin, 0, free, 0)))) {
            free++;
        }
        int height = Math.min(wantedHeight, free - 1);
        if (height < 3) {
            return false;
        }
        int radius = height >= 9 ? 5 : height >= 6 ? 4 : 3;
        BlockState stem = Blocks.MUSHROOM_STEM.defaultBlockState()
                .setValue(HugeMushroomBlock.UP, false).setValue(HugeMushroomBlock.DOWN, false);
        for (int y = 0; y < height; y++) {
            level.setBlock(pos.setWithOffset(origin, 0, y, 0), stem, 2);
        }
        BlockState cap = ModBlocks.BLUE_MUSHROOM_BLOCK.get().defaultBlockState()
                .setValue(HugeMushroomBlock.UP, true).setValue(HugeMushroomBlock.DOWN, false);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!inCap(dx, dz, radius)) {
                    continue;
                }
                pos.setWithOffset(origin, dx, height, dz);
                if (!isFree(level.getBlockState(pos))) {
                    continue;
                }
                float roll = level.getRandom().nextFloat();
                BlockState top = roll < 0.06F ? ModBlocks.GLOWING_BLUE_MUSHROOM_BLOCK.get().defaultBlockState()
                        : roll < 0.22F ? ModBlocks.SPOTTED_BLUE_MUSHROOM_BLOCK.get().defaultBlockState()
                        : cap.setValue(HugeMushroomBlock.WEST, !inCap(dx - 1, dz, radius))
                        .setValue(HugeMushroomBlock.EAST, !inCap(dx + 1, dz, radius))
                        .setValue(HugeMushroomBlock.NORTH, !inCap(dx, dz - 1, radius))
                        .setValue(HugeMushroomBlock.SOUTH, !inCap(dx, dz + 1, radius));
                level.setBlock(pos, top, 2);
                // Gills under the inner part of the cap.
                int distance = dx * dx + dz * dz;
                if (radius >= 4 && distance > 1 && inCap(dx, dz, radius - 1)) {
                    BlockPos below = pos.below();
                    if (isFree(level.getBlockState(below))) {
                        level.setBlock(below, ModBlocks.BLUE_MUSHROOM_GILLS.get().defaultBlockState(), 2);
                    }
                }
            }
        }
        return true;
    }

    /** A rounded disc: radius 3 is 7 wide, radius 5 is 11 wide. */
    public static boolean inCap(int dx, int dz, int radius) {
        return dx * dx + dz * dz <= radius * radius + radius;
    }

    private static boolean isFree(BlockState state) {
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES);
    }

    private static boolean isSoil(BlockState state) {
        return state.is(ModBlocks.MUSHROOM_MOSS.get()) || state.is(BlockTags.DIRT) || state.is(BlockTags.MUSHROOM_GROW_BLOCK)
                || state.is(BlockTags.BASE_STONE_OVERWORLD);
    }
}
