package com.soulinchains.arcaneartifacts.client.necro;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Draws a chain between two points as two crossed strips of the vanilla chain texture, repeated along its length. */
public final class ChainDraw {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/chain.png");
    private static final float HALF_WIDTH = 0.09375F;

    private ChainDraw() {
    }

    /** {@code from} and {@code to} are relative to the current pose origin. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffers, Vec3 from, Vec3 to, int light, int argb) {
        Vec3 dir = to.subtract(from);
        double length = dir.length();
        if (length < 1.0E-3) {
            return;
        }
        Vec3 axis = dir.scale(1.0 / length);
        Vec3 ref = Math.abs(axis.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side1 = axis.cross(ref).normalize().scale(HALF_WIDTH);
        Vec3 side2 = axis.cross(side1).normalize().scale(HALF_WIDTH);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        float v = (float) length;
        strip(consumer, pose, from, to, side1, 0F, 3F / 16F, v, light, argb);
        strip(consumer, pose, from, to, side2, 3F / 16F, 6F / 16F, v, light, argb);
    }

    private static void strip(VertexConsumer c, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 side, float u0, float u1, float v, int light, int argb) {
        vertex(c, pose, a.subtract(side), u0, 0, light, argb);
        vertex(c, pose, a.add(side), u1, 0, light, argb);
        vertex(c, pose, b.add(side), u1, v, light, argb);
        vertex(c, pose, b.subtract(side), u0, v, light, argb);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, Vec3 p, float u, float v, int light, int argb) {
        c.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(argb).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
    }
}
