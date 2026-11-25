package com.soulinchains.arcaneartifacts.rite;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Ritual Censer. Like a lantern it stands on the floor or hangs - placed against a ceiling or under a chain it hangs. Over
 * the offering pedestal (1-4 blocks up) it lifts a rite's quality by 8%. Smokes.
 */
public class RitualCenserBlock extends LanternBlock {
    public static final MapCodec<RitualCenserBlock> CODEC = simpleCodec(p -> new RitualCenserBlock());

    public RitualCenserBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.LANTERN).noOcclusion()
                .lightLevel(s -> 8).pushReaction(PushReaction.DESTROY));
    }

    @Override
    public MapCodec<LanternBlock> codec() {
        return (MapCodec<LanternBlock>) (MapCodec<?>) CODEC;
    }

    /** The outline follows the censer's model, standing or hanging. */
    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                                                  net.minecraft.world.phys.shapes.CollisionContext context) {
        return com.soulinchains.arcaneartifacts.util.ModelShapes.of(state, super.getShape(state, level, pos, context));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double y = pos.getY() + (state.getValue(HANGING) ? 0.35 : 0.7);
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2, y, pos.getZ() + 0.5
                + (random.nextDouble() - 0.5) * 0.2, 0, 0.03, 0);
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, y + 0.1, pos.getZ() + 0.5, 0, 0.05, 0);
        }
    }
}
