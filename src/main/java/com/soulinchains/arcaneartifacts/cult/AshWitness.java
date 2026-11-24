package com.soulinchains.arcaneartifacts.cult;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Ash Witness: what the Rite of the Ash Witness brings up for a while. It keeps near the Confessor and marks every hostile
 * creature around the pair (they glow); it fights only a little. Fades after {@link #LIFE_TICKS} (elite: shorter).
 */
public class AshWitness extends PathfinderMob {
    public enum Grade {
        FAINT(0.5, 0.5), COMMON(1.0, 1.0), EMPOWERED(1.5, 1.5), ELITE(2.5, 2.0);

        public final double health;
        public final double damage;

        Grade(double health, double damage) {
            this.health = health;
            this.damage = damage;
        }
    }

    public static final int LIFE_TICKS = 1800;
    public static final int ELITE_LIFE_TICKS = 1200;
    public static final double MARK_RANGE = 12;
    private int lifeLeft = LIFE_TICKS;
    @Nullable
    private UUID confessorId;
    @Nullable
    private UUID wardenId;
    private Grade grade = Grade.COMMON;

    public AshWitness(EntityType<? extends AshWitness> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24);
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

    /** Set by the rite that raised it. */
    public void setup(Grade newGrade, @Nullable Player confessor, @Nullable Player warden) {
        grade = newGrade;
        confessorId = confessor == null ? null : confessor.getUUID();
        wardenId = warden == null ? null : warden.getUUID();
        lifeLeft = grade == Grade.ELITE ? ELITE_LIFE_TICKS : LIFE_TICKS;
        applyGrade();
        setHealth(getMaxHealth());
    }

    public Grade grade() {
        return grade;
    }

    public int lifeLeft() {
        return lifeLeft;
    }

    private void applyGrade() {
        var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.addOrReplacePermanentModifier(new AttributeModifier(ArcaneArtifacts.id("ash_grade_health"), grade.health - 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        var attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.addOrReplacePermanentModifier(new AttributeModifier(ArcaneArtifacts.id("ash_grade_damage"), grade.damage - 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.ASH, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
            }
            return;
        }
        if (--lifeLeft <= 0) {
            ((ServerLevel) level()).sendParticles(ParticleTypes.ASH, getX(), getY() + 1, getZ(), 40, 0.4, 0.8, 0.4, 0.02);
            discard();
            return;
        }
        if (tickCount % 40 == 0) {
            markAround();
        }
        Player confessor = confessorId == null ? null : level().getPlayerByUUID(confessorId);
        if (confessor != null && getTarget() == null && distanceTo(confessor) > 5) {
            getNavigation().moveTo(confessor, 1.2);
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

    @Override
    public boolean isAlliedTo(Entity other) {
        UUID id = other.getUUID();
        if (id.equals(confessorId) || id.equals(wardenId) || other instanceof AshWitness) {
            return true;
        }
        return super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isAlliedTo(target) && !(target instanceof Player) && super.canAttack(target);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeLeft", lifeLeft);
        tag.putString("Grade", grade.name());
        if (confessorId != null) {
            tag.putUUID("Confessor", confessorId);
        }
        if (wardenId != null) {
            tag.putUUID("Warden", wardenId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeLeft = tag.contains("LifeLeft") ? tag.getInt("LifeLeft") : LIFE_TICKS;
        try {
            grade = Grade.valueOf(tag.getString("Grade"));
        } catch (IllegalArgumentException e) {
            grade = Grade.COMMON;
        }
        confessorId = tag.hasUUID("Confessor") ? tag.getUUID("Confessor") : null;
        wardenId = tag.hasUUID("Warden") ? tag.getUUID("Warden") : null;
    }
}
