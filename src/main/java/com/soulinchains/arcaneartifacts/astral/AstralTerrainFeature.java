package com.soulinchains.arcaneartifacts.astral;

import com.mojang.serialization.Codec;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.jetbrains.annotations.Nullable;

/**
 * The land of the Astral, built chunk by chunk: wide, gentle rises over the flat plain, drifts of the snow of oblivion
 * (never on a monolith) and the monoliths. Both are pure
 * functions of the seed, and every chunk writes only its own blocks - the rises under it and its slice of every monolith
 * that passes through it - so a monolith may lean as far as it likes.
 *
 * <p>The plain is cut into cells of {@link #CELL} blocks; about six in ten hold a monolith of solid, plain stone. Some rise
 * to the top of the world, some stop high above, some are sunk deep with only their tops out of the ground. Most stand
 * straight, some lean a little; the sunk ones may lean more.
 */
public class AstralTerrainFeature extends Feature<NoneFeatureConfiguration> {
    public static final int CELL = 72;
    /** How far a monolith may reach from its cell, sideways, at any height. */
    static final int REACH = 80;
    /** The highest a rise lifts the plain. */
    public static final int HILLS = 16;

    public enum Kind { WHOLE, TALL, SUNK }

    /**
     * One monolith: where it stands at the plain's level, its half-width there, its shape, how fast it thins, how far it
     * leans per block of height, and the height it stops below.
     */
    public record Plan(int x, int z, int half, int shape, boolean alongX, int taper, double tiltX, double tiltZ, int top, Kind kind) {
        /** Half-width at height y: it thins slowly going up, never below 2 or half its width. */
        public int half(int y) {
            if (y <= Astral.FLOOR) {
                return half;
            }
            return Math.max(Math.max(2, half / 2), half - (y - Astral.FLOOR) / taper);
        }

        public int centerX(int y) {
            return x + (int) Math.round(tiltX * (y - Astral.FLOOR));
        }

        public int centerZ(int y) {
            return z + (int) Math.round(tiltZ * (y - Astral.FLOOR));
        }

        /** Whether (dx, dz) from the centre is inside a slice of half-width h: square, chamfered, or a slab. */
        public boolean inside(int dx, int dz, int h) {
            int ax = Math.abs(dx);
            int az = Math.abs(dz);
            return switch (shape) {
                case 1 -> ax <= h && az <= h && ax + az <= h + h / 2;
                case 2 -> alongX ? ax <= h && az <= Math.max(1, h / 2) : az <= h && ax <= Math.max(1, h / 2);
                default -> ax <= h && az <= h;
            };
        }

        public boolean contains(int bx, int y, int bz) {
            return y >= 1 && y < top && inside(bx - centerX(y), bz - centerZ(y), half(y));
        }
    }

    public AstralTerrainFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    // ------------------------------------------------------------------ the rises

