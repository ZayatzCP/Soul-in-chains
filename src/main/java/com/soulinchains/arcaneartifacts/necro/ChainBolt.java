package com.soulinchains.arcaneartifacts.necro;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A thrown chain (Chain Sickle, Chain form). It flies straight; the first creature it hooks is reeled in to the thrower
 * (or, when {@link #pullSelf}, the thrower is reeled to the creature or the block). It carries the rest of a spell when
 * thrown by the Chain form. Heavy creatures (bosses, mobs over {@link #HEAVY_HEALTH} health) cannot be pulled: the chain
 * just hits them. Drawn as a chain from the thrower's hand.
 */
public class ChainBolt extends Projectile {
    public static final float HEAVY_HEALTH = 150F;
    private static final int MAX_REEL_TICKS = 25;
    private static final EntityDataAccessor<Integer> HOOKED = SynchedEntityData.defineId(ChainBolt.class, EntityDataSerializers.INT);

    @Nullable
    private SpellResolver resolver;
    private boolean pullSelf;
    private double range;
    private Vec3 start = Vec3.ZERO;
    /** Reeling: towards this point (thrower pulled to a block), or the hooked entity. */
    @Nullable
    private Vec3 anchor;
    private boolean reeling;
    private int reelTicks;

    public ChainBolt(EntityType<? extends ChainBolt> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public ChainBolt(Level level, LivingEntity thrower, @Nullable SpellResolver resolver, boolean pullSelf, float speed) {
        this(ModEntities.CHAIN_BOLT.get(), level);
        setOwner(thrower);
        this.resolver = resolver;
        this.pullSelf = pullSelf;
        this.range = Config.CHAIN_RANGE.get();
        Vec3 eye = thrower.getEyePosition();
        setPos(eye.x, eye.y - 0.2, eye.z);
        start = position();
        shootFromRotation(thrower, thrower.getXRot(), thrower.getYRot(), 0.0F, speed, 0.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HOOKED, -1);
    }

    @Nullable
    public Entity hooked() {
        int id = entityData.get(HOOKED);
        return id < 0 ? null : level().getEntity(id);
    }

    public boolean isReeling() {
        return reeling;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Entity hooked = hooked();
            if (hooked != null) {
                setPos(hooked.position().add(0, hooked.getBbHeight() * 0.5, 0));
            }
            return;
        }
        Entity owner = getOwner();
        if (!(owner instanceof LivingEntity thrower) || !thrower.isAlive() || owner.level() != level()) {
            discard();
            return;
        }
        if (reeling) {
            reel(thrower);
            return;
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
            if (isRemoved() || reeling) {
                return;
            }
        }
        setPos(position().add(getDeltaMovement()));
        if (position().distanceTo(start) > range || tickCount > 60) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target instanceof LivingEntity && target != getOwner();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (resolver != null) {
            resolver.onResolveEffect(level(), hit);
        }
        Entity target = hit.getEntity();
        level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1.0F, 0.8F);
        if (!target.isAlive()) {
            discard();
            return;
        }
        boolean heavy = target.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES)
                || target instanceof LivingEntity living && living.getMaxHealth() > HEAVY_HEALTH;
        // A creature already in shackles stays where the chains hold it.
        boolean held = target instanceof LivingEntity living && living.hasEffect(com.soulinchains.arcaneartifacts.registry.ModEffects.SHACKLED);
        if ((heavy || held) && !pullSelf) {
            discard();
            return;
        }
        entityData.set(HOOKED, target.getId());
        reeling = true;
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        if (resolver != null) {
            resolver.onResolveEffect(level(), hit);
        }
        level().playSound(null, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (pullSelf) {
            anchor = hit.getLocation();
            setPos(anchor);
            reeling = true;
            setDeltaMovement(Vec3.ZERO);
        } else {
            discard();
        }
    }

    /** Pulls the hooked creature to the thrower, or the thrower to the creature or block. */
    private void reel(LivingEntity thrower) {
        Entity hooked = hooked();
        Vec3 target;
        if (anchor != null) {
            target = anchor;
        } else if (hooked != null && hooked.isAlive()) {
            target = hooked.position().add(0, hooked.getBbHeight() * 0.5, 0);
            setPos(target);
        } else {
            discard();
            return;
        }
        Entity moving = pullSelf ? thrower : hooked;
        Vec3 to = pullSelf ? target.subtract(thrower.position().add(0, thrower.getBbHeight() * 0.5, 0))
                : thrower.position().add(0, thrower.getBbHeight() * 0.5, 0).subtract(target);
        double distance = to.length();
        if (moving == null || distance < 2.2 || ++reelTicks > MAX_REEL_TICKS) {
            discard();
            return;
        }
        double speed = Math.min(1.4, 0.35 + distance * 0.18);
        Vec3 motion = to.normalize().scale(speed).add(0, moving.onGround() ? 0.15 : 0.04, 0);
        moving.setDeltaMovement(motion);
        moving.hurtMarked = true;
        moving.fallDistance = 0;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }
}
