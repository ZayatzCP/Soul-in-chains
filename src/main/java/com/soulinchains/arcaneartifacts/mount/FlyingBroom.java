package com.soulinchains.arcaneartifacts.mount;

import com.hollingsworth.arsnouveau.common.capability.ManaCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Spellcaster's Flying Broom. Flies where the rider looks (forward/back, strafing), hovers when left
 * alone. Low over the ground (up to {@link #FREE_HEIGHT} blocks) it flies for free; higher up it drinks the
 * rider's mana: {@link #MANA_PER_SECOND} a second. Out of mana it only glides down,
 * gently. Neither broom nor rider takes fall damage while riding.
 */
public class FlyingBroom extends ClassMount {
    public static final int MANA_PER_SECOND = 10;
    /** Up to this many blocks above the ground (or water) the broom costs nothing. */
    public static final int FREE_HEIGHT = 8;
    /** Blocks per tick at full speed (before the Pseudomage's spurs, if anyone dares). */
    public static final double SPEED = 0.6;
    /** Momentum (0..1) the rider has built up holding forward; diving builds it faster, climbing drains it. */
    private float momentum;
    private static final EntityDataAccessor<Boolean> CHARGED = SynchedEntityData.defineId(FlyingBroom.class, EntityDataSerializers.BOOLEAN);

    public FlyingBroom(EntityType<? extends FlyingBroom> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.FLYING_SPEED, SPEED);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGED, true);
    }

    public boolean isCharged() {
        return entityData.get(CHARGED);
    }

    @Override
    protected Item item() {
        return ModItems.FLYING_BROOM.get();
    }

    @Override
    public void tick() {
        super.tick();
        resetFallDistance();
        for (Entity passenger : getPassengers()) {
            passenger.resetFallDistance();
        }
        if (!level().isClientSide && tickCount % 20 == 0 && getControllingPassenger() instanceof Player rider) {
            entityData.set(CHARGED, nearGround() || drink(rider));
        }
        if (level().isClientSide && isVehicle() && isCharged() && random.nextInt(3) == 0) {
            Vec3 tail = position().add(Vec3.directionFromRotation(0, getYRot()).scale(-0.9)).add(0, 0.35, 0);
            level().addParticle(ParticleTypes.WITCH, tail.x, tail.y, tail.z, 0, 0, 0);
        }
    }

    /** Something solid (or water) within {@link #FREE_HEIGHT} blocks below the broom. */
    public boolean nearGround() {
        Vec3 from = position();
        var hit = level().clip(new net.minecraft.world.level.ClipContext(from, from.subtract(0, FREE_HEIGHT, 0),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.ANY, this));
        return hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS;
    }

    /** Takes this second's mana from the rider; false if there is not enough (creative riders fly free). */
    public static boolean drink(Player rider) {
        if (rider.getAbilities().instabuild) {
            return true;
        }
        ManaCap mana = CapabilityRegistry.getMana(rider);
        if (mana == null || mana.getCurrentMana() < MANA_PER_SECOND) {
            return false;
        }
        mana.removeMana(MANA_PER_SECOND);
        if (rider instanceof ServerPlayer player) {
            mana.syncToClient(player);
        }
        return true;
    }

    public float getMomentum() {
        return momentum;
    }

    @Override
    public void travel(Vec3 input) {
        if (getControllingPassenger() instanceof Player rider) {
            double max = getAttributeValue(Attributes.MOVEMENT_SPEED);
            float pitch = rider.getXRot();
            if (input.z > 0.01) {
                momentum += 0.008F + Math.max(0F, pitch) / 90F * 0.02F;     // pushing on; diving speeds up more
                if (pitch < -20F) {
                    momentum -= 0.006F;                                     // climbing costs speed
                }
            } else {
                momentum -= 0.006F;
            }
            momentum = Math.max(0F, Math.min(1F, momentum));
            double speed = max * (0.4 + 1.0 * momentum);
            Vec3 look = Vec3.directionFromRotation(pitch, rider.getYRot());
            Vec3 left = Vec3.directionFromRotation(0F, rider.getYRot() - 90F);
            Vec3 v = getDeltaMovement();
            if (!isCharged()) {
                v = new Vec3(v.x * 0.97, Math.max(v.y - 0.02, -0.15), v.z * 0.97);    // no mana: a slow glide down
                momentum = 0F;
            } else if (input.lengthSqr() < 1.0E-4) {
                v = v.scale(0.965).add(0, Math.sin(tickCount * 0.1) * 0.002, 0);      // let go: it glides on and slows
            } else {
                Vec3 want = look.scale(input.z * speed).add(left.scale(input.x * max * 0.6));
                v = v.lerp(want, 0.07);
            }
            setDeltaMovement(v);
            move(MoverType.SELF, getDeltaMovement());
            if (horizontalCollision) {
                momentum *= 0.5F;
            }
            calculateEntityAnimation(false);
            return;
        }
        momentum = 0F;
        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(0.8, 0.8, 0.8).add(0, -0.01, 0));   // settles slowly when let go
        }
        super.travel(input);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
