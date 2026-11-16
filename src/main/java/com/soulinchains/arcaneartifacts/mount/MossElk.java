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
    /** Jumps it may still make in the air before touching the ground again. */
    public static final int AIR_JUMPS = 3;
    private float pendingJump;
    private int airJumps;

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
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;   // it comes down like a leaf
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
        return com.soulinchains.arcaneartifacts.druid.RootMagic.onLivingGround(this);
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
        if (onGround()) {
            airJumps = 0;
        }
        if (pendingJump > 0F && (onGround() || airJumps < AIR_JUMPS)) {
            boolean inAir = !onGround();
            if (inAir) {
                airJumps++;
            }
            Vec3 forward = Vec3.directionFromRotation(0F, getYRot()).scale((inAir ? 0.6 : 0.4) * pendingJump * Math.max(0F, player.zza));
            Vec3 v = getDeltaMovement();
            setDeltaMovement(v.x + forward.x, (inAir ? 0.45 : 0.5) + 0.45 * pendingJump, v.z + forward.z);
            hasImpulse = true;
            playSound(SoundEvents.GOAT_LONG_JUMP, 0.6F, inAir ? 1.3F : 1.0F);
            if (inAir) {
                for (int i = 0; i < 12; i++) {
                    level().addParticle(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR,
                            getRandomX(0.8), getY() + 0.1, getRandomZ(0.8), 0, -0.05, 0);
                }
            }
        }
        pendingJump = 0F;
        Vec3 v = getDeltaMovement();
        if (!onGround() && v.y < -0.3) {
            setDeltaMovement(v.x, -0.3, v.z);   // between jumps it drifts down instead of falling
        }
    }

    public int airJumpsUsed() {
        return airJumps;
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
