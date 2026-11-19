package com.soulinchains.arcaneartifacts.client.necro;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soulinchains.arcaneartifacts.necro.BondLink;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** A spectral chain between two bonded creatures. */
public class BondLinkRenderer extends EntityRenderer<BondLink> {
    public BondLinkRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(BondLink link, Frustum frustum, double x, double y, double z) {
        return link.first() != null && link.second() != null;
    }

    @Override
    public void render(BondLink link, float yaw, float partial, PoseStack poseStack, MultiBufferSource buffers, int light) {
        Entity a = link.first();
        Entity b = link.second();
        if (a == null || b == null) {
            return;
        }
        Vec3 here = link.getPosition(partial);
        Vec3 from = a.getPosition(partial).add(0, a.getBbHeight() * 0.55, 0).subtract(here);
        Vec3 to = b.getPosition(partial).add(0, b.getBbHeight() * 0.55, 0).subtract(here);
        ChainDraw.draw(poseStack, buffers, from, to, light, 0xFF9FE8FF);
    }

    @Override
    public ResourceLocation getTextureLocation(BondLink link) {
        return ResourceLocation.withDefaultNamespace("textures/block/chain.png");
    }
}
