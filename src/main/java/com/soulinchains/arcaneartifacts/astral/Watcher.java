package com.soulinchains.arcaneartifacts.astral;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * A Watcher: a tall black silhouette standing on top of a monolith, far from a traveller of the Astral and facing them.
 * It cannot be touched, hurt or reached. Look straight at it for a moment - it is gone. It also goes when the traveller
 * comes close, leaves, or after a while. It is never saved.
 */
public class Watcher extends Mob {
    /** How long it stands, at most, in ticks. */
    public static final int LIFE = 20 * 45;
    /** How straight one must look at it (the cosine of the angle), and for how many ticks. */
    public static final double GAZE = 0.995;
    public static final int GAZE_TICKS = 4;
    /** Closer than this, it is never there. */
    public static final double NEAR = 24;

    private WeakReference<Player> watched = new WeakReference<>(null);
    private int gazed;
    private int age;

    public Watcher(EntityType<? extends Watcher> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(true);
        setInvulnerable(true);
        setSilent(true);
        noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0);
    }

    public void watch(Player player) {
        watched = new WeakReference<>(player);
        face(player);
    }

    @Nullable
    public Player watched() {
        return watched.get();
    }

    private void face(Player player) {
        double dx = player.getX() - getX();
        double dz = player.getZ() - getZ();
        float yaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90F;
        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
        yRotO = yaw;
        yBodyRotO = yaw;
        yHeadRotO = yaw;
    }

    /** Whether the player looks straight at this Watcher (whatever stands between). */
    public boolean seenBy(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 to = new Vec3(getX(), getY() + getBbHeight() * 0.6, getZ()).subtract(eye).normalize();
        return player.getViewVector(1F).dot(to) >= GAZE;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel)) {
            return;
        }
        Player player = watched.get();
        age++;
        if (player == null || player.isRemoved() || player.level() != level() || age > LIFE
                || player.distanceToSqr(getX(), player.getY(), getZ()) < NEAR * NEAR) {
            discard();
            return;
        }
        face(player);
        gazed = seenBy(player) ? gazed + 1 : 0;
        if (gazed >= GAZE_TICKS) {
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    /** Stands it on top of a block (the top of a monolith). */
    public void standOn(BlockPos top) {
        moveTo(top.getX() + 0.5, top.getY() + 1, top.getZ() + 0.5, getYRot(), 0F);
    }
}
