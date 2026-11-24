package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.Config;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The wisp a soul leaves over the body: a clot of soul-sand sparks around an invisible core. It shies away from
 * players and fades after {@code soul_wisp_seconds}. It has to be caught: a blow of the Soul Flail, or a chain (Chain
 * Sickle, a spell with the Chain form). Reap sends the soul straight to the lantern instead. Not saved with the world.
 */
public class SoulWisp extends Entity {
    /** It shies away from players this close. */
    public static final double SHY_RANGE = 5.0;
    private static final EntityDataAccessor<Byte> SOUL = SynchedEntityData.defineId(SoulWisp.class, EntityDataSerializers.BYTE);

    public SoulWisp(EntityType<? extends SoulWisp> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SOUL, (byte) 0);
    }

    public SoulType soul() {
        return SoulType.byOrdinal(entityData.get(SOUL));
    }

    public void setSoul(SoulType type) {
        entityData.set(SOUL, (byte) type.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            particles();
            return;
        }
        if (tickCount > Config.SOUL_WISP_SECONDS.get() * 20) {
            level().playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.SOUL_ESCAPE.value(),
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.5F, 0.6F);
            discard();
            return;
        }
        // Skittish: it drifts away from whoever comes close; stronger souls are quicker.
        Player near = null;
        double best = SHY_RANGE * SHY_RANGE;
        for (Player player : level().players()) {
            double d = player.distanceToSqr(this);
            if (d < best && !player.isSpectator()) {
                best = d;
                near = player;
            }
        }
        Vec3 motion;
        if (near != null) {
            Vec3 away = position().subtract(near.getEyePosition()).multiply(1, 0.3, 1);
            motion = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 0.02, 0) : away.normalize().scale(0.03 + 0.015 * soul().strength);
        } else {
            motion = new Vec3(Math.sin(tickCount * 0.2) * 0.02, 0.005, Math.cos(tickCount * 0.2) * 0.02);
        }
        setDeltaMovement(motion);
        setPos(position().add(motion));
    }

    /**
     * Caught: by a blow of the Soul Flail, or by a chain (Chain Sickle, Chain form). The soul goes into the catcher's
     * lantern; with no room it stays where it is.
     */
    public boolean capture(LivingEntity catcher) {
        if (isRemoved() || !(level() instanceof ServerLevel level)) {
            return false;
        }
        if (!Souls.catchSoul(catcher, soul())) {
            if (catcher instanceof Player player) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        Souls.lantern(player).isPresent() ? "arcane_artifacts.soul_wisp.full" : "arcane_artifacts.soul_wisp.no_lantern"), true);
            }
            return false;
        }
        burst(level, position(), soul());
        level.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.SOUL_ESCAPE.value(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.4F);
        discard();
        return true;
    }

    /** Players can swing at it: the Soul Flail catches it, anything else passes through it. */
    @Override
    public boolean isPickable() {
        return true;
    }

    /** Arrows and spell projectiles fly through; only chains catch it (see ChainBolt). */
    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof Player player && !level().isClientSide) {
            if (player.getMainHandItem().getItem() instanceof SoulFlailItem) {
                capture(player);
            } else {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("arcane_artifacts.soul_wisp.slips"), true);
            }
        }
        return true;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    /** A clot of soul-sand sparks: coloured dust around the core, soul flames, now and then a rising soul. */
    private void particles() {
        int c = soul().color;
        Vector3f color = new Vector3f(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F);
        for (int i = 0; i < 3; i++) {
            level().addParticle(new DustParticleOptions(color, 0.9F + random.nextFloat() * 0.6F), getX() + (random.nextDouble() - 0.5) * 0.35,
                    getY() + 0.2 + (random.nextDouble() - 0.5) * 0.35, getZ() + (random.nextDouble() - 0.5) * 0.35, 0, 0.005, 0);
        }
        if (tickCount % 3 == 0) {
            level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX() + (random.nextDouble() - 0.5) * 0.2, getY() + 0.2,
                    getZ() + (random.nextDouble() - 0.5) * 0.2, 0, 0.01, 0);
        }
        if (tickCount % 10 == 0) {
            level().addParticle(ParticleTypes.SOUL, getX(), getY() + 0.3, getZ(), 0, 0.03, 0);
        }
    }

    /** The puff when a soul is caught. */
    public static void burst(ServerLevel level, Vec3 at, SoulType type) {
        int c = type.color;
        Vector3f color = new Vector3f(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F);
        level.sendParticles(new DustParticleOptions(color, 1.5F), at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.02);
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.02);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setSoul(SoulType.byOrdinal(tag.getByte("Soul")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putByte("Soul", (byte) soul().ordinal());
    }
}
