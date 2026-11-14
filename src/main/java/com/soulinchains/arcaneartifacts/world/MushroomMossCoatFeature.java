package com.soulinchains.arcaneartifacts.world;

import com.mojang.serialization.Codec;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Coats the Mushroom Caves with mushroom moss: once per chunk, every stone-like block that touches cave
 * air inside the biome (floor, walls and ceiling alike) becomes moss. Ores are left alone, so they stay
 * visible in the walls.
 */
public class MushroomMossCoatFeature extends Feature<NoneFeatureConfiguration> {
    private static final Direction[] DIRECTIONS = Direction.values();

    public MushroomMossCoatFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunkPos = new ChunkPos(context.origin());
        ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z);
        BlockState moss = ModBlocks.MUSHROOM_MOSS.get().defaultBlockState();
        int minY = level.getMinBuildHeight() + 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos side = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunkPos.getMinBlockX() + dx;
                int z = chunkPos.getMinBlockZ() + dz;
                int top = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
                // The biome is looked up once per 4 blocks of height: that is how finely biomes are stored.
                int checkedCell = Integer.MIN_VALUE;
                boolean inBiome = false;
                for (int y = minY; y <= top; y++) {
                    pos.set(x, y, z);
                    if (!chunk.getBlockState(pos).isAir()) {
                        continue;
                    }
                    int cell = y >> 2;
                    if (cell != checkedCell) {
                        checkedCell = cell;
                        inBiome = level.getBiome(pos).is(ModBiomes.MUSHROOM_CAVES);
                    }
                    if (!inBiome) {
                        continue;
                    }
                    for (Direction direction : DIRECTIONS) {
                        side.setWithOffset(pos, direction);
                        if (MossZones.mossy(side) && level.getBlockState(side).is(ModTags.MUSHROOM_MOSS_REPLACEABLE)) {
                            level.setBlock(side, moss, 2);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }
}
