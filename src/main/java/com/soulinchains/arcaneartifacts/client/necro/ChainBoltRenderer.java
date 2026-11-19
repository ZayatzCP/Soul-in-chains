package com.soulinchains.arcaneartifacts.client.necro;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soulinchains.arcaneartifacts.necro.ChainBolt;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** A thrown chain: from the thrower's hand to the hook (or the hooked creature). */
public class ChainBoltRenderer extends EntityRenderer<ChainBolt> {
    public ChainBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(ChainBolt bolt, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(ChainBolt bolt, float yaw, float partial, PoseStack poseStack, MultiBufferSource buffers, int light) {
        Entity owner = bolt.getOwner();
        if (owner == null) {
            return;
        }
        Vec3 here = bolt.getPosition(partial);
        float bodyYaw = Mth.lerp(partial, owner.yRotO, owner.getYRot()) * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Math.cos(bodyYaw), 0, -Math.sin(bodyYaw)).scale(0.35);
        Vec3 hand = owner.getPosition(partial).add(0, owner.getBbHeight() * 0.62, 0).add(right);
        ChainDraw.draw(poseStack, buffers, Vec3.ZERO, hand.subtract(here), light, 0xFFFFFFFF);
        super.render(bolt, yaw, partial, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ChainBolt bolt) {
        return ResourceLocation.withDefaultNamespace("textures/block/chain.png");
    }
}
