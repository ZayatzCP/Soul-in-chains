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
 * The monoliths of the Astral. The plain is cut into cells of {@link #CELL} blocks; most cells hold one monolith, placed
 * by the seed alone, so neighbouring chunks agree. A monolith rises from under the floor to the top of the world: a
 * stepped plinth, then a column (square, chamfered or a slab) that slowly thins and leans a block now and then, with
 * glowing seams down the middle of each face and glowing bands every few dozen blocks. It is built by the chunk that holds
 * its centre and never reaches more than 15 blocks from it, so it stays inside the chunks a feature may write to.
 */
public class AstralMonolithFeature extends Feature<NoneFeatureConfiguration> {
    public static final int CELL = 72;
    /** How often the column may lean by a block. */
    static final int SEGMENT = 48;
    static final int MAX_LEAN = 4;

    /** One monolith: its centre, half-width at the floor, shape, how fast it thins, its lean and its band spacing. */
    public record Plan(int x, int z, int half, int shape, boolean alongX, int taper, int[] leanX, int[] leanZ, int band) {
        /** Half-width at height y (the plinth is wider). */
        public int half(int y) {
            int floor = Astral.FLOOR;
            if (y <= floor + 2) {
                return half + 2;
            }
            if (y <= floor + 4) {
                return half + 1;
            }
            int thin = (y - floor) / taper;
            return Math.max(Math.max(2, half / 2), half - thin);
        }

        public int leanX(int y) {
            return leanX[Math.min(leanX.length - 1, Math.max(0, (y - Astral.FLOOR) / SEGMENT))];
        }

        public int leanZ(int y) {
            return leanZ[Math.min(leanZ.length - 1, Math.max(0, (y - Astral.FLOOR) / SEGMENT))];
        }

        /** Whether the block at (dx, dz) from the (leaned) centre is inside the monolith with half-width h. */
        public boolean inside(int dx, int dz, int h) {
            int ax = Math.abs(dx);
            int az = Math.abs(dz);
            return switch (shape) {
                case 1 -> ax <= h && az <= h && ax + az <= h + h / 2;
                case 2 -> alongX ? ax <= h && az <= Math.max(1, h / 2) : az <= h && ax <= Math.max(1, h / 2);
                default -> ax <= h && az <= h;
            };
        }

        /** The farthest any block of this monolith reaches from its centre. */
        public int reach() {
            return half + 2 + MAX_LEAN;
        }
    }

    public AstralMonolithFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** The monolith of a cell, or null when the cell is empty. Depends only on the seed and the cell. */
    @Nullable
    public static Plan plan(long seed, int cellX, int cellZ, int top) {
        RandomSource r = RandomSource.create(seed ^ (cellX * 341873128712L) ^ (cellZ * 132897987541L) ^ 0x2F6A_1B3CL);
        if (r.nextFloat() > 0.62F) {
            return null;
        }
        int x = cellX * CELL + 8 + r.nextInt(CELL - 16);
        int z = cellZ * CELL + 8 + r.nextInt(CELL - 16);
        int half = 3 + r.nextInt(7);
        int shape = r.nextInt(3);
        boolean alongX = r.nextBoolean();
        int taper = 70 + r.nextInt(110);
        int segments = Math.max(1, (top - Astral.FLOOR) / SEGMENT + 1);
        int[] leanX = new int[segments];
        int[] leanZ = new int[segments];
        for (int i = 1; i < segments; i++) {
            leanX[i] = clampLean(leanX[i - 1] + (r.nextInt(3) == 0 ? r.nextInt(3) - 1 : 0));
            leanZ[i] = clampLean(leanZ[i - 1] + (r.nextInt(3) == 0 ? r.nextInt(3) - 1 : 0));
        }
        int band = 24 + 8 * r.nextInt(4);
        return new Plan(x, z, half, shape, alongX, taper, leanX, leanZ, band);
    }

    private static int clampLean(int v) {
        return Math.max(-MAX_LEAN, Math.min(MAX_LEAN, v));
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        int top = level.getMaxBuildHeight();
        boolean placed = false;
        for (int cx = Math.floorDiv(minX, CELL); cx <= Math.floorDiv(minX + 15, CELL); cx++) {
            for (int cz = Math.floorDiv(minZ, CELL); cz <= Math.floorDiv(minZ + 15, CELL); cz++) {
                Plan plan = plan(level.getSeed(), cx, cz, top);
                if (plan != null && plan.x() >= minX && plan.x() < minX + 16 && plan.z() >= minZ && plan.z() < minZ + 16) {
                    build(level, plan, top);
                    placed = true;
                }
            }
        }
        return placed;
    }

    static void build(WorldGenLevel level, Plan plan, int top) {
        BlockState stone = ModBlocks.ASTRAL_MONOLITH.get().defaultBlockState();
        BlockState vein = ModBlocks.ASTRAL_VEIN.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = Astral.FLOOR - 3; y < top; y++) {
            int h = plan.half(y);
            int cx = plan.x() + plan.leanX(y);
            int cz = plan.z() + plan.leanZ(y);
            boolean band = y > Astral.FLOOR + 4 && y % plan.band() == 0;
            for (int dx = -h; dx <= h; dx++) {
                for (int dz = -h; dz <= h; dz++) {
                    if (!plan.inside(dx, dz, h)) {
                        continue;
                    }
                    boolean shell = !plan.inside(dx + 1, dz, h) || !plan.inside(dx - 1, dz, h)
                            || !plan.inside(dx, dz + 1, h) || !plan.inside(dx, dz - 1, h);
                    boolean seam = shell && y > Astral.FLOOR + 4 && (dx == 0 || dz == 0);
                    pos.set(cx + dx, y, cz + dz);
                    level.setBlock(pos, shell && (seam || band) ? vein : stone, 2);
                }
            }
        }
    }
}
