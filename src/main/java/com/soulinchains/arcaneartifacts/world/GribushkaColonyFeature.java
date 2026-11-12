package com.soulinchains.arcaneartifacts.world;

import com.mojang.serialization.Codec;
import com.soulinchains.arcaneartifacts.entity.Gribushka;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A colony of 5 to 10 Gribushkas on the floor of a Mushroom Cave; they stay around this spot. */
public class GribushkaColonyFeature extends Feature<NoneFeatureConfiguration> {
    public GribushkaColonyFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (!level.getBiome(origin).is(ModBiomes.MUSHROOM_CAVES)) {
            return false;
        }
        int count = 5 + random.nextInt(6);
        int placed = 0;
        for (int attempt = 0; attempt < count * 4 && placed < count; attempt++) {
            BlockPos pos = floor(level, origin.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4));
            if (pos == null) {
                continue;
            }
            Gribushka gribushka = ModEntities.GRIBUSHKA.get().create(level.getLevel());
            if (gribushka == null) {
                return placed > 0;
            }
            gribushka.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360F, 0);
            gribushka.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
            gribushka.setHome(origin);
            gribushka.setPersistenceRequired();
            level.addFreshEntityWithPassengers(gribushka);
            placed++;
        }
        return placed > 0;
    }

    /** The first air block above solid ground near {@code start}, searching 4 blocks up and down. */
    private static BlockPos floor(WorldGenLevel level, BlockPos start) {
        BlockPos.MutableBlockPos pos = start.mutable();
        for (int dy = 4; dy >= -4; dy--) {
            pos.set(start.getX(), start.getY() + dy, start.getZ());
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                    && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
                return pos.immutable();
            }
        }
        return null;
    }
}
