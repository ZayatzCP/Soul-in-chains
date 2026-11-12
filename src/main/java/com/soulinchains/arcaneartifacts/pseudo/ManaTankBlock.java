package com.soulinchains.arcaneartifacts.pseudo;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Mana Tank: stores Source. Like Create's fluid tank, tank blocks that touch merge into one tank (at most
 * 3 × 3 wide and 5 high); a block that would make the tank bigger cannot be placed against it. The UP
 * and DOWN properties hide the inner rims so a stack looks like one tall tank.
 */
public class ManaTankBlock extends BaseEntityBlock {
    public static final MapCodec<ManaTankBlock> CODEC = simpleCodec(ManaTankBlock::new);
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public ManaTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!ManaTanks.canJoin(context.getLevel(), context.getClickedPos())) {
            return null;
        }
        BlockPos pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(UP, context.getLevel().getBlockState(pos.above()).is(this))
                .setValue(DOWN, context.getLevel().getBlockState(pos.below()).is(this));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP) {
            return state.setValue(UP, neighbor.is(this));
        }
        if (direction == Direction.DOWN) {
            return state.setValue(DOWN, neighbor.is(this));
        }
        return state;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaTankBlockEntity(pos, state);
    }
}
