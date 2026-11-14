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
 * alone, and drinks the rider's mana: {@link #MANA_PER_SECOND} a second. Out of mana it only glides down,
 * gently. Neither broom nor rider takes fall damage while riding.
 */
public class FlyingBroom extends ClassMount {
    public static final int MANA_PER_SECOND = 3;
    /** Blocks per tick at full speed (before the Pseudomage's spurs, if anyone dares). */
    public static final double SPEED = 0.6;
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
            entityData.set(CHARGED, drink(rider));
        }
        if (level().isClientSide && isVehicle() && isCharged() && random.nextInt(3) == 0) {
            Vec3 tail = position().add(Vec3.directionFromRotation(0, getYRot()).scale(-0.9)).add(0, 0.35, 0);
            level().addParticle(ParticleTypes.WITCH, tail.x, tail.y, tail.z, 0, 0, 0);
        }
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

    @Override
    public void travel(Vec3 input) {
        if (getControllingPassenger() instanceof Player rider) {
            double speed = getAttributeValue(Attributes.MOVEMENT_SPEED);
            Vec3 look = Vec3.directionFromRotation(rider.getXRot(), rider.getYRot());
            Vec3 left = Vec3.directionFromRotation(0F, rider.getYRot() - 90F);
            Vec3 want = look.scale(input.z).add(left.scale(input.x)).scale(speed);
            if (!isCharged()) {
                want = new Vec3(want.x * 0.4, Math.min(want.y, 0) - 0.12, want.z * 0.4);   // no mana: a slow glide down
            } else if (input.lengthSqr() < 1.0E-4) {
                want = new Vec3(0, Math.sin(tickCount * 0.1) * 0.02, 0);                // hovering, bobbing a little
            }
            setDeltaMovement(getDeltaMovement().lerp(want, 0.18));
            move(MoverType.SELF, getDeltaMovement());
            calculateEntityAnimation(false);
            return;
        }
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
