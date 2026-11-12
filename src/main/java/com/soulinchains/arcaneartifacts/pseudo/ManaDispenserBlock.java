package com.soulinchains.arcaneartifacts.pseudo;

import com.mojang.serialization.MapCodec;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Mana Dispenser: takes Source from the Mana Tanks of its pipe network and fills the Source Jars within 3
 * blocks. Right-click switches it to DRAIN: it then empties those jars into the tanks.
 */
public class ManaDispenserBlock extends BaseEntityBlock {
    public static final MapCodec<ManaDispenserBlock> CODEC = simpleCodec(ManaDispenserBlock::new);
    public static final BooleanProperty DRAIN = BooleanProperty.create("drain");

    public ManaDispenserBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DRAIN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DRAIN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            boolean drain = !state.getValue(DRAIN);
            level.setBlock(pos, state.setValue(DRAIN, drain), 3);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, drain ? 0.7f : 1.0f);
            player.displayClientMessage(Component.translatable(drain ? "arcane_artifacts.dispenser.drain" : "arcane_artifacts.dispenser.fill")
                    .withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaDispenserBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.MANA_DISPENSER_ENTITY.get(), ManaDispenserBlockEntity::serverTick);
    }
}
