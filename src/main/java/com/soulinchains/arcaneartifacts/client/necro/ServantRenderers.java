package com.soulinchains.arcaneartifacts.client.necro;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soulinchains.arcaneartifacts.necro.Servant;
import com.soulinchains.arcaneartifacts.necro.ServantKind;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Servants are drawn with the vanilla skeleton, drowned and wolf models and textures, plus a ghostly tint in the colour
 * of their trade, and a chain running to their master.
 */
public final class ServantRenderers {
    private ServantRenderers() {
    }

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace("textures/entity/" + path + ".png");
    }

    static ResourceLocation texture(ServantKind kind) {
        return switch (kind) {
            case SKELETON -> vanilla("skeleton/skeleton");
            case KNIGHT, ASSASSIN -> vanilla("skeleton/wither_skeleton");
            case PLAGUE_DOCTOR -> vanilla("skeleton/stray");
            case PORTER -> vanilla("skeleton/skeleton");
            case ROWER -> vanilla("zombie/drowned");
            case HOUND -> vanilla("wolf/wolf");
        };
    }

    /** ARGB of the ghostly overlay. */
    static int tint(ServantKind kind) {
        return switch (kind) {
            case SKELETON -> 0x5590C8FF;
            case HOUND -> 0x8870B8FF;
            case KNIGHT -> 0x66FF7A1E;
            case ASSASSIN -> 0x77602090;
            case ROWER -> 0x553FB8C8;
            case PORTER -> 0x44C8B890;
            case PLAGUE_DOCTOR -> 0x6650D050;
        };
    }

    static float scale(ServantKind kind) {
        return kind == ServantKind.KNIGHT ? 1.1F : 1.0F;
    }

    /** A translucent copy of the model in the servant's colour, slightly larger. */
    static final class GhostLayer<M extends EntityModel<Servant>> extends RenderLayer<Servant, M> {
        GhostLayer(RenderLayerParent<Servant, M> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, Servant servant, float limbSwing, float limbAmount,
                           float partial, float age, float headYaw, float headPitch) {
            poseStack.pushPose();
            poseStack.scale(1.04F, 1.02F, 1.04F);
            getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucent(texture(servant.kind))),
                    light, OverlayTexture.NO_OVERLAY, tint(servant.kind));
            poseStack.popPose();
        }
    }

    static void chainToMaster(Servant servant, float partial, PoseStack poseStack, MultiBufferSource buffers, int light) {
        LivingEntity master = servant.getOwner();
        if (master == null || master.distanceTo(servant) > 32) {
            return;
        }
        Vec3 here = servant.getPosition(partial);
        Vec3 neck = new Vec3(0, servant.getBbHeight() * 0.75, 0);
        Vec3 waist = master.getPosition(partial).add(0, master.getBbHeight() * 0.45, 0).subtract(here);
        ChainDraw.draw(poseStack, buffers, neck, waist, light, 0xFFB8C8FF);
    }

    /** Skeleton-like and drowned servants. */
    public static class Humanoid extends HumanoidMobRenderer<Servant, HumanoidModel<Servant>> {
        public Humanoid(EntityRendererProvider.Context context, boolean drowned) {
            super(context, new HumanoidModel<>(context.bakeLayer(drowned ? ModelLayers.DROWNED : ModelLayers.SKELETON)), 0.5F);
            addLayer(new GhostLayer<>(this));
        }

        @Override
        public ResourceLocation getTextureLocation(Servant servant) {
            return texture(servant.kind);
        }

        @Override
        protected void scale(Servant servant, PoseStack poseStack, float partial) {
            float s = ServantRenderers.scale(servant.kind);
            poseStack.scale(s, s, s);
        }

        @Override
        public void render(Servant servant, float yaw, float partial, PoseStack poseStack, MultiBufferSource buffers, int light) {
            super.render(servant, yaw, partial, poseStack, buffers, light);
            chainToMaster(servant, partial, poseStack, buffers, light);
        }
    }

    /** The Ghost Hound, on the vanilla wolf geometry. */
    public static class Hound extends MobRenderer<Servant, HoundModel> {
        public Hound(EntityRendererProvider.Context context) {
            super(context, new HoundModel(context.bakeLayer(ModelLayers.WOLF)), 0.5F);
            addLayer(new GhostLayer<>(this));
        }

        @Override
        public ResourceLocation getTextureLocation(Servant servant) {
            return texture(servant.kind);
        }

        @Override
        public void render(Servant servant, float yaw, float partial, PoseStack poseStack, MultiBufferSource buffers, int light) {
            super.render(servant, yaw, partial, poseStack, buffers, light);
            chainToMaster(servant, partial, poseStack, buffers, light);
        }
    }

    /** The wolf layer's parts, animated as a simple trotting quadruped. */
    public static class HoundModel extends EntityModel<Servant> {
        private final ModelPart head;
        private final ModelPart body;
        private final ModelPart upperBody;
        private final ModelPart rightHindLeg;
        private final ModelPart leftHindLeg;
        private final ModelPart rightFrontLeg;
        private final ModelPart leftFrontLeg;
        private final ModelPart tail;

        public HoundModel(ModelPart root) {
            head = root.getChild("head");
            body = root.getChild("body");
            upperBody = root.getChild("upper_body");
            rightHindLeg = root.getChild("right_hind_leg");
            leftHindLeg = root.getChild("left_hind_leg");
            rightFrontLeg = root.getChild("right_front_leg");
            leftFrontLeg = root.getChild("left_front_leg");
            tail = root.getChild("tail");
        }

        @Override
        public void setupAnim(Servant servant, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
            head.xRot = headPitch * Mth.DEG_TO_RAD;
            head.yRot = headYaw * Mth.DEG_TO_RAD;
            float swing = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbAmount;
            rightHindLeg.xRot = swing;
            leftHindLeg.xRot = -swing;
            rightFrontLeg.xRot = -swing;
            leftFrontLeg.xRot = swing;
            tail.xRot = 0.9F + Mth.cos(age * 0.2F) * 0.1F;
            tail.yRot = Mth.cos(limbSwing * 0.6662F) * 0.5F * limbAmount;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer buffer, int light, int overlay, int color) {
            head.render(poseStack, buffer, light, overlay, color);
            body.render(poseStack, buffer, light, overlay, color);
            upperBody.render(poseStack, buffer, light, overlay, color);
            rightHindLeg.render(poseStack, buffer, light, overlay, color);
            leftHindLeg.render(poseStack, buffer, light, overlay, color);
            rightFrontLeg.render(poseStack, buffer, light, overlay, color);
            leftFrontLeg.render(poseStack, buffer, light, overlay, color);
            tail.render(poseStack, buffer, light, overlay, color);
        }
    }
}
