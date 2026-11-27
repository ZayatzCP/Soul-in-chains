package com.soulinchains.arcaneartifacts.client;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.necro.Servant;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client smoke test, only with {@code -Darcane_artifacts.smoke=true} (CI runs it under a virtual display):
 * <ol>
 *     <li>at the first screen: every item of the mod has a baked model, every block state has one, every entity type a
 *     renderer;</li>
 *     <li>creates a creative world, spawns every entity of the mod around the player, puts every item into the hotbar and
 *     through the hand, equips every accessory and looks at it all in first and third person;</li>
 *     <li>logs {@code ARCANE_SMOKE_OK} (or {@code ARCANE_SMOKE_FAIL} with the problems) and quits.</li>
 * </ol>
 * Any crash in a renderer, model or screen kills the client before the OK line, which fails the CI step.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, value = Dist.CLIENT)
public final class ClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("arcane_artifacts.smoke");
    private static final String[] CURIO_SLOTS = {"head", "necklace", "back", "body", "belt", "hands", "ring", "feet", "charm", "relic"};
    private static final List<String> problems = new ArrayList<>();
    private static int stage;
    private static int ticks;
    private static List<Item> items = List.of();
    private static final List<Item> curiosLeft = new ArrayList<>();

    private ClientSmokeTest() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        try {
            tick(mc);
        } catch (Throwable t) {
            ArcaneArtifacts.LOGGER.error("ARCANE_SMOKE_FAIL: exception", t);
            problems.add("exception " + t);
            finish(mc);
        }
    }

    private static void tick(Minecraft mc) {
        ticks++;
        switch (stage) {
            case 0 -> {
                if (mc.getOverlay() != null || mc.screen == null || ticks < 40) {
                    return;
                }
                checkModelsAndRenderers(mc);
                LevelSettings settings = new LevelSettings("arcane_smoke", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                        new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("arcane_smoke", settings, new WorldOptions(20251120L, false, false),
                        registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions(), mc.screen);
                stage = 1;
                ticks = 0;
                ArcaneArtifacts.LOGGER.info("ARCANE_SMOKE: models checked, creating world");
            }
            case 1 -> {
                if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
                    if (ticks > 20 * 300) {
                        problems.add("world did not open in 5 minutes");
                        finish(mc);
                    }
                    return;
                }
                if (ticks < 100) {
                    return;
                }
                MinecraftServer server = mc.getSingleplayerServer();
                server.execute(() -> spawnEverything(server));
                stage = 2;
                ticks = 0;
                ArcaneArtifacts.LOGGER.info("ARCANE_SMOKE: in world, spawning {} entity types", entityTypes().size());
            }
            case 2 -> {
                // Hotbar: nine items at a time, each one in the hand for a moment; second pass in third person.
                int pass = items.size() / 9 + 1;
                int step = ticks / 9;
                if (step == pass) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                }
                if (ticks % 9 == 0) {
                    int group = step % pass;
                    MinecraftServer server = mc.getSingleplayerServer();
                    if (server != null) {
                        server.execute(() -> fillHotbar(server, group));
                    }
                }
                if (mc.player != null) {
                    mc.player.getInventory().selected = ticks % 9;
                }
                if (step >= pass * 2) {
                    stage = 3;
                    ticks = 0;
                    ArcaneArtifacts.LOGGER.info("ARCANE_SMOKE: items shown, equipping accessories");
                }
            }
            case 3 -> {
                if (ticks % 20 == 1) {
                    MinecraftServer server = mc.getSingleplayerServer();
                    if (server != null) {
                        server.execute(() -> equipNext(server));
                    }
                }
                if (ticks > 20 * 20 || curiosLeft.isEmpty() && ticks > 60) {
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    stage = 4;
                    ticks = 0;
                    MinecraftServer server = mc.getSingleplayerServer();
                    if (server != null) {
                        server.execute(() -> ascend(server));
                    }
                    ArcaneArtifacts.LOGGER.info("ARCANE_SMOKE: accessories worn, ascending into the Astral");
                }
            }
            case 4 -> {
                // the Astral: a few seconds there (sky, fog, monoliths drawn), then home through the gate
                if (ticks == 160) {
                    if (mc.level == null || !com.soulinchains.arcaneartifacts.astral.Astral.isAstral(mc.level)) {
                        problems.add("the Ascension did not bring the player into the Astral");
                    }
                    MinecraftServer server = mc.getSingleplayerServer();
                    if (server != null) {
                        server.execute(() -> {
                            ServerPlayer player = player(server);
                            if (player != null) {
                                com.soulinchains.arcaneartifacts.astral.Astral.leave(player);
                            }
                        });
                    }
                }
                if (ticks == 260) {
                    if (mc.level == null || com.soulinchains.arcaneartifacts.astral.Astral.isAstral(mc.level)) {
                        problems.add("the Astral Gate did not bring the player home");
                    }
                    stage = 5;
                    ticks = 0;
                    portrait = 0;
                    mc.options.hideGui = true;
                    MinecraftServer server = mc.getSingleplayerServer();
                    if (server != null) {
                        server.execute(() -> buildStudio(server));
                    }
                    ArcaneArtifacts.LOGGER.info("ARCANE_SMOKE: portraits of {} creatures", entityTypes().size());
                }
            }
            case 5 -> {
                // one portrait every PORTRAIT ticks (the first waits for the studio's chunks)
                List<EntityType<?>> types = entityTypes();
                int first = 100;
                if (ticks < first) {
                    return;
                }
                int t = (ticks - first) % PORTRAIT;
                if (portrait >= types.size()) {
                    mc.options.hideGui = false;
                    finish(mc);
                    return;
                }
                MinecraftServer server = mc.getSingleplayerServer();
                if (t == 0 && server != null) {
                    EntityType<?> type = types.get(portrait);
                    server.execute(() -> pose(server, type));
                } else if (t == PORTRAIT - 1) {
                    if (posed) {
                        shoot(mc, BuiltInRegistries.ENTITY_TYPE.getKey(types.get(portrait)).getPath());
                    }
                    portrait++;
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ portraits

    static final int PORTRAIT = 24;
    private static int portrait;
    private static volatile boolean posed;
    private static Entity sitter;
    private static final BlockPos STUDIO = new BlockPos(0, 230, 0);

    /** A grass floor high in the sky under a wide black roof (no sun on the undead), lit all round by invisible lights. */
    private static void buildStudio(MinecraftServer server) {
        ServerLevel level = server.overworld();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                level.setBlock(STUDIO.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            }
        }
        for (int dx = -22; dx <= 22; dx++) {
            for (int dz = -22; dz <= 22; dz++) {
                level.setBlock(STUDIO.offset(dx, 16, dz), net.minecraft.world.level.block.Blocks.BLACK_CONCRETE.defaultBlockState(), 2);
            }
        }
        for (int dx = -6; dx <= 6; dx += 3) {
            for (int dz = -6; dz <= 6; dz += 3) {
                for (int dy : new int[]{1, 6}) {
                    level.setBlock(STUDIO.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.LIGHT.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15), 2);
                }
            }
        }
        ServerPlayer player = player(server);
        if (player != null) {
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(level, STUDIO.getX() + 0.5, STUDIO.getY(), STUDIO.getZ() - 6.5, 0F, 10F);
        }
    }

    /** Stands one creature in the studio facing the camera, and puts the camera in front of it. */
    private static void pose(MinecraftServer server, EntityType<?> type) {
        posed = false;
        ServerLevel level = server.overworld();
        if (sitter != null) {
            sitter.discard();
            sitter = null;
        }
        ServerPlayer player = player(server);
        Entity entity;
        try {
            entity = type.create(level);
        } catch (Throwable t) {
            return;
        }
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity living) || player == null) {
            if (entity != null) {
                entity.discard();
            }
            return;
        }
        living.moveTo(STUDIO.getX() + 0.5, STUDIO.getY(), STUDIO.getZ() + 0.5, 180F, 0F);
        living.setYHeadRot(180F);
        living.yBodyRot = 180F;
        living.setInvulnerable(true);
        if (living instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setPersistenceRequired();
        }
        if (living instanceof Servant servant) {
            servant.bind(player);
        }
        if (living instanceof com.soulinchains.arcaneartifacts.astral.Watcher watcher) {
            watcher.posed = true;
        }
        level.addFreshEntity(living);
        sitter = living;
        double h = living.getBbHeight() * (living instanceof com.soulinchains.arcaneartifacts.astral.Watcher ? 2.5 : 1);
        double w = living.getBbWidth();
        double distance = Math.max(2.6, h * 1.35 + w * 0.9);
        double eye = STUDIO.getY() + h * 0.55 + distance * 0.18;
        float pitch = (float) Math.toDegrees(Math.atan2(eye - (STUDIO.getY() + h * 0.5), distance));
        player.teleportTo(level, STUDIO.getX() + 0.5, eye - 1.62, STUDIO.getZ() + 0.5 - distance, 0F, pitch);
        posed = true;
    }

    /** The last frame, squared and shrunk to 320x320, written into the log as JPEG in Base64 (to be put together outside). */
    private static void shoot(Minecraft mc, String name) {
        try (com.mojang.blaze3d.platform.NativeImage frame = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            int w = frame.getWidth();
            int h = frame.getHeight();
            int side = Math.min(w, h);
            int x0 = (w - side) / 2;
            int y0 = (h - side) / 2;
            int size = 320;
            java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int c = frame.getPixelRGBA(x0 + x * side / size, y0 + y * side / size);
                    out.setRGB(x, y, (c & 255) << 16 | (c >> 8 & 255) << 8 | (c >> 16 & 255));
                }
            }
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(out, "jpg", bytes);
            String data = java.util.Base64.getEncoder().encodeToString(bytes.toByteArray());
            int parts = (data.length() + 3999) / 4000;
            for (int i = 0; i < parts; i++) {
                ArcaneArtifacts.LOGGER.info("ARCANE_SHOT|{}|{}/{}|{}", name, i + 1, parts, data.substring(i * 4000, Math.min(data.length(), (i + 1) * 4000)));
            }
        } catch (Throwable t) {
            ArcaneArtifacts.LOGGER.warn("ARCANE_SMOKE: no portrait of {}", name, t);
        }
    }

    private static List<EntityType<?>> entityTypes() {
        return BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals(ArcaneArtifacts.MODID)).toList();
    }

    @SuppressWarnings("unchecked")
    private static void checkModelsAndRenderers(Minecraft mc) {
        BakedModel missing = mc.getModelManager().getMissingModel();
        items = BuiltInRegistries.ITEM.stream().filter(i -> BuiltInRegistries.ITEM.getKey(i).getNamespace().equals(ArcaneArtifacts.MODID)).toList();
        for (Item item : items) {
            if (mc.getItemRenderer().getItemModelShaper().getItemModel(new ItemStack(item)) == missing) {
                problems.add("item model missing: " + BuiltInRegistries.ITEM.getKey(item));
            }
        }
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(ArcaneArtifacts.MODID)) {
                continue;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (mc.getBlockRenderer().getBlockModel(state) == missing) {
                    problems.add("block model missing: " + state);
                    break;
                }
            }
        }
        try {
            Field field = EntityRenderDispatcher.class.getDeclaredField("renderers");
            field.setAccessible(true);
            Map<EntityType<?>, ?> renderers = (Map<EntityType<?>, ?>) field.get(mc.getEntityRenderDispatcher());
            for (EntityType<?> type : entityTypes()) {
                if (!renderers.containsKey(type)) {
                    problems.add("entity renderer missing: " + BuiltInRegistries.ENTITY_TYPE.getKey(type));
                }
            }
        } catch (ReflectiveOperationException e) {
            ArcaneArtifacts.LOGGER.warn("ARCANE_SMOKE: could not read the renderer map", e);
        }
        ArcaneArtifacts.LOGGER.info("ARCANE_SMOKE: {} items checked, {} problems so far", items.size(), problems.size());
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().isEmpty() ? null : server.getPlayerList().getPlayers().get(0);
    }

    private static void spawnEverything(MinecraftServer server) {
        ServerPlayer player = player(server);
        if (player == null) {
            problems.add("no player on the integrated server");
            return;
        }
        ServerLevel level = player.serverLevel();
        level.setDayTime(18000);
        List<EntityType<?>> types = entityTypes();
        for (int i = 0; i < types.size(); i++) {
            EntityType<?> type = types.get(i);
            try {
                Entity entity = type.create(level);
                if (entity == null) {
                    continue;
                }
                double angle = Math.PI * 2 * i / types.size();
                double radius = 5 + (i % 3) * 2;
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                entity.moveTo(x, player.getY() + 0.5, z, 0, 0);
                if (entity instanceof Mob mob) {
                    mob.setNoAi(true);
                    mob.setPersistenceRequired();
                }
                if (entity instanceof Servant servant) {
                    servant.bind(player);
                }
                level.addFreshEntity(entity);
            } catch (Throwable t) {
                problems.add("spawning " + BuiltInRegistries.ENTITY_TYPE.getKey(type) + ": " + t);
                ArcaneArtifacts.LOGGER.error("ARCANE_SMOKE: spawning {}", type, t);
            }
        }
        player.setYRot(0);
        for (Item item : items) {
            for (String slot : CURIO_SLOTS) {
                if (new ItemStack(item).is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", slot)))) {
                    curiosLeft.add(item);
                    break;
                }
            }
        }
    }

    private static void fillHotbar(MinecraftServer server, int group) {
        ServerPlayer player = player(server);
        if (player == null) {
            return;
        }
        for (int slot = 0; slot < 9; slot++) {
            int index = group * 9 + slot;
            player.getInventory().setItem(slot, index < items.size() ? new ItemStack(items.get(index)) : ItemStack.EMPTY);
        }
    }

    /** Puts on as many not yet worn accessories as there are free slots (one per slot kind). */
    private static void equipNext(MinecraftServer server) {
        ServerPlayer player = player(server);
        if (player == null || curiosLeft.isEmpty()) {
            return;
        }
        CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
            List<String> used = new ArrayList<>();
            var it = curiosLeft.iterator();
            while (it.hasNext()) {
                ItemStack stack = new ItemStack(it.next());
                for (String slot : CURIO_SLOTS) {
                    if (!used.contains(slot) && stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", slot)))) {
                        inv.setEquippedCurio(slot, 0, stack);
                        used.add(slot);
                        it.remove();
                        break;
                    }
                }
            }
        });
    }

    private static void ascend(MinecraftServer server) {
        ServerPlayer player = player(server);
        if (player == null) {
            return;
        }
        if (com.soulinchains.arcaneartifacts.astral.Astral.level(server) == null) {
            problems.add("the Astral dimension is not loaded");
            return;
        }
        BlockPos gate = com.soulinchains.arcaneartifacts.astral.Astral.ascend(player.serverLevel(), player.blockPosition(), List.of(player));
        ArcaneArtifacts.LOGGER.info("ARCANE_SMOKE: Astral gate at {}", gate);
    }

    private static void finish(Minecraft mc) {
        stage = 99;
        if (problems.isEmpty()) {
            ArcaneArtifacts.LOGGER.info("ARCANE_SMOKE_OK");
        } else {
            for (String problem : problems) {
                ArcaneArtifacts.LOGGER.error("ARCANE_SMOKE_FAIL: {}", problem);
            }
        }
        mc.stop();
    }
}
