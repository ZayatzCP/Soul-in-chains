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
            addLayer(new NecroMobRenderers.Overlay<>(this, TEXTURE, 0xCC3A0A0A));
        }

        @Override
        public ResourceLocation getTextureLocation(Echo mob) {
            return TEXTURE;
        }
    }

    /** The creature shut in a ritual cage, drawn small inside it. */
    public static class CageRenderer implements BlockEntityRenderer<com.soulinchains.arcaneartifacts.rite.RiteCageBlockEntity> {
        public CageRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(com.soulinchains.arcaneartifacts.rite.RiteCageBlockEntity cage, float partial, PoseStack pose, MultiBufferSource buffers,
                           int light, int overlay) {
            if (!cage.occupied() || cage.shownFailed || cage.getLevel() == null) {
                return;
            }
            try {
                if (cage.shown == null) {
                    cage.shown = net.minecraft.world.entity.EntityType.loadEntityRecursive(cage.creature().copy(), cage.getLevel(), e -> e);
                    if (cage.shown == null) {
                        cage.shownFailed = true;
                        return;
                    }
                }
                net.minecraft.world.entity.Entity entity = cage.shown;
                // the creature is never ticked: pin every rotation (old and new alike) so nothing is interpolated between frames
                entity.setYRot(0F);
                entity.yRotO = 0F;
                entity.setXRot(0F);
                entity.xRotO = 0F;
                if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                    living.yBodyRot = 0F;
                    living.yBodyRotO = 0F;
                    living.yHeadRot = 0F;
                    living.yHeadRotO = 0F;
                }
                float scale = Math.min(1F, Math.min(0.8F / Math.max(0.1F, entity.getBbWidth()), 1.8F / Math.max(0.1F, entity.getBbHeight())));
                pose.pushPose();
                pose.translate(0.5, 0.06, 0.5);
                pose.scale(scale, scale, scale);
                Minecraft.getInstance().getEntityRenderDispatcher().render(entity, 0, 0, 0, 0F, partial, pose, buffers, light);
                pose.popPose();
            } catch (RuntimeException e) {
                cage.shownFailed = true;
            }
        }

        @Override
        public net.minecraft.world.phys.AABB getRenderBoundingBox(com.soulinchains.arcaneartifacts.rite.RiteCageBlockEntity cage) {
            return new net.minecraft.world.phys.AABB(cage.getBlockPos()).expandTowards(0, 1, 0);
        }
    }
}
