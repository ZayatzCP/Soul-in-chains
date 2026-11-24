package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Ash Witness: what the Rite of the Ash Witness brings up for 90 s (elite: 60 s). It keeps near the Confessor and marks
 * every hostile creature around the pair (they glow); it fights only a little.
 */
public class AshWitness extends RitualServant {
    /** Kept for the callers of 1.32: the grades live in {@link RitualServant.Grade}. */
    public static final int LIFE_TICKS = 1800;
    public static final int ELITE_LIFE_TICKS = 1200;
    public static final double MARK_RANGE = 12;

    public AshWitness(EntityType<? extends AshWitness> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected int defaultLife() {
        return LIFE_TICKS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> e instanceof Enemy && !RitualSummons.summoned(e)));
    }

    @Override
    public void setup(Grade newGrade, Player confessor, Player warden) {
        setup(newGrade, confessor, warden, newGrade == Grade.ELITE ? ELITE_LIFE_TICKS : LIFE_TICKS);
    }

    @Override
    protected void ability(ServerLevel level) {
        if (tickCount % 40 == 0) {
            markAround();
        }
    }

    /** Every hostile creature near the witness glows for a few seconds. Returns how many. */
    public int markAround() {
        int marked = 0;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(MARK_RANGE),
                e -> e instanceof Enemy && e.isAlive() && !RitualSummons.summoned(e))) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
            marked++;
        }
        return marked;
    }
}
