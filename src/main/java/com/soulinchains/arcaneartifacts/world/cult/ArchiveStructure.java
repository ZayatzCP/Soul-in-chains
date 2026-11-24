package com.soulinchains.arcaneartifacts.world.cult;

import com.mojang.serialization.MapCodec;
import com.soulinchains.arcaneartifacts.world.village.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** The Archive of Unkept Promises: three halls deep underground, a ladder shaft up to a marked stone on the surface. */
public class ArchiveStructure extends Structure {
    public static final MapCodec<ArchiveStructure> CODEC = simpleCodec(ArchiveStructure::new);

    public ArchiveStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ARCHIVE.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int surface = ConfessionHouseStructure.surface(context, x, z);
        if (surface < context.chunkGenerator().getSeaLevel()) {
            return Optional.empty();
        }
        NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        if (!column.getBlock(surface).getFluidState().isEmpty() || !column.getBlock(surface + 1).getFluidState().isEmpty()) {
            return Optional.empty();
        }
        int floor = Math.max(context.heightAccessor().getMinBuildHeight() + 12, Math.min(surface - 18, 30));
        BlockPos origin = new BlockPos(x, floor, z);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new ArchivePiece(origin, surface))));
    }
}
