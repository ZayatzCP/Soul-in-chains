package com.soulinchains.arcaneartifacts.client.necro;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soulinchains.arcaneartifacts.necro.ShackleAnchor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Four chains from the ground around a shackled creature up to its middle, with a faint ghostly tint. */
public class ShackleAnchorRenderer extends EntityRenderer<ShackleAnchor> {
    private static final double SPREAD = 0.8;

    public ShackleAnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(ShackleAnchor anchor, Frustum frustum, double x, double y, double z) {
        return anchor.target() != null;
    }

    @Override
    public void render(ShackleAnchor anchor, float yaw, float partial, PoseStack poseStack, MultiBufferSource buffers, int light) {
        Entity target = anchor.target();
        if (target == null) {
            return;
        }
        Vec3 here = anchor.getPosition(partial);
        Vec3 body = target.getPosition(partial).add(0, target.getBbHeight() * 0.5, 0).subtract(here);
        double spread = SPREAD + target.getBbWidth() * 0.5;
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 4 + i * Math.PI / 2;
            Vec3 ground = new Vec3(Math.cos(a) * spread, 0.02, Math.sin(a) * spread);
            ChainDraw.draw(poseStack, buffers, ground, body, light, 0xFFC8D8FF);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ShackleAnchor anchor) {
        return ResourceLocation.withDefaultNamespace("textures/block/chain.png");
    }
}
