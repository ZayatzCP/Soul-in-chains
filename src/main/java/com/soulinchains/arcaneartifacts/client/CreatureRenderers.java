package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.entity.Gribushka;
import com.soulinchains.arcaneartifacts.entity.Mudzya;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/** Renderers of the Bli-Blik (glows when full of Source) and the Mycelinoid folk (Mycelinoid, Amanitoid). */
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

    /** The Mycelinoid folk, all drawn from the designer's model with their own texture and size. */
    public static final class MycelinoidRenderer<T extends Gribushka> extends MobRenderer<T, GribushkaModel<T>> {
        public static final ResourceLocation MYCELINOID = ArcaneArtifacts.id("textures/entity/gribushka.png");
        public static final ResourceLocation AMANITOID = ArcaneArtifacts.id("textures/entity/amanitoid.png");
        private final ResourceLocation texture;
        /** The designer's model is 30 pixels tall: scaled to each one's size. */
        private final float scale;

        public MycelinoidRenderer(EntityRendererProvider.Context context, ResourceLocation texture, float scale, float shadow) {
            super(context, new GribushkaModel<>(context.getResourceManager()), shadow);
            this.texture = texture;
            this.scale = scale;
            addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        }

        @Override
        protected void scale(T entity, com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
            pose.scale(scale, scale, scale);
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return texture;
        }
    }
}
