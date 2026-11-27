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
import net.minecraft.world.phys.Vec3;
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
                    mc.options.fov().set(40);
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
                    mc.options.fov().set(70);
                    stage = 6;
                    ticks = 0;
                    scene = 0;
                    sceneTicks = 0;
                    MinecraftServer server = mc.getSingleplayerServer();
                    if (server != null) {
                        server.execute(() -> spellStudio(server));
                    }
                    return;
                }
                MinecraftServer server = mc.getSingleplayerServer();
                if (t == 0 && server != null) {
                    EntityType<?> type = types.get(portrait);
                    server.execute(() -> pose(server, type));
                } else if (t == PORTRAIT - 1) {
                    if (posed) {
                        shoot(mc, BuiltInRegistries.ENTITY_TYPE.getKey(types.get(portrait)).getPath(), false);
                    }
                    portrait++;
                }
            }
            case 6 -> {
                // the stone spells, scene by scene: set up, keep the caster's visuals going, then shoot
                MinecraftServer server = mc.getSingleplayerServer();
                if (server == null || ticks < 100) {
                    return;
                }
                if (scene >= SCENES.size()) {
                    server.execute(() -> com.soulinchains.arcaneartifacts.geo.HoardVisuals.clear(caster == null ? java.util.UUID.randomUUID() : caster.getUUID()));
                    mc.options.hideGui = false;
                    mc.options.fov().set(70);
                    finish(mc);
                    return;
                }
                Scene current = SCENES.get(scene);
                if (sceneTicks == 0) {
                    server.execute(() -> {
                        ServerLevel level = server.overworld();
                        if (current.setup() != null) {
                            current.setup().accept(level);
                        }
                        ServerPlayer player = player(server);
                        if (player != null) {
                            Vec3 cam = SPELLS.add(current.cam());
                            Vec3 look = SPELLS.add(current.look()).subtract(cam);
                            float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
                            float pitch = (float) -Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z)));
                            player.teleportTo(level, cam.x, cam.y - 1.62, cam.z, yaw, pitch);
                        }
                    });
                }
                server.execute(() -> {
                    if (caster != null) {
                        com.soulinchains.arcaneartifacts.geo.HoardVisuals.update(server.overworld(), caster);
                        com.soulinchains.arcaneartifacts.geo.HoardVisuals.updateWings(server.overworld(), caster);
                    }
                });
                sceneTicks++;
                if (sceneTicks >= current.wait()) {
                    shoot(mc, "spell_" + current.name(), true);
                    scene++;
                    sceneTicks = 0;
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ the stone spells, photographed

    /** An open sky over a wide grass field, far from the creature studio. */
    private static final Vec3 SPELLS = new Vec3(300.5, 201, 0.5);
    private static int scene;
    private static int sceneTicks;
    private static net.neoforged.neoforge.common.util.FakePlayer caster;
    private static net.minecraft.world.entity.decoration.ArmorStand body;
    private static net.minecraft.world.entity.monster.Husk victim;
    private static com.soulinchains.arcaneartifacts.geo.StonePhenomena.Cage cage;

    record Scene(String name, int wait, @org.jetbrains.annotations.Nullable java.util.function.Consumer<ServerLevel> setup, Vec3 cam, Vec3 look) {
    }

    private static final List<BlockState> MIX = List.of(net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(),
            net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),
            net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState(), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(),
            net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState());

    private static List<BlockState> mix(int n) {
        List<BlockState> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(MIX.get(i % MIX.size()));
        }
        return out;
    }

    private static void fill(int n) {
        com.soulinchains.arcaneartifacts.geo.StoneHoard.clear(caster);
        for (BlockState state : mix(n)) {
            com.soulinchains.arcaneartifacts.geo.StoneHoard.add(caster, state);
        }
    }

    private static void wings(boolean open, boolean flying) {
        com.soulinchains.arcaneartifacts.geo.HoardVisuals.grantWings(caster, open ? 20 * 60 : 0);
        caster.getAbilities().flying = flying;
        if (!open) {
            caster.getPersistentData().remove(ArcaneArtifacts.MODID + ":stone_wings_until");
        }
    }

    private static net.minecraft.world.entity.monster.Husk victim(ServerLevel level, Vec3 at) {
        if (victim != null) {
            victim.discard();
        }
        victim = EntityType.HUSK.create(level);
        victim.moveTo(at.x, at.y, at.z, 180F, 0F);
        victim.setYHeadRot(180F);
        victim.yBodyRot = 180F;
        victim.setNoAi(true);
        victim.setInvulnerable(true);
        level.addFreshEntity(victim);
        return victim;
    }

    private static final Vec3 C = Vec3.ZERO;
    private static final Vec3 BODY = new Vec3(0, 1.4, 0);
    private static final Vec3 T = new Vec3(0, 0, 7);
    private static final Vec3 M = new Vec3(0, 0, 12);

    private static final List<Scene> SCENES = List.of(
            new Scene("hoard_rings", 12, l -> { fill(32); wings(false, false); }, new Vec3(3.5, 2.6, 6.5), BODY),
            new Scene("hoard_rings_close", 8, null, new Vec3(1.8, 1.9, 3.2), BODY),
            new Scene("wings_open_front", 16, l -> wings(true, false), new Vec3(0, 4, 24), new Vec3(0, 3, 0)),
            new Scene("wings_flying_back_1", 12, l -> wings(true, true), new Vec3(0, 7, -27), new Vec3(0, 3, 0)),
            new Scene("wings_flying_back_2", 10, null, new Vec3(0, 7, -27), new Vec3(0, 3, 0)),
            new Scene("wings_flying_back_3", 10, null, new Vec3(0, 7, -27), new Vec3(0, 3, 0)),
            new Scene("wings_flying_back_4", 10, null, new Vec3(0, 7, -27), new Vec3(0, 3, 0)),
            new Scene("wings_side", 6, null, new Vec3(26, 5, 2), new Vec3(0, 3, 0)),
            new Scene("wings_three_quarter", 6, null, new Vec3(-16, 6, 18), new Vec3(0, 3, 0)),
            new Scene("wings_top", 6, null, new Vec3(0.2, 32, -6), new Vec3(0, 1, 0)),
            new Scene("wings_close_back", 6, null, new Vec3(2, 3.2, -6), new Vec3(0, 2.2, 0)),
            new Scene("earthen_surge", 7, l -> {
                wings(false, false);
                com.soulinchains.arcaneartifacts.geo.StoneHoard.clear(caster);
                List<BlockPos> where = new ArrayList<>();
                List<BlockState> what = new ArrayList<>();
                com.soulinchains.arcaneartifacts.geo.StoneGlyphs.EarthenSurge.surge(l, caster, BlockPos.containing(SPELLS.add(4, -1, 3)), 3, 16, where, what);
                com.soulinchains.arcaneartifacts.phenomena.Phenomena.add(new com.soulinchains.arcaneartifacts.geo.StonePhenomena.Uplift(l, caster, where, what));
            }, new Vec3(9, 3.5, 8), new Vec3(2, 1, 2)),
            new Scene("stone_cage", 6, l -> {
                fill(32);
                net.minecraft.world.entity.monster.Husk husk = victim(l, SPELLS.add(T));
                cage = new com.soulinchains.arcaneartifacts.geo.StonePhenomena.Cage(l, caster, husk, 20 * 30);
                cage.raise(mix(12));
            }, new Vec3(3.5, 3.2, 12.5), T.add(0, 1, 0)),
            new Scene("stone_lances", 8, l -> {
                if (cage != null) {
                    cage.placed().forEach(p -> l.removeBlock(p, false));
                    cage.placed().clear();
                }
                victim(l, SPELLS.add(T));
                com.soulinchains.arcaneartifacts.phenomena.Phenomena.add(new com.soulinchains.arcaneartifacts.geo.StonePhenomena.Lances(l, caster,
                        SPELLS.add(T), mix(10), 0F, 1.5, 0));
            }, new Vec3(4.5, 3, 13.5), T.add(0, 1.2, 0)),
            new Scene("myriad_volley", 14, l -> {
                fill(32);
                net.minecraft.world.entity.monster.Husk husk = victim(l, SPELLS.add(0, 0, 16));
                com.soulinchains.arcaneartifacts.phenomena.Phenomena.add(new com.soulinchains.arcaneartifacts.geo.StonePhenomena.Volley(l, caster,
                        com.soulinchains.arcaneartifacts.geo.StoneHoard.take(caster, 32), List.of(husk), husk.position(), 0F));
            }, new Vec3(11, 4, 7), new Vec3(0, 1.5, 7)),
            new Scene("cataclysm_falling", 12, l -> {
                if (victim != null) {
                    victim.discard();
                    victim = null;
                }
                com.soulinchains.arcaneartifacts.phenomena.Phenomena.add(new com.soulinchains.arcaneartifacts.geo.StonePhenomena.Meteor(l, caster,
                        SPELLS.add(M), mix(24), 0F, 6, true));
            }, new Vec3(32, 14, -14), M.add(0, 26, 0)),
            new Scene("cataclysm_low", 14, null, new Vec3(30, 10, -12), M.add(0, 10, 0)),
            new Scene("cataclysm_impact", 9, null, new Vec3(22, 9, -8), M.add(0, 2, 0)),
            new Scene("cataclysm_shockwave", 5, null, new Vec3(22, 12, -10), M.add(0, 1, 0)),
            new Scene("cataclysm_crater", 40, null, new Vec3(10, 12, 1), M));

    /** The open field, the caster (a stand-in player who holds the hoard) and a body for the eye (an armour stand). */
    private static void spellStudio(MinecraftServer server) {
        ServerLevel level = server.overworld();
        level.setDayTime(6000);
        BlockPos base = BlockPos.containing(SPELLS);
        for (int dx = -30; dx <= 30; dx++) {
            for (int dz = -24; dz <= 36; dz++) {
                level.setBlock(base.offset(dx, -2, dz), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 2);
                level.setBlock(base.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            }
        }
        caster = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "stone_mage"));
        caster.setGameMode(GameType.SURVIVAL);
        caster.moveTo(SPELLS.x, SPELLS.y, SPELLS.z, 0F, 0F);
        caster.setYRot(0F);
        caster.yBodyRot = 0F;
        caster.setYHeadRot(0F);
        body = EntityType.ARMOR_STAND.create(level);
        body.moveTo(SPELLS.x, SPELLS.y, SPELLS.z, 0F, 0F);
        body.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
        body.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(net.minecraft.world.item.Items.LEATHER_CHESTPLATE));
        body.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(net.minecraft.world.item.Items.LEATHER_LEGGINGS));
        body.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(net.minecraft.world.item.Items.LEATHER_BOOTS));
        level.addFreshEntity(body);
        ServerPlayer player = player(server);
        if (player != null) {
            player.teleportTo(level, SPELLS.x, SPELLS.y + 3, SPELLS.z - 10, 0F, 10F);
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
            // bound to a stand-in, so no chain runs from it to the camera
            servant.bind(net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "portrait_master")));
        }
        if (living instanceof com.soulinchains.arcaneartifacts.astral.Watcher watcher) {
            watcher.posed = true;
        }
        level.addFreshEntity(living);
        sitter = living;
        double h = living.getBbHeight();
        double w = living.getBbWidth();
        // a 40-degree view shows about 0.73 of the distance from top to bottom: the creature fills two thirds of it
        double distance = Math.max(1.8, 2.05 * Math.max(h, w * 1.1)) + w / 2;
        double eye = STUDIO.getY() + h * 0.5 + distance * 0.12;
        float pitch = (float) Math.toDegrees(Math.atan2(eye - (STUDIO.getY() + h * 0.5), distance));
        player.teleportTo(level, STUDIO.getX() + 0.5, eye - 1.62, STUDIO.getZ() + 0.5 - distance, 0F, pitch);
        posed = true;
    }

    /** The last frame, squared and shrunk to 320x320, written into the log as JPEG in Base64 (to be put together outside). */
    private static void shoot(Minecraft mc, String name, boolean wide) {
        try (com.mojang.blaze3d.platform.NativeImage frame = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            int w = frame.getWidth();
            int h = frame.getHeight();
            int sw = wide ? w : Math.min(w, h);
            int sh = wide ? h : Math.min(w, h);
            int x0 = (w - sw) / 2;
            int y0 = (h - sh) / 2;
            int ow = wide ? 640 : 320;
            int oh = wide ? ow * sh / sw : 320;
            java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(ow, oh, java.awt.image.BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < oh; y++) {
                for (int x = 0; x < ow; x++) {
                    int c = frame.getPixelRGBA(x0 + x * sw / ow, y0 + y * sh / oh);
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
