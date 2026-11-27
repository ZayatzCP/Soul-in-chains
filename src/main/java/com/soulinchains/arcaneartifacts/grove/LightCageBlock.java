package com.soulinchains.arcaneartifacts.grove;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Caged Light: the illagers of the woodland mansions keep Thicket Lights shut in iron cages, as lamps. Break the cage and
 * the light is free - but it is not yours: it is wild, and it must be won with glow berries like any other.
 */
public class LightCageBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 10, 12), Block.box(6, 10, 6, 10, 12, 10));

    public LightCageBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.CHAIN).noOcclusion().lightLevel(s -> 9));
        registerDefaultState(stateDefinition.any().setValue(LightJarBlock.COLOR, LightColor.AMBER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LightJarBlock.COLOR);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!next.is(this) && level instanceof ServerLevel server) {
            ThicketLight light = ThicketLight.release(server, null, state.getValue(LightJarBlock.COLOR), Vec3.atCenterOf(pos));
            if (light != null) {
                light.setPersistenceRequired();
            }
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1F, 1.6F);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(new DustParticleOptions(state.getValue(LightJarBlock.COLOR).vector(), 0.5F), pos.getX() + 0.5,
                    pos.getY() + 0.4 + random.nextDouble() * 0.2, pos.getZ() + 0.5, 0, 0, 0);
        }
    }
}
