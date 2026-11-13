package com.soulinchains.arcaneartifacts.world;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A Mycelinoid house: a huge mushroom with a doorway and windows cut into its stem, a floor, a lamp under
 * the cap and some furniture. The stem is hollow; the cap is red, brown or blue.
 */
public final class MushroomHouse {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, ArcaneArtifacts.id("chests/mycelinoid_house"));
    public static final int HEIGHT = 5;

    private MushroomHouse() {
    }

    /** The cap block of a house: red, brown or the caves' own blue. */
    private static BlockState cap(RandomSource random) {
        return switch (random.nextInt(3)) {
            case 0 -> Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();
            case 1 -> Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState();
            default -> ModBlocks.BLUE_MUSHROOM_BLOCK.get().defaultBlockState();
        };
    }

    /** The outer radius of a house's stem (2 small, 3 large) and of its cap. */
    public static int stemRadius(boolean large) {
        return large ? 3 : 2;
    }

    public static int capRadius(boolean large) {
        return stemRadius(large) + 2;
    }

    private static boolean inside(int dx, int dz, int radius) {
        return dx * dx + dz * dz <= radius * radius + radius / 2;
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).is(Blocks.BEDROCK)) {
            level.setBlock(pos, state, 2);
        }
    }

    /**
     * Builds a house standing on {@code floor} (the first air block above the ground); the door faces
     * {@code door}. Whatever rock is in the way is carved out.
     */
    public static void build(WorldGenLevel level, BlockPos floor, boolean large, Direction door, RandomSource random) {
        int r = stemRadius(large);
        int capR = capRadius(large);
        BlockState stem = Blocks.MUSHROOM_STEM.defaultBlockState();
        BlockState cap = cap(random);
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!inside(dx, dz, r)) {
                    continue;
                }
                boolean wall = !inside(dx, dz, r - 1);
                set(level, floor.offset(dx, -1, dz), stem);                 // the floor
                for (int y = 0; y < HEIGHT; y++) {
                    set(level, floor.offset(dx, y, dz), wall ? stem : air);
                }
            }
        }
        // The cap: an overhanging skirt, then a dome.
        for (int layer = 0; layer < 3; layer++) {
            int radius = capR - layer;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (inside(dx, dz, radius)) {
                        set(level, floor.offset(dx, HEIGHT + layer, dz), cap);
                    }
                }
            }
        }
        for (int dx = -capR; dx <= capR; dx++) {
            for (int dz = -capR; dz <= capR; dz++) {
                if (inside(dx, dz, capR) && !inside(dx, dz, capR - 1)) {
                    set(level, floor.offset(dx, HEIGHT - 1, dz), cap);
                }
            }
        }
        // The doorway (2 high) and two windows, cut into the stem.
        BlockPos doorway = floor.relative(door, r);
        set(level, doorway, air);
        set(level, doorway.above(), air);
        set(level, doorway.relative(door), air);                             // step out of the door
        set(level, doorway.relative(door).above(), air);
        for (Direction side : new Direction[]{door.getClockWise(), door.getCounterClockWise()}) {
            set(level, floor.relative(side, r).above(2), air);
        }
        // Furniture: a lamp under the cap, a table with stools, a barrel of spare mushrooms.
        set(level, floor.above(HEIGHT - 1), ModBlocks.MUSHROOM_LAMP.get().defaultBlockState());
        Direction back = door.getOpposite();
        if (large) {
            BlockPos table = floor.relative(back);
            set(level, table, ModBlocks.MUSHROOM_TABLE.get().defaultBlockState());
            set(level, table.relative(door.getClockWise()), ModBlocks.MUSHROOM_STOOL.get().defaultBlockState());
            set(level, table.relative(door.getCounterClockWise()), ModBlocks.MUSHROOM_STOOL.get().defaultBlockState());
            BlockPos barrel = floor.relative(back, r - 1).relative(door.getClockWise(), 1);
            barrel(level, barrel, random);
            set(level, floor.relative(back, r - 1).relative(door.getCounterClockWise(), 1), Blocks.MOSS_CARPET.defaultBlockState());
        } else {
            set(level, floor.relative(back), ModBlocks.MUSHROOM_STOOL.get().defaultBlockState());
            barrel(level, floor.relative(door.getClockWise()), random);
        }
    }

    private static void barrel(WorldGenLevel level, BlockPos pos, RandomSource random) {
        set(level, pos, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        RandomizableContainer.setBlockEntityLootTable(level, random, pos, LOOT);
    }
}
