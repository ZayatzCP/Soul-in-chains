package com.soulinchains.arcaneartifacts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.soulinchains.arcaneartifacts.block.ResearchTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Shows the observed sample slowly turning above the open book. */
public class ResearchTableRenderer implements BlockEntityRenderer<ResearchTableBlockEntity> {
    public ResearchTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ResearchTableBlockEntity table, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = table.inventory().getStackInSlot(0);
        if (stack.isEmpty() || table.getLevel() == null) {
            return;
        }
        float time = table.getLevel().getGameTime() + partialTick;
        pose.pushPose();
        pose.translate(0.5, 1.25 + Math.sin(time / 12.0) * 0.05, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time * 2f));
        pose.scale(0.6f, 0.6f, 0.6f);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, light, overlay, pose, buffers, table.getLevel(), 0);
        pose.popPose();
    }
}
