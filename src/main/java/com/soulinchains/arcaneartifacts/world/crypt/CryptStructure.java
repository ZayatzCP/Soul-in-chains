package com.soulinchains.arcaneartifacts.world.crypt;

import com.mojang.serialization.MapCodec;
import com.soulinchains.arcaneartifacts.world.village.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A crypt: a small stone mausoleum on dry, fairly flat land; a ladder inside goes down to a burial hall with graves,
 * a chest, a Mourner and Restless.
 */
public class CryptStructure extends Structure {
    public static final MapCodec<CryptStructure> CODEC = simpleCodec(CryptStructure::new);

    public CryptStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CRYPT.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int y = surface(context, x, z);
        int min = y;
        int max = y;
        for (int dx = -4; dx <= 4; dx += 8) {
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
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new CryptPiece(origin))));
    }

    private static int surface(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()) - 1;
    }
}
