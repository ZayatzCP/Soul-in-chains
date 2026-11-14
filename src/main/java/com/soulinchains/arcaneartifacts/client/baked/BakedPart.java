package com.soulinchains.arcaneartifacts.client.baked;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** One movable part of a baked designer model: textured quads in entity model space and a pivot. */
public final class BakedPart {
    public record Quad(Vector3f[] vertices, float[][] uvs, Vector3f normal) {
    }

    public static final BakedPart EMPTY = new BakedPart(new Vector3f(), List.of());

    public final Vector3f pivot;
    public final List<Quad> quads;
    public float xRot;
    public float yRot;
    public float zRot;

    public BakedPart(Vector3f pivot, List<Quad> quads) {
        this.pivot = pivot;
        this.quads = new ArrayList<>(quads);
    }

    public void reset() {
        xRot = yRot = zRot = 0F;
    }

    /** +1 if the part is on the model's right (negative x), -1 on its left: the way "outwards" turns. */
    public float outwards() {
        return pivot.x <= 0 ? 1F : -1F;
    }

    /** Rotates the pose around the pivot (model pixels). */
    public void apply(PoseStack pose) {
        pose.translate(pivot.x / 16f, pivot.y / 16f, pivot.z / 16f);
        if (zRot != 0) {
            pose.mulPose(Axis.ZP.rotation(zRot));
        }
        if (yRot != 0) {
            pose.mulPose(Axis.YP.rotation(yRot));
        }
        if (xRot != 0) {
            pose.mulPose(Axis.XP.rotation(xRot));
        }
        pose.translate(-pivot.x / 16f, -pivot.y / 16f, -pivot.z / 16f);
    }

    public void render(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        pose.pushPose();
        apply(pose);
        emit(pose, buffer, light, overlay, color);
        pose.popPose();
    }

    public void emit(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        PoseStack.Pose last = pose.last();
        for (Quad quad : quads) {
            for (int i = 0; i < 4; i++) {
                Vector3f v = quad.vertices()[i];
                buffer.addVertex(last, v.x / 16f, v.y / 16f, v.z / 16f).setColor(color)
                        .setUv(quad.uvs()[i][0], quad.uvs()[i][1]).setOverlay(overlay).setLight(light)
                        .setNormal(last, quad.normal().x, quad.normal().y, quad.normal().z);
            }
        }
    }
}
