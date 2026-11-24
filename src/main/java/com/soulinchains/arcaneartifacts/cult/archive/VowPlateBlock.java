package com.soulinchains.arcaneartifacts.cult.archive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Vow Plate: a bone plate in the Archive floor. Two of them, stood on together, open the hall's door. */
public class VowPlateBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 1, 15);

    public VowPlateBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(-1.0F, 3600000.0F).noCollission().sound(SoundType.BONE_BLOCK));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player) || entity.isSpectator()) {
            return;
        }
        ArchiveLockBlockEntity lock = ArchiveLockBlockEntity.find(level, pos);
        if (lock != null && !lock.opened()) {
            lock.press(pos, level.getGameTime());
            if (level.getGameTime() % 10 == 0) {
                server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.3);
            }
        }
    }
}
