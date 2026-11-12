package com.soulinchains.arcaneartifacts.pseudo;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The tanks a pipe can reach, seen by Ars Nouveau as one Source container: machines near a pipe draw
 * from the tanks, Source Links and other producers near a pipe pour into them.
 */
public final class ManaNetwork implements ISourceTile {
    private static final int MAX_PIPES = 512;

    private final Level level;
    private final BlockPos start;

    public ManaNetwork(Level level, BlockPos start) {
        this.level = level;
        this.start = start.immutable();
    }

    /** One block of each tank reachable through pipes from {@code start}. */
    public static List<BlockPos> tanks(Level level, BlockPos start) {
        List<BlockPos> tanks = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Set<BlockPos> tankBlocks = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() < MAX_PIPES) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!seen.add(next) || !level.isLoaded(next)) {
                    continue;
                }
                if (ManaTanks.isTank(level, next)) {
                    if (tankBlocks.add(next)) {
                        tanks.add(next);
                        tankBlocks.addAll(ManaTanks.group(level, next));
                    }
                } else if (level.getBlockState(next).is(ModBlocks.MANA_PIPE.get())) {
                    queue.add(next);
                }
            }
        }
        return tanks;
    }

    private List<BlockPos> tanks() {
        return tanks(level, start);
    }

    @Override
    public int getTransferRate() {
        return Integer.MAX_VALUE / 4;
    }

    @Override
    public boolean canAcceptSource() {
        return getMaxSource() > getSource();
    }

    @Override
    public int getSource() {
        return tanks().stream().mapToInt(tank -> ManaTanks.stored(level, tank)).sum();
    }

    @Override
    public int getMaxSource() {
        return tanks().stream().mapToInt(tank -> ManaTanks.capacity(level, tank)).sum();
    }

    @Override
    public int setSource(int source) {
        int current = getSource();
        if (source > current) {
            addSource(source - current);
        } else if (source < current) {
            removeSource(current - source);
        }
        return getSource();
    }

    @Override
    public int addSource(int source, boolean simulate) {
        if (simulate) {
            return Math.max(0, Math.min(source, getMaxSource() - getSource()));
        }
        int left = source;
        for (BlockPos tank : tanks()) {
            left -= ManaTanks.insert(level, tank, left);
            if (left <= 0) {
                break;
            }
        }
        return source - left;
    }

    @Override
    public int addSource(int source) {
        addSource(source, false);
        return getSource();
    }

    @Override
    public int removeSource(int source, boolean simulate) {
        if (simulate) {
            return Math.max(0, Math.min(source, getSource()));
        }
        int left = source;
        for (BlockPos tank : tanks()) {
            left -= ManaTanks.extract(level, tank, left);
            if (left <= 0) {
                break;
            }
        }
        return source - left;
    }

    @Override
    public int removeSource(int source) {
        removeSource(source, false);
        return getSource();
    }
}
