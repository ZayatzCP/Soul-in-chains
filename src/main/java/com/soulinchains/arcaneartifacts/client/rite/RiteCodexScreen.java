package com.soulinchains.arcaneartifacts.client.rite;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.rite.ChalkColor;
import com.soulinchains.arcaneartifacts.rite.ChalkMarkBlock;
import com.soulinchains.arcaneartifacts.rite.RiteCatalog;
import com.soulinchains.arcaneartifacts.rite.Sigil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Treatise of Sigils, an open book written in blood: a title page, an isometric drawing of a finished site, how to
 * build it and hold a rite, the roles and the rewards, then one page per sigil and one per rite the server knows - its
 * four sigils around the altar (the pedestal with the first offering on it), the field, the offerings, the cage, the gift.
 */
public class RiteCodexScreen extends Screen {
    private static final int PW = 150;
    private static final int PH = 196;
    private static final int GUTTER = 14;
    private static final int MARGIN = 10;
    private static final int BW = PW * 2 + GUTTER + MARGIN * 2;
    private static final int BH = PH + 24;
    private static final int COVER = 0xFF2A0C08;
    private static final int COVER_EDGE = 0xFF140504;
    private static final int PAPER = 0xFFD6C29C;
    private static final int PAPER_SHADE = 0xFFC2AA7E;
    private static final int INK = 0xFF4A0606;
    private static final int MUTED = 0xFF6B4A35;
    private static final int TITLE = 0xFF8E1212;
    private static final int BLOOD = 0xFF7A0C0C;
    private static final int BLOOD_DARK = 0xFF3F0505;
    private static final int GRID = 0xFFC9B289;

    private interface Page {
        void draw(GuiGraphics g, int x, int y);
    }

    private final List<Page> pages = new ArrayList<>();
    private int spread;
    private Button prev;
    private Button next;

    public RiteCodexScreen() {
        super(Component.translatable("item.arcane_artifacts.sigil_codex"));
    }

