package com.soulinchains.arcaneartifacts.client.catto;

import com.soulinchains.arcaneartifacts.catto.Catto;
import com.soulinchains.arcaneartifacts.catto.CattoDialog;
import com.soulinchains.arcaneartifacts.catto.CattoTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A conversation with Catto: Catto on the left, its line in a speech box, the player's answers under it (also on the
 * number keys). Answers that are orders go to the server, and Catto's reply comes back as its next line.
 */
public class CattoDialogScreen extends Screen {
    private enum Node { ROOT, JOBS, COUNT, PLANS, AFTER }

    private static final int PANEL_BG = 0xE0161020;
    private static final int PANEL_EDGE = 0xFF8E62B5;
    private static final int NAME = 0xFFE8C8FF;
    private static final int TEXT = 0xFFF0F0F0;
    private static final int QUIET = 0xFFB0A0C0;

    private final int entityId;
    private CompoundTag context;
    private Node node = Node.ROOT;
    private Component speech;
    private float shown;
    /** The order waiting for an amount: mine, fetch or craft. */
    private String pending = "";
    private final List<Option> options = new ArrayList<>();

    private record Option(Component label, Runnable action) {
    }

    public CattoDialogScreen(int entityId, CompoundTag context) {
        super(Component.translatable("entity.arcane_artifacts.catto"));
        this.entityId = entityId;
        this.context = context;
        this.speech = greeting();
    }

