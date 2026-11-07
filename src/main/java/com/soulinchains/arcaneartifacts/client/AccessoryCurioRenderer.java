package com.soulinchains.arcaneartifacts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import java.util.List;

/**
 * Draws an accessory's worn models on the wearer's body parts (see {@link WornModels} for the model
 * convention). The models are rendered as item models, so tint indexes (wreath flowers) and
 * Blockbench textures work exactly as in inventory icons. Curios' own "render" toggle per slot
 * is respected because Curios only calls this renderer for visible slots.
 */
public class AccessoryCurioRenderer implements ICurioRenderer {
    private final List<WornModels.Attachment> attachments;

    public AccessoryCurioRenderer(List<WornModels.Attachment> attachments) {
        this.attachments = attachments;
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack,
                                                                          RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light,
                                                                          float limbSwing, float limbSwingAmount, float partialTicks,
                                                                          float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(parent.getModel() instanceof HumanoidModel<?> humanoid)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ModelManager models = minecraft.getModelManager();
        BakedModel missing = models.getMissingModel();
        for (WornModels.Attachment attachment : attachments) {
            BakedModel model = models.getModel(attachment.location());
            if (model == null || model == missing) {
                continue;
            }
            ModelPart part = switch (attachment.part()) {
                case HEAD -> humanoid.head;
                case BODY -> humanoid.body;
                case RIGHT_ARM -> humanoid.rightArm;
                case LEFT_ARM -> humanoid.leftArm;
                case RIGHT_LEG -> humanoid.rightLeg;
                case LEFT_LEG -> humanoid.leftLeg;
            };
            if (!part.visible) {
                continue;
            }
            poseStack.pushPose();
            part.translateAndRotate(poseStack);
            // Item space is Y-up with north as the front; entity model space is Y-down.
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.scale(1.0F, -1.0F, -1.0F);
            minecraft.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, poseStack, buffers, light,
                    OverlayTexture.NO_OVERLAY, model);
            poseStack.popPose();
        }
    }
}
