package com.soulinchains.arcaneartifacts.world;

import com.mojang.serialization.Codec;
import com.soulinchains.arcaneartifacts.entity.Gribushka;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A Mycelinoid colony on the floor of a Mushroom Cave: one to three mushroom houses, 7 to 12 Mycelinoids and
 * one or two Amanitoid guardians, all staying around the colony.
 */
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
        // The houses: one in the middle, up to two small ones on either side (all within 15 blocks).
        BlockPos center = floor(level, origin);
        if (center != null) {
            MushroomHouse.build(level, center, random.nextBoolean(), Direction.Plane.HORIZONTAL.getRandomDirection(random), random);
            Direction axis = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            for (Direction side : new Direction[]{axis, axis.getOpposite()}) {
                if (random.nextInt(3) == 0) {
                    continue;
                }
                BlockPos spot = floor(level, center.relative(side, 10));
                if (spot != null) {
                    MushroomHouse.build(level, spot, false, side.getOpposite(), random);
                }
            }
        }
        BlockPos home = center == null ? origin : center;
        int count = 7 + random.nextInt(6);
        int placed = 0;
        for (int attempt = 0; attempt < count * 4 && placed < count; attempt++) {
            if (spawn(level, random, home, ModEntities.GRIBUSHKA.get().create(level.getLevel()))) {
                placed++;
            }
        }
        int guards = 1 + random.nextInt(2);
        for (int attempt = 0; attempt < guards * 4 && guards > 0; attempt++) {
            if (spawn(level, random, home, ModEntities.AMANITOID.get().create(level.getLevel()))) {
                guards--;
            }
        }
        return placed > 0;
    }

    private static boolean spawn(WorldGenLevel level, RandomSource random, BlockPos home, Gribushka mob) {
        if (mob == null) {
            return false;
        }
        BlockPos pos = floor(level, home.offset(random.nextInt(13) - 6, 0, random.nextInt(13) - 6));
        if (pos == null || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360F, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        mob.setHome(home);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
        return true;
    }

    /** The first air block above solid ground near {@code start}, searching 4 blocks up and down. */
    private static BlockPos floor(WorldGenLevel level, BlockPos start) {
        BlockPos.MutableBlockPos pos = start.mutable();
        for (int dy = 4; dy >= -4; dy--) {
            pos.set(start.getX(), start.getY() + dy, start.getZ());
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                    && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                return pos.immutable();
            }
        }
        return null;
    }
}
