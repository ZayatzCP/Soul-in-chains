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
 * back. During a rite it takes only the offering the Heart is asking for.
 */
public class OfferingPedestalBlock extends BaseEntityBlock {
    public static final MapCodec<OfferingPedestalBlock> CODEC = simpleCodec(p -> new OfferingPedestalBlock());
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);

    public OfferingPedestalBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.0F, 6.0F).sound(SoundType.POLISHED_DEEPSLATE)
                .requiresCorrectToolForDrops().noOcclusion());
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
