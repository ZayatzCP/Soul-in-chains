package com.soulinchains.arcaneartifacts.world.village;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.registry.MushroomWood;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Builds one Mycelinoid house: a huge mushroom with a hollow stem, a plank floor, a door and glass windows
 * cut into the stem, a lamp under the cap, and furniture that depends on what the house is for. Only
 * blocks inside {@code box} are placed, so a house can be built chunk by chunk.
 */
public final class HouseBuilder {
    public static final ResourceKey<LootTable> HOUSE_LOOT = ResourceKey.create(Registries.LOOT_TABLE, ArcaneArtifacts.id("chests/mycelinoid_house"));
    public static final ResourceKey<LootTable> GUARD_LOOT = ResourceKey.create(Registries.LOOT_TABLE, ArcaneArtifacts.id("chests/mycelinoid_guardhouse"));

    private final WorldGenLevel level;
    private final BoundingBox box;
    private final RandomSource random;

    public HouseBuilder(WorldGenLevel level, BoundingBox box, RandomSource random) {
        this.level = level;
        this.box = box;
        this.random = random;
    }

    private void set(BlockPos pos, BlockState state) {
        if (box.isInside(pos) && !level.getBlockState(pos).is(Blocks.BEDROCK)) {
            level.setBlock(pos, state, 2);
        }
    }

    private static boolean inside(int dx, int dz, int radius) {
        return dx * dx + dz * dz <= radius * radius + radius / 2;
    }

    private static BlockState cap(MushroomWood.Kind kind) {
        return switch (kind) {
            case RED -> Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();
            case BROWN -> Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState();
            case BLUE -> ModBlocks.BLUE_MUSHROOM_BLOCK.get().defaultBlockState();
        };
    }

