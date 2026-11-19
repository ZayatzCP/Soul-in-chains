package com.soulinchains.arcaneartifacts.necro.mob;

import com.soulinchains.arcaneartifacts.necro.Servant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Mourner: she keeps her distance and wails. Each wail calls the dead out of the ground (up to {@link #MAX_CALLED}
 * at a time) and tears at a Shackled's servants nearby.
 */
public class Mourner extends Monster {
    public static final int WAIL_TICKS = 160;
    public static final int MAX_CALLED = 4;
    public static final float SERVANT_DAMAGE = 8F;
    private static final String CALLED_TAG = "arcane_artifacts.called_by_mourner";
    private int wailCooldown = 60;

    public Mourner(EntityType<? extends Mourner> type, Level level) {
        super(type, level);
        xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 6.0F, 1.0, 1.2));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel level && --wailCooldown <= 0) {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive() && distanceTo(target) < 16) {
                wail(level);
                wailCooldown = WAIL_TICKS;
            } else {
                wailCooldown = 20;
            }
        }
    }

    /** The wail: the dead rise, servants nearby are torn at. Returns how many were called. */
    public int wail(ServerLevel level) {
        level.playSound(null, blockPosition(), SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 1.5F, 0.5F);
        level.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1.6, getZ(), 20, 0.6, 0.4, 0.6, 0.05);
        for (Servant servant : level.getEntitiesOfClass(Servant.class, getBoundingBox().inflate(10))) {
            servant.hurt(level.damageSources().mobAttack(this), SERVANT_DAMAGE);
        }
        AABB around = getBoundingBox().inflate(16);
        long alive = level.getEntitiesOfClass(Mob.class, around, m -> m.getTags().contains(CALLED_TAG) && m.isAlive()).size();
        int called = 0;
        for (int i = 0; i < 2 && alive + called < MAX_CALLED; i++) {
            Mob dead = (random.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON).create(level);
            if (dead == null) {
                continue;
            }
            BlockPos at = blockPosition().offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            dead.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360, 0);
            if (!level.noCollision(dead)) {
                continue;
            }
            dead.addTag(CALLED_TAG);
            dead.setTarget(getTarget());
            if (dead instanceof net.minecraft.world.entity.monster.AbstractSkeleton skeleton) {
                skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));
            }
            level.addFreshEntity(dead);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, 10, 0.3, 0.1, 0.3, 0.01);
            called++;
        }
        return called;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.WITCH_AMBIENT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.WITCH_DEATH;
    }
}
