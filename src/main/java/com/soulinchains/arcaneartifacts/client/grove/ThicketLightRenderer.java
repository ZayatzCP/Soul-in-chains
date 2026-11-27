package com.soulinchains.arcaneartifacts.client.grove;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.grove.ThicketLight;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A Thicket Light: no body, only light - a soft halo of its colour and a bright core with four short rays, always facing the
 * eye, drawn at full brightness. It bobs and its glow breathes.
 */
public class ThicketLightRenderer extends EntityRenderer<ThicketLight> {
    private static final ResourceLocation HALO = ResourceLocation.fromNamespaceAndPath(ArcaneArtifacts.MODID, "textures/entity/thicket_light_halo.png");
    private static final ResourceLocation CORE = ResourceLocation.fromNamespaceAndPath(ArcaneArtifacts.MODID, "textures/entity/thicket_light_core.png");

    public ThicketLightRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public void render(ThicketLight light, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        float time = light.tickCount + partial + light.getId() * 13;
        float breath = 0.8F + 0.2F * Mth.sin(time * 0.15F);
        int rgb = light.color().rgb;
        pose.pushPose();
        pose.translate(0, light.getBbHeight() / 2 + Mth.sin(time * 0.08F) * 0.06F, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        quad(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(HALO)), 0.95F * breath, rgb, (int) (150 * breath));
        quad(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(CORE)), 0.42F, blend(rgb), 255);
        pose.popPose();
        super.render(light, yaw, partial, pose, buffers, packedLight);
    }

    /** The core is nearly white, only touched by the colour. */
    private static int blend(int rgb) {
        int r = (rgb >> 16 & 255) + (255 - (rgb >> 16 & 255)) * 3 / 4;
        int g = (rgb >> 8 & 255) + (255 - (rgb >> 8 & 255)) * 3 / 4;
        int b = (rgb & 255) + (255 - (rgb & 255)) * 3 / 4;
        return r << 16 | g << 8 | b;
    }

    private static void quad(PoseStack pose, VertexConsumer vc, float size, int rgb, int alpha) {
        PoseStack.Pose last = pose.last();
        int color = alpha << 24 | rgb;
        float h = size / 2;
        vertex(vc, last, -h, -h, 0, 1, color);
        vertex(vc, last, h, -h, 1, 1, color);
        vertex(vc, last, h, h, 1, 0, color);
        vertex(vc, last, -h, h, 0, 0, color);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float u, float v, int color) {
        vc.addVertex(pose, x, y, 0).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(ThicketLight light) {
        return CORE;
    }
}
