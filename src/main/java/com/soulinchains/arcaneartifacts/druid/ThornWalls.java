package com.soulinchains.arcaneartifacts.druid;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** The berry walls of the Thorns circle wither away when their time is up (only bushes still standing are removed). */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class ThornWalls {
    private record Bush(ResourceKey<Level> dimension, BlockPos pos, long until) {
    }

    private static final List<Bush> BUSHES = new ArrayList<>();

    private ThornWalls() {
    }

    static void add(ServerLevel level, BlockPos pos, long until) {
        BUSHES.add(new Bush(level.dimension(), pos.immutable(), until));
    }

    @SubscribeEvent
    public static void onTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || BUSHES.isEmpty() || level.getGameTime() % 10 != 0) {
            return;
        }
        Iterator<Bush> it = BUSHES.iterator();
        while (it.hasNext()) {
            Bush bush = it.next();
            if (bush.dimension() != level.dimension() || bush.until() > level.getGameTime()) {
                continue;
            }
            if (level.isLoaded(bush.pos()) && level.getBlockState(bush.pos()).is(Blocks.SWEET_BERRY_BUSH)) {
                level.removeBlock(bush.pos(), false);
            }
            it.remove();
        }
    }
}
