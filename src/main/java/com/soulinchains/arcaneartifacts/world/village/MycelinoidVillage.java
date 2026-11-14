package com.soulinchains.arcaneartifacts.world.village;

import com.mojang.serialization.MapCodec;
import com.soulinchains.arcaneartifacts.registry.MushroomWood;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A Mycelinoid village, deep in the Mushroom Caves at the level of the deepslate: a hall in the middle and 2 to
 * 7 houses around it (3 to 8 in all), each with its own purpose, moss paths with lamp posts, a well, and on
 * the outskirts a watchtower, a market and a garden. It only appears in a large cave: there must be a wide,
 * high open floor around the centre, otherwise there is no village here.
 */
public class MycelinoidVillage extends Structure {
    public static final MapCodec<MycelinoidVillage> CODEC = simpleCodec(MycelinoidVillage::new);
    /** Distance of the houses from the hall. */
    public static final int RADIUS = 14;
    /** Distance of the watchtower, the market and the garden from the hall. */
    public static final int OUTER_RADIUS = 26;
    private static final int CLEARANCE = 10;

    public MycelinoidVillage(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.VILLAGE.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        WorldgenRandom random = context.random();
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int minY = context.heightAccessor().getMinBuildHeight() + 8;
        NoiseColumn center = column(context, x, z);
        int floorY = Integer.MIN_VALUE;
        // Deep down, at the level of the deepslate caves.
        for (int y = 0; y > minY; y--) {
            if (isFloor(center, y, CLEARANCE + 2)) {
                floorY = y;
                break;
            }
        }
        if (floorY == Integer.MIN_VALUE) {
            return Optional.empty();
        }
        // A large cave: open floor all around the centre.
        int open = 0;
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            if (floorNear(context, x + (int) Math.round(Math.cos(angle) * 10), z + (int) Math.round(Math.sin(angle) * 10), floorY) != Integer.MIN_VALUE) {
                open++;
            }
        }
        if (open < 6) {
            return Optional.empty();
        }
        BlockPos hallFloor = new BlockPos(x, floorY, z);
        int wanted = 2 + random.nextInt(6);
        double start = random.nextDouble() * Math.PI * 2;
        record House(BlockPos floor, HouseType type, Direction door) {
        }
        List<House> houses = new ArrayList<>();
        houses.add(new House(hallFloor, HouseType.HALL, Direction.Plane.HORIZONTAL.getRandomDirection(random)));
        for (int i = 0; i < wanted; i++) {
            double angle = start + i * Math.PI * 2 / wanted + (random.nextDouble() - 0.5) * 0.3;
            int hx = x + (int) Math.round(Math.cos(angle) * RADIUS);
            int hz = z + (int) Math.round(Math.sin(angle) * RADIUS);
            int hy = floorNear(context, hx, hz, floorY);
            if (hy == Integer.MIN_VALUE) {
                continue;
            }
            // The door faces the hall.
            Direction door = Math.abs(x - hx) > Math.abs(z - hz) ? (x > hx ? Direction.EAST : Direction.WEST) : (z > hz ? Direction.SOUTH : Direction.NORTH);
            houses.add(new House(new BlockPos(hx, hy, hz), HouseType.AROUND[i % HouseType.AROUND.length], door));
        }
        if (houses.size() < 3) {
            return Optional.empty();
        }
        MushroomWood.Kind[] kinds = MushroomWood.Kind.values();
        List<net.minecraft.world.level.levelgen.structure.StructurePiece> pieces = new ArrayList<>();
        for (House house : houses) {
            pieces.add(new VillageHousePiece(house.floor(), hallFloor, house.type(), kinds[random.nextInt(kinds.length)], house.door(), random.nextLong()));
        }
        // Paths of moss from the hall to every house, a lamp post on each.
        for (House house : houses.subList(1, houses.size())) {
            double angle = Math.atan2(house.floor().getZ() - z, house.floor().getX() - x);
            BlockPos from = new BlockPos(x + (int) Math.round(Math.cos(angle) * 5), floorY, z + (int) Math.round(Math.sin(angle) * 5));
            BlockPos to = new BlockPos(x + (int) Math.round(Math.cos(angle) * (RADIUS - 4)), house.floor().getY(),
                    z + (int) Math.round(Math.sin(angle) * (RADIUS - 4)));
            pieces.add(new VillagePathPiece(from, to));
            int lx = x + (int) Math.round(Math.cos(angle + 0.2) * 9);
            int lz = z + (int) Math.round(Math.sin(angle + 0.2) * 9);
            int ly = floorNear(context, lx, lz, floorY, 5);
            if (ly != Integer.MIN_VALUE) {
                pieces.add(new VillageHousePiece(new BlockPos(lx, ly, lz), hallFloor, HouseType.LAMP, kinds[random.nextInt(kinds.length)], Direction.NORTH, random.nextLong()));
            }
        }
        // The well, between the hall and the first two houses.
        if (houses.size() >= 3) {
            double a1 = Math.atan2(houses.get(1).floor().getZ() - z, houses.get(1).floor().getX() - x);
            double a2 = Math.atan2(houses.get(2).floor().getZ() - z, houses.get(2).floor().getX() - x);
            double mid = Math.atan2(Math.sin(a1) + Math.sin(a2), Math.cos(a1) + Math.cos(a2));
            int wx = x + (int) Math.round(Math.cos(mid) * 8);
            int wz = z + (int) Math.round(Math.sin(mid) * 8);
            int wy = floorNear(context, wx, wz, floorY, 6);
            if (wy != Integer.MIN_VALUE) {
                pieces.add(new VillageHousePiece(new BlockPos(wx, wy, wz), hallFloor, HouseType.WELL, kinds[random.nextInt(kinds.length)], Direction.NORTH, random.nextLong()));
            }
        }
        // The outskirts: a watchtower, a market and a garden on the outer ring, where there is floor for them.
        for (int i = 0; i < HouseType.OUTSKIRTS.length; i++) {
            HouseType type = HouseType.OUTSKIRTS[i];
            double angle = start + Math.PI / wanted + i * Math.PI * 2 / HouseType.OUTSKIRTS.length;
            int ox = x + (int) Math.round(Math.cos(angle) * OUTER_RADIUS);
            int oz = z + (int) Math.round(Math.sin(angle) * OUTER_RADIUS);
            int oy = floorNear(context, ox, oz, floorY, type == HouseType.WATCHTOWER ? CLEARANCE : 6);
            if (oy == Integer.MIN_VALUE) {
                continue;
            }
            Direction door = Math.abs(x - ox) > Math.abs(z - oz) ? (x > ox ? Direction.EAST : Direction.WEST) : (z > oz ? Direction.SOUTH : Direction.NORTH);
            pieces.add(new VillageHousePiece(new BlockPos(ox, oy, oz), hallFloor, type, kinds[random.nextInt(kinds.length)], door, random.nextLong()));
        }
        return Optional.of(new GenerationStub(hallFloor, builder -> pieces.forEach(builder::addPiece)));
    }

    private static NoiseColumn column(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
    }

    private static boolean solid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    /** Solid ground at {@code y - 1} and {@code clearance} blocks of air above it. */
    private static boolean isFloor(NoiseColumn column, int y, int clearance) {
        if (!solid(column.getBlock(y - 1))) {
            return false;
        }
        for (int dy = 0; dy < clearance; dy++) {
            if (!column.getBlock(y + dy).isAir()) {
                return false;
            }
        }
        return true;
    }

    /** The floor of the cave at (x, z) within 5 blocks of {@code y}, or MIN_VALUE if there is no room for a house. */
    private static int floorNear(GenerationContext context, int x, int z, int y) {
        return floorNear(context, x, z, y, CLEARANCE);
    }

    private static int floorNear(GenerationContext context, int x, int z, int y, int clearance) {
        NoiseColumn column = column(context, x, z);
        for (int dy = 0; dy <= 5; dy++) {
            for (int sign : new int[]{1, -1}) {
                if (isFloor(column, y + dy * sign, clearance)) {
                    return y + dy * sign;
                }
            }
        }
        return Integer.MIN_VALUE;
    }
}
