package com.soulinchains.arcaneartifacts.pseudo;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Mana Tank: stores Source. Like Create's fluid tank, tank blocks that touch merge into one tank (at most
 * 3 × 3 wide and 5 high); a block that would make the tank bigger cannot be placed against it. The six
 * connection properties hide the inner walls, so the tank looks like one structure with tall windows.
 */
public class ManaTankBlock extends BaseEntityBlock {
    public static final MapCodec<ManaTankBlock> CODEC = simpleCodec(ManaTankBlock::new);
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    public ManaTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(UP, false).setValue(DOWN, false)
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN, NORTH, EAST, SOUTH, WEST);
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
        List<BlockPos> rest = restOfLayer(context.getLevel(), context.getClickedPos());
        for (BlockPos pos : rest) {
            if (!context.getLevel().getBlockState(pos).canBeReplaced()) {
                refuse(context.getPlayer(), Component.translatable("arcane_artifacts.mana_tank.blocked"));
                return null;
            }
        }
        Player player = context.getPlayer();
        if (player != null && !player.getAbilities().instabuild && !rest.isEmpty()) {
            int have = player.getInventory().countItem(asItem());
            if (have < rest.size() + 1) {
                refuse(player, Component.translatable("arcane_artifacts.mana_tank.need", rest.size() + 1, have));
                return null;
            }
        }
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(property(direction), context.getLevel().getBlockState(context.getClickedPos().relative(direction)).is(this));
        }
        return state;
    }

    /** Stacking one block on a tank builds the whole new layer (3 × 3, 2 × 2...) at once. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        List<BlockPos> rest = restOfLayer(level, pos);
        for (BlockPos other : rest) {
            level.setBlock(other, connected(level, other), Block.UPDATE_ALL);
        }
        if (!rest.isEmpty() && placer instanceof Player player && !player.getAbilities().instabuild) {
            ContainerHelper.clearOrCountMatchingItems(player.getInventory(), s -> s.is(asItem()), rest.size(), false);
        }
    }

    /** The places of the new layer still missing, when {@code pos} is stacked on top of a tank. */
    public static List<BlockPos> restOfLayer(BlockGetter level, BlockPos pos) {
        List<BlockPos> rest = new ArrayList<>();
        if (!ManaTanks.isTank(level, pos.below())) {
            return rest;
        }
        for (BlockPos part : ManaTanks.group(level, pos.below())) {
            if (part.getY() == pos.getY() - 1) {
                BlockPos above = part.above();
                if (!above.equals(pos) && !ManaTanks.isTank(level, above)) {
                    rest.add(above);
                }
            }
        }
        return rest;
    }

    private BlockState connected(BlockGetter level, BlockPos pos) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(property(direction), level.getBlockState(pos.relative(direction)).is(this));
        }
        return state;
    }

    private static void refuse(@Nullable Player player, Component message) {
        if (player != null && !player.level().isClientSide) {
            player.displayClientMessage(message, true);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(property(direction), neighbor.is(this));
    }

    /** True on a side means "another tank block is there": that wall is inside the tank and is not drawn. */
    public static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case UP -> UP;
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
        };
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
