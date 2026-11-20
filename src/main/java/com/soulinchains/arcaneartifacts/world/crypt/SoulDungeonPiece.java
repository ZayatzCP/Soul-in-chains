package com.soulinchains.arcaneartifacts.world.crypt;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.necro.mob.ChainGhost;
import com.soulinchains.arcaneartifacts.necro.mob.Jailer;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import com.soulinchains.arcaneartifacts.world.village.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * The Soul Dungeon in one piece, 23x23 and three storeys high:
 * <ul>
 *     <li>top - cell blocks behind iron bars, Chain Ghosts, tunnels out into the valley on all four sides;</li>
 *     <li>middle - more cells, wither skeleton guards;</li>
 *     <li>bottom - the Jailer's hall: four chained pillars, soul-fire braziers, his chest.</li>
 * </ul>
 * A ladder in one corner joins the storeys.
 */
public class SoulDungeonPiece extends StructurePiece {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, ArcaneArtifacts.id("chests/soul_dungeon"));
    public static final ResourceKey<LootTable> JAILER_LOOT = ResourceKey.create(Registries.LOOT_TABLE, ArcaneArtifacts.id("chests/soul_dungeon_jailer"));
    public static final int HALF = 11;
    public static final int STOREY = 7;
    public static final int TUNNEL = 22;
    private final BlockPos origin;

    public SoulDungeonPiece(BlockPos origin) {
        super(ModStructures.SOUL_DUNGEON_PIECE.get(), 0, new BoundingBox(origin.getX() - HALF - TUNNEL, origin.getY() - 1, origin.getZ() - HALF - TUNNEL,
                origin.getX() + HALF + TUNNEL, origin.getY() + STOREY * 3 + 1, origin.getZ() + HALF + TUNNEL));
        this.origin = origin;
    }

    public SoulDungeonPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.SOUL_DUNGEON_PIECE.get(), tag);
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

    private static BlockState brick(RandomSource random) {
        int r = random.nextInt(10);
        return r < 2 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                : r < 3 ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    }

    /** Builds the dungeon around {@code origin} (the bottom floor's centre), only inside {@code box}. */
    public static void build(WorldGenLevel level, BoundingBox box, RandomSource random, BlockPos origin, boolean keepers) {
        int top = STOREY * 3;
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                boolean wall = Math.abs(x) == HALF || Math.abs(z) == HALF;
                for (int y = 0; y <= top; y++) {
                    boolean slab = y % STOREY == 0;
                    BlockState state;
                    if (wall) {
                        state = brick(random);
                    } else if (slab) {
                        state = y == 0 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    set(level, box, origin.offset(x, y, z), state);
                }
            }
        }
        // ---- bottom: the Jailer's hall
        for (int px = -6; px <= 6; px += 12) {
            for (int pz = -6; pz <= 6; pz += 12) {
                for (int y = 1; y < STOREY; y++) {
                    set(level, box, origin.offset(px, y, pz), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
                }
                for (int y = 2; y < STOREY; y++) {
                    set(level, box, origin.offset(px + Integer.signum(-px), y, pz), Blocks.CHAIN.defaultBlockState());
                }
            }
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos fire = origin.relative(dir, 9);
            set(level, box, fire, Blocks.SOUL_SOIL.defaultBlockState());
            set(level, box, fire.above(), Blocks.SOUL_FIRE.defaultBlockState());
        }
        chest(level, box, random, origin.offset(0, 1, -HALF + 1), Direction.SOUTH, JAILER_LOOT);
        // ---- middle and top: cell blocks behind bars
        for (int storey = 1; storey <= 2; storey++) {
            int floor = storey * STOREY;
            for (int x = -HALF + 1; x <= HALF - 1; x++) {
                for (int side = -1; side <= 1; side += 2) {
                    int barZ = side * 7;
                    for (int y = floor + 1; y < floor + STOREY; y++) {
                        boolean divider = x % 4 == 0;
                        set(level, box, origin.offset(x, y, barZ), divider ? brick(random) : Blocks.IRON_BARS.defaultBlockState());
                        if (divider) {
                            for (int z = 8; z < HALF; z++) {
                                set(level, box, origin.offset(x, y, side * z), brick(random));
                            }
                        }
                    }
                    if (x % 4 == 2) {
                        set(level, box, origin.offset(x, floor + 1, side * 9), random.nextBoolean() ? Blocks.BONE_BLOCK.defaultBlockState() : Blocks.SOUL_SOIL.defaultBlockState());
                        set(level, box, origin.offset(x, floor + STOREY - 1, side * 9), Blocks.CHAIN.defaultBlockState());
                    }
                }
            }
            for (int x = -4; x <= 4; x += 8) {
                set(level, box, origin.offset(x, floor + STOREY - 1, 0), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            }
            chest(level, box, random, origin.offset(storey == 1 ? -6 : 6, floor + 1, storey == 1 ? 9 : -9), storey == 1 ? Direction.NORTH : Direction.SOUTH, LOOT);
        }
        // ---- the ladder joining the storeys, in the corner
        for (int y = 1; y < top; y++) {
            set(level, box, origin.offset(HALF - 1, y, HALF - 1), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST));
        }
        // ---- tunnels out of the top storey into the valley
        int tunnelY = STOREY * 2 + 1;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            Direction side = dir.getClockWise();
            for (int d = HALF; d <= HALF + TUNNEL; d++) {
                for (int w = -1; w <= 1; w++) {
                    for (int h = 0; h < 3; h++) {
                        BlockPos pos = origin.relative(dir, d).relative(side, w).above(tunnelY + h);
                        set(level, box, pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        // ---- keepers, once, from the chunk holding the centre
        BlockPos centre = origin.above(1);
        if (keepers && box.isInside(centre)) {
            Jailer jailer = ModEntities.JAILER.get().create(level.getLevel());
            if (jailer != null) {
                jailer.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, random.nextFloat() * 360, 0);
                jailer.setPersistenceRequired();
                level.addFreshEntity(jailer);
            }
            for (int i = 0; i < 3; i++) {
                Mob guard = EntityType.WITHER_SKELETON.create(level.getLevel());
                if (guard != null) {
                    guard.moveTo(centre.getX() - 4 + i * 4 + 0.5, centre.getY() + STOREY, centre.getZ() + 0.5, random.nextFloat() * 360, 0);
                    guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
                    guard.setPersistenceRequired();
                    level.addFreshEntity(guard);
                }
            }
            for (int i = -1; i <= 1; i += 2) {
                ChainGhost ghost = ModEntities.CHAIN_GHOST.get().create(level.getLevel());
                if (ghost != null) {
                    ghost.moveTo(centre.getX() + i * 3 + 0.5, centre.getY() + STOREY * 2 + 1, centre.getZ() + 0.5, 0, 0);
                    ghost.setPersistenceRequired();
                    level.addFreshEntity(ghost);
                }
            }
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

    private static void set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }
}
