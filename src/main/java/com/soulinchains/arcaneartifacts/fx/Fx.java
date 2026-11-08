package com.soulinchains.arcaneartifacts.fx;

import com.hollingsworth.arsnouveau.api.particle.ParticleEmitter;
import com.hollingsworth.arsnouveau.api.particle.timelines.IParticleTimelineType;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/** Draws the addon's visuals with Ars Nouveau particle emitters (spell-styled, sent to nearby clients). */
public final class Fx {
    private Fx() {
    }

    /** The glyph's timeline as styled in the spell (the default style when the spell has none). */
    public static GlyphTimeline timeline(SpellContext context, Supplier<IParticleTimelineType<GlyphTimeline>> type) {
        return context.getParticleTimeline(type.get());
    }

    public static GlyphTimeline defaults(Supplier<IParticleTimelineType<GlyphTimeline>> type) {
        return type.get().create();
    }

    /** One burst at a point. */
    public static void burst(ServerLevel level, TimelineEntryData entry, Vec3 at) {
        new ParticleEmitter(() -> at, () -> Vec2.ZERO, entry).tick(level);
    }

    /** Marks a circular area like Ars' Burst: a burst in the centre and bursts around the edge. */
    public static void area(ServerLevel level, TimelineEntryData entry, Vec3 center, double radius) {
        burst(level, entry, center);
        int points = Math.max(8, Math.min(32, (int) Math.round(radius * 4)));
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            burst(level, entry, center.add(Math.cos(a) * radius, 0.1, Math.sin(a) * radius));
        }
    }

    /** A trail from one point to another (the trail motion draws between the previous and the current position). */
    public static void line(ServerLevel level, TimelineEntryData entry, Vec3 from, Vec3 to) {
        Vec3[] pos = {from};
        ParticleEmitter emitter = new ParticleEmitter(() -> pos[0], () -> Vec2.ZERO, entry);
        double length = to.distanceTo(from);
        int segments = Math.max(1, (int) Math.ceil(length / 6.0));
        emitter.previousPosition = from;
        for (int i = 1; i <= segments; i++) {
            pos[0] = from.add(to.subtract(from).scale(i / (double) segments));
            emitter.tick(level);
        }
    }

    /** An emitter that follows a moving point and keeps its age between ticks (spirals, tornadoes, trails). */
    public static ParticleEmitter follow(TimelineEntryData entry, Supplier<Vec3> position) {
        return new ParticleEmitter(position, () -> Vec2.ZERO, entry);
    }
}
