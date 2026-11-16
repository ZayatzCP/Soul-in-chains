package com.soulinchains.arcaneartifacts.druid;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Root Magic, the Druid's school: everything draws strength from living ground (grass, moss, leaves,
 * dirt, mycelium) under the feet.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class RootMagic {
    private RootMagic() {
    }

    /** Grass, moss, leaves, dirt, mycelium, or standing in grass and flowers. */
    public static boolean onLivingGround(Entity entity) {
        BlockPos below = entity.getBlockPosBelowThatAffectsMyMovement();
        BlockState state = entity.level().getBlockState(below);
        return state.is(BlockTags.DIRT) || state.is(BlockTags.LEAVES) || state.is(Blocks.MOSS_CARPET)
                || entity.level().getBlockState(entity.blockPosition()).is(BlockTags.REPLACEABLE_BY_TREES);
    }

    public static boolean wears(LivingEntity entity, net.minecraft.world.item.Item item) {
        return CurioHelper.findEquipped(entity, item).isPresent();
    }

    /** All four pieces of the Druid's gear. */
    public static boolean fullSet(LivingEntity entity) {
        return wears(entity, ModItems.DRUID_MANTLE.get()) && wears(entity, ModItems.DRUID_HAT.get())
                && wears(entity, ModItems.DRUID_GLOVES.get()) && wears(entity, ModItems.DRUID_BOOTS.get());
    }

    /**
     * The gloves: crops, saplings and other growing things around the wearer get extra growth ticks.
     * Returns how many plants were nudged.
     */
    public static int tendGarden(ServerLevel level, BlockPos centre, int radius, int tries) {
        int grown = 0;
        for (int i = 0; i < tries; i++) {
            BlockPos pos = centre.offset(level.random.nextInt(radius * 2 + 1) - radius, level.random.nextInt(5) - 2,
                    level.random.nextInt(radius * 2 + 1) - radius);
            BlockState state = level.getBlockState(pos);
            if ((state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS) || state.is(Blocks.SWEET_BERRY_BUSH)
                    || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.BAMBOO) || state.is(Blocks.CACTUS)) && state.isRandomlyTicking()) {
                state.randomTick(level, pos, level.random);
                grown++;
            }
        }
        return grown;
    }

    /** Druid's Boots: farmland is not trampled. */
    @SubscribeEvent
    public static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof LivingEntity living && wears(living, ModItems.DRUID_BOOTS.get())) {
            event.setCanceled(true);
        }
    }
}
