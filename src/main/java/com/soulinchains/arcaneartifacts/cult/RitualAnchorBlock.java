package com.soulinchains.arcaneartifacts.cult;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A ritual anchor: a short blackstone post with a red candle on top. Lit and mended with the Oath Shield. */
public class RitualAnchorBlock extends BaseEntityBlock {
    public static final MapCodec<RitualAnchorBlock> CODEC = simpleCodec(p -> new RitualAnchorBlock());
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 14, 12);

    public RitualAnchorBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(3.0F, 9.0F)
                .sound(SoundType.POLISHED_DEEPSLATE).noOcclusion().lightLevel(s -> s.getValue(LIT) ? 9 : 0));
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
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
        return new RitualAnchorBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!(stack.getItem() instanceof OathShieldItem)) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof RitualAnchorBlockEntity anchor && stack.isEmpty()) {
                player.displayClientMessage(Component.translatable("arcane_artifacts.cult.anchor_state", anchor.stability(),
                        Component.translatable(anchor.broken() ? "arcane_artifacts.cult.anchor_broken" : anchor.active()
                                ? "arcane_artifacts.cult.anchor_lit" : "arcane_artifacts.cult.anchor_dark")), true);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof RitualAnchorBlockEntity anchor)) {
            return ItemInteractionResult.SUCCESS;
        }
        RitualSealBlockEntity seal = RitualSealBlockEntity.near(level, pos);
        if (seal != null) {
            seal.onShieldAtAnchor(player, anchor);
        } else {
            player.displayClientMessage(Component.translatable("arcane_artifacts.cult.no_seal"), true);
        }
        return ItemInteractionResult.CONSUME;
    }
}
