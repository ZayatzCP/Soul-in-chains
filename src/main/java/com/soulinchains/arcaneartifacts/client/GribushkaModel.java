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
 * "left leg", "right leg"; the face looks west). Animated: walking, examining a mushroom before a trade,
 * greeting a friend and dancing. Every element keeps its own per-face UVs, which vanilla
 * entity cubes cannot do, so the quads are built here and the groups are animated like limbs.
 */
public class GribushkaModel<T extends Gribushka> extends EntityModel<T> implements ArmedModel {
    public static final ResourceLocation MODEL = ArcaneArtifacts.id("entity_models/gribushka.json");

    private final Part body = new Part();
    private final Part rightArm = new Part();
    private final Part leftArm = new Part();
    private final Part rightLeg = new Part();
    private final Part leftLeg = new Part();
    /** How high it is off the ground this frame (dancing), in model pixels. */
    private float hop;

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
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float partialTick = ageInTicks - entity.tickCount;
        float walk = limbSwing * 0.8F;
        rightLeg.xRot = Mth.cos(walk) * 1.0F * limbSwingAmount;
        leftLeg.xRot = Mth.cos(walk + Mth.PI) * 1.0F * limbSwingAmount;
        rightArm.xRot = Mth.cos(walk + Mth.PI) * 0.7F * limbSwingAmount;
        leftArm.xRot = Mth.cos(walk) * 0.7F * limbSwingAmount;
        rightArm.zRot = 0.08F;
        leftArm.zRot = -0.08F;
        rightArm.yRot = leftArm.yRot = 0;
        body.xRot = 0;
        body.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.25F;
        body.zRot = Mth.cos(ageInTicks * 0.09F) * 0.03F + Mth.cos(walk) * 0.06F * limbSwingAmount;
        hop = 0;
        if (entity.isAdmiring()) {
            examine(entity.admireAge(partialTick), ageInTicks);
        } else if (entity.getMood() == Gribushka.Mood.GREET) {
            greet(entity.moodAge(partialTick));
        } else if (entity.getMood() == Gribushka.Mood.DANCE) {
            dance(entity.moodAge(partialTick));
        }
        if (entity.isBlocking()) {
            // The shield (off hand) raised in front of the body.
            leftArm.xRot = -1.25F;
            leftArm.yRot = 0.25F;
            leftArm.zRot = -0.1F;
        }
        if (attackTime > 0F) {
            // A heavy overhead swing with the club.
            float swing = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -2.6F + swing * 2.4F;
            rightArm.zRot = 0.08F;
            body.xRot = swing * 0.25F;
        }
        keepArmsClear(rightArm, 1F);
        keepArmsClear(leftArm, -1F);
    }

    /**
     * The cap and the shelf fungi stick out around the shoulders: an arm swung far back is held in, and a
     * raised arm is turned outwards, so neither goes through the mushroom.
     */
    private static void keepArmsClear(Part arm, float outwards) {
        if (arm.xRot > 0.3F) {
            arm.xRot = 0.3F + (arm.xRot - 0.3F) * 0.3F;
        }
        float raised = Mth.clamp(-arm.xRot / 1.5F, 0F, 1F);
        arm.zRot += outwards * 0.55F * raised;
    }

    /**
     * Before a trade: lifts the mushroom to its face, turns it over and tilts its cap left and right,
     * rubs its chin with the other hand, then nods.
     */
    private void examine(float t, float age) {
        float lift = Mth.clamp(t / 10F, 0F, 1F);
        rightArm.xRot = Mth.lerp(lift, 0F, -1.45F) + Mth.sin(age * 0.25F) * 0.08F * lift;
        rightArm.zRot = 0.08F + Mth.sin(age * 0.35F) * 0.35F * lift;     // turning the mushroom over
        rightArm.yRot = 0F;
        leftArm.xRot = Mth.lerp(lift, 0F, -0.9F) + Mth.sin(age * 0.7F) * 0.12F * lift;
        leftArm.yRot = 0.2F * lift;
        body.zRot = Mth.sin(age * 0.12F) * 0.12F * lift;                // a curious tilt of the cap
        body.xRot = 0.12F * lift;
        if (t > 52F) {
            float nod = Mth.clamp((t - 52F) / 6F, 0F, 1F);
            body.xRot = 0.12F + Mth.abs(Mth.sin((t - 52F) * 0.6F)) * 0.25F * nod;
            body.zRot *= 1F - nod;
        }
    }

    /** Meeting a friend: a little bow, then the right arm goes up and waves. */
    private void greet(float t) {
        float up = Mth.clamp(t / 6F, 0F, 1F) * Mth.clamp((Gribushka.GREET_TICKS - t) / 6F, 0F, 1F);
        rightArm.xRot = -2.7F * up;
        rightArm.zRot = 0.08F + (0.35F + Mth.sin(t * 0.8F) * 0.45F) * up;
        body.xRot = t < 10F ? Mth.sin(t / 10F * Mth.PI) * 0.3F : 0F;
        body.zRot = Mth.sin(t * 0.4F) * 0.05F * up;
    }

    /** Dancing together: hopping, swaying, arms up in turn, a twirl now and then. */
    private void dance(float t) {
        float beat = t * 0.45F;
        hop = Mth.abs(Mth.sin(beat)) * 2.0F;
        body.yRot += Mth.sin(t * 0.08F) * 0.9F;
        body.zRot = Mth.sin(beat) * 0.18F;
        rightArm.xRot = -2.4F + Mth.sin(beat) * 0.7F;
        leftArm.xRot = -2.4F - Mth.sin(beat) * 0.7F;
        rightArm.zRot = 0.3F + Mth.cos(beat) * 0.3F;
        leftArm.zRot = -0.3F + Mth.cos(beat) * 0.3F;
        rightLeg.xRot = Mth.sin(beat) * 0.5F;
        leftLeg.xRot = -Mth.sin(beat) * 0.5F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        int c = color;
        pose.pushPose();
        pose.translate(0, -hop / 16f, 0);
        leftLeg.render(pose, buffer, light, overlay, c);
        rightLeg.render(pose, buffer, light, overlay, c);
        pose.pushPose();
        body.apply(pose);
        body.emit(pose, buffer, light, overlay, c);
        rightArm.render(pose, buffer, light, overlay, c);
        leftArm.render(pose, buffer, light, overlay, c);
        pose.popPose();
        pose.popPose();
    }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack pose) {
        pose.translate(0, -hop / 16f, 0);
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
