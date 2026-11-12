package com.soulinchains.arcaneartifacts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.pseudo.ManaTankBlock;
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
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Draws the Source of a whole Mana Tank as one body of liquid: the level is that of the whole tank
 * (stored / capacity over all its layers), and each block draws its own part of it, without the faces
 * that touch another tank block, so the liquid fills any shape of tank seamlessly.
 */
public class ManaTankRenderer implements BlockEntityRenderer<ManaTankBlockEntity> {
    private static final float EDGE = 0.02f;

    public ManaTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ManaTankBlockEntity tank, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = tank.getLevel();
        if (level == null) {
            return;
        }
        List<BlockPos> group = ManaTanks.group(level, tank.getBlockPos());
        if (group.isEmpty()) {
            return;
        }
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int stored = 0;
        for (BlockPos pos : group) {
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
            if (level.getBlockEntity(pos) instanceof ManaTankBlockEntity part) {
                stored += part.getSource();
            }
        }
        int capacity = group.size() * ManaTankBlockEntity.capacityPerBlock();
        if (stored <= 0 || capacity <= 0) {
            return;
        }
        float surface = (maxY - minY + 1) * Math.min(1f, (float) stored / capacity);
        float fill = Math.min(1f, surface - (tank.getBlockPos().getY() - minY));
        if (fill <= 0.005f) {
            return;
        }
        BlockState state = tank.getBlockState();
        boolean up = state.getValue(ManaTankBlock.UP), down = state.getValue(ManaTankBlock.DOWN);
        boolean north = state.getValue(ManaTankBlock.NORTH), south = state.getValue(ManaTankBlock.SOUTH);
        boolean west = state.getValue(ManaTankBlock.WEST), east = state.getValue(ManaTankBlock.EAST);
        float x0 = west ? 0 : EDGE, x1 = east ? 1 : 1 - EDGE;
        float z0 = north ? 0 : EDGE, z1 = south ? 1 : 1 - EDGE;
        float y0 = down ? 0 : EDGE;
        boolean full = fill >= 0.999f && up;
        float y1 = full ? 1 : Math.min(fill, 1 - EDGE);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ArcaneArtifacts.id("block/mana_tank_liquid"));
        VertexConsumer buffer = buffers.getBuffer(RenderType.translucent());
        Matrix4f m = pose.last().pose();
        int bright = 0xF000F0;
        if (!full) {
            face(buffer, m, sprite, bright, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0);
        }
        if (!north) {
            face(buffer, m, sprite, bright, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1);
        }
        if (!south) {
            face(buffer, m, sprite, bright, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1);
        }
        if (!west) {
            face(buffer, m, sprite, bright, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0);
        }
        if (!east) {
            face(buffer, m, sprite, bright, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0);
        }
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
}
