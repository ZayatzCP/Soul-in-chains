package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ritual Censer: set it within 3 blocks of the seal and the rite breathes easier - the inscription windows are wider and
 * one Torn Acolyte fewer comes with each wave. Smokes all the time.
 */
public class RitualCenserBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 9, 12);

    public RitualCenserBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.LANTERN).noOcclusion().lightLevel(s -> 8));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2, pos.getY() + 0.7, pos.getZ() + 0.5
                + (random.nextDouble() - 0.5) * 0.2, 0, 0.03, 0);
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 0, 0.05, 0);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 0, 0.01, 0);
        }
    }
}
