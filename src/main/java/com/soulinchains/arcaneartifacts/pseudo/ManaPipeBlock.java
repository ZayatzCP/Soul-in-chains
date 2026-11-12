package com.soulinchains.arcaneartifacts.pseudo;

import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Mana Pipe: joins Mana Tanks, Mana Dispensers and Source Jars into one network. */
public class ManaPipeBlock extends Block {
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        ARMS.put(Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11));
        ARMS.put(Direction.UP, Block.box(5, 11, 5, 11, 16, 11));
        ARMS.put(Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5));
        ARMS.put(Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16));
        ARMS.put(Direction.WEST, Block.box(0, 5, 5, 5, 11, 11));
        ARMS.put(Direction.EAST, Block.box(11, 5, 5, 16, 11, 11));
    }

    public ManaPipeBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (Direction direction : Direction.values()) {
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN);
    }

    /** Pipes connect to pipes, tanks, dispensers and Source Jars. */
    public static boolean connectsTo(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(ModBlocks.MANA_PIPE.get()) || state.is(ModBlocks.MANA_TANK.get()) || state.is(ModBlocks.MANA_DISPENSER.get())
                || level.getBlockEntity(pos) instanceof SourceJarTile;
    }

    private BlockState connections(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), connectsTo(level, pos.relative(direction)));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connections(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), connectsTo(level, neighborPos));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction direction : Direction.values()) {
            if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                shape = Shapes.or(shape, ARMS.get(direction));
            }
        }
        return shape;
    }
}
