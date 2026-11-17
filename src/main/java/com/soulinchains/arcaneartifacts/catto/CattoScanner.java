package com.soulinchains.arcaneartifacts.catto;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Looks through a box of blocks a slice at a time (a fixed budget per tick), only in loaded chunks: Catto never loads or
 * keeps chunks loaded. Results are re-checked when used, because the world keeps changing.
 */
public final class CattoScanner {
    public static final int BUDGET = 2048;
    public static final int MAX_FOUND = 4096;

    private final BlockPos min;
    private final int sx, sy, sz;
    private final long total;
    private final BiPredicate<BlockPos, BlockState> filter;
    private long cursor;
    public final List<BlockPos> found = new ArrayList<>();
    public final String key;
    public final long startedAt;

    public CattoScanner(String key, BlockPos a, BlockPos b, BiPredicate<BlockPos, BlockState> filter, long now) {
        this.key = key;
        this.min = BlockPos.min(a, b);
        BlockPos max = BlockPos.max(a, b);
        this.sx = max.getX() - min.getX() + 1;
        this.sy = max.getY() - min.getY() + 1;
        this.sz = max.getZ() - min.getZ() + 1;
        this.total = (long) sx * sy * sz;
        this.filter = filter;
        this.startedAt = now;
    }

    public static CattoScanner around(String key, BlockPos centre, int radius, int height, BiPredicate<BlockPos, BlockState> filter, long now) {
        return new CattoScanner(key, centre.offset(-radius, -height, -radius), centre.offset(radius, height, radius), filter, now);
    }

    public boolean complete() {
        return cursor >= total;
    }

    /** Scans up to {@code budget} positions; returns true once the whole box has been seen. */
    public boolean tick(Level level, int budget) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int done = 0;
        while (cursor < total && done < budget) {
            long i = cursor++;
            int x = (int) (i % sx);
            int z = (int) ((i / sx) % sz);
            int y = (int) (i / ((long) sx * sz));
            pos.set(min.getX() + x, min.getY() + y, min.getZ() + z);
            done++;
            if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) {
                continue;
            }
            if (filter.test(pos, level.getBlockState(pos)) && found.size() < MAX_FOUND) {
                found.add(pos.immutable());
            }
        }
        return complete();
    }
}
