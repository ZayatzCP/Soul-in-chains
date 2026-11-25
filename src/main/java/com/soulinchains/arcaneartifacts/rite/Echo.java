package com.soulinchains.arcaneartifacts.rite;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Echo: what a rite stirs up beyond its boundary, and what crawls out of corrupted soil at night. It goes for the
 * participants who are busy with the rite first, puts out candles on its way and smudges chalk under its feet.
 * Fades after a minute and a half.
 */
public class Echo extends Monster {
    public static final int LIFE = 1800;
    private int life = LIFE;

    public Echo(EntityType<? extends Echo> type, Level level) {
        super(type, level);
        xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(3, new SnuffCandleGoal(this));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, RiteHeartBlockEntity::busy));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && --life <= 0) {
            discard();
        }
    }

    /** Puts out a lit candle next to it; returns whether it did. */
    public boolean snuffNear() {
        for (BlockPos pos : BlockPos.betweenClosed(blockPosition().offset(-1, -1, -1), blockPosition().offset(1, 1, 1))) {
            BlockState state = level().getBlockState(pos);
            if (state.getBlock() instanceof CandleBlock && state.getValue(CandleBlock.LIT)) {
                level().setBlock(pos, state.setValue(CandleBlock.LIT, false), 3);
                level().playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.HOSTILE, 1.0F, 0.8F);
                return true;
            }
        }
        return false;
    }

    /** With no one to fight, walks to the nearest lit candle within 12 blocks and puts it out. */
    static final class SnuffCandleGoal extends Goal {
        private final Echo echo;
        @Nullable
        private BlockPos candle;

        SnuffCandleGoal(Echo echo) {
            this.echo = echo;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (echo.getTarget() != null || echo.tickCount % 20 != 0) {
                return false;
            }
            candle = null;
            double best = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(echo.blockPosition().offset(-12, -2, -12), echo.blockPosition().offset(12, 3, 12))) {
                BlockState state = echo.level().getBlockState(pos);
                if (state.getBlock() instanceof CandleBlock && state.getValue(CandleBlock.LIT)) {
                    double d = echo.distanceToSqr(Vec3.atCenterOf(pos));
                    if (d < best) {
                        best = d;
                        candle = pos.immutable();
                    }
                }
            }
            return candle != null;
        }

        @Override
        public boolean canContinueToUse() {
            return candle != null && echo.getTarget() == null && echo.level().getBlockState(candle).getBlock() instanceof CandleBlock
                    && echo.level().getBlockState(candle).getValue(CandleBlock.LIT);
        }

        @Override
        public void tick() {
            if (candle == null) {
                return;
            }
            if (echo.distanceToSqr(Vec3.atCenterOf(candle)) > 3) {
                if (echo.tickCount % 10 == 0) {
                    echo.getNavigation().moveTo(candle.getX() + 0.5, candle.getY(), candle.getZ() + 0.5, 1.0);
                }
            } else if (echo.snuffNear()) {
                candle = null;
            }
        }
    }
}
