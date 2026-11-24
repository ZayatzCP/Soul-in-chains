package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Faceless Sentry (heavy, the Archive): stands in a passage and shuts it - every second it shoves back players within
 * 3 blocks. A Warden who provokes it (strikes it, or draws it with the Call of the Seal Hammer) keeps it busy for 6 s:
 * while it is busy it shoves no one, and the Confessor slips past.
 */
public class FacelessSentry extends Monster {
    public static final double SHOVE_RANGE = 3;
    public static final int BUSY_TICKS = 120;
    @Nullable
    private BlockPos post;
    private long busyUntil;

    public FacelessSentry(EntityType<? extends FacelessSentry> type, Level level) {
        super(type, level);
        xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 70)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(4, new ReturnToPostGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void guardPost(BlockPos pos) {
        post = pos.immutable();
    }

    public boolean busy() {
        return level().getGameTime() < busyUntil;
    }

    /** A Warden draws it: it shoves no one for 6 s. */
    public void provoke() {
        busyUntil = level().getGameTime() + BUSY_TICKS;
    }

    /** Shoves the player back out of the passage; false while it is busy (or the player cannot be moved). */
    public boolean shove(Player player) {
        if (busy() || player.isCreative() || player.isSpectator() || player.distanceTo(this) > SHOVE_RANGE) {
            return false;
        }
        Vec3 away = player.position().subtract(position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1.0E-4 ? getLookAngle().multiply(1, 0, 1) : away.normalize();
        player.push(away.x * 1.2, 0.35, away.z * 1.2);
        player.hurtMarked = true;
        level().playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 0.8F, 0.6F);
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && Cult.role(player) == CultRole.WARDEN) {
            provoke();
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (post == null) {
            post = blockPosition();
        }
        if (getTarget() instanceof Player target && Cult.role(target) == CultRole.WARDEN && target.distanceTo(this) < 10) {
            provoke();
        }
        if (tickCount % 20 == 0 && !busy()) {
            for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(SHOVE_RANGE))) {
                shove(player);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (post != null) {
            tag.put("Post", NbtUtils.writeBlockPos(post));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        post = NbtUtils.readBlockPos(tag, "Post").orElse(null);
    }

    /** With nothing to fight, it walks back into its passage. */
    static final class ReturnToPostGoal extends Goal {
        private final FacelessSentry sentry;

        ReturnToPostGoal(FacelessSentry sentry) {
            this.sentry = sentry;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return sentry.getTarget() == null && sentry.post != null && sentry.distanceToSqr(Vec3.atBottomCenterOf(sentry.post)) > 4;
        }

        @Override
        public void start() {
            Vec3 at = Vec3.atBottomCenterOf(sentry.post);
            sentry.getNavigation().moveTo(at.x, at.y, at.z, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && !sentry.getNavigation().isDone();
        }
    }
}
