package com.soulinchains.arcaneartifacts.cult;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A creature a rite brought up for a while: bound to the pair (never hurts them or any player), graded by the rite's
 * quality, fades when its time is up. Ash Witness, Red Acolyte, Bone Gatekeeper, Psalmist, Votive Beast.
 */
public abstract class RitualServant extends PathfinderMob {
    public enum Grade {
        FAINT(0.5, 0.5), COMMON(1.0, 1.0), EMPOWERED(1.5, 1.5), ELITE(2.5, 2.0);

        public final double health;
        public final double damage;

        Grade(double health, double damage) {
            this.health = health;
            this.damage = damage;
        }
    }

    private int lifeLeft;
    @Nullable
    private UUID confessorId;
    @Nullable
    private UUID wardenId;
    private Grade grade = Grade.COMMON;

    protected RitualServant(EntityType<? extends RitualServant> type, Level level) {
        super(type, level);
        xpReward = 0;
        lifeLeft = defaultLife();
    }

    /** How long it lasts, in ticks, unless the rite says otherwise. */
    protected abstract int defaultLife();

    /** What it does on its own, every tick on the server. */
    protected void ability(ServerLevel level) {
    }

    protected ParticleOptions trail() {
        return ParticleTypes.ASH;
    }

    /** Whether it keeps near the Confessor when it has nothing to fight. */
    protected boolean followsConfessor() {
        return true;
    }

    /** Set by the rite that raised it; {@code life} 0 keeps the default. */
    public void setup(Grade newGrade, @Nullable Player confessor, @Nullable Player warden, int life) {
        grade = newGrade;
        confessorId = confessor == null ? null : confessor.getUUID();
        wardenId = warden == null ? null : warden.getUUID();
        lifeLeft = life > 0 ? life : defaultLife();
        applyGrade();
        setHealth(getMaxHealth());
    }

    public void setup(Grade newGrade, @Nullable Player confessor, @Nullable Player warden) {
        setup(newGrade, confessor, warden, 0);
    }

    public Grade grade() {
        return grade;
    }

    public int lifeLeft() {
        return lifeLeft;
    }

    /** Whether this was raised by (or for) the player with this id. */
    public boolean servesPair(UUID player) {
        return player.equals(confessorId) || player.equals(wardenId);
    }

    @Nullable
    public Player confessor() {
        return confessorId == null ? null : level().getPlayerByUUID(confessorId);
    }

    private void applyGrade() {
        var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.addOrReplacePermanentModifier(new AttributeModifier(ArcaneArtifacts.id("rite_grade_health"), grade.health - 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        var attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.addOrReplacePermanentModifier(new AttributeModifier(ArcaneArtifacts.id("rite_grade_damage"), grade.damage - 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(2) == 0) {
                level().addParticle(trail(), getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
            }
            return;
        }
        if (--lifeLeft <= 0) {
            ((ServerLevel) level()).sendParticles(trail(), getX(), getY() + 1, getZ(), 40, 0.4, 0.8, 0.4, 0.02);
            discard();
            return;
        }
        ability((ServerLevel) level());
        Player confessor = followsConfessor() ? confessor() : null;
        if (confessor != null && getTarget() == null && distanceTo(confessor) > 5) {
            getNavigation().moveTo(confessor, 1.2);
        }
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        UUID id = other.getUUID();
        if (id.equals(confessorId) || id.equals(wardenId) || other instanceof RitualServant) {
            return true;
        }
        return super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isAlliedTo(target) && !(target instanceof Player) && !RitualSummons.summoned(target) && super.canAttack(target);
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
        lifeLeft = tag.contains("LifeLeft") ? tag.getInt("LifeLeft") : defaultLife();
        try {
            grade = Grade.valueOf(tag.getString("Grade"));
        } catch (IllegalArgumentException e) {
            grade = Grade.COMMON;
        }
        confessorId = tag.hasUUID("Confessor") ? tag.getUUID("Confessor") : null;
        wardenId = tag.hasUUID("Warden") ? tag.getUUID("Warden") : null;
    }
}
