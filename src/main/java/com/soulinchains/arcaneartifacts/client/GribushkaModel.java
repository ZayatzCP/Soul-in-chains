package com.soulinchains.arcaneartifacts.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.entity.Gribushka;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * The Gribushka, drawn straight from the designer's Blockbench model
 * ({@code assets/arcane_artifacts/entity_models/gribushka.json}, groups "body", "right arm", "left arm",
 * "left leg", "right leg"; the face looks west). Every element keeps its own per-face UVs, which vanilla
 * entity cubes cannot do, so the quads are built here and the groups are animated like limbs.
 */
public class GribushkaModel extends EntityModel<Gribushka> implements ArmedModel {
    public static final ResourceLocation MODEL = ArcaneArtifacts.id("entity_models/gribushka.json");
    /** Variant tints over the one texture: natural, blue, violet. */
    private static final int[] TINTS = {0xFFFFFFFF, 0xFFB4CCFF, 0xFFD6B4FF};

    private final Part body = new Part();
    private final Part rightArm = new Part();
    private final Part leftArm = new Part();
    private final Part rightLeg = new Part();
    private final Part leftLeg = new Part();
    private int tint = -1;

    public GribushkaModel(ResourceManager resources) {
        try (Reader reader = resources.getResourceOrThrow(MODEL).openAsReader()) {
            load(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (Exception e) {
            ArcaneArtifacts.LOGGER.error("Could not load the Gribushka model {}", MODEL, e);
        }
    }

    // ------------------------------------------------------------------ loading

    private void load(JsonObject json) {
        float texScale = 16f;   // UVs are given on a 16 grid whatever the texture size
        JsonArray elements = json.getAsJsonArray("elements");
        for (JsonElement group : json.getAsJsonArray("groups")) {
            collect(group.getAsJsonObject(), elements, body, texScale);
        }
        legPivot(leftLeg);
        legPivot(rightLeg);
        armPivot(rightArm);
        armPivot(leftArm);
        body.pivot.set(0, 18, 0);
    }

    private void collect(JsonObject group, JsonArray elements, Part inherited, float texScale) {
        String name = group.get("name").getAsString();
        Part part = switch (name) {
            case "right arm" -> rightArm;
            case "left arm" -> leftArm;
            case "right leg" -> rightLeg;
            case "left leg" -> leftLeg;
            default -> inherited;
        };
        for (JsonElement child : group.getAsJsonArray("children")) {
            if (child.isJsonPrimitive()) {
                addElement(part, elements.get(child.getAsInt()).getAsJsonObject(), texScale);
            } else {
                collect(child.getAsJsonObject(), elements, part, texScale);
            }
        }
    }

    /** Block-model space (pixels, y up, face to the west) to model space (y down from 24, face to -z). */
    private static Vector3f toModel(float x, float y, float z) {
        return new Vector3f(z - 8f, 24f - y, x - 8f);
    }

    private static void addElement(Part part, JsonObject element, float texScale) {
        float[] from = floats(element.getAsJsonArray("from"));
        float[] to = floats(element.getAsJsonArray("to"));
        Matrix3f rotation = new Matrix3f();
        Vector3f origin = new Vector3f(8, 8, 8);
        if (element.has("rotation")) {
            JsonObject r = element.getAsJsonObject("rotation");
            float[] o = floats(r.getAsJsonArray("origin"));
            origin.set(o[0], o[1], o[2]);
            if (r.has("axis")) {
                float angle = r.get("angle").getAsFloat() * Mth.DEG_TO_RAD;
                switch (r.get("axis").getAsString()) {
                    case "x" -> rotation.rotationX(angle);
                    case "y" -> rotation.rotationY(angle);
                    default -> rotation.rotationZ(angle);
                }
            } else {
                rotation.rotationZYX(get(r, "z") * Mth.DEG_TO_RAD, get(r, "y") * Mth.DEG_TO_RAD, get(r, "x") * Mth.DEG_TO_RAD);
            }
        }
        float x0 = from[0], y0 = from[1], z0 = from[2], x1 = to[0], y1 = to[1], z1 = to[2];
        JsonObject faces = element.getAsJsonObject("faces");
        for (String face : faces.keySet()) {
            float[][] corners = switch (face) {
                case "down" -> new float[][]{{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}};
                case "up" -> new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
                case "north" -> new float[][]{{x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}};
                case "south" -> new float[][]{{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}};
                case "west" -> new float[][]{{x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}};
                default -> new float[][]{{x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}};
            };
            float[] uv = floats(faces.getAsJsonObject(face).getAsJsonArray("uv"));
            float u0 = uv[0] / texScale, v0 = uv[1] / texScale, u1 = uv[2] / texScale, v1 = uv[3] / texScale;
            float[][] uvs = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
            Vector3f[] vertices = new Vector3f[4];
            for (int i = 0; i < 4; i++) {
                Vector3f v = new Vector3f(corners[i][0], corners[i][1], corners[i][2]).sub(origin);
                rotation.transform(v).add(origin);
                vertices[i] = toModel(v.x, v.y, v.z);
            }
            Vector3f normal = new Vector3f(vertices[1]).sub(vertices[0]).cross(new Vector3f(vertices[2]).sub(vertices[0])).normalize();
            part.quads.add(new Quad(vertices, uvs, normal));
            for (Vector3f v : vertices) {
                part.include(v);
            }
        }
    }

    private static float get(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsFloat() : 0f;
    }

    private static float[] floats(JsonArray array) {
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }

    /** Legs swing from their top. */
    private static void legPivot(Part leg) {
        leg.pivot.set((leg.min.x + leg.max.x) / 2, leg.min.y, (leg.min.z + leg.max.z) / 2);
    }

    /** Arms swing from the shoulder: the top of the arm. */
    private static void armPivot(Part arm) {
        arm.pivot.set((arm.min.x + arm.max.x) / 2, arm.min.y + 1, (arm.min.z + arm.max.z) / 2);
    }

    // ------------------------------------------------------------------ animation and rendering

    @Override
    public void setupAnim(Gribushka entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        tint = TINTS[Math.floorMod(entity.getVariant(), TINTS.length)];
        rightLeg.xRot = Mth.cos(limbSwing * 0.8F) * 1.0F * limbSwingAmount;
        leftLeg.xRot = Mth.cos(limbSwing * 0.8F + (float) Math.PI) * 1.0F * limbSwingAmount;
        rightArm.xRot = Mth.cos(limbSwing * 0.8F + (float) Math.PI) * 0.7F * limbSwingAmount;
        leftArm.xRot = Mth.cos(limbSwing * 0.8F) * 0.7F * limbSwingAmount;
        body.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.25F;
        body.zRot = Mth.cos(ageInTicks * 0.09F) * 0.03F + Mth.cos(limbSwing * 0.8F) * 0.06F * limbSwingAmount;
        if (entity.isAdmiring()) {
            // Holds the mushroom up in front of its face and turns it over.
            rightArm.xRot = -1.3F + Mth.cos(ageInTicks * 0.3F) * 0.1F;
            leftArm.xRot = -1.3F + Mth.sin(ageInTicks * 0.3F) * 0.1F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        int c = FastColor.ARGB32.multiply(color, tint);
        leftLeg.render(pose, buffer, light, overlay, c);
        rightLeg.render(pose, buffer, light, overlay, c);
        pose.pushPose();
        body.apply(pose);
        body.emit(pose, buffer, light, overlay, c);
        rightArm.render(pose, buffer, light, overlay, c);
        leftArm.render(pose, buffer, light, overlay, c);
        pose.popPose();
    }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack pose) {
        body.apply(pose);
        Part arm = side == HumanoidArm.RIGHT ? rightArm : leftArm;
        arm.apply(pose);
        // At the shoulder, like a vanilla arm: the item layer goes down the arm from here.
        pose.translate(arm.pivot.x / 16f, arm.pivot.y / 16f, arm.pivot.z / 16f);
    }

    private record Quad(Vector3f[] vertices, float[][] uvs, Vector3f normal) {
    }

    private static final class Part {
        final List<Quad> quads = new ArrayList<>();
        final Vector3f pivot = new Vector3f();
        final Vector3f min = new Vector3f(Float.MAX_VALUE);
        final Vector3f max = new Vector3f(-Float.MAX_VALUE);
        float xRot;
        float yRot;
        float zRot;

        void include(Vector3f v) {
            min.min(v);
            max.max(v);
        }

        /** Rotates the pose around the pivot (model pixels). */
        void apply(PoseStack pose) {
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

        void render(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
            pose.pushPose();
            apply(pose);
            emit(pose, buffer, light, overlay, color);
            pose.popPose();
        }

        void emit(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
            PoseStack.Pose last = pose.last();
            for (Quad quad : quads) {
                for (int i = 0; i < 4; i++) {
                    Vector3f v = quad.vertices[i];
                    buffer.addVertex(last, v.x / 16f, v.y / 16f, v.z / 16f).setColor(color)
                            .setUv(quad.uvs[i][0], quad.uvs[i][1]).setOverlay(overlay).setLight(light)
                            .setNormal(last, quad.normal.x, quad.normal.y, quad.normal.z);
                }
            }
        }
    }
}
