package com.soulinchains.arcaneartifacts.rite;

import com.mojang.serialization.MapCodec;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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

/** A set-down ritual cage, two blocks tall, with its creature. Breaking it gives back the full cage; sneak-use lets the creature out. */
public class RiteCageBlock extends BaseEntityBlock {
    public static final MapCodec<RiteCageBlock> CODEC = simpleCodec(p -> new RiteCageBlock());
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.world.level.block.state.properties.DoubleBlockHalf> HALF =
            net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape LOWER = Block.box(1, 0, 1, 15, 16, 15);
    private static final VoxelShape UPPER = Block.box(1, 0, 1, 15, 15, 15);

    public RiteCageBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.CHAIN).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    public static boolean lower(BlockState state) {
        return state.getValue(HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER;
    }

    /** The two halves stand and fall together. */
    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighbor,
                                     net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean lower = lower(state);
        if (direction == (lower ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.DOWN)
                && !(neighbor.is(this) && lower(neighbor) != lower)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
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
        return lower(state) ? LOWER : UPPER;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return lower(state) ? new RiteCageBlockEntity(pos, state) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!lower(state)) {
            pos = pos.below();
        }
        if (!(level.getBlockEntity(pos) instanceof RiteCageBlockEntity cage)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (RiteHeartBlockEntity.offerCage(level, pos, player)) {
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && cage.occupied() && level instanceof ServerLevel server) {
            RiteCageItem.release(server, cage.asItemData(), pos);
            cage.consume();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RiteCageBlockEntity cage) {
            ItemStack drop = new ItemStack(ModItems.RITE_CAGE.get());
            if (cage.occupied()) {
                drop.set(DataComponents.CUSTOM_DATA, CustomData.of(cage.asItemData()));
            }
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 0, 0.02, 0);
        }
    }
}
