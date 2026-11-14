package com.soulinchains.arcaneartifacts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.client.baked.BakedModels;
import com.soulinchains.arcaneartifacts.client.baked.BakedPart;
import com.soulinchains.arcaneartifacts.mount.ClassMount;
import com.soulinchains.arcaneartifacts.mount.FlyingBroom;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;

import java.util.Map;

/**
 * The class mounts, drawn from baked models in {@code entity_models/<name>.json} (made by
 * tools/generate_mounts.py): the broom is one piece that stays level and bobs; the elk and
 * the iron horse walk on four legs (leg_front_left, leg_front_right, leg_back_left, leg_back_right).
 */
public class MountRenderer<T extends ClassMount> extends MobRenderer<T, MountRenderer.Model<T>> {
    private final ResourceLocation texture;

    public MountRenderer(EntityRendererProvider.Context context, String name, float shadow) {
        super(context, new Model<>(context.getResourceManager(), ArcaneArtifacts.id("entity_models/" + name + ".json")), shadow);
        this.texture = ArcaneArtifacts.id("textures/entity/" + name + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, com.mojang.blaze3d.vertex.PoseStack pose,
                       net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        if (entity instanceof FlyingBroom broom && broom.getControllingPassenger() instanceof net.minecraft.world.entity.player.Player rider) {
            // The broom is drawn under the seated rider, just below the hips (BroomRiding).
            net.minecraft.world.phys.Vec3 at = rider.getPosition(partialTick).subtract(entity.getPosition(partialTick));
            pose.pushPose();
            pose.translate(at.x, at.y + BroomRiding.HIPS - BroomRiding.HANDLE_DROP - HANDLE_HEIGHT, at.z);
            super.render(entity, entityYaw, partialTick, pose, buffers, light);
            pose.popPose();
            return;
        }
        super.render(entity, entityYaw, partialTick, pose, buffers, light);
    }

    /** Height of the broom handle (the broom model's pivot) above the broom's feet, in blocks. */
    private static final float HANDLE_HEIGHT = 11F / 16F;

    @Override
    protected boolean shouldShowName(T entity) {
        return entity.hasCustomName() && super.shouldShowName(entity);
    }

    public static final class Model<T extends ClassMount> extends EntityModel<T> {
        private final BakedPart body;
        private final BakedPart[] legs;
        private float lift;

        Model(ResourceManager resources, ResourceLocation id) {
            Map<String, BakedPart> parts = BakedModels.load(resources, id);
            body = BakedModels.part(parts, "body");
            legs = new BakedPart[]{BakedModels.part(parts, "leg_front_left"), BakedModels.part(parts, "leg_back_right"),
                    BakedModels.part(parts, "leg_front_right"), BakedModels.part(parts, "leg_back_left")};
        }

        @Override
        public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            body.reset();
            lift = 0F;
            if (entity instanceof FlyingBroom broom) {
                body.zRot = Mth.sin(ageInTicks * 0.08F) * 0.03F;
                lift = broom.isVehicle() ? 0F : 3F + Mth.sin(ageInTicks * 0.1F) * 0.6F;    // an empty broom floats a little
                return;
            }
            float walk = limbSwing * 0.6662F;
            for (int i = 0; i < legs.length; i++) {
                legs[i].xRot = Mth.cos(walk + (i < 2 ? 0F : Mth.PI)) * 1.1F * limbSwingAmount;
            }
            body.zRot = Mth.cos(walk) * 0.03F * limbSwingAmount;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
            pose.pushPose();
            pose.translate(0, -lift / 16f, 0);
            for (BakedPart leg : legs) {
                leg.render(pose, buffer, light, overlay, color);
            }
            body.render(pose, buffer, light, overlay, color);
            pose.popPose();
        }
    }
}
