package com.soulinchains.arcaneartifacts.rite;

import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;

/**
 * Corrupted Soil: grows around a site where something not of this world was called. It kills plants on it, puts out candles
 * standing on it and, in the dark of night, lets Echoes crawl out. A Rite of Cleansing turns it back into dirt.
 */
public class CorruptedSoilBlock extends Block {
    public CorruptedSoilBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.6F).sound(SoundType.ROOTED_DIRT).randomTicks());
    }

    /** Whether corruption can take this block. */
    public static boolean corruptible(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL)
                || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MYCELIUM);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos up = pos.above();
        BlockState above = level.getBlockState(up);
        if (above.is(BlockTags.REPLACEABLE_BY_TREES) || above.is(BlockTags.FLOWERS) || above.is(BlockTags.SAPLINGS)) {
            level.setBlock(up, Blocks.AIR.defaultBlockState(), 3);
        } else if (above.getBlock() instanceof CandleBlock && above.getValue(CandleBlock.LIT)) {
            level.setBlock(up, above.setValue(CandleBlock.LIT, false), 3);
        }
        if (level.isNight() && level.getBrightness(LightLayer.BLOCK, up) < 5 && above.isAir() && random.nextInt(30) == 0
                && level.getEntitiesOfClass(Echo.class, new AABB(pos).inflate(16)).size() < 3) {
            Echo echo = ModEntities.ECHO.get().create(level);
            if (echo != null) {
                echo.moveTo(up.getX() + 0.5, up.getY(), up.getZ() + 0.5, random.nextFloat() * 360, 0);
                level.addFreshEntity(echo);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.MYCELIUM, pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(), 0, 0, 0);
        }
    }
}
