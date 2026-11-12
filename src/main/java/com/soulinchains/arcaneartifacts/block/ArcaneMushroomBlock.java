package com.soulinchains.arcaneartifacts.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * A mushroom of the Mushroom Caves. It behaves like a vanilla mushroom (spreads slowly in the dark, grows
 * anywhere on mushroom moss); only the blue one can be grown into a huge mushroom with bone meal.
 */
public class ArcaneMushroomBlock extends MushroomBlock {
    private final boolean growsHuge;

    public ArcaneMushroomBlock(ResourceKey<ConfiguredFeature<?, ?>> hugeFeature, boolean growsHuge, Properties properties) {
        super(hugeFeature, properties);
        this.growsHuge = growsHuge;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return growsHuge;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return growsHuge && random.nextFloat() < 0.4F;
    }
}
