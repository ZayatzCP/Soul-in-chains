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

/**
 * A little jelly floating in the air: a translucent cyan dome with a glowing pink core, two big dark eyes,
 * two fan-like fins on top that flap like ears, two curled antennae, a frilled skirt and six short
 * tentacles that sway underneath. Rendered translucent (see {@link CreatureRenderers.MudzyaRenderer}).
 */
public class MudzyaModel extends HierarchicalModel<Mudzya> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcaneArtifacts.id("mudzya"), "main");
    private static final int TENTACLES = 6;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart leftAntenna;
    private final ModelPart rightAntenna;
    private final ModelPart[] tentacles = new ModelPart[TENTACLES];

    public MudzyaModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.leftFin = body.getChild("left_fin");
        this.rightFin = body.getChild("right_fin");
        this.leftAntenna = body.getChild("left_antenna");
        this.rightAntenna = body.getChild("right_antenna");
        for (int i = 0; i < TENTACLES; i++) {
            tentacles[i] = body.getChild("tentacle_" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        // The glowing core, inside the jelly.
                        .texOffs(0, 16).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 5.0F, 6.0F)
                        // The jelly dome.
                        .texOffs(0, 0).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 7.0F, 8.0F)
                        // The frilled skirt around the rim.
                        .texOffs(0, 28).addBox(-4.5F, -1.0F, -4.5F, 9.0F, 1.0F, 9.0F, new CubeDeformation(0.01F)),
                PartPose.offset(0.0F, 20.0F, 0.0F));
        body.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(1.8F, -3.5F, -4.0F));
        body.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(-1.8F, -3.5F, -4.0F));
        body.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(40, 8).addBox(0.0F, -6.0F, -1.0F, 0.0F, 6.0F, 5.0F),
                PartPose.offsetAndRotation(2.0F, -6.0F, 0.0F, -0.35F, 0.0F, 0.45F));
        body.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(40, 8).mirror().addBox(0.0F, -6.0F, -1.0F, 0.0F, 6.0F, 5.0F),
                PartPose.offsetAndRotation(-2.0F, -6.0F, 0.0F, -0.35F, 0.0F, -0.45F));
        body.addOrReplaceChild("left_antenna", CubeListBuilder.create().texOffs(52, 0).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(1.0F, -6.5F, -2.0F, 0.35F, 0.0F, 0.25F));
        body.addOrReplaceChild("right_antenna", CubeListBuilder.create().texOffs(52, 0).mirror().addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(-1.0F, -6.5F, -2.0F, 0.35F, 0.0F, -0.25F));
        for (int i = 0; i < TENTACLES; i++) {
            float angle = (float) (i * Math.PI * 2 / TENTACLES + Math.PI / TENTACLES);
            body.addOrReplaceChild("tentacle_" + i, CubeListBuilder.create().texOffs(52, 8).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                    PartPose.offsetAndRotation(Mth.sin(angle) * 2.8F, 0.0F, Mth.cos(angle) * 2.8F, 0.0F, angle, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Mudzya entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float flap = Mth.cos(ageInTicks * 0.45F) * 0.3F;
        leftFin.zRot = 0.45F + flap;
        rightFin.zRot = -0.45F - flap;
        leftAntenna.xRot = 0.35F + Mth.sin(ageInTicks * 0.2F) * 0.12F;
        rightAntenna.xRot = 0.35F + Mth.sin(ageInTicks * 0.2F + 1.0F) * 0.12F;
        for (int i = 0; i < TENTACLES; i++) {
            tentacles[i].xRot = 0.35F + Mth.sin(ageInTicks * 0.25F + i * 1.1F) * 0.25F;
        }
        body.y = 20.0F + Mth.cos(ageInTicks * 0.15F) * 1.0F;
        body.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.3F;
    }
}