    /** A dialog packet: opens the conversation, or continues the one on screen. */
    public static void receive(CattoDialog.DialogPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof CattoDialogScreen open && open.entityId == payload.entityId()) {
            open.context = payload.context();
            payload.say().ifPresent(line -> open.say(line, Node.AFTER));
            return;
        }
        CattoDialogScreen screen = new CattoDialogScreen(payload.entityId(), payload.context());
        payload.say().ifPresent(line -> screen.speech = line);
        minecraft.setScreen(screen);
    }

    // ------------------------------------------------------------------ helpers

    private String nature() {
        return context.getString("Nature").toLowerCase(Locale.ROOT);
    }

    private Component line(String key, Object... args) {
        return Component.translatable("arcane_artifacts.catto.dialog." + key + "." + nature(), args);
    }

    private static Component option(String key, Object... args) {
        return Component.translatable("arcane_artifacts.catto.dialog.opt." + key, args);
    }

    private Component greeting() {
        int variant = (int) (Math.abs(entityId * 31L + System.currentTimeMillis() / 60000L) % 3);
        return line("greet" + variant);
    }

    private void say(Component text, Node next) {
        speech = text;
        shown = 0;
        node = next;
        rebuild();
    }

    private void go(Node next, Component text) {
        say(text, next);
    }

    private void send(String command, int count, long pos) {
        PacketDistributor.sendToServer(new CattoDialog.CommandPayload(entityId, command, count, pos));
    }

    private long lookPos() {
        return context.getLong("LookPos");
    }

    private ItemStack offhand() {
        if (!context.contains("Offhand") || minecraft == null || minecraft.level == null) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parseOptional(minecraft.level.registryAccess(), context.getCompound("Offhand"));
    }

    private Component lookBlockName() {
        ResourceLocation id = ResourceLocation.tryParse(context.getString("LookBlock"));
        return id == null ? Component.literal("?") : BuiltInRegistries.BLOCK.get(id).getName();
    }

    // ------------------------------------------------------------------ the conversation

    private void fillOptions() {
        options.clear();
        switch (node) {
            case ROOT, AFTER -> {
                if (node == Node.AFTER) {
                    options.add(new Option(option("more"), () -> go(Node.ROOT, line("more"))));
                }
                options.add(new Option(option("job"), () -> go(Node.JOBS, line("jobs"))));
                options.add(new Option(option("plans"), () -> go(Node.PLANS, plans())));
                options.add(new Option(option("gear"), () -> {
                    send("inventory", 0, 0);
                    onClose();
                }));
                options.add(new Option(option("give"), () -> send("give_all", 0, 0)));
                if (context.getBoolean("Stay")) {
                    options.add(new Option(option("follow"), () -> send("follow", 0, 0)));
                } else {
                    options.add(new Option(option("stay"), () -> send("stay", 0, 0)));
                }
                options.add(new Option(option(node == Node.AFTER ? "thanks" : "bye"), this::onClose));
            }
            case JOBS -> {
                if (context.contains("LookBlock")) {
                    options.add(new Option(option("mine", lookBlockName()), () -> {
                        pending = "mine";
                        go(Node.COUNT, line("how_many"));
                    }));
                }
                options.add(new Option(option("collect"), () -> send("collect", 0, lookPos())));
                ItemStack sample = offhand();
                if (!sample.isEmpty()) {
                    options.add(new Option(option("fetch", sample.getHoverName()), () -> {
                        pending = "fetch";
                        go(Node.COUNT, line("how_many"));
                    }));
                    options.add(new Option(option("craft", sample.getHoverName()), () -> {
                        pending = "craft";
                        go(Node.COUNT, line("how_many"));
                    }));
                } else {
                    options.add(new Option(option("fetch_hint"), () -> go(Node.JOBS, line("hint_sample"))));
                }
                options.add(new Option(option("explore"), () -> send("explore", 0, lookPos())));
                options.add(new Option(option("build"), () -> go(Node.JOBS, line("hint_build"))));
                options.add(new Option(option("back"), () -> go(Node.ROOT, line("more"))));
            }
            case COUNT -> {
                for (int n : new int[]{8, 16, 32, 64}) {
                    options.add(new Option(option("count", n), () -> send(pending, n, lookPos())));
                }
                ItemStack sample = offhand();
                if (!"mine".equals(pending) && !sample.isEmpty()) {
                    options.add(new Option(option("held", sample.getCount()), () -> send(pending, 0, lookPos())));
                }
                options.add(new Option(option("back"), () -> go(Node.JOBS, line("jobs"))));
            }
            case PLANS -> {
                if (context.getInt("QueueSize") > 0) {
                    options.add(new Option(option("cancel_first"), () -> send("cancel_first", 0, 0)));
                    options.add(new Option(option("clear"), () -> send("clear", 0, 0)));
                }
                if (context.getBoolean("Paused")) {
                    options.add(new Option(option("resume"), () -> send("resume", 0, 0)));
                } else if (context.getInt("QueueSize") > 0) {
                    options.add(new Option(option("pause"), () -> send("pause", 0, 0)));
                }
                options.add(new Option(option("back"), () -> go(Node.ROOT, line("more"))));
            }
        }
    }

    /** What Catto is busy with, in its own words. */
    private Component plans() {
        List<CattoTask> tasks = new ArrayList<>();
        if (minecraft != null && minecraft.level != null) {
            for (Tag t : context.getList("Queue", Tag.TAG_COMPOUND)) {
                CattoTask task = CattoTask.load((CompoundTag) t, minecraft.level.registryAccess());
                if (task != null) {
                    tasks.add(task);
                }
            }
        }
        Component mood = Component.translatable("arcane_artifacts.catto.dialog.mood", context.getInt("Bond"),
                context.getInt("Source"), context.getInt("MaxSource"));
        if (tasks.isEmpty()) {
            return line("plans_none").copy().append(" ").append(mood);
        }
        Component now = line("plans_now", tasks.get(0).describe());
        var text = now.copy();
        if (tasks.size() > 1) {
            text.append(" ").append(Component.translatable("arcane_artifacts.catto.dialog.plans_next", tasks.get(1).describe(),
                    context.getInt("QueueSize") - 1));
        }
        if (context.getBoolean("Paused")) {
            text.append(" ").append(Component.translatable("arcane_artifacts.catto.dialog.plans_paused"));
        }
        return text.append(" ").append(mood);
    }

    // ------------------------------------------------------------------ layout

    private int panelW() {
        return Math.min(440, width - 20);
    }

    private int panelX() {
        return (width - panelW()) / 2;
    }

    private int textW() {
        return panelW() - 130;
    }

    private int speechTop() {
        return Math.max(10, height / 2 - 100);
    }

    private int speechHeight() {
        return Math.max(40, font.split(speech, textW() - 12).size() * 10 + 26);
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        fillOptions();
        int x = panelX() + 124;
        int y = speechTop() + speechHeight() + 6;
        for (int i = 0; i < options.size(); i++) {
            Option opt = options.get(i);
            Component label = Component.literal((i + 1) + ". ").append(opt.label());
            String text = font.plainSubstrByWidth(label.getString(), textW() - 12);
            addRenderableWidget(Button.builder(Component.literal(text), b -> opt.action().run())
                    .bounds(x, y + i * 21, textW(), 19).build());
        }
    }

    @Override
    public void tick() {
        shown += 3.0F;
        Catto cat = cat();
        if (cat == null || !cat.isAlive() || minecraft != null && minecraft.player != null && cat.distanceToSqr(minecraft.player) > 24 * 24) {
            onClose();
        }
    }

    private Catto cat() {
        return minecraft != null && minecraft.level != null && minecraft.level.getEntity(entityId) instanceof Catto cat ? cat : null;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            int index = key - GLFW.GLFW_KEY_1;
            if (index < options.size()) {
                options.get(index).action().run();
                return true;
            }
        }
        if (key == GLFW.GLFW_KEY_SPACE) {
            shown = Float.MAX_VALUE;
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int px = panelX();
        int top = speechTop();
        // Catto, big, on the left.
        int boxH = Math.max(150, speechHeight() + 6 + options.size() * 21);
        g.fill(px, top, px + 116, top + boxH, PANEL_BG);
        g.renderOutline(px, top, 116, boxH, PANEL_EDGE);
        Catto cat = cat();
        if (cat != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, px + 4, top + 4, px + 112, top + boxH - 4, 52, 0.0625F, mouseX, mouseY, cat);
        }
        // The speech box with Catto's name and nature.
        int sx = px + 124;
        int sw = textW();
        int sh = speechHeight();
        g.fill(sx, top, sx + sw, top + sh, PANEL_BG);
        g.renderOutline(sx, top, sw, sh, PANEL_EDGE);
        Component name = cat != null ? cat.getDisplayName() : title;
        g.drawString(font, name, sx + 6, top + 6, NAME, false);
        Component nature = Component.translatable("arcane_artifacts.catto.nature." + nature());
        g.drawString(font, nature, sx + sw - 6 - font.width(nature), top + 6, QUIET, false);
        String full = speech.getString();
        String visible = full.substring(0, (int) Math.min(full.length(), shown));
        int y = top + 20;
        for (FormattedCharSequence line : font.split(Component.literal(visible), sw - 12)) {
            g.drawString(font, line, sx + 6, y, TEXT, false);
            y += 10;
        }
        // A little tail of the speech bubble pointing at Catto.
        g.fill(sx - 6, top + 14, sx, top + 16, PANEL_EDGE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @SuppressWarnings("unused")
    private static BlockPos at(long pos) {
        return BlockPos.of(pos);
    }
}
