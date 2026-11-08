package com.soulinchains.arcaneartifacts.fx;

import com.google.common.collect.ImmutableList;
import com.hollingsworth.arsnouveau.api.particle.PropertyParticleOptions;
import com.hollingsworth.arsnouveau.api.particle.configurations.BurstMotion;
import com.hollingsworth.arsnouveau.api.particle.configurations.IParticleMotionType;
import com.hollingsworth.arsnouveau.api.particle.configurations.ParticleMotion;
import com.hollingsworth.arsnouveau.api.particle.configurations.properties.BaseProperty;
import com.hollingsworth.arsnouveau.api.particle.configurations.properties.MotionProperty;
import com.hollingsworth.arsnouveau.api.particle.timelines.BaseTimeline;
import com.hollingsworth.arsnouveau.api.particle.timelines.IParticleTimelineType;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineOption;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Particle timeline of one of the addon's glyphs, plugged into Ars Nouveau's spell style system: it is
 * listed in the spell book's particle editor next to the Ars glyphs, saved with the spell, and every
 * visual of the glyph is drawn from it, so players restyle it like any Ars glyph.
 * <p>
 * Each timeline has a main effect (a trail along a path, or a ticking effect while something lasts)
 * and an optional impact effect (a burst).
 */
public final class GlyphTimeline extends BaseTimeline<GlyphTimeline> {
    /** How a glyph's timeline is laid out in the editor and what it starts with. */
    public record Spec(Supplier<IParticleTimelineType<GlyphTimeline>> type, ResourceLocation mainName,
                       Supplier<List<IParticleMotionType<?>>> mainOptions, Supplier<ParticleMotion> mainDefault,
                       boolean hasImpact) {
    }

    private final Spec spec;
    public TimelineEntryData main;
    public TimelineEntryData impact;

    public GlyphTimeline(Spec spec) {
        this(spec, new TimelineEntryData(spec.mainDefault().get(), new PropertyParticleOptions()),
                new TimelineEntryData(new BurstMotion(), new PropertyParticleOptions()));
    }

    public GlyphTimeline(Spec spec, TimelineEntryData main, TimelineEntryData impact) {
        this.spec = spec;
        this.main = main;
        this.impact = impact;
    }

    public static MapCodec<GlyphTimeline> codec(Spec spec) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                TimelineEntryData.CODEC.fieldOf("main").forGetter(t -> t.main),
                TimelineEntryData.CODEC.fieldOf("impact").forGetter(t -> t.impact)
        ).apply(instance, (main, impact) -> new GlyphTimeline(spec, main, impact)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, GlyphTimeline> streamCodec(Spec spec) {
        return StreamCodec.composite(
                TimelineEntryData.STREAM, t -> t.main,
                TimelineEntryData.STREAM, t -> t.impact,
                (main, impact) -> new GlyphTimeline(spec, main, impact));
    }

    @Override
    public IParticleTimelineType<GlyphTimeline> getType() {
        return spec.type().get();
    }

    @Override
    public List<BaseProperty<?>> getProperties() {
        List<BaseProperty<?>> properties = new ArrayList<>();
        properties.add(new MotionProperty(new TimelineOption(spec.mainName(), main, ImmutableList.copyOf(spec.mainOptions().get()))));
        if (spec.hasImpact()) {
            properties.add(new MotionProperty(new TimelineOption(TimelineOption.IMPACT, impact, ImmutableList.copyOf(ModTimelines.impactOptions()))));
        }
        return properties;
    }
}
