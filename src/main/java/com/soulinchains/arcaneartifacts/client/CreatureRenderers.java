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

    /** The Mycelinoid folk, drawn from the designer's baked models, each with its textures (variants) and size. */
    public static final class MycelinoidRenderer<T extends Gribushka> extends MobRenderer<T, GribushkaModel<T>> {
        private final ResourceLocation[] textures;
        private final float scale;

        public MycelinoidRenderer(EntityRendererProvider.Context context, String model, float scale, float shadow, String... textures) {
            super(context, new GribushkaModel<>(context.getResourceManager(), ArcaneArtifacts.id("entity_models/" + model + ".json")), shadow);
            this.textures = java.util.Arrays.stream(textures).map(t -> ArcaneArtifacts.id("textures/entity/" + t + ".png"))
                    .toArray(ResourceLocation[]::new);
            this.scale = scale;
            addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        }

        @Override
        protected void scale(T entity, com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
            pose.scale(scale, scale, scale);
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return textures[Math.floorMod(entity.getVariant(), textures.length)];
        }
    }

    public static final class GribulkaRenderer extends MobRenderer<com.soulinchains.arcaneartifacts.entity.Gribulka, GribulkaModels.Little> {
        private static final ResourceLocation TEXTURE = ArcaneArtifacts.id("textures/entity/gribulka.png");

        public GribulkaRenderer(EntityRendererProvider.Context context) {
            super(context, new GribulkaModels.Little(context.getResourceManager(), ArcaneArtifacts.id("entity_models/gribulka.json")), 0.3F);
        }

        @Override
        public ResourceLocation getTextureLocation(com.soulinchains.arcaneartifacts.entity.Gribulka entity) {
            return TEXTURE;
        }
    }

    public static final class GribulkaHiveRenderer extends MobRenderer<com.soulinchains.arcaneartifacts.entity.GribulkaHive, GribulkaModels.Hive> {
        private static final ResourceLocation TEXTURE = ArcaneArtifacts.id("textures/entity/gribulka_hive.png");
        private static final float SCALE = 0.9F;

        public GribulkaHiveRenderer(EntityRendererProvider.Context context) {
            super(context, new GribulkaModels.Hive(context.getResourceManager(), ArcaneArtifacts.id("entity_models/gribulka_hive.json")), 0.8F);
        }

        @Override
        protected void scale(com.soulinchains.arcaneartifacts.entity.GribulkaHive entity, com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
            pose.scale(SCALE, SCALE, SCALE);
        }

        @Override
        public ResourceLocation getTextureLocation(com.soulinchains.arcaneartifacts.entity.GribulkaHive entity) {
            return TEXTURE;
        }
    }
}
