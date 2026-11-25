package com.soulinchains.arcaneartifacts.necro;

import com.mojang.serialization.MapCodec;
import com.soulinchains.arcaneartifacts.necro.mob.Restless;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A grave: an earth mound with a headstone. Dug up with the Gravedigger's Shovel it gives bones and old trinkets,
 * sometimes a soul - and sometimes its tenant, a Restless. A dug grave slowly settles back.
 */
public class GraveBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<GraveBlock> CODEC = simpleCodec(p -> new GraveBlock());
    public static final BooleanProperty DUG = BooleanProperty.create("dug");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 3, 15), Block.box(5, 0, 12, 11, 12, 14));

    public GraveBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(1.0F, 3.0F).sound(SoundType.ROOTED_DIRT).noOcclusion().randomTicks());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DUG, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DUG);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return com.soulinchains.arcaneartifacts.util.ModelShapes.of(state, SHAPE);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(DUG) && random.nextInt(40) == 0) {
            level.setBlockAndUpdate(pos, state.setValue(DUG, false));
        }
    }

    /** Digs the grave up. Returns false when it has already been dug. */
    public static boolean dig(ServerLevel level, BlockPos pos, @Nullable Player digger) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GraveBlock) || state.getValue(DUG)) {
            return false;
        }
        level.setBlockAndUpdate(pos, state.setValue(DUG, true));
        level.playSound(null, pos, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
        RandomSource random = level.random;
        drop(level, pos, new ItemStack(Items.BONE, 1 + random.nextInt(3)));
        if (random.nextInt(2) == 0) {
            drop(level, pos, new ItemStack(Items.ROTTEN_FLESH, 1 + random.nextInt(2)));
        }
        if (random.nextFloat() < 0.25F) {
            drop(level, pos, new ItemStack(Items.IRON_NUGGET, 1 + random.nextInt(4)));
        }
        if (random.nextFloat() < 0.10F) {
            drop(level, pos, new ItemStack(Items.GOLD_NUGGET, 1 + random.nextInt(3)));
        }
        if (random.nextFloat() < 0.03F) {
            drop(level, pos, new ItemStack(Items.EMERALD));
        }
        if (random.nextFloat() < 0.30F) {
            var wisp = new SoulWisp(ModEntities.SOUL_WISP.get(), level);
            wisp.setSoul(SoulType.WANDERING);
            wisp.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            level.addFreshEntity(wisp);
        }
        if (random.nextFloat() < 0.15F) {
            Restless ghost = ModEntities.RESTLESS.get().create(level);
            if (ghost != null) {
                ghost.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0);
                ghost.shortLived();
                if (digger != null) {
                    ghost.setTarget(digger);
                }
                level.addFreshEntity(ghost);
            }
        }
        return true;
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, stack));
    }
}
