package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.entity.Gribushka;
import com.soulinchains.arcaneartifacts.entity.Mudzya;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/** Renderers of the Mudzya (glows when full of Source) and the Gribushka (three cap colours). */
public final class CreatureRenderers {
    private CreatureRenderers() {
    }

    public static final class MudzyaRenderer extends MobRenderer<Mudzya, MudzyaModel> {
        private static final ResourceLocation TEXTURE = ArcaneArtifacts.id("textures/entity/mudzya.png");
        private static final ResourceLocation CHARGED = ArcaneArtifacts.id("textures/entity/mudzya_charged.png");

        public MudzyaRenderer(EntityRendererProvider.Context context) {
            super(context, new MudzyaModel(context.bakeLayer(MudzyaModel.LAYER)), 0.2F);
        }

        @Override
        public ResourceLocation getTextureLocation(Mudzya entity) {
            return entity.isCharged() ? CHARGED : TEXTURE;
        }

        /** The jelly is see-through. */
        @Override
        protected net.minecraft.client.renderer.RenderType getRenderType(Mudzya entity, boolean bodyVisible, boolean translucent, boolean glowing) {
            if (bodyVisible || translucent) {
                return net.minecraft.client.renderer.RenderType.entityTranslucent(getTextureLocation(entity));
            }
            return glowing ? net.minecraft.client.renderer.RenderType.outline(getTextureLocation(entity)) : null;
        }

        @Override
        protected int getBlockLightLevel(Mudzya entity, net.minecraft.core.BlockPos pos) {
            return entity.isCharged() ? 12 : super.getBlockLightLevel(entity, pos);
        }
    }

    public static final class GribushkaRenderer extends MobRenderer<Gribushka, GribushkaModel> {
        /** One texture; the variants are tinted by the model. */
        private static final ResourceLocation TEXTURE = ArcaneArtifacts.id("textures/entity/gribushka.png");
        /** The designer's model is 30 pixels tall: scaled to the Gribushka's size. */
        private static final float SCALE = 0.6F;

        public GribushkaRenderer(EntityRendererProvider.Context context) {
            super(context, new GribushkaModel(context.getResourceManager()), 0.4F);
            addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        }

        @Override
        protected void scale(Gribushka entity, com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
            pose.scale(SCALE, SCALE, SCALE);
        }

        @Override
        public ResourceLocation getTextureLocation(Gribushka entity) {
            return TEXTURE;
        }
    }
}
