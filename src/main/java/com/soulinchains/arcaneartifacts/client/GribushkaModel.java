package com.soulinchains.arcaneartifacts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.soulinchains.arcaneartifacts.client.baked.BakedModels;
import com.soulinchains.arcaneartifacts.client.baked.BakedPart;
import com.soulinchains.arcaneartifacts.entity.Gribushka;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

import java.util.Map;

/**
 * The Mycelinoid folk (Mycelinoid, Amanitoid, Elder), drawn from the designer's Blockbench models baked
 * into {@code assets/arcane_artifacts/entity_models/}: parts body, right_arm, left_arm, right_leg, left_leg.
 * Animated: walking, examining a mushroom before a trade, greeting a friend, dancing, the shield and the club.
 */
public class GribushkaModel<T extends Gribushka> extends EntityModel<T> implements ArmedModel {
    private final BakedPart body;
    private final BakedPart rightArm;
    private final BakedPart leftArm;
    private final BakedPart rightLeg;
    private final BakedPart leftLeg;
    /** How high it is off the ground this frame (dancing), in model pixels. */
    private float hop;

    public GribushkaModel(ResourceManager resources, ResourceLocation model) {
        Map<String, BakedPart> parts = BakedModels.load(resources, model);
        body = BakedModels.part(parts, "body");
        rightArm = BakedModels.part(parts, "right_arm");
        leftArm = BakedModels.part(parts, "left_arm");
        rightLeg = BakedModels.part(parts, "right_leg");
        leftLeg = BakedModels.part(parts, "left_leg");
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
        rightArm.zRot = 0.08F * rightArm.outwards();
        leftArm.zRot = 0.08F * leftArm.outwards();
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
            leftArm.zRot = 0.1F * leftArm.outwards();
        }
        if (attackTime > 0F) {
            // A heavy overhead swing with the club.
            float swing = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -2.6F + swing * 2.4F;
            rightArm.zRot = 0.08F * rightArm.outwards();
            body.xRot = swing * 0.25F;
        }
        keepArmsClear(rightArm);
        keepArmsClear(leftArm);
    }

    /**
     * The cap and the shelf fungi stick out around the shoulders: an arm swung far back is held in, and a
     * raised arm is turned outwards, so neither goes through the mushroom.
     */
    private static void keepArmsClear(BakedPart arm) {
        float outwards = arm.outwards();
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
        rightArm.zRot = (0.08F + Mth.sin(age * 0.35F) * 0.35F * lift) * rightArm.outwards();     // turning the mushroom over
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
        rightArm.zRot = (0.08F + (0.35F + Mth.sin(t * 0.8F) * 0.45F) * up) * rightArm.outwards();
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
        rightArm.zRot = (0.3F + Mth.cos(beat) * 0.3F) * rightArm.outwards();
        leftArm.zRot = (0.3F - Mth.cos(beat) * 0.3F) * leftArm.outwards();
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
        BakedPart arm = side == HumanoidArm.RIGHT ? rightArm : leftArm;
        arm.apply(pose);
        // At the shoulder, like a vanilla arm: the item layer goes down the arm from here.
        pose.translate(arm.pivot.x / 16f, arm.pivot.y / 16f, arm.pivot.z / 16f);
    }
}
