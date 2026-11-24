package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

/**
 * Bone Gatekeeper (Red Veil): a slow, heavy guard, 60-120 s by the rite's quality. Every 2 s it draws onto itself the
 * creatures that went for the Confessor within 6 blocks.
 */
public class BoneGatekeeper extends RitualServant {
    public static final double GUARD_RANGE = 6;

    public BoneGatekeeper(EntityType<? extends BoneGatekeeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected int defaultLife() {
        return 1200;
    }

    @Override
    protected ParticleOptions trail() {
        return ParticleTypes.WHITE_ASH;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void ability(ServerLevel level) {
        if (tickCount % 40 == 0) {
            guard();
        }
    }

    /** Draws the Confessor's attackers onto itself. Returns how many. */
    public int guard() {
        return guard(confessor());
    }

    /** Draws the attackers of {@code confessor} within 6 blocks onto itself. */
    public int guard(@javax.annotation.Nullable Player confessor) {
        if (confessor == null) {
            return 0;
        }
        int drawn = 0;
        for (Mob mob : level().getEntitiesOfClass(Mob.class, confessor.getBoundingBox().inflate(GUARD_RANGE),
                m -> m.getTarget() == confessor && !m.getType().is(Tags.EntityTypes.BOSSES))) {
            mob.setTarget(this);
            if (getTarget() == null) {
                setTarget(mob);
            }
            drawn++;
        }
        return drawn;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target);
    }
}
