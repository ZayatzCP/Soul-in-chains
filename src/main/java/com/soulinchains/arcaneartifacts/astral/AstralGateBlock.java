package com.soulinchains.arcaneartifacts.astral;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Astral Gate: a short obelisk the Ascension rite raises where its participants land. Right-click sends you back to
 * where you ascended from. It cannot be broken, so the way home is never lost; outside the Astral it is silent.
 */
public class AstralGateBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 3, 14),
            Block.box(5, 3, 5, 11, 13, 11),
            Block.box(4, 13, 4, 12, 15, 12));

    public AstralGateBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(-1F, 3600000F).noLootTable()
                .lightLevel(s -> 12).sound(SoundType.AMETHYST).noOcclusion().pushReaction(PushReaction.BLOCK));
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
        if (!Astral.isAstral(level)) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.astral.gate_silent"), true);
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer sp) {
            Astral.leave(sp);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.0,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.03 + random.nextDouble() * 0.03, 0);
        }
    }
}
