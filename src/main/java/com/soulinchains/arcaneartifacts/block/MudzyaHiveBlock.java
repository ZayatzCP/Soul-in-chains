package com.soulinchains.arcaneartifacts.block;

import com.mojang.serialization.MapCodec;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Mudzya Hive: full Mudzyas nearby fly to it and unload their Source. The hive passes the Source to Source
 * Jars within 6 blocks; a glass bottle takes 100 Source out as a Bottle of Source.
 */
public class MudzyaHiveBlock extends BaseEntityBlock {
    public static final MapCodec<MudzyaHiveBlock> CODEC = simpleCodec(MudzyaHiveBlock::new);

    public MudzyaHiveBlock(Properties properties) {
        super(properties);
    }

    /** Both the hive a player crafts and the wild one Mudzyas build on beaches. */
    public static boolean isHive(BlockState state) {
        return state.getBlock() instanceof MudzyaHiveBlock;
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MudzyaHiveBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.MUDZYA_HIVE_ENTITY.get(), MudzyaHiveBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.GLASS_BOTTLE) || !(level.getBlockEntity(pos) instanceof MudzyaHiveBlockEntity hive)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            if (hive.takeSource(MudzyaHiveBlockEntity.BOTTLE_COST)) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.BOTTLE_OF_SOURCE.get())));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.2f);
            } else {
                player.displayClientMessage(Component.translatable("arcane_artifacts.mudzya_hive.empty", hive.getSource(),
                        MudzyaHiveBlockEntity.BOTTLE_COST).withStyle(ChatFormatting.GRAY), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MudzyaHiveBlockEntity hive) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.mudzya_hive.status", hive.getSource(),
                    MudzyaHiveBlockEntity.CAPACITY).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
