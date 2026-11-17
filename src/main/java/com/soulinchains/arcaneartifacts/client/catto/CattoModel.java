package com.soulinchains.arcaneartifacts.client.catto;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.catto.Catto;
import com.soulinchains.arcaneartifacts.catto.CattoBrain;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Placeholder Catto until the designer's model: a cat standing on its hind legs, a hood with ear sleeves, a bell at the
 * neck, a pack on the back and a long tail. Walks, sits, swings its tool while working and flattens its ears in danger.
 */
public class CattoModel extends HierarchicalModel<Catto> implements ArmedModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcaneArtifacts.id("catto"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart tail;

    public CattoModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.rightEar = head.getChild("right_ear");
        this.leftEar = head.getChild("left_ear");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 14).addBox(-3.0F, -7.0F, -2.0F, 6.0F, 7.0F, 4.0F), PartPose.offset(0.0F, 19.0F, 0.0F));
        body.addOrReplaceChild("pack", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-2.5F, -6.5F, 2.0F, 5.0F, 5.0F, 2.0F), PartPose.ZERO);
        body.addOrReplaceChild("bell", CubeListBuilder.create()
                .texOffs(16, 48).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -6.6F, -2.4F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -6.0F, -3.0F, 7.0F, 6.0F, 6.0F)
                        .texOffs(28, 5).addBox(-1.5F, -2.5F, -4.0F, 3.0F, 2.0F, 1.0F)
                        // The hood, one pixel bigger than the head, open at the face.
                        .texOffs(0, 32).addBox(-4.0F, -6.5F, -3.5F, 8.0F, 7.0F, 7.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(32, 32).addBox(-1.5F, -3.0F, -1.0F, 3.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-2.3F, -6.3F, 0.0F, 0.0F, 0.0F, -0.25F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(32, 32).mirror().addBox(-1.5F, -3.0F, -1.0F, 3.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(2.3F, -6.3F, 0.0F, 0.0F, 0.0F, 0.25F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(22, 14).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offset(-4.0F, 12.5F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(22, 14).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offset(4.0F, 12.5F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(32, 14).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offset(-1.5F, 19.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(32, 14).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offset(1.5F, 19.0F, 0.0F));
        root.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(42, 14).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 18.0F, 1.8F, 0.6F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Catto cat, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float swing = Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightArm.xRot = -swing * 0.8F;
        leftArm.xRot = swing * 0.8F;
        // A little waddle while walking, a lazy tail always.
        body.zRot = Mth.cos(limbSwing * 0.6662F) * 0.08F * limbSwingAmount;
        tail.yRot = Mth.sin(ageInTicks * 0.12F) * 0.35F;
        tail.xRot = 0.6F + Mth.sin(ageInTicks * 0.07F) * 0.1F;
        CattoBrain.State state = cat.shownState();
        if (state == CattoBrain.State.WORK) {
            rightArm.xRot = -1.6F + Mth.sin(ageInTicks * 0.8F) * 0.7F;
            leftArm.xRot = -0.7F;
            head.xRot = 0.3F;
        }
        if (state == CattoBrain.State.RETREAT || cat.alert()) {
            rightEar.zRot = -1.0F;
            leftEar.zRot = 1.0F;
        } else {
            float twitch = Mth.sin(ageInTicks * 0.05F) > 0.97F ? 0.25F : 0.0F;
            rightEar.zRot -= twitch;
        }
        if (cat.isInSittingPose()) {
            for (ModelPart part : new ModelPart[]{body, head, rightArm, leftArm, tail}) {
                part.y += 4.0F;
            }
            rightLeg.y = 22.0F;
            leftLeg.y = 22.0F;
            rightLeg.xRot = -1.45F;
            leftLeg.xRot = -1.45F;
            rightLeg.yRot = 0.15F;
            leftLeg.yRot = -0.15F;
            tail.xRot = 1.4F;
        }
    }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack pose) {
        (side == HumanoidArm.RIGHT ? rightArm : leftArm).translateAndRotate(pose);
        // The arm is short: bring the item up to the paw and make it cat-sized.
        pose.translate(0.0F, -0.25F, 0.0F);
        pose.scale(0.7F, 0.7F, 0.7F);
    }
}
