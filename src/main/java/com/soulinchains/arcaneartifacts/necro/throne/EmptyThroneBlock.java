package com.soulinchains.arcaneartifacts.necro.throne;

import com.mojang.serialization.MapCodec;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Empty Throne in the middle of the Throne of Chains. A Shackle Key used on it wakes the Imprisoned King. */
public class EmptyThroneBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<EmptyThroneBlock> CODEC = simpleCodec(p -> new EmptyThroneBlock());
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 8, 15), Block.box(1, 8, 1, 15, 16, 4));

    public EmptyThroneBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(5.0F, 1200.0F)
                .sound(SoundType.POLISHED_DEEPSLATE).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return com.soulinchains.arcaneartifacts.util.ModelShapes.of(state, SHAPE);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stack.is(ModItems.SHACKLE_KEY.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level instanceof ServerLevel server)) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!ThroneOfChains.complete(level, pos)) {
            ThroneOfChains.tell(player, "incomplete");
        } else if (ThroneOfChains.lit(level, pos) < 4) {
            ThroneOfChains.tell(player, "need_lanterns", ThroneOfChains.lit(level, pos));
        } else if (!ThroneOfChains.rightTime(level)) {
            ThroneOfChains.tell(player, "need_night");
        } else if (ThroneOfChains.summon(server, pos) != null) {
            stack.consume(1, player);
            player.getCooldowns().addCooldown(stack.getItem(), 40);
            ThroneOfChains.tell(player, "summoned");
        }
        return ItemInteractionResult.CONSUME;
    }

}
