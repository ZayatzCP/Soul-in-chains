package com.soulinchains.arcaneartifacts.rite;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Offering Pedestal: stands on the Heart of the Circle. Use with an item to lay the stack on it, with an empty hand to take it
 * back. During a rite it takes only the offering the Heart is asking for. Set on a chalk mark it keeps the mark under
 * itself (the mark still counts in its sigil), and chalk can be drawn on it directly.
 */
public class OfferingPedestalBlock extends BaseEntityBlock {
    public static final MapCodec<OfferingPedestalBlock> CODEC = simpleCodec(p -> new OfferingPedestalBlock());
    /** Chalk under the pedestal: set when it is placed on a chalk mark (or chalk is drawn on it), with the mark's colour and lines. */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty CHALKED =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("chalked");
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);

    public OfferingPedestalBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.0F, 6.0F).sound(SoundType.POLISHED_DEEPSLATE)
                .requiresCorrectToolForDrops().noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(CHALKED, false).setValue(ChalkMarkBlock.COLOR, ChalkColor.WHITE)
                .setValue(ChalkMarkBlock.NORTH, false).setValue(ChalkMarkBlock.EAST, false)
                .setValue(ChalkMarkBlock.SOUTH, false).setValue(ChalkMarkBlock.WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(CHALKED, ChalkMarkBlock.COLOR, ChalkMarkBlock.NORTH, ChalkMarkBlock.EAST, ChalkMarkBlock.SOUTH, ChalkMarkBlock.WEST);
    }

    /** The pedestal without its chalk. */
    public static BlockState clean(BlockState state) {
        return state.setValue(CHALKED, false).setValue(ChalkMarkBlock.NORTH, false).setValue(ChalkMarkBlock.EAST, false)
                .setValue(ChalkMarkBlock.SOUTH, false).setValue(ChalkMarkBlock.WEST, false);
    }

    /** Set down on a chalk mark, the pedestal keeps the mark (its colour and lines) underneath. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (old.getBlock() instanceof ChalkMarkBlock && !state.getValue(CHALKED)) {
            level.setBlock(pos, state.setValue(CHALKED, true).setValue(ChalkMarkBlock.COLOR, old.getValue(ChalkMarkBlock.COLOR))
                    .setValue(ChalkMarkBlock.NORTH, old.getValue(ChalkMarkBlock.NORTH)).setValue(ChalkMarkBlock.EAST, old.getValue(ChalkMarkBlock.EAST))
                    .setValue(ChalkMarkBlock.SOUTH, old.getValue(ChalkMarkBlock.SOUTH)).setValue(ChalkMarkBlock.WEST, old.getValue(ChalkMarkBlock.WEST)), 3);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighbor,
                                     net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(CHALKED) && direction.getAxis().isHorizontal() && state.getValue(ChalkMarkBlock.side(direction))
                && !ChalkMarkBlock.holdsChalk(neighbor)) {
            return state.setValue(ChalkMarkBlock.side(direction), false);
        }
        return state;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OfferingPedestalBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof SigilBookItem || stack.getItem() instanceof ChalkItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof OfferingPedestalBlockEntity pedestal) || !pedestal.item().isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!RiteHeartBlockEntity.acceptsOffering(level, pos.below(), stack, player)) {
            return ItemInteractionResult.FAIL;
        }
        pedestal.setItem(stack.copy());
        if (!player.getAbilities().instabuild) {
            stack.setCount(0);
        }
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 0.8F);
        RiteHeartBlockEntity.offered(level, pos.below(), player);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof OfferingPedestalBlockEntity pedestal) || pedestal.item().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (RiteHeartBlockEntity.running(level, pos.below())) {
            return InteractionResult.FAIL;
        }
        if (!player.addItem(pedestal.item().copy())) {
            player.drop(pedestal.item().copy(), false);
        }
        pedestal.setItem(ItemStack.EMPTY);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof OfferingPedestalBlockEntity pedestal && !pedestal.item().isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, pedestal.item());
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
