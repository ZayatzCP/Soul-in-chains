package com.soulinchains.arcaneartifacts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.soulinchains.arcaneartifacts.pseudo.ManaTankBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;

/** Draws the Source inside a Mana Tank block as a glowing purple column, as high as the block is full. */
public class ManaTankRenderer implements BlockEntityRenderer<ManaTankBlockEntity> {
    private static final float INSET = 1.5f / 16f;

    public ManaTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ManaTankBlockEntity tank, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float fill = tank.fill();
        if (fill <= 0.001f) {
            return;
        }
        boolean above = tank.getLevel() != null && tank.getLevel().getBlockEntity(tank.getBlockPos().above()) instanceof ManaTankBlockEntity up && up.getSource() > 0;
        float bottom = tank.getBlockState().getValue(com.soulinchains.arcaneartifacts.pseudo.ManaTankBlock.DOWN) ? 0f : INSET;
        float top = above ? 1f : bottom + (1f - INSET - bottom) * fill;
        VertexConsumer buffer = buffers.getBuffer(RenderType.debugQuads());
        Matrix4f matrix = pose.last().pose();
        float min = INSET, max = 1f - INSET;
        int r = 180, gr = 76, b = 240, a = 170;
        // Four sides and the top surface.
        quad(buffer, matrix, min, bottom, min, max, top, min, r, gr, b, a);
        quad(buffer, matrix, max, bottom, max, min, top, max, r, gr, b, a);
        quadX(buffer, matrix, min, bottom, max, min, top, min, r, gr, b, a);
        quadX(buffer, matrix, max, bottom, min, max, top, max, r, gr, b, a);
        buffer.addVertex(matrix, min, top, min).setColor(r + 30, gr + 60, b, a);
        buffer.addVertex(matrix, min, top, max).setColor(r + 30, gr + 60, b, a);
        buffer.addVertex(matrix, max, top, max).setColor(r + 30, gr + 60, b, a);
        buffer.addVertex(matrix, max, top, min).setColor(r + 30, gr + 60, b, a);
    }

    private static void quad(VertexConsumer buffer, Matrix4f m, float x0, float y0, float z, float x1, float y1, float zEnd, int r, int g, int b, int a) {
        buffer.addVertex(m, x0, y0, z).setColor(r, g, b, a);
        buffer.addVertex(m, x0, y1, z).setColor(r, g, b, a);
        buffer.addVertex(m, x1, y1, zEnd).setColor(r, g, b, a);
        buffer.addVertex(m, x1, y0, zEnd).setColor(r, g, b, a);
    }

    private static void quadX(VertexConsumer buffer, Matrix4f m, float x, float y0, float z0, float xEnd, float y1, float z1, int r, int g, int b, int a) {
        buffer.addVertex(m, x, y0, z0).setColor(r, g, b, a);
        buffer.addVertex(m, x, y1, z0).setColor(r, g, b, a);
        buffer.addVertex(m, xEnd, y1, z1).setColor(r, g, b, a);
        buffer.addVertex(m, xEnd, y0, z1).setColor(r, g, b, a);
    }
}
