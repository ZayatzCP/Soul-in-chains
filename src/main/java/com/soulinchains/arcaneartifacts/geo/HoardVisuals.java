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
 * The hoard made visible: small blocks circling their owner in two slow rings (block displays, moved on the server, eased
 * on the client). While Stone Wings hold, a dozen of them spread behind the back as wings and the owner may fly. The
 * displays are never saved.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class HoardVisuals {
    public static final String TAG = "arcane_artifacts.hoard";
    /** At most this many blocks are drawn around a player. */
    static final int SHOWN = 32;
    static final float SCALE = 0.4F;
    public static final int WING_BLOCKS = 12;
    private static final String WINGS = ArcaneArtifacts.MODID + ":stone_wings_until";
    private static final String WINGS_FLIGHT = ArcaneArtifacts.MODID + ":stone_wings_flight";

    private static final Map<UUID, List<Display.BlockDisplay>> SHOWN_BY = new HashMap<>();
    private static final Map<UUID, List<BlockState>> STATES_BY = new HashMap<>();

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
            player.getPersistentData().putBoolean(WINGS_FLIGHT, true);
            player.onUpdateAbilities();
        } else if (!wings && granted) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
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
    }

    /** Brings the displays around a player in line with the hoard and moves them along their rings (or wings). */
    public static void update(ServerLevel level, Player player) {
        List<BlockState> hoard = StoneHoard.of(player);
        int n = player.isAlive() && !player.isSpectator() ? Math.min(SHOWN, hoard.size()) : 0;
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
        for (int i = 0; i < shown.size(); i++) {
            Vec3 at = slot(player, i, n);
            shown.get(i).setPos(at.x, at.y, at.z);
        }
    }

    /** Where block {@code i} of {@code n} floats: a wing feather while the wings hold, otherwise a place on a ring. */
    public static Vec3 slot(Player player, int i, int n) {
        double time = player.level().getGameTime() + i * 7;
        if (wings(player) && i < WING_BLOCKS) {
            double yaw = Math.toRadians(player.yBodyRot);
            Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
            Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
            int side = i % 2 == 0 ? 1 : -1;
            int k = i / 2;
            double flap = Math.sin(player.level().getGameTime() * 0.35) * 0.08 * k;
            return player.position().add(0, 1.35 + 0.16 * k + flap, 0).add(back.scale(0.45 + 0.05 * k)).add(right.scale(side * (0.45 + 0.42 * k)));
        }
        int ring = i % 2;
        int onRing = Math.max(1, (n + 1 - ring) / 2);
        double angle = Math.PI * 2 * (i / 2) / onRing + player.level().getGameTime() * (ring == 0 ? 0.04 : -0.03);
        double radius = ring == 0 ? 1.6 : 2.1;
        double y = ring == 0 ? 0.7 : 1.5;
        return player.position().add(Math.cos(angle) * radius, y + Math.sin(time * 0.08) * 0.08, Math.sin(angle) * radius);
    }

    public static void clear(UUID player) {
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
