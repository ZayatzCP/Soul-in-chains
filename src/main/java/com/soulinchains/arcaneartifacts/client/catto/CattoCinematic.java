package com.soulinchains.arcaneartifacts.client.catto;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.catto.Catto;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The cutscene camera of a conversation with Catto, done like a shoulder camera: it sits behind the player and a
 * little to the right, at head height, and looks at Catto. It eases in when the talk starts and eases back to wherever
 * the game camera is when it ends; it never goes through walls. The player is not turned: they just stand there.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, value = Dist.CLIENT)
public final class CattoCinematic {
    private CattoCinematic() {
    }

    public record Shot(Vec3 position, float yaw, float pitch) {
    }

    /** Ticks to ease in or out. */
    private static final float BLEND_TICKS = 14.0F;
    private static final double BACK = 2.4;
    private static final double SIDE = 1.15;
    private static final double UP = 0.35;

    private static int catId = -1;
    private static boolean active;
    private static float blend;
    private static float previousBlend;
    @Nullable
    private static CameraType savedCamera;
    private static boolean savedHideGui;
    @Nullable
    private static Vec3 lastCatPos;

    /** The conversation started: remember the player's view settings and switch to a third-person shot. */
    public static void start(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active && blend <= 0.0F) {
            savedCamera = minecraft.options.getCameraType();
            savedHideGui = minecraft.options.hideGui;
        }
        catId = entityId;
        active = true;
        // Third person so the player's own model is drawn in the shot.
        minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        minecraft.options.hideGui = true;
    }

    /** The conversation ended: ease back; the view settings come back once the camera is home. */
    public static void stop() {
        active = false;
    }

    public static boolean running() {
        return active || blend > 0.0F;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        previousBlend = blend;
        if (active) {
            blend = Math.min(1.0F, blend + 1.0F / BLEND_TICKS);
        } else if (blend > 0.0F) {
            blend = Math.max(0.0F, blend - 1.0F / BLEND_TICKS);
            if (blend == 0.0F) {
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.options.setCameraType(savedCamera == null ? CameraType.FIRST_PERSON : savedCamera);
                minecraft.options.hideGui = savedHideGui;
                savedCamera = null;
                lastCatPos = null;
            }
        }
    }

    private static float ease(float t) {
        return -(Mth.cos((float) Math.PI * t) - 1.0F) / 2.0F;
    }

    /** Where the camera goes this frame, or null when no conversation shot is running. */
    @Nullable
    public static Shot shot(Entity viewer, Vec3 gamePosition, float gameYaw, float gamePitch, float partialTick) {
        if (blend <= 0.0F && previousBlend <= 0.0F) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || viewer != minecraft.player) {
            return null;
        }
        Vec3 catPos = lastCatPos;
        if (minecraft.level.getEntity(catId) instanceof Catto cat) {
            catPos = cat.getPosition(partialTick).add(0.0, cat.getBbHeight() * 0.55, 0.0);
            lastCatPos = catPos;
        }
        if (catPos == null) {
            return null;
        }
        Vec3 player = viewer.getPosition(partialTick);
        Vec3 eye = player.add(0.0, viewer.getEyeHeight(), 0.0);
        // Behind the player, looking from the player toward Catto, shifted to the right.
        Vec3 flat = new Vec3(catPos.x - player.x, 0.0, catPos.z - player.z);
        Vec3 forward = flat.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, viewer.getYRot()) : flat.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 wanted = eye.subtract(forward.scale(BACK)).add(right.scale(SIDE)).add(0.0, UP, 0.0);
        // Not through walls: stop short of whatever is between the player's eyes and the wanted spot.
        var hit = minecraft.level.clip(new ClipContext(eye, wanted, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, viewer));
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 back = hit.getLocation().subtract(eye);
            wanted = eye.add(back.scale(Math.max(0.0, (back.length() - 0.3) / Math.max(0.001, back.length()))));
        }
        Vec3 look = catPos.subtract(wanted);
        float yaw = (float) (Mth.atan2(look.z, look.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * Mth.RAD_TO_DEG);
        float t = ease(Mth.lerp(partialTick, previousBlend, blend));
        Vec3 position = gamePosition.lerp(wanted, t);
        float blendedYaw = gameYaw + Mth.wrapDegrees(yaw - gameYaw) * t;
        float blendedPitch = Mth.lerp(t, gamePitch, pitch);
        return new Shot(position, blendedYaw, blendedPitch);
    }
}
