package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.level.Level;

/** Votive Beast: the rare reward of the Second Coming - a bone-white boar, 80 health, 10 damage, 60 s. */
public class VotiveBeast extends RitualServant implements HoglinBase {
    private int attackAnimation;

    public VotiveBeast(EntityType<? extends VotiveBeast> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80)
                .add(Attributes.ATTACK_DAMAGE, 10)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected int defaultLife() {
        return 1200;
    }

    @Override
    protected ParticleOptions trail() {
        return ParticleTypes.CRIMSON_SPORE;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> e instanceof Enemy && !RitualSummons.summoned(e)));
    }

    @Override
    public void aiStep() {
        if (attackAnimation > 0) {
            attackAnimation--;
        }
        super.aiStep();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        attackAnimation = 10;
        level().broadcastEntityEvent(this, (byte) 4);
        return super.doHurtTarget(target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            attackAnimation = 10;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public int getAttackAnimationRemainingTicks() {
        return attackAnimation;
    }
}
