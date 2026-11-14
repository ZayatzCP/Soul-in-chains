package com.soulinchains.arcaneartifacts.mount;

import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Druid's Moss Elk: a quick, sure-footed mount with a charged jump (hold space, like a horse) that
 * clears four blocks, takes no fall damage under six, and on living ground (grass, moss, leaves, dirt)
 * slowly heals itself and its rider.
 */
public class MossElk extends ClassMount implements PlayerRideableJumping {
    public static final int HEAL_INTERVAL = 100;
    private float pendingJump;

    public MossElk(EntityType<? extends MossElk> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.STEP_HEIGHT, 1.1)
                .add(Attributes.SAFE_FALL_DISTANCE, 6.0);
    }

    @Override
    protected Item item() {
        return ModItems.MOSS_ELK_CALL.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % HEAL_INTERVAL == 0 && onLivingGround()) {
            heal(1F);
            for (Entity passenger : getPassengers()) {
                if (passenger instanceof LivingEntity rider) {
                    rider.heal(1F);
                }
            }
        }
    }

    /** Grass, moss, leaves, dirt, mycelium: ground a druid's elk draws strength from. */
    public boolean onLivingGround() {
        BlockPos below = getBlockPosBelowThatAffectsMyMovement();
        BlockState state = level().getBlockState(below);
        return state.is(BlockTags.DIRT) || state.is(BlockTags.LEAVES) || state.is(Blocks.MOSS_CARPET)
                || level().getBlockState(blockPosition()).is(BlockTags.REPLACEABLE_BY_TREES);
    }

    // ------------------------------------------------------------------ the charged jump

    @Override
    public void onPlayerJump(int power) {
        if (power <= 0) {
            return;
        }
        pendingJump = power >= 90 ? 1.0F : 0.4F + 0.4F * power / 90F;
    }

    @Override
    public boolean canJump() {
        return isVehicle();
    }

    @Override
    public void handleStartJump(int power) {
    }

    @Override
    public void handleStopJump() {
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        if (pendingJump > 0F && onGround()) {
            Vec3 forward = Vec3.directionFromRotation(0F, getYRot()).scale(0.4 * pendingJump * Math.max(0F, player.zza));
            Vec3 v = getDeltaMovement();
            setDeltaMovement(v.x + forward.x, 0.5 + 0.45 * pendingJump, v.z + forward.z);
            hasImpulse = true;
            playSound(SoundEvents.GOAT_LONG_JUMP, 0.6F, 1.0F);
        }
        if (onGround()) {
            pendingJump = 0F;
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.HORSE_STEP, 0.15F, 1.1F);
    }
}
