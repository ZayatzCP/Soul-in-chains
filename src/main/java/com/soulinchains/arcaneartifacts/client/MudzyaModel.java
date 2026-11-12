package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.entity.Mudzya;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** A round little purple body with two antennae and two fluttering wings, about the size of a bee. */
public class MudzyaModel extends HierarchicalModel<Mudzya> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcaneArtifacts.id("mudzya"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public MudzyaModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -3.0F, -3.5F, 6.0F, 5.0F, 7.0F)
                        .texOffs(0, 12).addBox(-1.5F, -1.0F, -4.5F, 3.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 19.0F, 0.0F));
        body.addOrReplaceChild("left_antenna", CubeListBuilder.create().texOffs(20, 0).addBox(0.0F, -3.0F, 0.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(1.0F, -3.0F, -2.5F, -0.3F, 0.0F, 0.2F));
        body.addOrReplaceChild("right_antenna", CubeListBuilder.create().texOffs(20, 0).mirror().addBox(-1.0F, -3.0F, 0.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-1.0F, -3.0F, -2.5F, -0.3F, 0.0F, -0.2F));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 18).addBox(0.0F, 0.0F, -1.0F, 7.0F, 0.0F, 5.0F, new CubeDeformation(0.001F)),
                PartPose.offset(1.5F, -3.0F, -0.5F));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-7.0F, 0.0F, -1.0F, 7.0F, 0.0F, 5.0F, new CubeDeformation(0.001F)),
                PartPose.offset(-1.5F, -3.0F, -0.5F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Mudzya entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float flap = Mth.cos(ageInTicks * 2.1F) * (float) Math.PI * 0.18F;
        leftWing.zRot = -0.25F - flap;
        rightWing.zRot = 0.25F + flap;
        body.y = 19.0F + Mth.cos(ageInTicks * 0.18F) * 0.9F;
        body.xRot = 0.1F;
        body.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.3F;
    }
}
