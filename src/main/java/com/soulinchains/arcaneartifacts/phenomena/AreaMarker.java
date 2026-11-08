package com.soulinchains.arcaneartifacts.phenomena;

import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData;
import com.soulinchains.arcaneartifacts.fx.Fx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Keeps the area of a lasting glyph visible: a ring of styled bursts on its edge every half second. */
public final class AreaMarker implements Phenomena.Phenomenon, Phenomena.Located {
    private final ServerLevel level;
    private final UUID owner;
    private final Vec3 center;
    private final double radius;
    private final TimelineEntryData style;
    private int ticksLeft;

    public AreaMarker(ServerLevel level, UUID owner, Vec3 center, double radius, int ticks, TimelineEntryData style) {
        this.level = level;
        this.owner = owner;
        this.center = center;
        this.radius = radius;
        this.ticksLeft = ticks;
        this.style = style;
    }

    @Override
    public ServerLevel level() {
        return level;
    }

    @Override
    public UUID owner() {
        return owner;
    }

    @Override
    public Vec3 center() {
        return center;
    }

    @Override
    public boolean tick() {
        if (ticksLeft-- <= 0) {
            return false;
        }
        if (ticksLeft % 10 == 0) {
            Fx.ring(level, style, center, radius, Math.max(10, (int) (radius * 3)));
        }
        return true;
    }
}
