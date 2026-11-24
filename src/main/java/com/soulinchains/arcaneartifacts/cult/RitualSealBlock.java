package com.soulinchains.arcaneartifacts.cult;

import com.mojang.serialization.MapCodec;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** The Ritual Seal: a low altar stone, centre and hearth of the circle. Reagents and the Ritual Knife go on it. */
public class RitualSealBlock extends BaseEntityBlock {
    public static final MapCodec<RitualSealBlock> CODEC = simpleCodec(p -> new RitualSealBlock());
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 10, 16);

    public RitualSealBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(4.0F, 9.0F)
                .sound(SoundType.POLISHED_DEEPSLATE).noOcclusion().lightLevel(s -> 4));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RitualSealBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.RITUAL_SEAL_ENTITY.get(), RitualSealBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RitualSealBlockEntity seal)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof RitualKnifeItem) {
            if (!level.isClientSide) {
                seal.onKnife(player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (RitualSealBlockEntity.isReagent(stack)) {
            if (!level.isClientSide) {
                if (seal.offer(stack)) {
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                player.displayClientMessage(seal.reagentsLine(), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.isEmpty() && !level.isClientSide) {
            player.displayClientMessage(seal.phase() == RitualSealBlockEntity.Phase.CHANNEL
                    ? Component.translatable("arcane_artifacts.cult.quality_now", Math.round(seal.quality())) : seal.reagentsLine(), true);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
