package com.soulinchains.arcaneartifacts.entity;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import com.soulinchains.arcaneartifacts.spell.MissileTargeting;
import com.soulinchains.arcaneartifacts.util.Allies;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Homing spell projectile used by the Arcane Missile form and by Splitting.
 * <p>
 * It reuses all of Ars Nouveau's projectile behaviour (resolver, pierce, prisms, portals, hit events)
 * and only replaces movement: every tick the velocity is steered toward the current target. The target
 * is chosen on the server and its entity id is synced, so clients steer identically between the
 * server's velocity updates.
 * <p>
 * Limits: it never re-targets an entity it gave up on, it gives up after {@code max_chase_ticks}, it
 * expires after its lifetime, and block collisions are the regular Ars projectile collisions.
 */
public class ArcaneMissileEntity extends EntityProjectileSpell {
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(ArcaneMissileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SPLIT_CHILD = SynchedEntityData.defineId(ArcaneMissileEntity.class, EntityDataSerializers.BOOLEAN);

    private static final Vector3f MAIN_COLOR = new Vector3f(0.71f, 0.28f, 0.96f);
    private static final Vector3f CHILD_COLOR = new Vector3f(0.18f, 0.84f, 0.71f);

    private int pursuitLevel;
    private int lifetime = 200;
    private int chaseTicks;
    private @Nullable UUID currentTargetId;
    /** Entities this missile must never target or hit (e.g. the creature its parent split on). */
    private final Set<UUID> excluded = new HashSet<>();
    /** Entities this missile already chased for too long. */
    private final Set<UUID> abandoned = new HashSet<>();

    public ArcaneMissileEntity(EntityType<? extends ArcaneMissileEntity> type, Level level) {
        super(type, level);
    }

    public ArcaneMissileEntity(Level level, SpellResolver resolver) {
        super(ModEntities.ARCANE_MISSILE.get(), level, resolver);
    }

    /** @param pursuit number of Pursuit augments on the form. */
    public ArcaneMissileEntity configure(int pursuit, boolean splitChild) {
        this.pursuitLevel = Math.max(0, pursuit);
        this.entityData.set(SPLIT_CHILD, splitChild);
        int base = splitChild ? Config.SPLIT_CHILD_LIFETIME_TICKS.get() : Config.MISSILE_LIFETIME_TICKS.get();
        this.lifetime = base + pursuitLevel * Config.PURSUIT_LIFETIME_BONUS_TICKS.get();
        return this;
    }

    public void exclude(UUID id) {
        excluded.add(id);
    }

    public boolean isSplitChild() {
        return this.entityData.get(SPLIT_CHILD);
    }

    public int getPursuitLevel() {
        return pursuitLevel;
    }

    public double searchRadius() {
        return Config.MISSILE_SEARCH_RADIUS.get() + pursuitLevel * Config.PURSUIT_RADIUS_BONUS.get();
    }

    private double turnRate() {
        return Mth.clamp(Config.MISSILE_TURN_RATE.get() + pursuitLevel * Config.PURSUIT_TURN_BONUS.get(), 0.01, 0.9);
    }

    /** Forces a new target (used by the Mantle of Reflection to send a missile back to its caster). */
    public void forceTarget(@Nullable LivingEntity target) {
        this.currentTargetId = target == null ? null : target.getUUID();
        this.chaseTicks = 0;
        this.entityData.set(TARGET_ID, target == null ? -1 : target.getId());
    }

    @Override
    public EntityType<?> getType() {
        return ModEntities.ARCANE_MISSILE.get();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_ID, -1);
        builder.define(SPLIT_CHILD, false);
    }

    @Override
    public int getExpirationTime() {
        return lifetime;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && !isRemoved()) {
            Vector3f color = isSplitChild() ? CHILD_COLOR : MAIN_COLOR;
            Vec3 pos = position().add(0, getBbHeight() / 2.0, 0);
            level().addParticle(new DustParticleOptions(color, isSplitChild() ? 0.6f : 0.9f), pos.x, pos.y, pos.z, 0, 0, 0);
            if (tickCount % 2 == 0) {
                level().addParticle(net.minecraft.core.particles.ParticleTypes.WITCH, pos.x, pos.y, pos.z, 0, 0, 0);
            }
        }
    }

    @Override
    public void tickNextPosition() {
        if (!level().isClientSide) {
            updateTarget();
        }
        Entity target = getTargetEntity();
        if (target != null && target.isAlive()) {
            steerToward(target);
        }
        super.tickNextPosition();
    }

    private @Nullable Entity getTargetEntity() {
        int id = this.entityData.get(TARGET_ID);
        return id < 0 ? null : level().getEntity(id);
    }

    private void updateTarget() {
        Entity owner = getOwner();
        Entity target = getTargetEntity();
        double radius = searchRadius();
        if (target != null) {
            boolean lost = !target.isAlive() || target.isRemoved()
                    || target.distanceToSqr(this) > radius * radius * 2.25
                    || !(target instanceof LivingEntity living) || !Allies.isValidHostileTarget(owner, living);
            if (!lost && ++chaseTicks > Config.MISSILE_MAX_CHASE_TICKS.get() + pursuitLevel * 20) {
                abandoned.add(target.getUUID());
                lost = true;
            }
            if (lost) {
                forceTarget(null);
            }
            return;
        }
        if (tickCount % 4 != 0) {
            return;
        }
        Set<UUID> skip = new HashSet<>(excluded);
        skip.addAll(abandoned);
        LivingEntity found = MissileTargeting.findTarget(level(), this, owner, radius, skip);
        if (found != null) {
            forceTarget(found);
        }
    }

    private void steerToward(Entity target) {
        Vec3 velocity = getDeltaMovement();
        double speed = Math.max(velocity.length(), 0.25);
        Vec3 aim = target.getBoundingBox().getCenter().subtract(position().add(0, getBbHeight() / 2.0, 0));
        if (aim.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 desired = aim.normalize().scale(speed);
        Vec3 steered = velocity.lerp(desired, turnRate());
        if (steered.lengthSqr() < 1.0E-6) {
            steered = desired;
        }
        steered = steered.normalize().scale(speed);
        setDeltaMovement(steered);
        double horizontal = steered.horizontalDistance();
        setYRot((float) (Mth.atan2(steered.x, steered.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(steered.y, horizontal) * Mth.RAD_TO_DEG));
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!super.canHitEntity(entity) || excluded.contains(entity.getUUID())) {
            return false;
        }
        Entity owner = getOwner();
        return !(entity instanceof LivingEntity && owner != null && Allies.areAllied(owner, entity));
    }
}
