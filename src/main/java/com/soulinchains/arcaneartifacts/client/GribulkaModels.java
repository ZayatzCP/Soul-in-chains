package com.soulinchains.arcaneartifacts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.soulinchains.arcaneartifacts.client.baked.BakedModels;
import com.soulinchains.arcaneartifacts.client.baked.BakedPart;
import com.soulinchains.arcaneartifacts.entity.Gribulka;
import com.soulinchains.arcaneartifacts.entity.GribulkaHive;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;

import java.util.Map;

/** The Gribulka and its walking hive, from the designer's baked models. */
public final class GribulkaModels {
    private GribulkaModels() {
    }

    /** A little mushroom on stubby legs: it waddles, waves its arms and bounces its cap. */
    public static final class Little extends EntityModel<Gribulka> {
        private final BakedPart body;
        private final BakedPart rightArm;
        private final BakedPart leftArm;
        private final BakedPart rightLeg;
        private final BakedPart leftLeg;
        private float bob;

        public Little(ResourceManager resources, ResourceLocation model) {
            Map<String, BakedPart> parts = BakedModels.load(resources, model);
            body = BakedModels.part(parts, "body");
            rightArm = BakedModels.part(parts, "right_arm");
            leftArm = BakedModels.part(parts, "left_arm");
            rightLeg = BakedModels.part(parts, "right_leg");
            leftLeg = BakedModels.part(parts, "left_leg");
        }

        @Override
        public void setupAnim(Gribulka entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            float walk = limbSwing * 1.3F;
            rightLeg.xRot = Mth.cos(walk) * 1.2F * limbSwingAmount;
            leftLeg.xRot = Mth.cos(walk + Mth.PI) * 1.2F * limbSwingAmount;
            rightArm.xRot = Mth.cos(walk + Mth.PI) * 0.9F * limbSwingAmount;
            leftArm.xRot = Mth.cos(walk) * 0.9F * limbSwingAmount;
            rightArm.zRot = (0.15F + Mth.sin(ageInTicks * 0.2F) * 0.1F) * rightArm.outwards();
            leftArm.zRot = (0.15F + Mth.sin(ageInTicks * 0.2F + 1.5F) * 0.1F) * leftArm.outwards();
            body.zRot = Mth.cos(walk) * 0.15F * limbSwingAmount;
            body.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
            bob = Mth.abs(Mth.sin(walk)) * 1.2F * limbSwingAmount;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
            pose.pushPose();
            pose.translate(0, -bob / 16f, 0);
            leftLeg.render(pose, buffer, light, overlay, color);
            rightLeg.render(pose, buffer, light, overlay, color);
            pose.pushPose();
            body.apply(pose);
            body.emit(pose, buffer, light, overlay, color);
            rightArm.render(pose, buffer, light, overlay, color);
            leftArm.render(pose, buffer, light, overlay, color);
            pose.popPose();
            pose.popPose();
        }
    }

    /** A walking hive on four legs: the legs step in pairs and the whole trunk sways. */
    public static final class Hive extends EntityModel<GribulkaHive> {
        private final BakedPart body;
        private final BakedPart[] legs;

        public Hive(ResourceManager resources, ResourceLocation model) {
            Map<String, BakedPart> parts = BakedModels.load(resources, model);
            body = BakedModels.part(parts, "body");
            legs = new BakedPart[]{BakedModels.part(parts, "leg_front_left"), BakedModels.part(parts, "leg_back_right"),
                    BakedModels.part(parts, "leg_front_right"), BakedModels.part(parts, "leg_back_left")};
        }

        @Override
        public void setupAnim(GribulkaHive entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            float walk = limbSwing * 0.6F;
            for (int i = 0; i < legs.length; i++) {
                legs[i].xRot = Mth.cos(walk + (i < 2 ? 0 : Mth.PI)) * 0.7F * limbSwingAmount;
            }
            body.zRot = Mth.cos(walk) * 0.05F * limbSwingAmount + Mth.sin(ageInTicks * 0.05F) * 0.015F;
            body.xRot = Mth.sin(ageInTicks * 0.04F) * 0.02F;
            float release = entity.releaseAnimation(ageInTicks - entity.tickCount);
            body.xRot -= release * 0.12F;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
            for (BakedPart leg : legs) {
                leg.render(pose, buffer, light, overlay, color);
            }
            body.render(pose, buffer, light, overlay, color);
        }
    }
}
