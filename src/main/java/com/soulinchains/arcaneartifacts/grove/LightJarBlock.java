package com.soulinchains.arcaneartifacts.grove;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * A Light in a Jar: a Thicket Light kept in a glass jar, a lamp of its colour. Shift + right-click with an empty hand lets
 * it out (the jar becomes a glass bottle). Broken, the jar comes away whole, the light still inside.
 */
public class LightJarBlock extends BaseEntityBlock {
    public static final MapCodec<LightJarBlock> CODEC = simpleCodec(p -> new LightJarBlock());
    public static final EnumProperty<LightColor> COLOR = EnumProperty.create("color", LightColor.class);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 8, 11), Block.box(6, 8, 6, 10, 10, 10));

    public LightJarBlock() {
        super(BlockBehaviour.Properties.of().strength(0.3F).sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 14));
        registerDefaultState(stateDefinition.any().setValue(COLOR, LightColor.AMBER));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LightJarBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            open(server, pos, state);
            Block.popResource(level, pos, new ItemStack(Items.GLASS_BOTTLE));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Lets the light out of the jar at this place; the jar is gone. */
    public static ThicketLight open(ServerLevel level, BlockPos pos, BlockState state) {
        net.minecraft.nbt.CompoundTag kept = level.getBlockEntity(pos) instanceof LightJarBlockEntity jar ? jar.light() : null;
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1F, 1.4F);
        return ThicketLight.release(level, kept, state.getValue(COLOR), Vec3.atCenterOf(pos));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative() && level.getBlockEntity(pos) instanceof LightJarBlockEntity jar) {
            Block.popResource(level, pos, item(jar, state, level));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** The jar as an item, still holding its light. */
    public static ItemStack item(LightJarBlockEntity jar, BlockState state, Level level) {
        ItemStack stack = new ItemStack(state.getBlock().asItem());
        jar.saveToItem(stack, level.registryAccess());
        stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of(COLOR.getName(), state.getValue(COLOR).getSerializedName())));
        return stack;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(new DustParticleOptions(state.getValue(COLOR).vector(), 0.5F), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2,
                    pos.getY() + 0.3 + random.nextDouble() * 0.3, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2, 0, 0, 0);
        }
    }
}
