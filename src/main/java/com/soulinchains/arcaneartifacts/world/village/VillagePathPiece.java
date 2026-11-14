package com.soulinchains.arcaneartifacts.world.village;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** A moss carpet path between the hall and a house of a Mycelinoid village. */
public class VillagePathPiece extends StructurePiece {
    private final BlockPos from;
    private final BlockPos to;

    public VillagePathPiece(BlockPos from, BlockPos to) {
        super(ModStructures.PATH_PIECE.get(), 0, new BoundingBox(Math.min(from.getX(), to.getX()) - 1, Math.min(from.getY(), to.getY()) - 4,
                Math.min(from.getZ(), to.getZ()) - 1, Math.max(from.getX(), to.getX()) + 1, Math.max(from.getY(), to.getY()) + 4,
                Math.max(from.getZ(), to.getZ()) + 1));
        this.from = from;
        this.to = to;
    }

    public VillagePathPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.PATH_PIECE.get(), tag);
        this.from = NbtUtils.readBlockPos(tag, "From").orElse(BlockPos.ZERO);
        this.to = NbtUtils.readBlockPos(tag, "To").orElse(BlockPos.ZERO);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.put("From", NbtUtils.writeBlockPos(from));
        tag.put("To", NbtUtils.writeBlockPos(to));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        new HouseBuilder(level, chunkBox, random).path(from, to);
    }
}
