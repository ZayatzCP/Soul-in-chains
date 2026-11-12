package com.soulinchains.arcaneartifacts.pseudo;

import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.common.entity.EntityFollowProjectile;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Moves Source between the tanks of its pipe network and the Source Jars around it. */
public class ManaDispenserBlockEntity extends BlockEntity {
    private static final int JAR_RANGE = 3;
    private static final int MAX_NETWORK = 256;

    public ManaDispenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MANA_DISPENSER_ENTITY.get(), pos, state);
    }

    /** One block of every tank reachable from {@code start} through pipes (or touching it directly). */
    public static List<BlockPos> connectedTanks(Level level, BlockPos start) {
        List<BlockPos> tanks = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Set<BlockPos> tankBlocks = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() < MAX_NETWORK) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!seen.add(next)) {
                    continue;
                }
                if (ManaTanks.isTank(level, next)) {
                    if (!tankBlocks.contains(next)) {
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaDispenserBlockEntity dispenser) {
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        List<BlockPos> tanks = connectedTanks(level, pos);
        if (tanks.isEmpty()) {
            return;
        }
        int rate = Config.MANA_DISPENSER_RATE.get();
        boolean drain = state.getValue(ManaDispenserBlock.DRAIN);
        if (drain) {
            for (ISpecialSourceProvider jar : SourceUtil.canTakeSource(pos, level, JAR_RANGE)) {
                int available = Math.min(rate, jar.getSource().getSource());
                for (BlockPos tank : tanks) {
                    int stored = ManaTanks.insert(level, tank, available);
                    if (stored > 0) {
                        jar.getSource().removeSource(stored);
                        trail(level, jar.getCurrentPos(), pos);
                        available -= stored;
                    }
                }
            }
        } else {
            for (ISpecialSourceProvider jar : SourceUtil.canGiveSource(pos, level, JAR_RANGE)) {
                var tile = jar.getSource();
                int room = Math.min(rate, Math.max(0, tile.getMaxSource() - tile.getSource()));
                for (BlockPos tank : tanks) {
                    if (room <= 0) {
                        break;
                    }
                    int taken = ManaTanks.extract(level, tank, room);
                    if (taken > 0) {
                        tile.addSource(taken);
                        trail(level, pos, jar.getCurrentPos());
                        room -= taken;
                    }
                }
            }
        }
    }

    private static void trail(Level level, BlockPos from, BlockPos to) {
        if (level instanceof ServerLevel server && level.getGameTime() % 40 == 0) {
            EntityFollowProjectile.spawn(server, from, to);
        }
    }
}
