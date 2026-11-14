package com.soulinchains.arcaneartifacts.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Blue mushroom flesh is springy: a fall onto it never hurts, and whoever lands on it bounces back up
 * (unless they are sneaking).
 */
public final class Bouncy {
    private Bouncy() {
    }

    static void fallOn(Level level, BlockPos pos, Entity entity, float fallDistance) {
        entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall());
    }

    static boolean bounce(Entity entity) {
        if (entity.isSuppressingBounce()) {
            return false;
        }
        Vec3 motion = entity.getDeltaMovement();
        if (motion.y < 0.0) {
            double factor = entity instanceof LivingEntity ? 0.75 : 0.6;
            entity.setDeltaMovement(motion.x, -motion.y * factor, motion.z);
        }
        return true;
    }

    /** A springy full block: the blue mushroom variants. */
    public static class BouncyBlock extends Block {
        public BouncyBlock(Properties properties) {
            super(properties);
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
            Bouncy.fallOn(level, pos, entity, fallDistance);
        }

        @Override
        public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
            if (!bounce(entity)) {
                super.updateEntityAfterFallOn(level, entity);
            }
        }
    }

    /** The blue mushroom's cap block, with the faces of a huge mushroom block. */
    public static class BouncyHugeMushroomBlock extends HugeMushroomBlock {
        public BouncyHugeMushroomBlock(Properties properties) {
            super(properties);
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
            Bouncy.fallOn(level, pos, entity, fallDistance);
        }

        @Override
        public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
            if (!bounce(entity)) {
                super.updateEntityAfterFallOn(level, entity);
            }
        }
    }
}
