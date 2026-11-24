package com.soulinchains.arcaneartifacts.world.cult;

import com.mojang.serialization.MapCodec;
import com.soulinchains.arcaneartifacts.world.village.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** The House of Confessions: a small dark house on flat ground with a training circle inside. */
public class ConfessionHouseStructure extends Structure {
    public static final MapCodec<ConfessionHouseStructure> CODEC = simpleCodec(ConfessionHouseStructure::new);

    public ConfessionHouseStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CONFESSION_HOUSE.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int y = surface(context, x, z);
        int min = y;
        int max = y;
        for (int dx = -5; dx <= 5; dx += 10) {
            for (int dz = -4; dz <= 4; dz += 8) {
                int h = surface(context, x + dx, z + dz);
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        if (max - min > 3 || y < context.chunkGenerator().getSeaLevel()) {
            return Optional.empty();
        }
        NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        if (!column.getBlock(y).getFluidState().isEmpty() || !column.getBlock(y + 1).getFluidState().isEmpty()) {
            return Optional.empty();
        }
        BlockPos origin = new BlockPos(x, y, z);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new ConfessionHousePiece(origin))));
    }

    static int surface(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()) - 1;
    }
}
