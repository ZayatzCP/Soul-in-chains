package com.soulinchains.arcaneartifacts.client.astral;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.astral.Watcher;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** A Watcher: a tall, thin, faceless shape, black as the monoliths, two and a half times an enderman's height. */
public class WatcherRenderer extends MobRenderer<Watcher, EndermanModel<Watcher>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ArcaneArtifacts.MODID, "textures/entity/watcher.png");
    static final float SCALE = 2.5F;

    public WatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new EndermanModel<>(context.bakeLayer(ModelLayers.ENDERMAN)), 0F);
    }

    @Override
    protected void scale(Watcher watcher, PoseStack pose, float partial) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(Watcher watcher) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(Watcher watcher) {
        return false;
    }
}
