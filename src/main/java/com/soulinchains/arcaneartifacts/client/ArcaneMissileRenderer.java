package com.soulinchains.arcaneartifacts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.entity.ArcaneMissileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * A spinning, full-bright, camera-facing rune sprite (same technique as the vanilla dragon fireball).
 * Split children use a smaller turquoise variant. The trail particles are spawned by the entity.
 */
public class ArcaneMissileRenderer extends EntityRenderer<ArcaneMissileEntity> {
    private static final ResourceLocation TEXTURE = ArcaneArtifacts.id("textures/entity/arcane_missile.png");
    private static final ResourceLocation CHILD_TEXTURE = ArcaneArtifacts.id("textures/entity/arcane_missile_shard.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public ArcaneMissileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ArcaneMissileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        boolean child = entity.isSplitChild();
        float scale = child ? 0.45f : 0.7f;
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getBbHeight() / 2.0F, 0.0F);
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTicks) * 18f));
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(child ? CHILD_TEXTURE : TEXTURE));
        vertex(consumer, pose, 0.0F, 0, 0, 1);
        vertex(consumer, pose, 1.0F, 0, 1, 1);
        vertex(consumer, pose, 1.0F, 1, 1, 0);
        vertex(consumer, pose, 0.0F, 1, 0, 0);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffers, packedLight);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, int y, int u, int v) {
        consumer.addVertex(pose, x - 0.5F, (float) y - 0.5F, 0.0F)
                .setColor(-1)
                .setUv((float) u, (float) v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(ArcaneMissileEntity entity) {
        return entity.isSplitChild() ? CHILD_TEXTURE : TEXTURE;
    }
}