    private static long mix(long h) {
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    /** A value in [-1, 1) for a lattice point. */
    private static double lattice(long seed, long xi, long zi) {
        long h = mix(seed ^ (xi * 0x9E3779B97F4A7C15L) ^ (zi * 0xC2B2AE3D27D4EB4FL));
        return (h >>> 11) * 0x1.0p-53 * 2 - 1;
    }

    private static double smooth(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    /** Smooth value noise in [-1, 1] with lattice spacing {@code scale}. */
    static double noise(long seed, double x, double z, double scale) {
        double gx = x / scale;
        double gz = z / scale;
        long xi = (long) Math.floor(gx);
        long zi = (long) Math.floor(gz);
        double fx = smooth(gx - xi);
        double fz = smooth(gz - zi);
        double a = lattice(seed, xi, zi);
        double b = lattice(seed, xi + 1, zi);
        double c = lattice(seed, xi, zi + 1);
        double d = lattice(seed, xi + 1, zi + 1);
        double top = a + (b - a) * fx;
        double bottom = c + (d - c) * fx;
        return top + (bottom - top) * fz;
    }

    /** The top solid block of the plain at (x, z): {@link Astral#FLOOR} in the lows, up to {@link #HILLS} higher on a rise. */
    public static int ground(long seed, int x, int z) {
        double n = 0.7 * noise(seed ^ 0x51A7L, x, z, 260) + 0.3 * noise(seed ^ 0x7A11L, x, z, 110);
        double lift = (n + 0.15) * HILLS;
        return Astral.FLOOR + (int) Math.max(0, Math.min(HILLS, Math.round(lift)));
    }

    /**
     * How many layers of the snow of oblivion lie at (x, z), 0 to 7: drifts in patches tens of blocks wide, nothing between
     * them.
     */
    public static int snow(long seed, int x, int z) {
        double n = 0.75 * noise(seed ^ 0x5A0L, x, z, 38) + 0.25 * noise(seed ^ 0x5A1L, x, z, 11);
        return (int) Math.max(0, Math.min(7, Math.round((n - 0.1) * 12)));
    }

    // ------------------------------------------------------------------ the monoliths

    /** The monolith of a cell, or null when the cell is empty. Depends only on the seed and the cell. */
    @Nullable
    public static Plan plan(long seed, int cellX, int cellZ, int worldTop) {
        RandomSource r = RandomSource.create(seed ^ (cellX * 341873128712L) ^ (cellZ * 132897987541L) ^ 0x2F6A_1B3CL);
        if (r.nextFloat() > 0.62F) {
            return null;
        }
        int x = cellX * CELL + 8 + r.nextInt(CELL - 16);
        int z = cellZ * CELL + 8 + r.nextInt(CELL - 16);
        int shape = r.nextInt(3);
        boolean alongX = r.nextBoolean();
        int taper = 90 + r.nextInt(160);
        float kindRoll = r.nextFloat();
        Kind kind = kindRoll < 0.45F ? Kind.WHOLE : kindRoll < 0.75F ? Kind.TALL : Kind.SUNK;
        int half;
        int top;
        double maxTilt;
        switch (kind) {
            case WHOLE -> {
                half = 4 + r.nextInt(8);
                top = worldTop;
                maxTilt = 0.1;
            }
            case TALL -> {
                half = 3 + r.nextInt(8);
                top = Math.min(worldTop, Astral.FLOOR + 110 + r.nextInt(260));
                maxTilt = 0.12;
            }
            default -> {
                half = 5 + r.nextInt(9);
                top = Astral.FLOOR + 8 + r.nextInt(40);
                maxTilt = 0.35;
            }
        }
        // two in five stand straight; the rest lean a little, in any direction
        double tiltX = 0;
        double tiltZ = 0;
        if (r.nextFloat() >= 0.4F) {
            double angle = r.nextDouble() * Math.PI * 2;
            double tilt = maxTilt * (0.3 + 0.7 * r.nextDouble());
            tiltX = Math.cos(angle) * tilt;
            tiltZ = Math.sin(angle) * tilt;
        }
        return new Plan(x, z, half, shape, alongX, taper, tiltX, tiltZ, top, kind);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        long seed = level.getSeed();
        int top = level.getMaxBuildHeight();
        BlockState stone = ModBlocks.ASTRAL_STONE.get().defaultBlockState();
        BlockState drift = ModBlocks.ASTRAL_SNOW.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int g = ground(seed, x, z);
                for (int y = Astral.FLOOR + 1; y <= g; y++) {
                    level.setBlock(pos.set(x, y, z), stone, 2);
                }
                int layers = snow(seed, x, z);
                if (layers > 0) {
                    level.setBlock(pos.set(x, g + 1, z), drift.setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, layers), 2);
                }
            }
        }
        for (int cx = Math.floorDiv(minX - REACH, CELL); cx <= Math.floorDiv(minX + 15 + REACH, CELL); cx++) {
            for (int cz = Math.floorDiv(minZ - REACH, CELL); cz <= Math.floorDiv(minZ + 15 + REACH, CELL); cz++) {
                Plan plan = plan(seed, cx, cz, top);
                if (plan != null) {
                    build(level, plan, minX, minZ);
                }
            }
        }
        return true;
    }

    /** Writes the part of a monolith that lies in the chunk starting at (minX, minZ). */
    public static void build(WorldGenLevel level, Plan plan, int minX, int minZ) {
        BlockState stone = ModBlocks.ASTRAL_MONOLITH.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 1; y < plan.top(); y++) {
            int h = plan.half(y);
            int cx = plan.centerX(y);
            int cz = plan.centerZ(y);
            int x0 = Math.max(minX, cx - h);
            int x1 = Math.min(minX + 15, cx + h);
            int z0 = Math.max(minZ, cz - h);
            int z1 = Math.min(minZ + 15, cz + h);
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    if (plan.inside(x - cx, z - cz, h)) {
                        level.setBlock(pos.set(x, y, z), stone, 2);
                    }
                }
            }
        }
    }
}
