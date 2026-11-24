package com.soulinchains.arcaneartifacts.world.cult;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.world.village.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The House of Confessions in one piece: 11x9 of dark oak and blackstone with bone corners. Inside - a training circle
 * (a Ritual Seal and three anchors, the fourth burnt out where a summoning went wrong), a censer, a lectern and a chest
 * with the first reagents and pages of formulas.
 */
public class ConfessionHousePiece extends StructurePiece {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, ArcaneArtifacts.id("chests/confession_house"));
    private final BlockPos origin;

    public ConfessionHousePiece(BlockPos origin) {
        super(ModStructures.CONFESSION_HOUSE_PIECE.get(), 0, new BoundingBox(origin.getX() - 6, origin.getY() - 4, origin.getZ() - 5,
                origin.getX() + 6, origin.getY() + 7, origin.getZ() + 5));
        this.origin = origin;
    }

    public ConfessionHousePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.CONFESSION_HOUSE_PIECE.get(), tag);
        this.origin = NbtUtils.readBlockPos(tag, "Origin").orElse(BlockPos.ZERO);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.put("Origin", NbtUtils.writeBlockPos(origin));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        build(level, box, random, origin);
    }

    static void set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    /** Builds the house with its floor at {@code origin} (the surface block in the middle), only inside {@code box}. */
    public static void build(WorldGenLevel level, BoundingBox box, RandomSource random, BlockPos origin) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -4; z <= 4; z++) {
                // foundation down to the ground
                for (int y = -1; y >= -4; y--) {
                    BlockPos p = origin.offset(x, y, z);
                    if (box.isInside(p) && !level.getBlockState(p).isSolid()) {
                        level.setBlock(p, Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 2);
                    }
                }
                boolean corner = Math.abs(x) == 5 && Math.abs(z) == 4;
                boolean wall = Math.abs(x) == 5 || Math.abs(z) == 4;
                set(level, box, origin.offset(x, 0, z), wall ? Blocks.POLISHED_BLACKSTONE.defaultBlockState()
                        : (x + z) % 2 == 0 ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
                for (int y = 1; y <= 4; y++) {
                    BlockState state;
                    if (corner) {
                        state = Blocks.BONE_BLOCK.defaultBlockState();
                    } else if (wall) {
                        boolean window = y == 2 && (Math.abs(x) == 5 && Math.abs(z) == 2 || Math.abs(z) == 4 && Math.abs(x) == 3);
                        boolean door = z == -4 && x == 0 && y <= 2;
                        state = door ? Blocks.AIR.defaultBlockState() : window ? Blocks.RED_STAINED_GLASS_PANE.defaultBlockState()
                                : y == 4 ? Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState() : Blocks.DARK_OAK_PLANKS.defaultBlockState();
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    set(level, box, origin.offset(x, y, z), state);
                }
                set(level, box, origin.offset(x, 5, z), Math.abs(x) == 5 || Math.abs(z) == 4 ? Blocks.DARK_OAK_SLAB.defaultBlockState()
                        : Blocks.DARK_OAK_PLANKS.defaultBlockState());
                if (Math.abs(x) <= 3 && Math.abs(z) <= 2) {
                    set(level, box, origin.offset(x, 6, z), Blocks.DARK_OAK_SLAB.defaultBlockState());
                }
            }
        }
        // the training circle
        set(level, box, origin.above(), ModBlocks.RITUAL_SEAL.get().defaultBlockState());
        set(level, box, origin.offset(-3, 1, 0), ModBlocks.RITUAL_ANCHOR.get().defaultBlockState());
        set(level, box, origin.offset(3, 1, 0), ModBlocks.RITUAL_ANCHOR.get().defaultBlockState());
        set(level, box, origin.offset(0, 1, -2), ModBlocks.RITUAL_ANCHOR.get().defaultBlockState());
        // where the fourth anchor stood: the failed summoning
        set(level, box, origin.offset(0, 0, 2), Blocks.COAL_BLOCK.defaultBlockState());
        set(level, box, origin.offset(1, 0, 2), Blocks.BLACKSTONE.defaultBlockState());
        set(level, box, origin.offset(-1, 1, 2), Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2));
        set(level, box, origin.offset(1, 1, 3), Blocks.BONE_BLOCK.defaultBlockState());
        set(level, box, origin.offset(-3, 1, -2), ModBlocks.RITUAL_CENSER.get().defaultBlockState());
        set(level, box, origin.offset(3, 1, 3), Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.WEST));
        set(level, box, origin.offset(4, 1, -3), Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        set(level, box, origin.offset(-4, 1, 3), Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1).setValue(CandleBlock.LIT, true));
        BlockPos chest = origin.offset(-3, 1, 3);
        if (box.isInside(chest)) {
            level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST), 2);
            if (level.getBlockEntity(chest) instanceof ChestBlockEntity entity) {
                entity.setLootTable(LOOT, random.nextLong());
            }
        }
    }
}
