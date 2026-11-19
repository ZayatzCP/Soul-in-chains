package com.soulinchains.arcaneartifacts.client.necro;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soulinchains.arcaneartifacts.necro.mob.Mourner;
import com.soulinchains.arcaneartifacts.necro.mob.Restless;
import com.soulinchains.arcaneartifacts.necro.mob.SoulEater;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.WitchModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** The Shackled's enemies on vanilla models and textures, each with a tinted spectral overlay. */
public final class NecroMobRenderers {
    private NecroMobRenderers() {
    }

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace("textures/entity/" + path + ".png");
    }

    /** A translucent tinted copy of the model, slightly larger. */
    static final class Overlay<T extends Mob, M extends EntityModel<T>> extends RenderLayer<T, M> {
        private final ResourceLocation texture;
        private final int argb;

        Overlay(RenderLayerParent<T, M> parent, ResourceLocation texture, int argb) {
            super(parent);
            this.texture = texture;
            this.argb = argb;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T mob, float limbSwing, float limbAmount, float partial,
                           float age, float headYaw, float headPitch) {
            pose.pushPose();
            pose.scale(1.04F, 1.02F, 1.04F);
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(texture)), light, OverlayTexture.NO_OVERLAY, argb);
            pose.popPose();
        }
    }

    /** Restless: a faint stray skeleton wrapped in pale blue. */
    public static class RestlessRenderer extends HumanoidMobRenderer<Restless, HumanoidModel<Restless>> {
        private static final ResourceLocation TEXTURE = vanilla("skeleton/stray");

        public RestlessRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.STRAY)), 0.0F);
            addLayer(new Overlay<>(this, TEXTURE, 0x669FE8FF));
        }

        @Override
        public ResourceLocation getTextureLocation(Restless mob) {
            return TEXTURE;
        }

        /** Drawn like an invisible mob seen by a spectator: barely there. */
        @Override
        protected boolean isBodyVisible(Restless mob) {
            return false;
        }
    }

    /** Soul Eater: a gaunt enderman in violet. */
    public static class SoulEaterRenderer extends MobRenderer<SoulEater, EndermanModel<SoulEater>> {
        private static final ResourceLocation TEXTURE = vanilla("enderman/enderman");

        public SoulEaterRenderer(EntityRendererProvider.Context context) {
            super(context, new EndermanModel<>(context.bakeLayer(ModelLayers.ENDERMAN)), 0.5F);
            addLayer(new Overlay<>(this, TEXTURE, 0x778E3FD0));
        }

        @Override
        public ResourceLocation getTextureLocation(SoulEater mob) {
            return TEXTURE;
        }

        @Override
        public void render(SoulEater mob, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            model.creepy = mob.getTarget() != null;
            super.render(mob, yaw, partial, pose, buffers, light);
        }
    }

    /** Mourner: a pale, grey-veiled witch. */
    public static class MournerRenderer extends MobRenderer<Mourner, WitchModel<Mourner>> {
        private static final ResourceLocation TEXTURE = vanilla("witch");

        public MournerRenderer(EntityRendererProvider.Context context) {
            super(context, new WitchModel<>(context.bakeLayer(ModelLayers.WITCH)), 0.5F);
            addLayer(new Overlay<>(this, TEXTURE, 0x77B8C8DC));
        }

        @Override
        public ResourceLocation getTextureLocation(Mourner mob) {
            return TEXTURE;
        }
    }
}
