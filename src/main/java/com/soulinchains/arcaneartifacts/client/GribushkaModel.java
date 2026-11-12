package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.entity.Gribushka;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/** A stubby mushroom person: a stem body with a face, a wide cap, short arms and legs. */
public class GribushkaModel extends HierarchicalModel<Gribushka> implements ArmedModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcaneArtifacts.id("gribushka"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart cap;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public GribushkaModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.cap = body.getChild("cap");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5F, -9.0F, -3.0F, 7.0F, 9.0F, 6.0F), PartPose.offset(0.0F, 20.0F, 0.0F));
        body.addOrReplaceChild("cap", CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-7.0F, -4.0F, -7.0F, 14.0F, 4.0F, 14.0F)
                        .texOffs(26, 0).addBox(-4.5F, -6.0F, -4.5F, 9.0F, 2.0F, 9.0F),
                PartPose.offset(0.0F, -8.5F, 0.0F));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 15).addBox(-2.0F, -1.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(-3.5F, -7.0F, 0.0F));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 15).mirror().addBox(0.0F, -1.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(3.5F, -7.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(8, 15).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(-1.8F, 20.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(8, 15).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(1.8F, 20.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Gribushka entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        rightLeg.xRot = Mth.cos(limbSwing * 0.8F) * 1.2F * limbSwingAmount;
        leftLeg.xRot = Mth.cos(limbSwing * 0.8F + (float) Math.PI) * 1.2F * limbSwingAmount;
        rightArm.xRot = Mth.cos(limbSwing * 0.8F + (float) Math.PI) * 0.8F * limbSwingAmount;
        leftArm.xRot = Mth.cos(limbSwing * 0.8F) * 0.8F * limbSwingAmount;
        body.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.25F;
        cap.zRot = Mth.cos(ageInTicks * 0.09F) * 0.04F;
        if (entity.isAdmiring()) {
            // Holds the mushroom up in front of its face and turns it over.
            rightArm.xRot = -1.3F + Mth.cos(ageInTicks * 0.3F) * 0.1F;
            leftArm.xRot = -1.3F + Mth.sin(ageInTicks * 0.3F) * 0.1F;
        }
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack pose) {
        body.translateAndRotate(pose);
        (arm == HumanoidArm.RIGHT ? rightArm : leftArm).translateAndRotate(pose);
    }
}