    @Override
    protected void init() {
        pages.clear();
        pages.add(this::titlePage);
        pages.add((g, x, y) -> {
            heading(g, x, y, Component.translatable("arcane_artifacts.codex.picture.title"));
            drawSite(g, x + PW / 2, y + 92, 7.2F, siteSigils());
            wrapped(g, Component.translatable("arcane_artifacts.codex.picture.text"), x, y + 148, PW, MUTED);
        });
        for (String key : new String[]{"site", "rite", "roles", "reward"}) {
            pages.add((g, x, y) -> text(g, x, y, "arcane_artifacts.codex." + key));
        }
        for (Sigil sigil : Sigil.values()) {
            pages.add((g, x, y) -> sigil(g, x, y, sigil));
        }
        List<RiteCatalog.Entry> rites = RiteCatalog.client;
        if (rites.isEmpty()) {
            pages.add((g, x, y) -> text(g, x, y, "arcane_artifacts.codex.no_rites"));
        }
        for (RiteCatalog.Entry rite : rites) {
            pages.add((g, x, y) -> rite(g, x, y, rite));
        }
        if (pages.size() % 2 == 1) {
            pages.add((g, x, y) -> { });
        }
        int left = (width - BW) / 2;
        int top = (height - BH) / 2;
        prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(left + 4, top + BH - 22, 18, 16).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(left + BW - 22, top + BH - 22, 18, 16).build());
        turn(0);
    }

    private void turn(int delta) {
        int spreads = pages.size() / 2;
        spread = Math.max(0, Math.min(spreads - 1, spread + delta));
        prev.active = spread > 0;
        next.active = spread < spreads - 1;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        turn(dy > 0 ? -1 : 1);
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        int left = (width - BW) / 2;
        int top = (height - BH) / 2;
        int lx = left + MARGIN;
        int rx = lx + PW + GUTTER;
        int y = top + 12;
        pages.get(spread * 2).draw(g, lx + 4, y + 4);
        pages.get(spread * 2 + 1).draw(g, rx + 4, y + 4);
        String a = String.valueOf(spread * 2 + 1);
        String b = String.valueOf(spread * 2 + 2);
        g.drawString(font, a, lx + PW / 2 - font.width(a) / 2, top + BH - 16, MUTED, false);
        g.drawString(font, b, rx + PW / 2 - font.width(b) / 2, top + BH - 16, MUTED, false);
    }

    /** The open book under everything: leather, two parchment pages, the gutter, worn edges and dried blood. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        int left = (width - BW) / 2;
        int top = (height - BH) / 2;
        g.fill(left - 3, top - 3, left + BW + 3, top + BH + 3, COVER_EDGE);
        g.fill(left, top, left + BW, top + BH, COVER);
        int lx = left + MARGIN;
        int rx = lx + PW + GUTTER;
        int y = top + 10;
        for (int px : new int[]{lx, rx}) {
            g.fill(px, y, px + PW + 8, y + PH + 4, PAPER);
            g.fill(px, y, px + PW + 8, y + 2, PAPER_SHADE);
            g.fill(px, y + PH + 2, px + PW + 8, y + PH + 4, PAPER_SHADE);
        }
        // the gutter: pages bend down into the spine
        for (int i = 0; i < 6; i++) {
            int shade = 0xFF000000 | (Math.max(0x60, 0xC2 - i * 18) << 16) | (Math.max(0x40, 0xAA - i * 18) << 8) | Math.max(0x30, 0x7E - i * 14);
            g.fill(lx + PW + 8 - 6 + i, y, lx + PW + 8 - 5 + i, y + PH + 4, shade);
            g.fill(rx + 5 - i, y, rx + 6 - i, y + PH + 4, shade);
        }
        g.fill(lx + PW + 8, y - 2, rx, y + PH + 6, COVER_EDGE);
        // stains and drips
        int[][] stains = {{lx + 20, y + 150, 14}, {rx + 118, y + 30, 10}, {rx + 30, y + 176, 8}};
        for (int[] s : stains) {
            g.fill(s[0], s[1], s[0] + s[2], s[1] + s[2] / 2, 0x40701010);
        }
        int[][] drips = {{lx + 26, 12}, {lx + 88, 6}, {lx + 131, 17}, {rx + 14, 9}, {rx + 71, 20}, {rx + 140, 7}};
        for (int[] d : drips) {
            g.fill(d[0], y, d[0] + 2, y + d[1], BLOOD);
            g.fill(d[0] - 1, y + d[1] - 1, d[0] + 3, y + d[1] + 2, BLOOD_DARK);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ text

    private int wrapped(GuiGraphics g, Component text, int x, int y, int width, int color) {
        for (FormattedCharSequence line : font.split(text, width)) {
            g.drawString(font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private void heading(GuiGraphics g, int x, int y, Component title) {
        g.drawString(font, title, x + PW / 2 - font.width(title) / 2, y, TITLE, false);
        g.fill(x + 20, y + 11, x + PW - 20, y + 12, BLOOD);
    }

    private void text(GuiGraphics g, int x, int y, String key) {
        heading(g, x, y, Component.translatable(key + ".title"));
        wrapped(g, Component.translatable(key + ".text"), x, y + 18, PW, INK);
    }

    private void titlePage(GuiGraphics g, int x, int y) {
        Component title = Component.translatable("item.arcane_artifacts.sigil_codex");
        g.pose().pushPose();
        g.pose().translate(x + PW / 2F, y + 20, 0);
        g.pose().scale(1.5F, 1.5F, 1F);
        g.drawString(font, title, -font.width(title) / 2, 0, TITLE, false);
        g.pose().popPose();
        grid(g, x + PW / 2 - 30, y + 50, Sigil.PECHAT, 20);
        wrapped(g, Component.translatable("arcane_artifacts.codex.epigraph"), x + 6, y + 122, PW - 12, MUTED);
    }

    // ------------------------------------------------------------------ sigils

    /** A sigil drawn in blood on its grid; {@code cell} pixels a block. Every line is a little uneven, as if by finger. */
    private void grid(GuiGraphics g, int x, int y, Sigil sigil, int cell) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                g.fill(x + c * cell, y + r * cell, x + (c + 1) * cell - 1, y + (r + 1) * cell - 1, GRID);
            }
        }
        int t = Math.max(1, cell / 6);
        for (int[] line : sigil.lines) {
            int ax = x + (line[0] % 3) * cell + cell / 2;
            int ay = y + (line[0] / 3) * cell + cell / 2;
            int bx = x + (line[1] % 3) * cell + cell / 2;
            int by = y + (line[1] / 3) * cell + cell / 2;
            g.fill(Math.min(ax, bx) - t, Math.min(ay, by) - t, Math.max(ax, bx) + t, Math.max(ay, by) + t, BLOOD);
            int wobble = ((line[0] * 7 + line[1] * 3) % 3) - 1;
            g.fill(Math.min(ax, bx) - t + wobble, Math.min(ay, by) - t + 1, Math.max(ax, bx) - t + wobble + 1, Math.max(ay, by) + t - 1, BLOOD_DARK);
        }
        for (int[] line : sigil.lines) {
            for (int cellIndex : line) {
                int cx = x + (cellIndex % 3) * cell + cell / 2;
                int cy = y + (cellIndex / 3) * cell + cell / 2;
                g.fill(cx - t * 2, cy - t * 2, cx + t * 2, cy + t * 2, BLOOD_DARK);
            }
        }
        int low = 0;
        for (int[] line : sigil.lines) {
            low = Math.max(low, Math.max(line[0], line[1]));
        }
        int dx = x + (low % 3) * cell + cell / 2;
        int dy = y + (low / 3) * cell + cell / 2;
        g.fill(dx, dy, dx + Math.max(1, t), dy + cell / 2 + 2, BLOOD);
    }

    private void sigil(GuiGraphics g, int x, int y, Sigil sigil) {
        heading(g, x, y, sigil.displayName());
        grid(g, x + PW / 2 - 30, y + 18, sigil, 20);
        int ty = y + 84;
        g.drawString(font, Component.translatable("arcane_artifacts.codex.chalk", Component.translatable("arcane_artifacts.rite.chalk." + sigil.color.id)),
                x, ty, MUTED, false);
        g.drawString(font, Component.translatable(sigil.modifier ? "arcane_artifacts.codex.modifier" : "arcane_artifacts.codex.base"), x, ty + 10, MUTED, false);
        wrapped(g, Component.translatable("arcane_artifacts.rite.sigil." + sigil.id + ".desc"), x, ty + 24, PW, INK);
    }

    // ------------------------------------------------------------------ rites

    private void rite(GuiGraphics g, int x, int y, RiteCatalog.Entry rite) {
        heading(g, x, y, Component.translatable("arcane_artifacts.rite.name." + rite.id()));
        g.drawString(font, Component.translatable("arcane_artifacts.codex.kind." + rite.kind()), x, y + 15, MUTED, false);
        // the five places around the altar: main north, sides; the altar is the pedestal with the first offering
        int cell = 6;
        int place = cell * 3;
        int ox = x + 4;
        int oy = y + 28;
        int cx = ox + place + 2;
        int cy = oy + place + 2;
        small(g, rite.main(), cx, oy, cell);
        small(g, rite.west(), ox, cy, cell);
        small(g, rite.east(), cx + place + 2, cy, cell);
        small(g, rite.south(), cx, cy + place + 2, cell);
        g.renderItem(new ItemStack(ModItems.OFFERING_PEDESTAL.get()), cx + 1, cy + 1);
        g.drawString(font, Component.translatable("arcane_artifacts.codex.north"), cx + place / 2 - 2, oy - 9, MUTED, false);
        int tx = ox + place * 3 + 10;
        int ty = oy;
        int participants = rite.minField() <= 9 ? 2 : rite.minField() <= 15 ? 3 : 4;
        ty = wrapped(g, Component.translatable("arcane_artifacts.codex.field", rite.minField(), rite.minField(), participants), tx, ty, PW - (tx - x), INK);
        if (rite.cage() > 0) {
            ty = wrapped(g, Component.translatable("arcane_artifacts.codex.cage", rite.cage()), tx, ty + 2, PW - (tx - x), INK);
        }
        ty = Math.max(ty, cy + place * 2 + 6);
        ty = wrapped(g, Component.translatable("arcane_artifacts.codex.sigils", name(rite.main()), name(rite.south()), name(rite.west()), name(rite.east())),
                x, ty, PW, MUTED);
        g.drawString(font, Component.translatable("arcane_artifacts.codex.offering"), x, ty + 2, INK, false);
        ty += 12;
        ty = item(g, x, ty, rite.offering(), rite.offeringCount());
        int[] extra = rite.extra();
        for (int i = 0; i + 1 < extra.length; i += 2) {
            ty = item(g, x, ty, extra[i], extra[i + 1]);
        }
        wrapped(g, Component.translatable("arcane_artifacts.codex.result." + rite.id()), x, ty + 2, PW, rite.otherworldly() ? TITLE : MUTED);
    }

    private static Component name(String sigil) {
        return Component.translatable("arcane_artifacts.rite.sigil." + sigil);
    }

    private void small(GuiGraphics g, String id, int x, int y, int cell) {
        Sigil sigil = Sigil.byId(id);
        if (sigil != null) {
            grid(g, x, y, sigil, cell);
        }
    }

    private int item(GuiGraphics g, int x, int y, int itemId, int count) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.byId(itemId), Math.max(1, count));
        g.renderItem(stack, x, y);
        g.drawString(font, Component.literal(count + " × ").append(stack.getHoverName()), x + 18, y + 4, INK, false);
        return y + 17;
    }

    // ------------------------------------------------------------------ the picture

    /** The sigils of the drawing: those of the first summoning the server knows, else Gate, Fence and Bonds. */
    private Map<String, Sigil> siteSigils() {
        Map<String, Sigil> map = new LinkedHashMap<>();
        RiteCatalog.Entry entry = RiteCatalog.client.stream().filter(e -> e.kind().equals("summon")).findFirst().orElse(null);
        map.put("main", entry == null ? Sigil.VRATA : Sigil.byId(entry.main()));
        map.put("south", entry == null ? Sigil.OGRADA : Sigil.byId(entry.south()));
        map.put("west", entry == null ? Sigil.UZY : Sigil.byId(entry.west()));
        map.put("east", entry == null ? Sigil.UZY : Sigil.byId(entry.east()));
        return map;
    }

    private static void sigilBlocks(Map<BlockPos, BlockState> scene, int cornerX, int cornerZ, Sigil sigil) {
        if (sigil == null) {
            return;
        }
        ChalkColor color = sigil.color;
        Map<Integer, BlockState> marks = new LinkedHashMap<>();
        for (int[] line : sigil.lines) {
            for (int cell : line) {
                marks.putIfAbsent(cell, ModBlocks.CHALK_MARK.get().defaultBlockState().setValue(ChalkMarkBlock.COLOR, color));
            }
        }
        for (int[] line : sigil.lines) {
            int a = line[0];
            int b = line[1];
            Direction ab = b - a == 1 ? Direction.EAST : b - a == -1 ? Direction.WEST : b - a == 3 ? Direction.SOUTH : Direction.NORTH;
            marks.put(a, marks.get(a).setValue(ChalkMarkBlock.side(ab), true));
            marks.put(b, marks.get(b).setValue(ChalkMarkBlock.side(ab.getOpposite()), true));
        }
        marks.forEach((cell, state) -> scene.put(new BlockPos(cornerX + cell % 3, 1, cornerZ + cell / 3), state));
    }

    /** An isometric drawing of a finished 9x9 site: floor, wall, lanterns, candles, boundary, sigils, Heart, pedestal, censer. */
    private void drawSite(GuiGraphics g, int centerX, int centerY, float scale, Map<String, Sigil> sigils) {
        Map<BlockPos, BlockState> scene = new LinkedHashMap<>();
        for (int x = -1; x <= 11; x++) {
            for (int z = -1; z <= 11; z++) {
                boolean wall = x == -1 || x == 11 || z == -1 || z == 11;
                scene.put(new BlockPos(x, 0, z), (wall ? Blocks.DEEPSLATE_BRICKS : Blocks.DEEPSLATE_TILES).defaultBlockState());
                if (wall && (x + z) % 2 == 0) {
                    scene.put(new BlockPos(x, 1, z), Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState());
                }
                if (!wall && (x == 0 || x == 10 || z == 0 || z == 10)) {
                    scene.put(new BlockPos(x, 1, z), ModBlocks.CHALK_MARK.get().defaultBlockState().setValue(ChalkMarkBlock.COLOR, ChalkColor.AMETHYST));
                }
            }
        }
        for (int[] c : new int[][]{{-1, -1}, {-1, 11}, {11, -1}, {11, 11}}) {
            scene.put(new BlockPos(c[0], 1, c[1]), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            scene.put(new BlockPos(c[0], 2, c[1]), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        }
        for (int[] c : new int[][]{{2, 2}, {8, 2}, {2, 8}, {8, 8}}) {
            scene.put(new BlockPos(c[0], 1, c[1]), Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        }
        sigilBlocks(scene, 4, 1, sigils.get("main"));
        sigilBlocks(scene, 4, 7, sigils.get("south"));
        sigilBlocks(scene, 1, 4, sigils.get("west"));
        sigilBlocks(scene, 7, 4, sigils.get("east"));
        scene.put(new BlockPos(5, 0, 5), ModBlocks.RITE_HEART.get().defaultBlockState());
        scene.put(new BlockPos(5, 1, 5), ModBlocks.OFFERING_PEDESTAL.get().defaultBlockState());
        scene.put(new BlockPos(5, 3, 5), ModBlocks.RITUAL_CENSER.get().defaultBlockState().setValue(LanternBlock.HANGING, true));
        PoseStack pose = g.pose();
        pose.pushPose();
        try {
            pose.translate(centerX, centerY, 150);
            pose.scale(scale, -scale, scale);
            pose.mulPose(Axis.XP.rotationDegrees(30));
            pose.mulPose(Axis.YP.rotationDegrees(225));
            pose.translate(-5.5, 0, -5.5);
            Lighting.setupFor3DItems();
            var dispatcher = Minecraft.getInstance().getBlockRenderer();
            for (Map.Entry<BlockPos, BlockState> e : scene.entrySet()) {
                pose.pushPose();
                pose.translate(e.getKey().getX(), e.getKey().getY(), e.getKey().getZ());
                dispatcher.renderSingleBlock(e.getValue(), pose, g.bufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            g.flush();
        } catch (RuntimeException ex) {
            ArcaneArtifacts.LOGGER.warn("Treatise of Sigils: could not draw the site", ex);
        } finally {
            pose.popPose();
        }
    }
}
