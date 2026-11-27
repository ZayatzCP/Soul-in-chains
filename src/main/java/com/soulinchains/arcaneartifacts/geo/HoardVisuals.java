package com.soulinchains.arcaneartifacts.geo;

import com.mojang.math.Transformation;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The hoard made visible: blocks circling their owner in two slow rings, each tumbling as it goes (block displays, moved
 * on the server, eased on the client). While Stone Wings hold, the rings are gone into two great wings of long stone
 * feathers, some fifteen blocks from tip to shoulder, slowly beating; the owner flies, faster than usual. The displays are
 * never saved.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class HoardVisuals {
    public static final String TAG = "arcane_artifacts.hoard";
    /** At most this many blocks are drawn around a player. */
    static final int SHOWN = 32;
    static final float SCALE = 0.62F;
    /** The wings: feathers per side and layer (long flight feathers below, short coverts over them). */
    public static final int FEATHERS = 12;
    static final int LAYERS = 2;
    static final float SPACING = 0.95F;
    /** Flight with the wings is faster than walking flight. */
    static final float WING_SPEED = 0.09F;
    static final float NORMAL_SPEED = 0.05F;
    public static final int WING_BLOCKS = 12;
    private static final String WINGS = ArcaneArtifacts.MODID + ":stone_wings_until";
    private static final String WINGS_FLIGHT = ArcaneArtifacts.MODID + ":stone_wings_flight";

    private static final Map<UUID, List<Display.BlockDisplay>> SHOWN_BY = new HashMap<>();
    private static final Map<UUID, List<BlockState>> STATES_BY = new HashMap<>();
    private static final Map<UUID, List<Display.BlockDisplay>> WINGS_BY = new HashMap<>();

    private HoardVisuals() {
    }

    /** A small block display, eased between server moves. */
    @Nullable
    public static Display.BlockDisplay display(ServerLevel level, BlockState state, float scale, Vec3 at) {
        Display.BlockDisplay entity = EntityType.BLOCK_DISPLAY.create(level);
        if (entity == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        tag.put("block_state", NbtUtils.writeBlockState(state));
        Transformation transformation = new Transformation(new Vector3f(-scale / 2, -scale / 2, -scale / 2), null, new Vector3f(scale, scale, scale), null);
        tag.put("transformation", Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, transformation).getOrThrow());
        tag.putInt("teleport_duration", 2);
        entity.load(tag);
        entity.addTag(TAG);
        entity.setPos(at.x, at.y, at.z);
        level.addFreshEntity(entity);
        return entity;
    }

    // ------------------------------------------------------------------ Stone Wings

    public static boolean wings(Player player) {
        return player.getPersistentData().getLong(WINGS) > player.level().getGameTime();
    }

    public static void grantWings(Player player, int ticks) {
        player.getPersistentData().putLong(WINGS, player.level().getGameTime() + ticks);
    }

    public static void wingFlight(ServerPlayer player) {
        boolean wings = wings(player) && StoneHoard.count(player) >= WING_BLOCKS;
        boolean granted = player.getPersistentData().getBoolean(WINGS_FLIGHT);
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (wings && !player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.getAbilities().setFlyingSpeed(WING_SPEED);
            player.getPersistentData().putBoolean(WINGS_FLIGHT, true);
            player.onUpdateAbilities();
        } else if (!wings && granted) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.getAbilities().setFlyingSpeed(NORMAL_SPEED);
            player.getPersistentData().remove(WINGS_FLIGHT);
            player.onUpdateAbilities();
            player.getPersistentData().remove(WINGS);
        }
        if (wings) {
            player.fallDistance = 0;
        }
    }

    // ------------------------------------------------------------------ the rings

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        wingFlight(player);
        update(level, player);
        updateWings(level, player);
    }

    /** Brings the displays around a player in line with the hoard and moves them along their rings (or wings). */
    public static void update(ServerLevel level, Player player) {
        List<BlockState> hoard = StoneHoard.of(player);
        // while the wings are open the hoard is in them: no ring
        int n = player.isAlive() && !player.isSpectator() && !wings(player) ? Math.min(SHOWN, hoard.size()) : 0;
        List<Display.BlockDisplay> shown = SHOWN_BY.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        List<BlockState> states = STATES_BY.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        // a display left in another level, or removed, starts the set again
        if (shown.stream().anyMatch(d -> d.isRemoved() || d.level() != level)) {
            clear(player.getUUID());
            shown = SHOWN_BY.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
            states = STATES_BY.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        }
        while (shown.size() > n) {
            shown.remove(shown.size() - 1).discard();
            states.remove(states.size() - 1);
        }
        for (int i = 0; i < n; i++) {
            BlockState state = hoard.get(i);
            if (i < shown.size() && states.get(i) != state) {
                shown.get(i).discard();
                Display.BlockDisplay replaced = display(level, state, SCALE, slot(player, i, n));
                if (replaced == null) {
                    return;
                }
                shown.set(i, replaced);
                states.set(i, state);
            } else if (i >= shown.size()) {
                Display.BlockDisplay added = display(level, state, SCALE, slot(player, i, n));
                if (added == null) {
                    return;
                }
                shown.add(added);
                states.add(state);
            }
        }
        long time = player.level().getGameTime();
        for (int i = 0; i < shown.size(); i++) {
            Vec3 at = slot(player, i, n);
            Display.BlockDisplay d = shown.get(i);
            d.setPos(at.x, at.y, at.z);
            // each block tumbles slowly as it circles
            d.setYRot((time * (3 + i % 4) + i * 37) % 360);
            d.setXRot((float) (Math.sin((time + i * 11) * 0.05) * 60));
        }
    }

    // ------------------------------------------------------------------ the wings

    /** Spreads (or folds) the great wings of a player: long feathers of hoard stone, slowly beating. */
    public static void updateWings(ServerLevel level, Player player) {
        boolean open = wings(player) && StoneHoard.count(player) >= WING_BLOCKS && player.isAlive() && !player.isSpectator();
        List<Display.BlockDisplay> feathers = WINGS_BY.get(player.getUUID());
        if (feathers != null && (!open || feathers.stream().anyMatch(d -> d.isRemoved() || d.level() != level))) {
            feathers.forEach(Display.BlockDisplay::discard);
            WINGS_BY.remove(player.getUUID());
            feathers = null;
        }
        if (!open) {
            return;
        }
        if (feathers == null) {
            feathers = new ArrayList<>();
            List<BlockState> hoard = StoneHoard.of(player);
            for (int i = 0; i < FEATHERS * 2 * LAYERS; i++) {
                int k = (i / 2) % FEATHERS;
                int layer = i / (2 * FEATHERS);
                BlockState state = hoard.get((i / 2) % hoard.size());
                Display.BlockDisplay feather = StonePhenomena.feather(level, state, size(k, layer), player.position());
                if (feather != null) {
                    feathers.add(feather);
                }
            }
            WINGS_BY.put(player.getUUID(), feathers);
        }
        for (int i = 0; i < feathers.size(); i++) {
            Feather f = feather(player, (i / 2) % FEATHERS, i % 2 == 0 ? 1 : -1, i / (2 * FEATHERS));
            Display.BlockDisplay d = feathers.get(i);
            d.setPos(f.pos.x, f.pos.y, f.pos.z);
            d.setYRot(f.yaw);
            d.setXRot(f.pitch);
        }
    }

    /** A feather's size: as wide as the spacing (they touch), long flight feathers shortening towards the tip, short coverts. */
    public static org.joml.Vector3f size(int k, int layer) {
        float length = layer == 0 ? 4.4F - 0.2F * k : 2.1F - 0.07F * k;
        return new org.joml.Vector3f(SPACING * 1.15F, layer == 0 ? 0.22F : 0.3F, length);
    }

    public record Feather(Vec3 pos, float yaw, float pitch) {
    }

    /** The flight feather {@code k} of one wing (see {@link #feather(Player, int, int, int)}). */
    public static Feather feather(Player player, int k, int side) {
        return feather(player, k, side, 0);
    }

    /**
     * Feather {@code k} (0 at the shoulder) of one wing ({@code side} +1 right, -1 left), in a layer (0 the long flight
     * feathers, 1 the coverts lying over them): the wing bone runs out from the shoulder, swept back towards the tip, and
     * rises and falls slowly; each feather hangs from the bone and points back, a little outwards.
     */
    public static Feather feather(Player player, int k, int side, int layer) {
        double time = player.level().getGameTime();
        double yaw = Math.toRadians(player.yBodyRot);
        Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        double beat = Math.sin(time * 0.16) * (player.getAbilities().flying ? 0.4 : 0.22);
        double lift = 0.18 + beat * (0.5 + 0.05 * k);
        Vec3 span = right.scale(side * Math.cos(lift)).add(0, Math.sin(lift), 0);
        double out = 0.55 + SPACING * k;
        Vec3 pos = player.position().add(0, 1.4 + (layer == 1 ? 0.2 : 0), 0)
                .add(back.scale(0.3 + 0.1 * k + (layer == 1 ? -0.1 : 0)))
                .add(span.scale(out));
        float featherYaw = (float) (player.yBodyRot + 180 + side * (4 + 2.2 * k));
        float pitch = (float) (-Math.toDegrees(lift) * 0.35 + 6);
        return new Feather(pos, featherYaw, pitch);
    }

    /** Where block {@code i} of {@code n} floats: a place on one of two slow rings around the player. */
    public static Vec3 slot(Player player, int i, int n) {
        double time = player.level().getGameTime() + i * 7;
        int ring = i % 2;
        int onRing = Math.max(1, (n + 1 - ring) / 2);
        double angle = Math.PI * 2 * (i / 2) / onRing + player.level().getGameTime() * (ring == 0 ? 0.035 : -0.025);
        double radius = ring == 0 ? 2.0 : 2.7;
        double y = ring == 0 ? 0.6 : 1.7;
        return player.position().add(Math.cos(angle) * radius, y + Math.sin(time * 0.08) * 0.08, Math.sin(angle) * radius);
    }

    public static void clear(UUID player) {
        List<Display.BlockDisplay> wings = WINGS_BY.remove(player);
        if (wings != null) {
            wings.forEach(Display.BlockDisplay::discard);
        }
        List<Display.BlockDisplay> shown = SHOWN_BY.remove(player);
        if (shown != null) {
            shown.forEach(Display.BlockDisplay::discard);
        }
        STATES_BY.remove(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clear(event.getEntity().getUUID());
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isCanceled()) {
            StoneHoard.spill(player);
            clear(player.getUUID());
            player.getPersistentData().remove(WINGS);
        }
    }

    /** Hoard displays left in a saved world are dropped when it loads. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity().getTags().contains(TAG)) {
            event.setCanceled(true);
        }
    }
}
