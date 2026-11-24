package com.soulinchains.arcaneartifacts.cult.archive;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/** The hidden lock of an Archive door: unbreakable, set into the wall above it. */
public class ArchiveLockBlock extends BaseEntityBlock {
    public static final MapCodec<ArchiveLockBlock> CODEC = simpleCodec(p -> new ArchiveLockBlock());

    public ArchiveLockBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 3600000.0F).sound(SoundType.POLISHED_DEEPSLATE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArchiveLockBlockEntity(pos, state);
    }
}
