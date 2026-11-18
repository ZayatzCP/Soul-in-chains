package com.soulinchains.arcaneartifacts.client.catto;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.catto.Catto;
import com.soulinchains.arcaneartifacts.catto.CattoDialog;
import com.soulinchains.arcaneartifacts.catto.CattoTask;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A conversation with Catto, filmed like a cutscene: the camera swings behind the player's shoulder to frame the
 * player and Catto, black bars close in from above and below, Catto's line runs along the bottom bar and the player's
 * answers line up on the right (also on the number keys). When the conversation ends, the camera goes back.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, value = Dist.CLIENT)
public class CattoDialogScreen extends Screen {
    private enum Node { ROOT, JOBS, COUNT, PLANS, GUARD, AFTER }

    private static final int BAR = 0xFF000000;
    private static final int NAME = 0xFFE8C8FF;
    private static final int TEXT = 0xFFF0F0F0;
    private static final int QUIET = 0xFFB0A0C0;
    private static final int OPTION_BG = 0xA0100818;
    private static final int OPTION_EDGE = 0xFF8E62B5;

    private final int entityId;
    private CompoundTag context;
    private Node node = Node.ROOT;
    private Component speech;
    private float shown;
    private String pending = "";
    private final List<Option> options = new ArrayList<>();
    /** 0..1: how far the cutscene framing has come in (bars, camera). */
    private float intro;
    private CameraType previousCamera;
    private boolean previousHideGui;
    private boolean cinematic;

    private record Option(Component label, Runnable action) {
    }

    public CattoDialogScreen(int entityId, CompoundTag context) {
        super(Component.translatable("entity.arcane_artifacts.catto"));
        this.entityId = entityId;
        this.context = context;
        this.speech = greeting();
    }

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

    /** The camera stays close over the shoulder while Catto is talked to. */
    @SubscribeEvent
    public static void cameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (Minecraft.getInstance().screen instanceof CattoDialogScreen dialog) {
            event.setDistance(Mth.lerp(dialog.intro, event.getDistance(), 2.6F));
        }
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

    private void send(String command, int count) {
        PacketDistributor.sendToServer(new CattoDialog.CommandPayload(entityId, command, count, context.getLong("Here")));
    }

    private ItemStack offhand() {
        if (!context.contains("Offhand") || minecraft == null || minecraft.level == null) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parseOptional(minecraft.level.registryAccess(), context.getCompound("Offhand"));
    }

    private Catto cat() {
        return minecraft != null && minecraft.level != null && minecraft.level.getEntity(entityId) instanceof Catto cat ? cat : null;
    }

    // ------------------------------------------------------------------ the conversation

