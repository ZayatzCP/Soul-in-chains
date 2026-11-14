package com.soulinchains.arcaneartifacts.cave;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Small plants of the Mushroom Caves: spore tufts and glowsprouts on the floor, hyphae hanging from the
 * ceiling. They need solid ground (or a solid ceiling) and fall off without it; walking goes through them.
 */
public class CavePlantBlock extends Block {
    private final boolean hanging;
    private final VoxelShape shape;

    public CavePlantBlock(boolean hanging, Properties properties) {
        super(properties);
        this.hanging = hanging;
        this.shape = hanging ? Block.box(3, 4, 3, 13, 16, 13) : Block.box(3, 0, 3, 13, 10, 13);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(properties -> new CavePlantBlock(hanging, properties));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction toward = hanging ? Direction.UP : Direction.DOWN;
        BlockPos support = pos.relative(toward);
        return level.getBlockState(support).isFaceSturdy(level, support, toward.getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
}
