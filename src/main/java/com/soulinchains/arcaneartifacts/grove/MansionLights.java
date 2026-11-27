package com.soulinchains.arcaneartifacts.grove;

import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * Captive lights in the woodland mansions: where the mansion puts a vindicator or an evoker, now and then a Caged Light
 * stands beside them, on the floor of the same room.
 */
public final class MansionLights {
    /** The chance that an illager of the mansion keeps a caged light. */
    public static final float CHANCE = 0.4F;

    private MansionLights() {
    }

    public static boolean keeper(String marker) {
        return "Warrior".equals(marker) || "Mage".equals(marker) || "Group of Allies".equals(marker);
    }

    /** Called for each data marker of a mansion piece; may place a cage next to the illager. Returns where, or null. */
    @Nullable
    public static BlockPos onMarker(String marker, BlockPos pos, LevelAccessor level, RandomSource random, @Nullable BoundingBox box) {
        if (!keeper(marker) || random.nextFloat() >= CHANCE) {
            return null;
        }
        BlockPos spot = spot(pos, level, random, box);
        if (spot != null) {
            level.setBlock(spot, ModBlocks.LIGHT_CAGE.get().defaultBlockState().setValue(LightJarBlock.COLOR, LightColor.random(random)), 2);
        }
        return spot;
    }

    /** An empty floor block within two of the marker, in the piece. */
    @Nullable
    static BlockPos spot(BlockPos pos, LevelAccessor level, RandomSource random, @Nullable BoundingBox box) {
        Direction first = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        for (int reach = 1; reach <= 2; reach++) {
            for (int turn = 0; turn < 4; turn++) {
                Direction d = Direction.from2DDataValue((first.get2DDataValue() + turn) % 4);
                BlockPos p = pos.relative(d, reach);
                if ((box == null || box.isInside(p)) && level.getBlockState(p).isAir() && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                    return p;
                }
            }
        }
        return null;
    }
}
