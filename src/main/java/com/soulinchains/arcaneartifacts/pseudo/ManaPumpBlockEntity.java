package com.soulinchains.arcaneartifacts.pseudo;

import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Moves Source from the intake side to the outlet side of a {@link ManaPumpBlock}, a batch every second. */
public class ManaPumpBlockEntity extends BlockEntity {
    public static final int INTERVAL = 20;
    public static final int BATCH = 1000;

    public ManaPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MANA_PUMP_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaPumpBlockEntity pump) {
        if (level.getGameTime() % INTERVAL != pos.asLong() % INTERVAL || level.hasNeighborSignal(pos)) {
            return;
        }
        int moved = pump(level, pos, state.getValue(ManaPumpBlock.FACING), BATCH);
        if (moved > 0 && level instanceof ServerLevel server) {
            Direction out = state.getValue(ManaPumpBlock.FACING);
            server.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5 + out.getStepX() * 0.55, pos.getY() + 0.5 + out.getStepY() * 0.55,
                    pos.getZ() + 0.5 + out.getStepZ() * 0.55, 4, 0.1, 0.1, 0.1, 0.02);
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.25f, 1.6f);
        }
    }

    /** Pushes up to {@code amount} Source from the tanks behind the pump to the tanks in front; returns how much moved. */
    public static int pump(Level level, BlockPos pos, Direction outlet, int amount) {
        List<BlockPos> from = tanksOn(level, pos, outlet.getOpposite());
        List<BlockPos> to = tanksOn(level, pos, outlet);
        to.removeIf(target -> from.stream().anyMatch(source -> ManaTanks.group(level, source).contains(target)));
        int available = 0;
        for (BlockPos tank : from) {
            available += ManaTanks.stored(level, tank);
        }
        int moved = 0;
        for (BlockPos tank : to) {
            int want = Math.min(amount - moved, available - moved);
            if (want <= 0) {
                break;
            }
            int room = ManaTanks.capacity(level, tank) - ManaTanks.stored(level, tank);
            int put = Math.min(want, room);
            if (put <= 0) {
                continue;
            }
            int taken = 0;
            for (BlockPos source : from) {
                taken += ManaTanks.extract(level, source, put - taken);
                if (taken >= put) {
                    break;
                }
            }
            moved += ManaTanks.insert(level, tank, taken);
        }
        return moved;
    }

    /** One block of each tank on {@code side} of the pump: the tank touching it, or the tanks its pipe reaches. */
    static List<BlockPos> tanksOn(Level level, BlockPos pos, Direction side) {
        BlockPos next = pos.relative(side);
        List<BlockPos> tanks = new ArrayList<>();
        if (ManaTanks.isTank(level, next)) {
            tanks.add(next);
        } else if (level.getBlockState(next).is(ModBlocks.MANA_PIPE.get())) {
            tanks.addAll(ManaNetwork.tanks(level, next));
        }
        return tanks;
    }
}
