package com.soulinchains.arcaneartifacts.client.cult;

import com.soulinchains.arcaneartifacts.client.necro.NecroMobRenderers;
import com.soulinchains.arcaneartifacts.cult.AshWitness;
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
}
