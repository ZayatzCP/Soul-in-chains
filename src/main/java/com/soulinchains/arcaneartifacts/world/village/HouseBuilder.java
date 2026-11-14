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
        switch (type) {
            case WELL -> {
                well(floor, wood);
                return;
            }
            case WATCHTOWER -> {
                watchtower(floor, kind, wood, door);
                return;
            }
            case MARKET -> {
                market(floor, wood, door);
                return;
            }
            case GARDEN -> {
                garden(floor, wood, door);
                return;
            }
            case LAMP -> {
                lamp(floor, wood);
                return;
            }
            default -> {
            }
        }
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

    // ------------------------------------------------------------------ the buildings between the houses

    /** Solid ground under {@code pos}: what was carved or hangs in the air gets a floor of planks. */
    private void ground(BlockPos pos, BlockState state) {
        if (box.isInside(pos) && !level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP)) {
            set(pos, state);
        }
    }

    private void clear(BlockPos from, BlockPos to) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            set(pos.immutable(), Blocks.AIR.defaultBlockState());
        }
    }

    /** The village well: a ring of stem around water, four posts and a little roof with a glowing cap. */
    private void well(BlockPos floor, MushroomWood.Set wood) {
        clear(floor.offset(-2, 0, -2), floor.offset(2, 4, 2));
        BlockState stem = Blocks.MUSHROOM_STEM.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    set(floor.below(), Blocks.WATER.defaultBlockState());
                    set(floor.below(2), Blocks.WATER.defaultBlockState());
                    set(floor.below(3), Blocks.MUSHROOM_STEM.defaultBlockState());
                } else {
                    set(floor.offset(dx, 0, dz), stem);
                    set(floor.offset(dx, -1, dz), stem);
                }
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    ground(floor.offset(dx, -1, dz), wood.planks.get().defaultBlockState());
                    set(floor.offset(dx, 0, dz), MushroomWood.MUSHROOM_MOSS_CARPET.get().defaultBlockState());
                }
            }
        }
        for (int[] corner : new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}}) {
            set(floor.offset(corner[0], 1, corner[1]), wood.fence.get().defaultBlockState());
            set(floor.offset(corner[0], 2, corner[1]), wood.fence.get().defaultBlockState());
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(floor.offset(dx, 3, dz), wood.slab.get().defaultBlockState());
            }
        }
        set(floor.offset(0, 4, 0), ModBlocks.GLOWING_BLUE_MUSHROOM_BLOCK.get().defaultBlockState());
    }

    /** A tall hollow mushroom with a ladder inside and a lookout on its cap. */
    private void watchtower(BlockPos floor, MushroomWood.Kind kind, MushroomWood.Set wood, Direction door) {
        int top = HouseType.WATCHTOWER.height;
        BlockState stem = Blocks.MUSHROOM_STEM.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(floor.offset(dx, -1, dz), wood.planks.get().defaultBlockState());
                for (int y = 0; y < top; y++) {
                    set(floor.offset(dx, y, dz), dx == 0 && dz == 0 ? Blocks.AIR.defaultBlockState() : stem);
                }
            }
        }
        // The ladder climbs the back wall from the floor to the lookout.
        Direction back = door.getOpposite();
        for (int y = 0; y <= top; y++) {
            set(floor.above(y), Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, door));
        }
        BlockPos doorway = floor.relative(door);
        set(doorway, Blocks.AIR.defaultBlockState());
        set(doorway.above(), Blocks.AIR.defaultBlockState());
        set(floor.relative(back).above(top - 2), Blocks.SHROOMLIGHT.defaultBlockState());
        // The lookout: a disc of cap with a railing, a hole for the ladder.
        BlockState capBlock = cap(kind);
        int r = HouseType.WATCHTOWER.capRadius();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!inside(dx, dz, r)) {
                    continue;
                }
                BlockPos pos = floor.offset(dx, top, dz);
                set(pos, dx == 0 && dz == 0 ? Blocks.LADDER.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.LadderBlock.FACING, door) : capBlock);
                set(pos.above(), !inside(dx, dz, r - 1) ? wood.fence.get().defaultBlockState() : Blocks.AIR.defaultBlockState());
                set(pos.above(2), Blocks.AIR.defaultBlockState());
            }
        }
        set(floor.offset(r - 1, top + 1, 0), Blocks.SHROOMLIGHT.defaultBlockState());
    }

    /** Market stalls: three counters under plank awnings, goods in barrels behind them. */
    private void market(BlockPos floor, MushroomWood.Set wood, Direction door) {
        Direction along = door.getClockWise();
        clear(floor.offset(-4, 0, -4), floor.offset(4, 4, 4));
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                ground(floor.offset(dx, -1, dz), wood.planks.get().defaultBlockState());
            }
        }
        for (int i = -1; i <= 1; i++) {
            BlockPos stall = floor.relative(along, i * 3);
            set(stall.relative(door), wood.slab.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP));
            barrel(stall.relative(door.getOpposite()), HOUSE_LOOT);
            for (Direction side : new Direction[]{along, along.getOpposite()}) {
                BlockPos post = stall.relative(door).relative(side);
                set(post, wood.fence.get().defaultBlockState());
                set(post.above(), wood.fence.get().defaultBlockState());
                BlockPos backPost = stall.relative(door.getOpposite()).relative(side);
                set(backPost, wood.fence.get().defaultBlockState());
                set(backPost.above(), wood.fence.get().defaultBlockState());
            }
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    BlockPos roof = stall.relative(along, a).relative(door, b).above(2);
                    set(roof, wood.slab.get().defaultBlockState());
                }
                set(stall.relative(along, a).relative(door, 2).above(2), wood.trapdoor.get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, door)
                        .setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true));
            }
        }
        set(floor.above(3), Blocks.SHROOMLIGHT.defaultBlockState());
    }

    /** A fenced garden of cave mushrooms around a little huge mushroom. */
    private void garden(BlockPos floor, MushroomWood.Set wood, Direction door) {
        int r = HouseType.GARDEN.radius;
        clear(floor.offset(-r, 0, -r), floor.offset(r, 4, r));
        java.util.List<Block> plants = new java.util.ArrayList<>();
        ModBlocks.MUSHROOMS.values().forEach(holder -> plants.add(holder.get()));
        plants.add(Blocks.RED_MUSHROOM);
        plants.add(Blocks.BROWN_MUSHROOM);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos pos = floor.offset(dx, 0, dz);
                boolean edge = Math.abs(dx) == r || Math.abs(dz) == r;
                set(pos.below(), edge ? wood.planks.get().defaultBlockState() : Blocks.MYCELIUM.defaultBlockState());
                if (edge) {
                    set(pos, wood.fence.get().defaultBlockState());
                } else if ((dx + dz) % 2 == 0 && (Math.abs(dx) > 1 || Math.abs(dz) > 1)) {
                    set(pos, plants.get(random.nextInt(plants.size())).defaultBlockState());
                }
            }
        }
        BlockPos gate = floor.relative(door, r);
        set(gate, wood.fenceGate.get().defaultBlockState().setValue(net.minecraft.world.level.block.FenceGateBlock.FACING, door));
        // A little huge mushroom in the middle.
        for (int y = 0; y < 3; y++) {
            set(floor.above(y), Blocks.MUSHROOM_STEM.defaultBlockState());
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(floor.offset(dx, 3, dz), (dx + dz) % 2 == 0 ? ModBlocks.SPOTTED_BLUE_MUSHROOM_BLOCK.get().defaultBlockState()
                        : ModBlocks.BLUE_MUSHROOM_BLOCK.get().defaultBlockState());
            }
        }
        set(floor.offset(0, 4, 0), ModBlocks.GLOWING_BLUE_MUSHROOM_BLOCK.get().defaultBlockState());
        set(floor.relative(door.getOpposite(), r - 1).relative(door.getClockWise(), r - 1), Blocks.COMPOSTER.defaultBlockState());
    }

    /** A lamp post: a fence pole with a glowing cap, on a little rug of moss. */
    private void lamp(BlockPos floor, MushroomWood.Set wood) {
        ground(floor.below(), wood.planks.get().defaultBlockState());
        for (int y = 0; y < 3; y++) {
            set(floor.above(y), wood.fence.get().defaultBlockState());
        }
        set(floor.above(3), Blocks.SHROOMLIGHT.defaultBlockState());
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos rug = floor.relative(side);
            if (level.getBlockState(rug).isAir() && level.getBlockState(rug.below()).isFaceSturdy(level, rug.below(), Direction.UP)) {
                set(rug, MushroomWood.MUSHROOM_MOSS_CARPET.get().defaultBlockState());
            }
        }
    }

    /** A path of moss carpet from {@code from} to {@code to}, laid on whatever floor there is. */
    public void path(BlockPos from, BlockPos to) {
        int steps = Math.max(Math.abs(to.getX() - from.getX()), Math.abs(to.getZ() - from.getZ()));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : (double) i / steps;
            int x = (int) Math.round(from.getX() + (to.getX() - from.getX()) * t);
            int z = (int) Math.round(from.getZ() + (to.getZ() - from.getZ()) * t);
            int y = (int) Math.round(from.getY() + (to.getY() - from.getY()) * t);
            for (int dy = 3; dy >= -3; dy--) {
                BlockPos pos = new BlockPos(x, y + dy, z);
                if (!box.isInside(pos)) {
                    break;
                }
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                    set(pos, MushroomWood.MUSHROOM_MOSS_CARPET.get().defaultBlockState());
                    break;
                }
            }
        }
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
