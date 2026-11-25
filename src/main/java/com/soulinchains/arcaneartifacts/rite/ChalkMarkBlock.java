package com.soulinchains.arcaneartifacts.rite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A chalk mark on top of a floor block. Its lines to the four neighbours are drawn one stroke at a time with a chalk
 * (see {@link ChalkItem}), so a figure keeps exactly the lines its author drew. Rain on open sky washes it away, a
 * creature walking over it smudges it, water and pistons wipe it. An offering pedestal set on a mark keeps it underneath.
 */
public class ChalkMarkBlock extends Block {
    public static final EnumProperty<ChalkColor> COLOR = EnumProperty.create("color", ChalkColor.class);
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);
    /** Chance per tick that a creature standing on a mark smudges it. */
    public static final float SMUDGE_CHANCE = 0.02F;

    public ChalkMarkBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).noCollission().instabreak().sound(SoundType.SAND).noOcclusion()
                .randomTicks().pushReaction(PushReaction.DESTROY).replaceable());
        registerDefaultState(stateDefinition.any().setValue(COLOR, ChalkColor.WHITE)
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR, NORTH, EAST, SOUTH, WEST);
    }

    public static BooleanProperty side(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            default -> WEST;
        };
    }

    /** Whether this block carries chalk: a mark, or an offering pedestal standing on one. */
    public static boolean holdsChalk(BlockState state) {
        return state.getBlock() instanceof ChalkMarkBlock
                || state.getBlock() instanceof OfferingPedestalBlock && state.getValue(OfferingPedestalBlock.CHALKED);
    }

    @Nullable
    public static ChalkColor color(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return holdsChalk(state) ? state.getValue(COLOR) : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (direction.getAxis().isHorizontal() && state.getValue(side(direction)) && !holdsChalk(neighbor)) {
            return state.setValue(side(direction), false);
        }
        return state;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isRainingAt(pos)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof Mob && level.random.nextFloat() < SMUDGE_CHANCE) {
            level.removeBlock(pos, false);
        }
    }

    /** Joins two horizontally adjacent marks of the same colour with a line. Returns false when they cannot be joined. */
    public static boolean join(Level level, BlockPos a, BlockPos b) {
        if (a.getY() != b.getY() || a.distManhattan(b) != 1) {
            return false;
        }
        BlockState sa = level.getBlockState(a);
        BlockState sb = level.getBlockState(b);
        if (!holdsChalk(sa) || !holdsChalk(sb) || sa.getValue(COLOR) != sb.getValue(COLOR)) {
            return false;
        }
        Direction toB = Direction.getNearest(b.getX() - a.getX(), 0, b.getZ() - a.getZ());
        if (sa.getValue(side(toB)) && sb.getValue(side(toB.getOpposite()))) {
            return false;
        }
        level.setBlock(a, sa.setValue(side(toB), true), 2);
        level.setBlock(b, sb.setValue(side(toB.getOpposite()), true), 2);
        return true;
    }

    /** Whether a line runs from {@code pos} to its neighbour on {@code direction} (both ends drawn). */
    public static boolean linked(BlockGetter level, BlockPos pos, Direction direction) {
        BlockState a = level.getBlockState(pos);
        BlockState b = level.getBlockState(pos.relative(direction));
        return holdsChalk(a) && holdsChalk(b) && a.getValue(side(direction)) && b.getValue(side(direction.getOpposite()));
    }
}
