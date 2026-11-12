package com.soulinchains.arcaneartifacts.alchemy;

import com.mojang.serialization.MapCodec;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Alchemical Distiller: a crafting station two blocks wide and two blocks high. Every part opens the same
 * interface; the bottom-left part holds the contents. Breaking any part breaks the whole distiller.
 */
public class AlchemicalDistillerBlock extends BaseEntityBlock {
    public static final MapCodec<AlchemicalDistillerBlock> CODEC = simpleCodec(AlchemicalDistillerBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DistillerPart> PART = EnumProperty.create("part", DistillerPart.class);
    private static final VoxelShape BOTTOM = Shapes.block();
    private static final VoxelShape TOP = Block.box(1, 0, 1, 15, 14, 15);

    public AlchemicalDistillerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, DistillerPart.BOTTOM_LEFT));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART).ordinal() >= 2 ? TOP : BOTTOM;
    }

    public static BlockPos mainPos(BlockState state, BlockPos pos) {
        return state.getValue(PART).main(pos, state.getValue(FACING));
    }

    /** Placed from the bottom-left block; the other three need free space. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos main = context.getClickedPos();
        Level level = context.getLevel();
        for (DistillerPart part : DistillerPart.values()) {
            BlockPos pos = part.from(main, facing);
            if (!part.isMain() && (!level.getBlockState(pos).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(pos)
                    || pos.getY() >= level.getMaxBuildHeight())) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing).setValue(PART, DistillerPart.BOTTOM_LEFT);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        for (DistillerPart part : DistillerPart.values()) {
            if (!part.isMain()) {
                level.setBlock(part.from(pos, facing), state.setValue(PART, part), 3);
            }
        }
    }

    /** A part whose partners are gone disappears too. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        BlockPos main = mainPos(state, pos);
        for (DistillerPart part : DistillerPart.values()) {
            BlockPos partPos = part.from(main, facing);
            if (partPos.equals(neighborPos)) {
                boolean intact = neighbor.is(this) && neighbor.getValue(PART) == part && neighbor.getValue(FACING) == facing;
                return intact ? state : Blocks.AIR.defaultBlockState();
            }
        }
        return state;
    }

    /** Breaking any part in survival drops the distiller once (from the main part). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !state.getValue(PART).isMain()) {
            BlockPos main = mainPos(state, pos);
            BlockState mainState = level.getBlockState(main);
            if (mainState.is(this)) {
                if (!player.isCreative()) {
                    Block.dropResources(mainState, level, main, level.getBlockEntity(main), player, player.getMainHandItem());
                }
                level.setBlock(main, Blocks.AIR.defaultBlockState(), 35);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AlchemicalDistillerBlockEntity distiller) {
            Containers.dropContents(level, pos, distiller);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART).isMain() ? new AlchemicalDistillerBlockEntity(pos, state) : null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || !state.getValue(PART).isMain() ? null
                : createTickerHelper(type, ModBlocks.ALCHEMICAL_DISTILLER_ENTITY.get(), AlchemicalDistillerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(mainPos(state, pos)) instanceof AlchemicalDistillerBlockEntity distiller) {
            player.openMenu(distiller);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
