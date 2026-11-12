package com.soulinchains.arcaneartifacts.pseudo;

import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A Mana Tank is every tank block connected to another: Source is shared, filling from the bottom up and
 * emptying from the top down. Limits: 3 × 3 wide, 5 high.
 */
public final class ManaTanks {
    public static final int MAX_WIDTH = 3;
    public static final int MAX_HEIGHT = 5;
    private static final int MAX_BLOCKS = MAX_WIDTH * MAX_WIDTH * MAX_HEIGHT;

    private ManaTanks() {
    }

    public static boolean isTank(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.MANA_TANK.get());
    }

    /** Every tank block connected to {@code start} (including it), bottom first. */
    public static List<BlockPos> group(BlockGetter level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        if (isTank(level, start)) {
            queue.add(start.immutable());
            seen.add(start.immutable());
        }
        while (!queue.isEmpty() && seen.size() <= MAX_BLOCKS) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!seen.contains(next) && isTank(level, next)) {
                    seen.add(next);
                    queue.add(next);
                }
            }
        }
        List<BlockPos> list = new ArrayList<>(seen);
        list.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
        return list;
    }

    /** Whether a tank block at {@code pos} keeps every tank it would join within 3 × 3 × 5. */
    public static boolean canJoin(BlockGetter level, BlockPos pos) {
        Set<BlockPos> merged = new HashSet<>();
        merged.add(pos);
        for (Direction direction : Direction.values()) {
            BlockPos next = pos.relative(direction);
            if (isTank(level, next) && !merged.contains(next)) {
                merged.addAll(group(level, next));
            }
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : merged) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxZ = Math.max(maxZ, p.getZ());
        }
        return maxX - minX < MAX_WIDTH && maxZ - minZ < MAX_WIDTH && maxY - minY < MAX_HEIGHT;
    }

    private static List<ManaTankBlockEntity> tiles(BlockGetter level, BlockPos start) {
        List<ManaTankBlockEntity> tiles = new ArrayList<>();
        for (BlockPos pos : group(level, start)) {
            if (level.getBlockEntity(pos) instanceof ManaTankBlockEntity tile) {
                tiles.add(tile);
            }
        }
        return tiles;
    }

    public static int stored(BlockGetter level, BlockPos start) {
        return tiles(level, start).stream().mapToInt(ManaTankBlockEntity::getSource).sum();
    }

    public static int capacity(BlockGetter level, BlockPos start) {
        return tiles(level, start).size() * ManaTankBlockEntity.capacityPerBlock();
    }

    /** Adds up to {@code amount}, lowest blocks first; returns how much went in. */
    public static int insert(BlockGetter level, BlockPos start, int amount) {
        int left = amount;
        for (ManaTankBlockEntity tile : tiles(level, start)) {
            if (left <= 0) {
                break;
            }
            int room = ManaTankBlockEntity.capacityPerBlock() - tile.getSource();
            int put = Math.min(room, left);
            if (put > 0) {
                tile.setSource(tile.getSource() + put);
                left -= put;
            }
        }
        return amount - left;
    }

    /** Takes up to {@code amount}, highest blocks first; returns how much came out. */
    public static int extract(BlockGetter level, BlockPos start, int amount) {
        List<ManaTankBlockEntity> tiles = tiles(level, start);
        int left = amount;
        for (int i = tiles.size() - 1; i >= 0 && left > 0; i--) {
            ManaTankBlockEntity tile = tiles.get(i);
            int take = Math.min(tile.getSource(), left);
            if (take > 0) {
                tile.setSource(tile.getSource() - take);
                left -= take;
            }
        }
        return amount - left;
    }
}
