package com.soulinchains.arcaneartifacts.client.catto;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soulinchains.arcaneartifacts.catto.Catto;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Catto on its hind legs with its tool in the paw, plus a bubble over its head: what it is busy with, "!" when stuck. */
public class CattoRenderer extends MobRenderer<Catto, CattoModel> {

    public CattoRenderer(EntityRendererProvider.Context context) {
        super(context, new CattoModel(context.bakeLayer(CattoModel.LAYER)), 0.35F);
        addLayer(new CattoGearLayer(this, context.getModelSet()));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    /** One texture per coat. */
    @Override
    public ResourceLocation getTextureLocation(Catto entity) {
        return ResourceLocation.fromNamespaceAndPath("arcane_artifacts", "textures/entity/catto/" + entity.coat().key() + ".png");
    }

    @Override
    protected void scale(Catto entity, PoseStack pose, float partialTicks) {
        pose.scale(0.8F, 0.8F, 0.8F);
    }

    @Override
    public void render(Catto entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTicks, pose, buffers, light);
        Catto cat = entity;
        if (cat.isInvisible()) {
            return;
        }
        renderSpeech(cat, partialTicks, pose, buffers);
        ItemStack icon = cat.bubble();
        boolean alert = cat.alert();
        if (icon.isEmpty() && !alert) {
            return;
        }
        pose.pushPose();
        float bob = Mth.sin((entity.tickCount + partialTicks) * 0.12F) * 0.04F;
        pose.translate(0.0, entity.getBbHeight() + 0.5 + bob, 0.0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        if (!icon.isEmpty()) {
            pose.pushPose();
            pose.scale(0.4F, 0.4F, 0.4F);
            Minecraft.getInstance().getItemRenderer().renderStatic(icon, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
            pose.popPose();
        }
        if (alert) {
            pose.pushPose();
            pose.translate(icon.isEmpty() ? 0.0 : 0.22, 0.12, 0.0);
            pose.scale(0.04F, -0.04F, 0.04F);
            Font font = getFont();
            String mark = "!";
            font.drawInBatch(mark, -font.width(mark) / 2.0F, 0, 0xFFFF5040, false, pose.last().pose(), buffers,
                    Font.DisplayMode.NORMAL, 0x80000000, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        pose.popPose();
    }

    /** What Catto says (to another Catto, to itself, a tune), in a little box above its head. */
    private void renderSpeech(Catto cat, float partialTicks, PoseStack pose, MultiBufferSource buffers) {
        Component speech = cat.speech();
        if (speech.getString().isEmpty() || entityRenderDispatcher.distanceToSqr(cat) > 32 * 32) {
            return;
        }
        Font font = getFont();
        java.util.List<net.minecraft.util.FormattedCharSequence> lines = font.split(speech, 150);
        pose.pushPose();
        pose.translate(0.0, cat.getBbHeight() + (cat.bubble().isEmpty() ? 0.55 : 1.0), 0.0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.scale(0.022F, -0.022F, 0.022F);
        float y = -lines.size() * 10.0F;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            float x = -font.width(line) / 2.0F;
            font.drawInBatch(line, x, y, 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0x90201030, LightTexture.FULL_BRIGHT);
            y += 10.0F;
        }
        pose.popPose();
    }
}