    public void build(BlockPos floor, HouseType type, MushroomWood.Kind kind, Direction door) {
        MushroomWood.Set wood = MushroomWood.SETS.get(kind);
        int r = type.radius;
        int height = type.height;
        int capR = type.capRadius();
        BlockState stem = Blocks.MUSHROOM_STEM.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState planks = type == HouseType.FARM ? ModBlocks.MUSHROOM_MOSS.get().defaultBlockState() : wood.planks.get().defaultBlockState();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!inside(dx, dz, r)) {
                    continue;
                }
                boolean wall = !inside(dx, dz, r - 1);
                set(floor.offset(dx, -1, dz), wall ? stem : planks);
                for (int y = 0; y < height; y++) {
                    set(floor.offset(dx, y, dz), wall ? stem : air);
                }
            }
        }
        BlockState capBlock = cap(kind);
        for (int layer = 0; layer < 3; layer++) {
            int radius = capR - layer;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (inside(dx, dz, radius)) {
                        set(floor.offset(dx, height + layer, dz), capBlock);
                    }
                }
            }
        }
        for (int dx = -capR; dx <= capR; dx++) {
            for (int dz = -capR; dz <= capR; dz++) {
                if (inside(dx, dz, capR) && !inside(dx, dz, capR - 1)) {
                    set(floor.offset(dx, height - 1, dz), capBlock);
                }
            }
        }
        // Clear a step in front of the door, then the door itself.
        BlockPos doorway = floor.relative(door, r);
        set(doorway.relative(door), air);
        set(doorway.relative(door).above(), air);
        BlockState doorState = wood.door.get().defaultBlockState().setValue(DoorBlock.FACING, door.getOpposite());
        set(doorway, doorState.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(doorway.above(), doorState.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        set(doorway.relative(door).below(), wood.planks.get().defaultBlockState());
        // Windows: glass panes in the stem on the three other sides.
        for (Direction side : new Direction[]{door.getClockWise(), door.getCounterClockWise(), door.getOpposite()}) {
            BlockState pane = Blocks.GLASS_PANE.defaultBlockState()
                    .setValue(paneSide(side.getClockWise()), true).setValue(paneSide(side.getCounterClockWise()), true);
            set(floor.relative(side, r).above(2), pane);
            if (type == HouseType.HALL) {
                set(floor.relative(side, r).above(3), pane);
            }
        }
        // A lamp under the cap.
        set(floor.above(height - 1), Blocks.SHROOMLIGHT.defaultBlockState());
        furnish(floor, type, wood, door);
    }

    private static net.minecraft.world.level.block.state.properties.BooleanProperty paneSide(Direction direction) {
        return switch (direction) {
            case NORTH -> CrossCollisionBlock.NORTH;
            case EAST -> CrossCollisionBlock.EAST;
            case SOUTH -> CrossCollisionBlock.SOUTH;
            default -> CrossCollisionBlock.WEST;
        };
    }

    private void table(BlockPos pos, MushroomWood.Set wood) {
        set(pos, wood.fence.get().defaultBlockState());
        set(pos.above(), wood.pressurePlate.get().defaultBlockState());
    }

    /** A chair at {@code pos}, its seat facing {@code towards}. */
    private void chair(BlockPos pos, MushroomWood.Set wood, Direction towards) {
        set(pos, wood.stairs.get().defaultBlockState().setValue(StairBlock.FACING, towards.getOpposite()));
    }

    private void barrel(BlockPos pos, ResourceKey<LootTable> loot) {
        if (!box.isInside(pos)) {
            return;
        }
        set(pos, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        RandomizableContainer.setBlockEntityLootTable(level, random, pos, loot);
    }

    private void furnish(BlockPos floor, HouseType type, MushroomWood.Set wood, Direction door) {
        Direction back = door.getOpposite();
        Direction left = door.getClockWise();
        Direction right = door.getCounterClockWise();
        Block carpet = MushroomWood.MUSHROOM_MOSS_CARPET.get();
        switch (type) {
            case HALL -> {
                // A long table across the hall with chairs on both sides, rugs by the door.
                for (int i = -1; i <= 1; i++) {
                    BlockPos spot = floor.relative(left, i);
                    table(spot, wood);
                    chair(spot.relative(door), wood, back);
                    chair(spot.relative(back), wood, door);
                }
                set(floor.relative(door, 3), carpet.defaultBlockState());
                set(floor.relative(back, 3), carpet.defaultBlockState());
                set(floor.relative(left, 3).above(type.height - 2), Blocks.SHROOMLIGHT.defaultBlockState());
                set(floor.relative(right, 3).above(type.height - 2), Blocks.SHROOMLIGHT.defaultBlockState());
                barrel(floor.relative(back, 2).relative(right, 2), HOUSE_LOOT);
            }
            case DWELLING -> {
                // Two moss sleeping mats, a table with a chair.
                set(floor.relative(back, 2), carpet.defaultBlockState());
                set(floor.relative(back, 2).relative(left), carpet.defaultBlockState());
                table(floor.relative(right), wood);
                chair(floor.relative(right).relative(door), wood, back);
                barrel(floor.relative(back).relative(right, 1).relative(right), HOUSE_LOOT);
            }
            case STORAGE -> {
                barrel(floor.relative(back, 2), HOUSE_LOOT);
                barrel(floor.relative(back).relative(left), HOUSE_LOOT);
                barrel(floor.relative(back).relative(right), HOUSE_LOOT);
                barrel(floor.relative(left, 2), HOUSE_LOOT);
                set(floor.relative(back, 2).above(), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
            }
            case FARM -> {
                // Mushrooms growing on the moss floor, a composter by the wall.
                Block[] plants = {Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM};
                int i = 0;
                for (BlockPos spot : new BlockPos[]{floor.relative(back, 2), floor.relative(left, 2), floor.relative(right, 2),
                        floor.relative(back).relative(left), floor.relative(back).relative(right)}) {
                    Block plant = i % 2 == 0 && !ModBlocks.MUSHROOMS.isEmpty()
                            ? ModBlocks.MUSHROOMS.values().stream().skip(random.nextInt(ModBlocks.MUSHROOMS.size())).findFirst().get().get()
                            : plants[random.nextInt(plants.length)];
                    set(spot, plant.defaultBlockState());
                    i++;
                }
                set(floor.relative(left).relative(door), Blocks.COMPOSTER.defaultBlockState());
            }
            case BREWERY -> {
                set(floor.relative(back, 2), Blocks.BREWING_STAND.defaultBlockState());
                set(floor.relative(back).relative(left), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
                barrel(floor.relative(back).relative(right), HOUSE_LOOT);
                table(floor.relative(left, 2), wood);
            }
            case GUARDHOUSE -> {
                barrel(floor.relative(back, 2), GUARD_LOOT);
                chair(floor.relative(left, 2), wood, right);
                chair(floor.relative(right, 2), wood, left);
                table(floor.relative(back).relative(left), wood);
                set(floor.relative(back).relative(right), wood.trapdoor.get().defaultBlockState());
            }
        }
    }
}
