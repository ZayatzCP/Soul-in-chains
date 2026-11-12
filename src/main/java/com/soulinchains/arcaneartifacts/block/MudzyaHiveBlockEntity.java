package com.soulinchains.arcaneartifacts.block;

import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.common.entity.EntityFollowProjectile;
import com.soulinchains.arcaneartifacts.entity.Mudzya;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Stores the Source brought by Mudzyas and pushes it into nearby Source Jars. */
public class MudzyaHiveBlockEntity extends BlockEntity {
    public static final int CAPACITY = 2000;
    public static final int BOTTLE_COST = 100;
    private static final int CALL_RANGE = 16;
    private static final int JAR_RANGE = 6;

    private int source;

    public MudzyaHiveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MUDZYA_HIVE_ENTITY.get(), pos, state);
    }

    public int getSource() {
        return source;
    }

    public boolean hasRoom() {
        return source < CAPACITY;
    }

    public void addSource(int amount) {
        source = Math.min(CAPACITY, source + Math.max(0, amount));
        setChanged();
    }

    public boolean takeSource(int amount) {
        if (source < amount) {
            return false;
        }
        source -= amount;
        setChanged();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MudzyaHiveBlockEntity hive) {
        long time = level.getGameTime();
        if (time % 40 == 0 && hive.hasRoom()) {
            List<Mudzya> full = level.getEntitiesOfClass(Mudzya.class, new AABB(pos).inflate(CALL_RANGE), Mudzya::isCharged);
            for (Mudzya mudzya : full) {
                mudzya.callToHive(pos);
            }
        }
        if (time % 100 == 0 && hive.source > 0) {
            List<ISpecialSourceProvider> jars = SourceUtil.canGiveSource(pos, level, JAR_RANGE);
            for (ISpecialSourceProvider jar : jars) {
                if (hive.source <= 0) {
                    break;
                }
                var tile = jar.getSource();
                int moved = Math.min(Math.min(hive.source, 500), Math.max(0, tile.getMaxSource() - tile.getSource()));
                if (moved > 0) {
                    tile.addSource(moved);
                }
                if (moved > 0) {
                    hive.source -= moved;
                    hive.setChanged();
                    if (level instanceof ServerLevel server) {
                        EntityFollowProjectile.spawn(server, pos, jar.getCurrentPos());
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Source", source);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        source = tag.getInt("Source");
    }
}
