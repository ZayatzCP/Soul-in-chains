package com.soulinchains.arcaneartifacts.phenomena;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Server-side, short-lived spell phenomena that are not entities: vortices (Black Hole, Tornado) and
 * falling meteors. They live for a few seconds, are ticked with their level and are simply dropped
 * when the server stops (nothing is saved, so a phenomenon never survives a restart).
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class Phenomena {
    private static final List<Phenomenon> ACTIVE = new ArrayList<>();

    private Phenomena() {
    }

    /** One running phenomenon; {@link #tick()} returns false when it is over. */
    public interface Phenomenon {
        ServerLevel level();

        UUID owner();

        boolean tick();

        /** Called when the phenomenon is cancelled early (Counterspell): clean up anything it spawned. */
        default void cancelled() {
        }
    }

    public static void add(Phenomenon phenomenon) {
        ACTIVE.add(phenomenon);
    }

    public static List<Phenomenon> active() {
        return List.copyOf(ACTIVE);
    }

    /** Removes the phenomena of a caster (Counterspell). Returns how many were removed. */
    public static int cancelOwnedBy(UUID owner) {
        int before = ACTIVE.size();
        ACTIVE.removeIf(p -> {
            boolean hit = owner.equals(p.owner());
            if (hit) p.cancelled();
            return hit;
        });
        return before - ACTIVE.size();
    }

    /** Removes every phenomenon centred within {@code radius} of the point (Counterspell on a block). */
    public static int cancelNear(ServerLevel level, net.minecraft.world.phys.Vec3 point, double radius) {
        int before = ACTIVE.size();
        ACTIVE.removeIf(p -> {
            boolean hit = p.level() == level && p instanceof Located located && located.center().distanceToSqr(point) <= radius * radius;
            if (hit) p.cancelled();
            return hit;
        });
        return before - ACTIVE.size();
    }

    /** A phenomenon with a position. */
    public interface Located {
        net.minecraft.world.phys.Vec3 center();
    }

    /** Leftover visuals of a phenomenon that was running when the world was saved are dropped on load. */
    @SubscribeEvent
    public static void onEntityJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity().getTags().contains(AnvilDrop.TAG)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Phenomenon> it = new ArrayList<>(ACTIVE).iterator();
        while (it.hasNext()) {
            Phenomenon phenomenon = it.next();
            if (phenomenon.level() != level) {
                continue;
            }
            boolean alive;
            try {
                alive = phenomenon.tick();
            } catch (RuntimeException e) {
                ArcaneArtifacts.LOGGER.error("Spell phenomenon failed and was removed", e);
                alive = false;
            }
            if (!alive) {
                ACTIVE.remove(phenomenon);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE.clear();
    }
}
