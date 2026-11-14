package com.soulinchains.arcaneartifacts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.mount.FlyingBroom;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * A rider on a Flying Broom leans into the flight like a player on elytra: upright while hovering, lying
 * along the gaze at full speed. Rider and broom turn about the same point, the rider's hips, so the
 * broom stays between the legs whatever the angle.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, value = Dist.CLIENT)
public final class BroomRiding {
    /** Height of the hips above a standing player's feet, in blocks. */
    public static final float HIPS = 0.75F;
    /** How far below the hips the handle runs. */
    public static final float HANDLE_DROP = 0.1F;

    private BroomRiding() {
    }

    /** 0 hovering .. 1 flat-out flight. */
    public static float lean(FlyingBroom broom, float partialTick) {
        return Mth.lerp(partialTick, broom.flightLeanO, broom.flightLean);
    }

    /** The broom's pitch in degrees (positive: nose down), following the rider's gaze as it speeds up. */
    public static float broomPitch(FlyingBroom broom, Player rider, float partialTick) {
        return lean(broom, partialTick) * rider.getViewXRot(partialTick);
    }

    /** How far the rider leans forward from upright, in degrees (90: lying flat). */
    public static float riderLean(FlyingBroom broom, Player rider, float partialTick) {
        return lean(broom, partialTick) * (90F + rider.getViewXRot(partialTick));
    }

    @SubscribeEvent
    public static void pre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!(player.getVehicle() instanceof FlyingBroom broom)) {
            return;
        }
        float partialTick = event.getPartialTick();
        float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0, HIPS, 0);
        pose.mulPose(Axis.YP.rotationDegrees(180F - yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-riderLean(broom, player, partialTick)));
        pose.mulPose(Axis.YP.rotationDegrees(yaw - 180F));
        pose.translate(0, -HIPS, 0);
    }

    @SubscribeEvent
    public static void post(RenderPlayerEvent.Post event) {
        if (event.getEntity().getVehicle() instanceof FlyingBroom) {
            event.getPoseStack().popPose();
        }
    }
}
