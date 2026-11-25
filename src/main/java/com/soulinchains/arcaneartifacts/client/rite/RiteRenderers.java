package com.soulinchains.arcaneartifacts.client.rite;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.soulinchains.arcaneartifacts.client.necro.NecroMobRenderers;
import com.soulinchains.arcaneartifacts.rite.Echo;
import com.soulinchains.arcaneartifacts.rite.OfferingPedestalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Looks of the chalk rites: the offering floating over its pedestal, and the Echo (a dark husk until the designer's model). */
public final class RiteRenderers {
    private RiteRenderers() {
    }

    public static class PedestalRenderer implements BlockEntityRenderer<OfferingPedestalBlockEntity> {
        public PedestalRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(OfferingPedestalBlockEntity pedestal, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            ItemStack stack = pedestal.item();
            if (stack.isEmpty() || pedestal.getLevel() == null) {
                return;
            }
            float time = (pedestal.getLevel().getGameTime() + partial) / 20F;
            pose.pushPose();
            pose.translate(0.5, 1.0 + Math.sin(time * 2) * 0.05, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(time * 45));
            pose.scale(0.6F, 0.6F, 0.6F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                    pedestal.getLevel(), 0);
            pose.popPose();
        }
    }

    public static class EchoRenderer extends HumanoidMobRenderer<Echo, HumanoidModel<Echo>> {
        private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/zombie/husk.png");

        public EchoRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.HUSK)), 0.4F);
            addLayer(new NecroMobRenderers.Overlay<>(this, TEXTURE, 0xCC2A1F3D));
        }

        @Override
        public ResourceLocation getTextureLocation(Echo mob) {
            return TEXTURE;
        }
    }
}
