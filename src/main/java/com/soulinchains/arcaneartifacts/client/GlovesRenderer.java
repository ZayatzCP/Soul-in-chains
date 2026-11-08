package com.soulinchains.arcaneartifacts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Focusing Gloves are a held casting tool, but they are drawn as a pair of gloves: one on each hand, in
 * third person (a player render layer) and in first person (both arms are drawn with a glove instead
 * of the held item). The inventory icon stays the flat sprite; the item model hides itself in hand.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, value = Dist.CLIENT)
public final class GlovesRenderer {
    private GlovesRenderer() {
    }

    public static boolean holdsGloves(Player player) {
        return player.getMainHandItem().is(ModItems.FOCUSING_GLOVES.get()) || player.getOffhandItem().is(ModItems.FOCUSING_GLOVES.get());
    }

    private static ItemStack gloveStack(Player player) {
        return player.getMainHandItem().is(ModItems.FOCUSING_GLOVES.get()) ? player.getMainHandItem() : player.getOffhandItem();
    }

    /** Draws the glove model attached to an arm part, in that part's model space. */
    static void renderGlove(PoseStack pose, MultiBufferSource buffers, int light, ModelPart arm, ItemStack stack, WornModels.Attachment attachment) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(attachment.location());
        if (model == null || model == minecraft.getModelManager().getMissingModel()) {
            return;
        }
        pose.pushPose();
        arm.translateAndRotate(pose);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.scale(1.0F, -1.0F, -1.0F);
        minecraft.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, pose, buffers, light, OverlayTexture.NO_OVERLAY, model);
        pose.popPose();
    }

    /** Third person: a glove on each arm while the gloves are held in either hand. */
    public static class Layer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        public Layer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!holdsGloves(player) || player.isInvisible()) {
                return;
            }
            ItemStack stack = gloveStack(player);
            PlayerModel<AbstractClientPlayer> model = getParentModel();
            renderGlove(pose, buffers, light, model.rightArm, stack, WornModels.GLOVES.get(0));
            renderGlove(pose, buffers, light, model.leftArm, stack, WornModels.GLOVES.get(1));
        }
    }

    /** First person: each hand is drawn as a bare arm wearing a glove. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer player = minecraft.player;
        if (player == null || player.isInvisible() || !holdsGloves(player)) {
            return;
        }
        ItemStack other = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem();
        if (!other.isEmpty() && !other.is(ModItems.FOCUSING_GLOVES.get())) {
            return; // the other hand holds something else: draw that normally
        }
        event.setCanceled(true);
        HumanoidArm side = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        renderArm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getEquipProgress(),
                event.getHand() == InteractionHand.MAIN_HAND ? event.getSwingProgress() : 0.0F, side, player, gloveStack(player));
    }

    /** Same transforms as vanilla's first-person bare arm, then the glove on top. */
    private static void renderArm(PoseStack pose, MultiBufferSource buffers, int light, float equip, float swing, HumanoidArm side,
                                  AbstractClientPlayer player, ItemStack stack) {
        boolean right = side != HumanoidArm.LEFT;
        float f = right ? 1.0F : -1.0F;
        float f1 = Mth.sqrt(swing);
        float f2 = -0.3F * Mth.sin(f1 * (float) Math.PI);
        float f3 = 0.4F * Mth.sin(f1 * (float) (Math.PI * 2));
        float f4 = -0.4F * Mth.sin(swing * (float) Math.PI);
        pose.pushPose();
        pose.translate(f * (f2 + 0.64000005F), f3 + -0.6F + equip * -0.6F, f4 + -0.71999997F);
        pose.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
        float f5 = Mth.sin(swing * swing * (float) Math.PI);
        float f6 = Mth.sin(f1 * (float) Math.PI);
        pose.mulPose(Axis.YP.rotationDegrees(f * f6 * 70.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(f * f5 * -20.0F));
        pose.translate(f * -1.0F, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        pose.mulPose(Axis.XP.rotationDegrees(200.0F));
        pose.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        pose.translate(f * 5.6F, 0.0F, 0.0F);
        PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (right) {
            renderer.renderRightHand(pose, buffers, light, player);
            renderGlove(pose, buffers, light, renderer.getModel().rightArm, stack, WornModels.GLOVES.get(0));
        } else {
            renderer.renderLeftHand(pose, buffers, light, player);
            renderGlove(pose, buffers, light, renderer.getModel().leftArm, stack, WornModels.GLOVES.get(1));
        }
        pose.popPose();
    }
}
