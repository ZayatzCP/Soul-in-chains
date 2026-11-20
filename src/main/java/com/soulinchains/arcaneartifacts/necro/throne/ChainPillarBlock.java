package com.soulinchains.arcaneartifacts.necro.throne;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/** Chain Pillar: right click its top with a Soul Lantern to hang a Great Soul Lantern (costs a great soul). */
public class ChainPillarBlock extends Block {
    public ChainPillarBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(3.0F, 9.0F).sound(SoundType.CHAIN));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stack.is(Items.SOUL_LANTERN) || hit.getDirection() != Direction.UP) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level instanceof ServerLevel server)) {
            return ItemInteractionResult.SUCCESS;
        }
        ThroneOfChains.HangResult result = ThroneOfChains.hang(server, pos, player);
        switch (result) {
            case NOT_A_THRONE -> {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            case HUNG -> {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                int[] found = ThroneOfChains.find(level, pos);
                int lit = found == null ? 0 : ThroneOfChains.lit(level, new BlockPos(found[0], found[1], found[2]));
                ThroneOfChains.tell(player, "hung", lit);
            }
            default -> ThroneOfChains.tell(player, result.name().toLowerCase(java.util.Locale.ROOT));
        }
        return ItemInteractionResult.CONSUME;
    }
}
