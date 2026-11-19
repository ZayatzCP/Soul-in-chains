package com.soulinchains.arcaneartifacts.necro.mob;

import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Restless: a ghost. It floats through walls, and its touch drains mana as well as health. It fades in daylight under
 * the open sky. Restless rise from dug-up graves, haunt crypts, and burst out of a Shackled who dies with a full lantern
 * (those last only {@link #SHORT_LIFE} ticks).
 */
public class Restless extends Monster {
    public static final int SHORT_LIFE = 2400;
    public static final float MANA_DRAIN = 15F;
    private int lifeLeft = -1;

    public Restless(EntityType<? extends Restless> type, Level level) {
        super(type, level);
        moveControl = new GhostMoveControl(this);
        setNoGravity(true);
        xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    /** A Restless that fades away after a while (from graves and from a fallen Shackled). */
    public void shortLived() {
        lifeLeft = SHORT_LIFE;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new HauntGoal(this));
        goalSelector.addGoal(6, new DriftGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        noPhysics = true;
        super.tick();
        noPhysics = false;
        setNoGravity(true);
        if (level().isClientSide) {
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.SOUL, getRandomX(0.4), getRandomY(), getRandomZ(0.4), 0, 0.02, 0);
            }
            return;
        }
        if (lifeLeft > 0 && --lifeLeft == 0) {
            fade();
            return;
        }
        if (tickCount % 20 == 0 && level().isDay() && level().canSeeSky(blockPosition()) && !level().isRaining()) {
            fade();
        }
    }

    private void fade() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1, getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        }
        discard();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof Player player) {
            var mana = CapabilityRegistry.getMana(player);
            if (mana != null) {
                mana.removeMana(MANA_DRAIN);
                if (player instanceof ServerPlayer serverPlayer) {
                    mana.syncToClient(serverPlayer);
                }
            }
        }
        return hit;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SOUL_ESCAPE.value();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeLeft", lifeLeft);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("LifeLeft")) {
            lifeLeft = tag.getInt("LifeLeft");
        }
    }

    /** Flies straight at where it wants to be, through anything. */
    static final class GhostMoveControl extends MoveControl {
        GhostMoveControl(Mob mob) {
            super(mob);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                return;
            }
            Vec3 to = new Vec3(wantedX - mob.getX(), wantedY - mob.getY(), wantedZ - mob.getZ());
            double distance = to.length();
            if (distance < mob.getBoundingBox().getSize()) {
                operation = Operation.WAIT;
                mob.setDeltaMovement(mob.getDeltaMovement().scale(0.5));
                return;
            }
            double speed = mob.getAttributeValue(Attributes.FLYING_SPEED) * speedModifier;
            mob.setDeltaMovement(mob.getDeltaMovement().add(to.scale(speed * 0.05 / distance)));
            Vec3 motion = mob.getDeltaMovement();
            mob.setYRot(-((float) Mth.atan2(motion.x, motion.z)) * Mth.RAD_TO_DEG);
            mob.yBodyRot = mob.getYRot();
        }
    }

    /** Swoops at its target and touches it. */
    static final class HauntGoal extends Goal {
        private final Restless ghost;
        private int cooldown;

        HauntGoal(Restless ghost) {
            this.ghost = ghost;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = ghost.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = ghost.getTarget();
            if (target == null) {
                return;
            }
            ghost.getMoveControl().setWantedPosition(target.getX(), target.getEyeY() - 0.4, target.getZ(), 1.2);
            ghost.getLookControl().setLookAt(target);
            if (cooldown > 0) {
                cooldown--;
            } else if (ghost.distanceToSqr(target) < 2.6) {
                ghost.doHurtTarget(target);
                cooldown = 20;
            }
        }
    }

    /** Idle: drifts about near where it is. */
    static final class DriftGoal extends Goal {
        private final Restless ghost;

        DriftGoal(Restless ghost) {
            this.ghost = ghost;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return ghost.getTarget() == null && !ghost.getMoveControl().hasWanted() && ghost.random.nextInt(20) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            BlockPos at = ghost.blockPosition().offset(ghost.random.nextInt(9) - 4, ghost.random.nextInt(5) - 2, ghost.random.nextInt(9) - 4);
            ghost.getMoveControl().setWantedPosition(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 0.6);
        }
    }
}
