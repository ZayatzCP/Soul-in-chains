package com.soulinchains.arcaneartifacts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.pseudo.ManaTankBlockEntity;
import com.soulinchains.arcaneartifacts.pseudo.ManaTanks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Draws the Source of a whole Mana Tank as one body of liquid. Only the tank's lowest corner block draws
 * it, over the full width and up to the level of the whole tank (stored / capacity).
 */
public class ManaTankRenderer implements BlockEntityRenderer<ManaTankBlockEntity> {
    private static final float INSET = 1.0f / 16f;

    public ManaTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ManaTankBlockEntity tank, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = tank.getLevel();
        if (level == null) {
            return;
        }
        List<BlockPos> group = ManaTanks.group(level, tank.getBlockPos());
        if (group.isEmpty() || !group.get(0).equals(tank.getBlockPos())) {
            return;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        int stored = 0;
        for (BlockPos pos : group) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
            if (level.getBlockEntity(pos) instanceof ManaTankBlockEntity part) {
                stored += part.getSource();
            }
        }
        int capacity = group.size() * ManaTankBlockEntity.capacityPerBlock();
        if (stored <= 0 || capacity <= 0) {
            return;
        }
        BlockPos origin = tank.getBlockPos();
        float x0 = minX - origin.getX() + INSET, x1 = maxX - origin.getX() + 1 - INSET;
        float z0 = minZ - origin.getZ() + INSET, z1 = maxZ - origin.getZ() + 1 - INSET;
        float y0 = INSET;
        float height = (maxY - minY + 1) - 2 * INSET;
        float y1 = y0 + height * Math.min(1f, (float) stored / capacity);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ArcaneArtifacts.id("block/mana_tank_liquid"));
        VertexConsumer buffer = buffers.getBuffer(RenderType.translucent());
        Matrix4f m = pose.last().pose();
        int bright = 0xF000F0;
        // Top surface and the four sides of the liquid body.
        face(buffer, m, sprite, bright, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0);
        face(buffer, m, sprite, bright, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1);
        face(buffer, m, sprite, bright, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1);
        face(buffer, m, sprite, bright, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0);
        face(buffer, m, sprite, bright, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0);
    }

    private static void face(VertexConsumer b, Matrix4f m, TextureAtlasSprite s, int light,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float nx, float ny, float nz) {
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        vertex(b, m, ax, ay, az, u0, v1, light, nx, ny, nz);
        vertex(b, m, bx, by, bz, u0, v0, light, nx, ny, nz);
        vertex(b, m, cx, cy, cz, u1, v0, light, nx, ny, nz);
        vertex(b, m, dx, dy, dz, u1, v1, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer b, Matrix4f m, float x, float y, float z, float u, float v, int light, float nx, float ny, float nz) {
        b.addVertex(m, x, y, z).setColor(255, 255, 255, 210).setUv(u, v).setLight(light).setNormal(nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen(ManaTankBlockEntity tank) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ManaTankBlockEntity tank) {
        BlockPos pos = tank.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + ManaTanks.MAX_WIDTH, pos.getY() + ManaTanks.MAX_HEIGHT, pos.getZ() + ManaTanks.MAX_WIDTH);
    }
}
