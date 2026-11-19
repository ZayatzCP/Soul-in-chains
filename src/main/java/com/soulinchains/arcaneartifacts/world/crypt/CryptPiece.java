package com.soulinchains.arcaneartifacts.world.crypt;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.necro.GraveBlock;
import com.soulinchains.arcaneartifacts.necro.mob.Mourner;
import com.soulinchains.arcaneartifacts.necro.mob.Restless;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
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
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The whole crypt in one piece: a 7x7 mausoleum whose floor is the surface, a ladder shaft down, and an 11x11 burial
 * hall ten blocks below with two rows of graves, hanging soul lanterns, a loot chest and its keepers.
 */
public class CryptPiece extends StructurePiece {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, ArcaneArtifacts.id("chests/crypt"));
    public static final int DEPTH = 10;
    private final BlockPos origin;

    public CryptPiece(BlockPos origin) {
        super(ModStructures.CRYPT_PIECE.get(), 0, new BoundingBox(origin.getX() - 6, origin.getY() - DEPTH - 1, origin.getZ() - 6,
                origin.getX() + 6, origin.getY() + 6, origin.getZ() + 6));
        this.origin = origin;
    }

    public CryptPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.CRYPT_PIECE.get(), tag);
        this.origin = NbtUtils.readBlockPos(tag, "Origin").orElse(BlockPos.ZERO);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.put("Origin", NbtUtils.writeBlockPos(origin));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        build(level, box, random, origin, true);
    }

    /** Builds the crypt at {@code origin} (the surface block under the mausoleum), only inside {@code box}. */
    public static void build(WorldGenLevel level, BoundingBox box, RandomSource random, BlockPos origin, boolean keepers) {
        int s = origin.getY();
        int floor = s - DEPTH;
        int ceiling = s - 5;
        // ---- burial hall
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = floor; y <= ceiling; y++) {
                    boolean wall = Math.abs(x) == 5 || Math.abs(z) == 5;
                    BlockState state;
                    if (y == floor) {
                        state = (Math.abs(x) == 3 && Math.abs(z) <= 4) ? ModBlocks.DESECRATED_SOIL.get().defaultBlockState()
                                : random.nextInt(4) == 0 ? Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
                    } else if (y == ceiling) {
                        state = Blocks.DEEPSLATE_TILES.defaultBlockState();
                    } else if (wall) {
                        state = random.nextInt(5) == 0 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    set(level, box, origin.offset(x, y - s, z), state);
                }
            }
        }
        for (int z = -3; z <= 3; z += 2) {
            set(level, box, origin.offset(-3, floor + 1 - s, z), ModBlocks.GRAVE.get().defaultBlockState().setValue(GraveBlock.FACING, Direction.EAST));
            set(level, box, origin.offset(3, floor + 1 - s, z), ModBlocks.GRAVE.get().defaultBlockState().setValue(GraveBlock.FACING, Direction.WEST));
        }
        for (int x = -2; x <= 2; x += 4) {
            for (int z = -2; z <= 2; z += 4) {
                set(level, box, origin.offset(x, ceiling - 1 - s, z), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            }
        }
        set(level, box, origin.offset(4, floor + 1 - s, 4), Blocks.COBWEB.defaultBlockState());
        set(level, box, origin.offset(-4, ceiling - 1 - s, -4), Blocks.COBWEB.defaultBlockState());
        BlockPos chest = origin.offset(0, floor + 1 - s, 4);
        if (box.isInside(chest)) {
            level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), 2);
            if (level.getBlockEntity(chest) instanceof ChestBlockEntity entity) {
                entity.setLootTable(LOOT, random.nextLong());
            }
        }
        // ---- shaft: a pillar to hold the ladder in the hall, the ladder all the way up
        for (int y = floor + 1; y < ceiling; y++) {
            set(level, box, origin.offset(0, y - s, -1), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        }
        for (int y = ceiling; y < s; y++) {
            set(level, box, origin.offset(0, y - s, -1), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            set(level, box, origin.offset(1, y - s, 0), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            set(level, box, origin.offset(-1, y - s, 0), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            set(level, box, origin.offset(0, y - s, 1), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        }
        for (int y = floor + 1; y <= s; y++) {
            set(level, box, origin.offset(0, y - s, 0), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        }
        // ---- mausoleum
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                boolean wall = Math.abs(x) == 3 || Math.abs(z) == 3;
                boolean corner = Math.abs(x) == 3 && Math.abs(z) == 3;
                for (int y = s - 2; y <= s + 5; y++) {
                    BlockPos pos = origin.offset(x, y - s, z);
                    if (x == 0 && z == 0 && y <= s) {
                        continue;
                    }
                    BlockState state;
                    if (y < s) {
                        state = wall ? Blocks.STONE_BRICKS.defaultBlockState() : null;
                    } else if (y == s) {
                        state = wall ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState();
                    } else if (y == s + 5) {
                        state = x == 0 && z == 0 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICK_SLAB.defaultBlockState();
                    } else if (corner) {
                        state = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                    } else if (wall) {
                        boolean door = z == 3 && x == 0 && y <= s + 2;
                        state = door ? Blocks.AIR.defaultBlockState()
                                : random.nextInt(4) == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                                : random.nextInt(6) == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    if (state != null) {
                        set(level, box, pos, state);
                    }
                }
            }
        }
        set(level, box, origin.offset(-2, 1, -2), Blocks.CANDLE.defaultBlockState());
        set(level, box, origin.offset(2, 1, -2), Blocks.CANDLE.defaultBlockState());
        set(level, box, origin.offset(0, 4, 0), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // ---- keepers: spawned once, by the chunk that holds the hall's centre
        BlockPos centre = origin.offset(0, floor + 1 - s, 0);
        if (keepers && box.isInside(centre)) {
            Mourner mourner = ModEntities.MOURNER.get().create(level.getLevel());
            if (mourner != null) {
                mourner.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() - 2.5, random.nextFloat() * 360, 0);
                mourner.setPersistenceRequired();
                level.addFreshEntity(mourner);
            }
            for (int i = -1; i <= 1; i += 2) {
                Restless ghost = ModEntities.RESTLESS.get().create(level.getLevel());
                if (ghost != null) {
                    ghost.moveTo(centre.getX() + 0.5 + i * 2, centre.getY() + 1, centre.getZ() + 0.5, random.nextFloat() * 360, 0);
                    ghost.setPersistenceRequired();
                    level.addFreshEntity(ghost);
                }
            }
        }
    }

    private static void set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }
}
