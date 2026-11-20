package com.soulinchains.arcaneartifacts.necro.throne;

import com.soulinchains.arcaneartifacts.necro.SoulType;
import com.soulinchains.arcaneartifacts.necro.SoulWisp;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/** A soul lantern with a great soul inside. Taken down (or put out) it lets the soul go as a wisp. */
public class GreatSoulLanternBlock extends LanternBlock {
    public static final com.mojang.serialization.MapCodec<GreatSoulLanternBlock> CODEC = simpleCodec(p -> new GreatSoulLanternBlock());

    @Override
    public com.mojang.serialization.MapCodec<LanternBlock> codec() {
        return (com.mojang.serialization.MapCodec<LanternBlock>) (com.mojang.serialization.MapCodec<?>) CODEC;
    }
    public GreatSoulLanternBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.5F).sound(SoundType.LANTERN)
                .lightLevel(state -> 15).noOcclusion());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !ThroneOfChains.consuming && level instanceof ServerLevel server) {
            SoulWisp wisp = ModEntities.SOUL_WISP.get().create(server);
            if (wisp != null) {
                wisp.setSoul(SoulType.GREAT);
                wisp.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                server.addFreshEntity(wisp);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
