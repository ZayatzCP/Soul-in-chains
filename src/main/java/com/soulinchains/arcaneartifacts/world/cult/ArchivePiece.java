package com.soulinchains.arcaneartifacts.world.cult;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.cult.Archdeacon;
import com.soulinchains.arcaneartifacts.cult.FacelessSentry;
import com.soulinchains.arcaneartifacts.cult.archive.ArchiveLockBlockEntity;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
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
 * The Archive of Unkept Promises in one piece (local x -9..9, z 0..44, floor at y 0, ceiling at y 6):
 * <ol>
 *     <li>z 1..11, the <b>hall of plates</b>: two Vow Plates 14 blocks apart; stood on together they open the bars at z 12;</li>
 *     <li>z 13..26, the <b>hall of the seal</b>: one holds the Holding Seal while the other lights the far Vow Candle; two
 *     Renegade Sealers and a Faceless Sentry by the bars at z 27;</li>
 *     <li>z 28..43, the <b>Archdeacon's hall</b>: a ritual circle, the Archdeacon, his chest.</li>
 * </ol>
 * A ladder in the first hall climbs to a marked stone on the surface.
 */
public class ArchivePiece extends StructurePiece {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, ArcaneArtifacts.id("chests/archive"));
    public static final ResourceKey<LootTable> ARCHDEACON_LOOT = ResourceKey.create(Registries.LOOT_TABLE, ArcaneArtifacts.id("chests/archive_archdeacon"));
    public static final int HALF = 9;
    public static final int LENGTH = 44;
    public static final int HEIGHT = 6;
    public static final BlockPos PLATE_A = new BlockPos(-7, 1, 6);
    public static final BlockPos PLATE_B = new BlockPos(7, 1, 6);
    public static final BlockPos LOCK_ONE = new BlockPos(0, 5, 12);
    public static final BlockPos HOLDING_SEAL = new BlockPos(-6, 1, 16);
    public static final BlockPos VOW_CANDLE = new BlockPos(7, 1, 24);
    public static final BlockPos LOCK_TWO = new BlockPos(0, 5, 27);
    public static final BlockPos ARENA = new BlockPos(0, 1, 36);
    private final BlockPos origin;
    private final int surface;

    public ArchivePiece(BlockPos origin, int surface) {
        super(ModStructures.ARCHIVE_PIECE.get(), 0, new BoundingBox(origin.getX() - HALF, origin.getY() - 1, origin.getZ() - 1,
                origin.getX() + HALF, Math.max(origin.getY() + HEIGHT, surface + 3), origin.getZ() + LENGTH + 1));
        this.origin = origin;
        this.surface = surface;
    }

    public ArchivePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.ARCHIVE_PIECE.get(), tag);
        this.origin = NbtUtils.readBlockPos(tag, "Origin").orElse(BlockPos.ZERO);
        this.surface = tag.getInt("Surface");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.put("Origin", NbtUtils.writeBlockPos(origin));
        tag.putInt("Surface", surface);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        build(level, box, random, origin, surface, true);
    }

    private static void set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    private static BlockState wall(RandomSource random) {
        int r = random.nextInt(8);
        return r == 0 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    }

    /** Builds the Archive with its first hall's floor at {@code origin}, only inside {@code box}. */
    public static void build(WorldGenLevel level, BoundingBox box, RandomSource random, BlockPos origin, int surface, boolean keepers) {
        // ---- shell, halls and partitions
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = 0; z <= LENGTH; z++) {
                for (int y = 0; y <= HEIGHT; y++) {
                    boolean shell = Math.abs(x) == HALF || z == 0 || z == LENGTH || z == 12 || z == 27;
                    BlockState state;
                    if (y == 0) {
                        state = (x + z) % 4 == 0 ? Blocks.BONE_BLOCK.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
                    } else if (y == HEIGHT) {
                        state = Blocks.DEEPSLATE_TILES.defaultBlockState();
                    } else if (shell) {
                        state = wall(random);
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    set(level, box, origin.offset(x, y, z), state);
                }
            }
        }
        // ---- doors: iron bars in the partitions, their locks hidden above
        for (int partition : new int[]{12, 27}) {
            for (int x = -1; x <= 1; x++) {
                for (int y = 1; y <= 3; y++) {
                    set(level, box, origin.offset(x, y, partition), Blocks.IRON_BARS.defaultBlockState());
                }
            }
        }
        lock(level, box, origin, LOCK_ONE, ArchiveLockBlockEntity.Mode.PLATES, PLATE_A, PLATE_B, 12);
        lock(level, box, origin, LOCK_TWO, ArchiveLockBlockEntity.Mode.SEAL, HOLDING_SEAL, VOW_CANDLE, 27);
        // ---- hall of plates
        set(level, box, origin.offset(PLATE_A), ModBlocks.VOW_PLATE.get().defaultBlockState());
        set(level, box, origin.offset(PLATE_B), ModBlocks.VOW_PLATE.get().defaultBlockState());
        // ---- hall of the seal
        set(level, box, origin.offset(HOLDING_SEAL), ModBlocks.HOLDING_SEAL.get().defaultBlockState());
        set(level, box, origin.offset(VOW_CANDLE), ModBlocks.VOW_CANDLE.get().defaultBlockState());
        for (int z = 15; z <= 24; z += 3) {
            set(level, box, origin.offset(-2, 1, z), Blocks.BOOKSHELF.defaultBlockState());
            set(level, box, origin.offset(2, 1, z), Blocks.BOOKSHELF.defaultBlockState());
            set(level, box, origin.offset(-2, 2, z), Blocks.BOOKSHELF.defaultBlockState());
            set(level, box, origin.offset(2, 2, z), Blocks.BOOKSHELF.defaultBlockState());
        }
        chest(level, box, random, origin.offset(-8, 1, 25), Direction.EAST, LOOT);
        // ---- the Archdeacon's hall
        set(level, box, origin.offset(ARENA), ModBlocks.RITUAL_SEAL.get().defaultBlockState());
        for (BlockPos a : new BlockPos[]{ARENA.offset(4, 0, 0), ARENA.offset(-4, 0, 0), ARENA.offset(0, 0, 4), ARENA.offset(0, 0, -4)}) {
            set(level, box, origin.offset(a), ModBlocks.RITUAL_ANCHOR.get().defaultBlockState());
        }
        for (int x = -6; x <= 6; x += 12) {
            for (int z = 30; z <= 42; z += 4) {
                for (int y = 1; y < HEIGHT; y++) {
                    set(level, box, origin.offset(x, y, z), Blocks.BONE_BLOCK.defaultBlockState());
                }
                set(level, box, origin.offset(x + (x > 0 ? -1 : 1), 1, z), Blocks.RED_CANDLE.defaultBlockState()
                        .setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
            }
        }
        chest(level, box, random, origin.offset(0, 1, 43), Direction.NORTH, ARCHDEACON_LOOT);
        for (int z = 4; z <= 40; z += 9) {
            set(level, box, origin.offset(0, HEIGHT - 1, z), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        // ---- the shaft: ladder on the south wall, up to the surface
        for (int y = 1; y <= surface - origin.getY(); y++) {
            if (y > HEIGHT) {
                set(level, box, origin.offset(-1, y, 1), wall(random));
                set(level, box, origin.offset(1, y, 1), wall(random));
                set(level, box, origin.offset(0, y, 2), wall(random));
                set(level, box, origin.offset(0, y, 0), wall(random));
            }
            set(level, box, origin.offset(0, y, 1), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        }
        int top = surface - origin.getY();
        for (int x = -1; x <= 1; x++) {
            for (int z = 0; z <= 2; z++) {
                if (x != 0 || z != 1) {
                    set(level, box, origin.offset(x, top, z), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                }
                set(level, box, origin.offset(x, top + 1, z), Blocks.AIR.defaultBlockState());
                set(level, box, origin.offset(x, top + 2, z), Blocks.AIR.defaultBlockState());
            }
        }
        set(level, box, origin.offset(1, top + 1, 2), Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2));
        set(level, box, origin.offset(-1, top + 1, 2), Blocks.BONE_BLOCK.defaultBlockState());
        if (!keepers) {
            return;
        }
        spawn(level, box, origin.offset(5, 1, 18), ModEntities.RENEGADE_SEALER.get().create(level.getLevel()));
        spawn(level, box, origin.offset(-3, 1, 22), ModEntities.RENEGADE_SEALER.get().create(level.getLevel()));
        FacelessSentry sentry = ModEntities.FACELESS_SENTRY.get().create(level.getLevel());
        if (sentry != null) {
            sentry.guardPost(origin.offset(0, 1, 25));
        }
        spawn(level, box, origin.offset(0, 1, 25), sentry);
        Archdeacon archdeacon = ModEntities.ARCHDEACON.get().create(level.getLevel());
        if (archdeacon != null) {
            archdeacon.setHome(origin.offset(ARENA));
        }
        spawn(level, box, origin.offset(0, 1, 39), archdeacon);
    }

    private static void spawn(WorldGenLevel level, BoundingBox box, BlockPos at, @org.jetbrains.annotations.Nullable Mob mob) {
        if (mob == null || !box.isInside(at)) {
            return;
        }
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
    }

    private static void lock(WorldGenLevel level, BoundingBox box, BlockPos origin, BlockPos at, ArchiveLockBlockEntity.Mode mode,
                             BlockPos first, BlockPos second, int partition) {
        BlockPos pos = origin.offset(at);
        if (!box.isInside(pos)) {
            return;
        }
        level.setBlock(pos, ModBlocks.ARCHIVE_LOCK.get().defaultBlockState(), 2);
        if (level.getBlockEntity(pos) instanceof ArchiveLockBlockEntity lock) {
            lock.configure(mode, origin.offset(first), origin.offset(second), origin.offset(-1, 1, partition), origin.offset(1, 3, partition));
        }
    }

    private static void chest(WorldGenLevel level, BoundingBox box, RandomSource random, BlockPos pos, Direction facing, ResourceKey<LootTable> loot) {
        if (box.isInside(pos)) {
            level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity entity) {
                entity.setLootTable(loot, random.nextLong());
            }
        }
    }
}
