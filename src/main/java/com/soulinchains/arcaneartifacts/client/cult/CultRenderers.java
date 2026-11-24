package com.soulinchains.arcaneartifacts.client.cult;

import com.soulinchains.arcaneartifacts.client.necro.NecroMobRenderers;
import com.soulinchains.arcaneartifacts.cult.AshWitness;
import com.soulinchains.arcaneartifacts.cult.BoneGatekeeper;
import com.soulinchains.arcaneartifacts.cult.Psalmist;
import com.soulinchains.arcaneartifacts.cult.RedAcolyte;
import com.soulinchains.arcaneartifacts.cult.VotiveBeast;
import com.soulinchains.arcaneartifacts.cult.TornAcolyte;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Placeholder looks for the Cultist creatures (vanilla models, tinted) until the designer's models arrive. */
public final class CultRenderers {
    private CultRenderers() {
    }

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace("textures/entity/" + path + ".png");
    }

    /** Ash Witness: a pale figure of ash. */
    public static class AshWitnessRenderer extends HumanoidMobRenderer<AshWitness, HumanoidModel<AshWitness>> {
        private static final ResourceLocation TEXTURE = vanilla("skeleton/stray");

        public AshWitnessRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.STRAY)), 0.4F);
            addLayer(new NecroMobRenderers.Overlay<>(this, TEXTURE, 0x88929292));
        }

        @Override
        public ResourceLocation getTextureLocation(AshWitness mob) {
            return TEXTURE;
        }
    }

    /** Torn Acolyte: a charred initiate under a crimson haze. */
    public static class TornAcolyteRenderer extends HumanoidMobRenderer<TornAcolyte, HumanoidModel<TornAcolyte>> {
        private static final ResourceLocation TEXTURE = vanilla("skeleton/wither_skeleton");

        public TornAcolyteRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.WITHER_SKELETON)), 0.5F);
            addLayer(new NecroMobRenderers.Overlay<>(this, TEXTURE, 0x66B9463D));
        }

        @Override
        public ResourceLocation getTextureLocation(TornAcolyte mob) {
            return TEXTURE;
        }
    }

    public static class RedAcolyteRenderer extends HumanoidMobRenderer<RedAcolyte, HumanoidModel<RedAcolyte>> {
        private static final ResourceLocation TEXTURE = vanilla("skeleton/stray");

        public RedAcolyteRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.STRAY)), 0.5F);
            addLayer(new NecroMobRenderers.Overlay<>(this, TEXTURE, 0x99B9463D));
        }

        @Override
        public ResourceLocation getTextureLocation(RedAcolyte mob) {
            return TEXTURE;
        }
    }

    public static class GatekeeperRenderer extends HumanoidMobRenderer<BoneGatekeeper, HumanoidModel<BoneGatekeeper>> {
        private static final ResourceLocation TEXTURE = vanilla("skeleton/skeleton");

        public GatekeeperRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.5F);
            addLayer(new NecroMobRenderers.Overlay<>(this, TEXTURE, 0x55F0E7DE));
        }

        @Override
        public ResourceLocation getTextureLocation(BoneGatekeeper mob) {
            return TEXTURE;
        }

        @Override
        protected void scale(BoneGatekeeper mob, com.mojang.blaze3d.vertex.PoseStack pose, float partial) {
            pose.scale(1.3F, 1.3F, 1.3F);
        }
    }

    public static class PsalmistRenderer extends HumanoidMobRenderer<Psalmist, HumanoidModel<Psalmist>> {
        private static final ResourceLocation TEXTURE = vanilla("skeleton/stray");

        public PsalmistRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.STRAY)), 0.5F);
            addLayer(new NecroMobRenderers.Overlay<>(this, TEXTURE, 0x88D6B2A5));
        }

        @Override
        public ResourceLocation getTextureLocation(Psalmist mob) {
            return TEXTURE;
        }
    }

    /** Votive Beast: a hoglin in bone and cinnabar. */
    public static class VotiveBeastRenderer extends net.minecraft.client.renderer.entity.MobRenderer<VotiveBeast, net.minecraft.client.model.HoglinModel<VotiveBeast>> {
        private static final ResourceLocation TEXTURE = vanilla("hoglin/hoglin");

        public VotiveBeastRenderer(EntityRendererProvider.Context context) {
            super(context, new net.minecraft.client.model.HoglinModel<>(context.bakeLayer(ModelLayers.HOGLIN)), 0.7F);
            addLayer(new NecroMobRenderers.Overlay<>(this, TEXTURE, 0x99F0E7DE));
        }

        @Override
        public ResourceLocation getTextureLocation(VotiveBeast mob) {
            return TEXTURE;
        }
    }

    /** Renegade Sealer: a husk in bordeaux. */
    public static class RenegadeSealerRenderer extends HumanoidMobRenderer<com.soulinchains.arcaneartifacts.cult.RenegadeSealer,
            HumanoidModel<com.soulinchains.arcaneartifacts.cult.RenegadeSealer>> {
        private static final ResourceLocation TEXTURE = vanilla("zombie/husk");

        public RenegadeSealerRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.HUSK)), 0.5F);
            addLayer(new NecroMobRenderers.Overlay<>(this, TEXTURE, 0x996F2927));
        }

        @Override
        public ResourceLocation getTextureLocation(com.soulinchains.arcaneartifacts.cult.RenegadeSealer mob) {
            return TEXTURE;
        }
    }

    /** Faceless Sentry: a tall charcoal wither skeleton. */
    public static class FacelessSentryRenderer extends HumanoidMobRenderer<com.soulinchains.arcaneartifacts.cult.FacelessSentry,
            HumanoidModel<com.soulinchains.arcaneartifacts.cult.FacelessSentry>> {
        private static final ResourceLocation TEXTURE = vanilla("skeleton/wither_skeleton");

        public FacelessSentryRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.WITHER_SKELETON)), 0.7F);
            addLayer(new NecroMobRenderers.Overlay<>(this, TEXTURE, 0x66171416));
        }

        @Override
        public ResourceLocation getTextureLocation(com.soulinchains.arcaneartifacts.cult.FacelessSentry mob) {
            return TEXTURE;
        }

        @Override
        protected void scale(com.soulinchains.arcaneartifacts.cult.FacelessSentry mob, com.mojang.blaze3d.vertex.PoseStack pose, float partial) {
            pose.scale(1.1F, 1.1F, 1.1F);
        }
    }

    /** The Archdeacon: a tall stray robed in cinnabar. */
    public static class ArchdeaconRenderer extends HumanoidMobRenderer<com.soulinchains.arcaneartifacts.cult.Archdeacon,
            HumanoidModel<com.soulinchains.arcaneartifacts.cult.Archdeacon>> {
        private static final ResourceLocation TEXTURE = vanilla("skeleton/stray");

        public ArchdeaconRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.STRAY)), 0.6F);
            addLayer(new NecroMobRenderers.Overlay<>(this, TEXTURE, 0xAAB9463D));
        }

        @Override
        public ResourceLocation getTextureLocation(com.soulinchains.arcaneartifacts.cult.Archdeacon mob) {
            return TEXTURE;
        }

        @Override
        protected void scale(com.soulinchains.arcaneartifacts.cult.Archdeacon mob, com.mojang.blaze3d.vertex.PoseStack pose, float partial) {
            pose.scale(1.25F, 1.25F, 1.25F);
        }
    }
}
