package com.soulinchains.arcaneartifacts.cult.archive;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/** Holding Seal: take hold of it (use) and stay by it for 10 s while your partner lights the Vow Candle. */
public class HoldingSealBlock extends Block {
    public HoldingSealBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(-1.0F, 3600000.0F).sound(SoundType.POLISHED_DEEPSLATE).lightLevel(s -> 6));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ArchiveLockBlockEntity lock = ArchiveLockBlockEntity.find(level, pos);
        return lock != null && lock.beginHold(player) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }
}
