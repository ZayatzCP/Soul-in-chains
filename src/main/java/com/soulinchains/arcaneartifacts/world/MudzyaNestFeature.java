package com.soulinchains.arcaneartifacts.world;

import com.mojang.serialization.Codec;
import com.soulinchains.arcaneartifacts.entity.Mudzya;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A wild Mudzya Hive on beach sand with two or three Mudzyas that live around it. */
public class MudzyaNestFeature extends Feature<NoneFeatureConfiguration> {
    public MudzyaNestFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.below()).is(BlockTags.SAND)) {
            return false;
        }
        level.setBlock(pos, ModBlocks.WILD_MUDZYA_HIVE.get().defaultBlockState(), 2);
        int count = 2 + context.random().nextInt(2);
        for (int i = 0; i < count; i++) {
            Mudzya mudzya = ModEntities.MUDZYA.get().create(level.getLevel());
            if (mudzya == null) {
                break;
            }
            mudzya.moveTo(pos.getX() + 0.5 + context.random().nextInt(5) - 2, pos.getY() + 1.5, pos.getZ() + 0.5 + context.random().nextInt(5) - 2,
                    context.random().nextFloat() * 360F, 0);
            mudzya.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
            mudzya.setHome(pos);
            mudzya.setPersistenceRequired();
            level.addFreshEntityWithPassengers(mudzya);
        }
        return true;
    }
}
