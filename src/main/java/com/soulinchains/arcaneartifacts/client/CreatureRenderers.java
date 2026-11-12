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
        private static final ResourceLocation[] TEXTURES = {
                ArcaneArtifacts.id("textures/entity/gribushka_blue.png"),
                ArcaneArtifacts.id("textures/entity/gribushka_purple.png"),
                ArcaneArtifacts.id("textures/entity/gribushka_brown.png")
        };

        public GribushkaRenderer(EntityRendererProvider.Context context) {
            super(context, new GribushkaModel(context.bakeLayer(GribushkaModel.LAYER)), 0.4F);
            addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        }

        @Override
        public ResourceLocation getTextureLocation(Gribushka entity) {
            return TEXTURES[entity.getVariant()];
        }
    }
}
