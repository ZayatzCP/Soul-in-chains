package com.soulinchains.arcaneartifacts.cult.archive;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Vow Candle: lit (use) while another holds the Holding Seal, it opens the hall's door. */
public class VowCandleBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 10, 11);

    public VowCandleBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(-1.0F, 3600000.0F).noOcclusion().sound(SoundType.CANDLE).lightLevel(s -> 4));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ArchiveLockBlockEntity lock = ArchiveLockBlockEntity.find(level, pos);
        return lock != null && lock.lightCandle(player) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }
}