    private void fillOptions() {
        options.clear();
        ItemStack sample = offhand();
        switch (node) {
            case ROOT, AFTER -> {
                if (node == Node.AFTER) {
                    options.add(new Option(option("more"), () -> say(line("more"), Node.ROOT)));
                }
                options.add(new Option(option("job"), () -> say(line("jobs"), Node.JOBS)));
                options.add(new Option(option("guard_menu"), () -> say(line("guard_ask"), Node.GUARD)));
                options.add(new Option(option("plans"), () -> say(plans(), Node.PLANS)));
                options.add(new Option(option("gear"), () -> {
                    send("inventory", 0);
                    onClose();
                }));
                options.add(new Option(option("give"), () -> send("give_all", 0)));
                options.add(new Option(context.getBoolean("Stay") ? option("follow") : option("stay"),
                        () -> send(context.getBoolean("Stay") ? "follow" : "stay", 0)));
                options.add(new Option(option(node == Node.AFTER ? "thanks" : "bye"), this::onClose));
            }
            case JOBS -> {
                if (context.getBoolean("OffhandBlock")) {
                    options.add(new Option(option("mine", sample.getHoverName()), () -> {
                        pending = "mine";
                        say(line("how_many"), Node.COUNT);
                    }));
                }
                if (!sample.isEmpty()) {
                    options.add(new Option(option("fetch", sample.getHoverName()), () -> {
                        pending = "fetch";
                        say(line("how_many"), Node.COUNT);
                    }));
                    options.add(new Option(option("craft", sample.getHoverName()), () -> {
                        pending = "craft";
                        say(line("how_many"), Node.COUNT);
                    }));
                } else {
                    options.add(new Option(option("fetch_hint"), () -> say(line("hint_sample"), Node.JOBS)));
                }
                options.add(new Option(option("collect"), () -> send("collect", 0)));
                options.add(new Option(option("explore"), () -> send("explore", 0)));
                options.add(new Option(option("build"), () -> say(line("hint_build"), Node.JOBS)));
                options.add(new Option(option("back"), () -> say(line("more"), Node.ROOT)));
            }
            case COUNT -> {
                for (int n : new int[]{8, 16, 32, 64}) {
                    options.add(new Option(option("count", n), () -> send(pending, n)));
                }
                if (!"mine".equals(pending) && !sample.isEmpty()) {
                    options.add(new Option(option("held", sample.getCount()), () -> send(pending, 0)));
                }
                options.add(new Option(option("back"), () -> say(line("jobs"), Node.JOBS)));
            }
            case GUARD -> {
                options.add(new Option(option("guard"), () -> send("guard", 0)));
                options.add(new Option(option("escort"), () -> send("escort", 0)));
                if (!"NONE".equals(context.getString("Mode"))) {
                    options.add(new Option(option("dismiss"), () -> send("dismiss", 0)));
                }
                options.add(new Option(option("back"), () -> say(line("more"), Node.ROOT)));
            }
            case PLANS -> {
                if (context.getInt("QueueSize") > 0) {
                    options.add(new Option(option("cancel_first"), () -> send("cancel_first", 0)));
                    options.add(new Option(option("clear"), () -> send("clear", 0)));
                }
                if (context.getBoolean("Paused")) {
                    options.add(new Option(option("resume"), () -> send("resume", 0)));
                } else if (context.getInt("QueueSize") > 0) {
                    options.add(new Option(option("pause"), () -> send("pause", 0)));
                }
                options.add(new Option(option("back"), () -> say(line("more"), Node.ROOT)));
            }
        }
    }

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
        String mode = context.getString("Mode");
        if (!"NONE".equals(mode) && !mode.isEmpty()) {
            return line("plans_" + mode.toLowerCase(Locale.ROOT)).copy().append(" ").append(mood);
        }
        if (tasks.isEmpty()) {
            return line("plans_none").copy().append(" ").append(mood);
        }
        var text = line("plans_now", tasks.get(0).describe()).copy();
        if (tasks.size() > 1) {
            text.append(" ").append(Component.translatable("arcane_artifacts.catto.dialog.plans_next", tasks.get(1).describe(),
                    context.getInt("QueueSize") - 1));
        }
        if (context.getBoolean("Paused")) {
            text.append(" ").append(Component.translatable("arcane_artifacts.catto.dialog.plans_paused"));
        }
        return text.append(" ").append(mood);
    }

    // ------------------------------------------------------------------ cutscene

    private int barHeight() {
        return Math.round(height * 0.14F * intro);
    }

    /** The lower bar holds the subtitles: tall enough for the name and three lines. */
    private int bottomBar() {
        return Math.round(Math.max(height * 0.14F, 52.0F) * intro);
    }

    private int optionWidth() {
        return Math.min(220, width / 3);
    }

    @Override
    protected void init() {
        if (!cinematic && minecraft != null) {
            cinematic = true;
            previousCamera = minecraft.options.getCameraType();
            previousHideGui = minecraft.options.hideGui;
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            minecraft.options.hideGui = true;
        }
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        fillOptions();
        int w = optionWidth();
        int x = width - w - 12;
        int total = options.size() * 21;
        int y = Math.max(barHeight() + 8, (height - total) / 2 - 10);
        for (int i = 0; i < options.size(); i++) {
            Option opt = options.get(i);
            String text = font.plainSubstrByWidth((i + 1) + ". " + opt.label().getString(), w - 10);
            addRenderableWidget(Button.builder(Component.literal(text), b -> opt.action().run()).bounds(x, y + i * 21, w, 19).build());
        }
    }

    @Override
    public void tick() {
        shown += 3.0F;
        float before = intro;
        intro = Math.min(1.0F, intro + 0.12F);
        if (before < 1.0F) {
            rebuild();
        }
        Catto cat = cat();
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (cat == null || player == null || !cat.isAlive() || cat.distanceToSqr(player) > 24 * 24) {
            onClose();
            return;
        }
        // Over the shoulder: the player turns toward Catto, a little to the side so both stay in the frame.
        double dx = cat.getX() - player.getX();
        double dz = cat.getZ() - player.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F - 22.0F;
        float pitch = (float) (Mth.atan2(player.getEyeY() - (cat.getY() + 0.5), Math.max(0.5, flat)) * Mth.RAD_TO_DEG) + 8.0F;
        float newYaw = player.getYRot() + Mth.wrapDegrees(yaw - player.getYRot()) * 0.25F;
        float newPitch = Mth.lerp(0.25F, player.getXRot(), Mth.clamp(pitch, -20.0F, 45.0F));
        player.setYRot(newYaw);
        player.setYHeadRot(newYaw);
        player.setXRot(newPitch);
    }

    @Override
    public void onClose() {
        if (cinematic && minecraft != null) {
            minecraft.options.setCameraType(previousCamera == null ? CameraType.FIRST_PERSON : previousCamera);
            minecraft.options.hideGui = previousHideGui;
            cinematic = false;
        }
        PacketDistributor.sendToServer(new CattoDialog.CommandPayload(entityId, "bye", 0, 0));
        super.onClose();
    }

    @Override
    public void removed() {
        // Closed by another screen (the inventory, death...): give the camera back too.
        if (cinematic && minecraft != null) {
            minecraft.options.setCameraType(previousCamera == null ? CameraType.FIRST_PERSON : previousCamera);
            minecraft.options.hideGui = previousHideGui;
            cinematic = false;
        }
        super.removed();
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
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // No blur and no dimming: the world is the scene.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int bar = barHeight();
        g.fill(0, 0, width, bar, BAR);
        int bottom = bottomBar();
        g.fill(0, height - bottom, width, height, BAR);
        // Options: a soft panel behind the buttons.
        if (!options.isEmpty()) {
            int w = optionWidth();
            int x = width - w - 12;
            int total = options.size() * 21;
            int y = Math.max(bar + 8, (height - total) / 2 - 10);
            g.fill(x - 6, y - 6, x + w + 6, y + total + 4, OPTION_BG);
            g.renderOutline(x - 6, y - 6, w + 12, total + 10, OPTION_EDGE);
        }
        super.render(g, mouseX, mouseY, partialTick);
        // Catto's line, like subtitles in the lower bar.
        Catto cat = cat();
        Component name = cat != null ? cat.getDisplayName() : title;
        Component nature = Component.translatable("arcane_artifacts.catto.nature." + nature());
        int textW = Math.min(width - 40, 420);
        int left = (width - textW) / 2;
        int top = height - bottom + 6;
        g.drawString(font, name.copy().append(Component.literal("  ·  ").append(nature).withStyle(s -> s.withColor(QUIET))), left, top, NAME, true);
        String full = speech.getString();
        String visible = full.substring(0, (int) Math.min(full.length(), shown));
        int y = top + 12;
        for (FormattedCharSequence line : font.split(Component.literal(visible), textW)) {
            g.drawString(font, line, left, y, TEXT, true);
            y += 10;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
