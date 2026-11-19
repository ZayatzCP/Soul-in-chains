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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The wisp a soul leaves over the body for a few seconds. It drifts up slowly; a player wearing a Soul Lantern with
 * room in it draws it in from {@link #PULL_RANGE} blocks. Nothing else can catch it. Not saved with the world.
 */
public class SoulWisp extends Entity {
    public static final double PULL_RANGE = 4.0;
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
        Player catcher = null;
        double best = PULL_RANGE * PULL_RANGE;
        for (Player player : level().players()) {
            double d = player.distanceToSqr(this);
            if (d < best && !player.isSpectator() && Souls.canStore(player)) {
                best = d;
                catcher = player;
            }
        }
        Vec3 motion;
        if (catcher != null) {
            Vec3 to = catcher.position().add(0, catcher.getBbHeight() * 0.5, 0).subtract(position());
            if (to.lengthSqr() < 1.2 * 1.2) {
                if (Souls.store(catcher, soul())) {
                    burst((ServerLevel) level(), position(), soul());
                    discard();
                    return;
                }
            }
            motion = to.normalize().scale(0.35);
        } else {
            motion = new Vec3(Math.sin(tickCount * 0.2) * 0.02, 0.01, Math.cos(tickCount * 0.2) * 0.02);
        }
        setDeltaMovement(motion);
        setPos(position().add(motion));
    }

    private void particles() {
        int c = soul().color;
        Vector3f color = new Vector3f(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F);
        level().addParticle(new DustParticleOptions(color, 1.2F), getX() + (random.nextDouble() - 0.5) * 0.25,
                getY() + (random.nextDouble() - 0.5) * 0.25, getZ() + (random.nextDouble() - 0.5) * 0.25, 0, 0.01, 0);
        if (tickCount % 4 == 0) {
            level().addParticle(ParticleTypes.SOUL, getX(), getY(), getZ(), 0, 0.02, 0);
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
    public boolean isPickable() {
        return false;
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
