package com.soulinchains.arcaneartifacts.fx;

import com.hollingsworth.arsnouveau.api.particle.configurations.IParticleMotionType;
import com.hollingsworth.arsnouveau.api.particle.configurations.SpiralMotion;
import com.hollingsworth.arsnouveau.api.particle.configurations.TrailMotion;
import com.hollingsworth.arsnouveau.api.particle.configurations.BurstMotion;
import com.hollingsworth.arsnouveau.api.particle.configurations.ParticleMotion;
import com.hollingsworth.arsnouveau.api.particle.configurations.properties.PropMap;
import com.hollingsworth.arsnouveau.api.particle.timelines.IParticleTimelineType;
import com.hollingsworth.arsnouveau.api.particle.timelines.SimpleParticleTimelineType;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineOption;
import com.hollingsworth.arsnouveau.api.registry.ParticleMotionRegistry;
import com.hollingsworth.arsnouveau.api.registry.ParticleTimelineRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.glyph.EffectBlackHole;
import com.soulinchains.arcaneartifacts.glyph.EffectCounterspell;
import com.soulinchains.arcaneartifacts.glyph.EffectMeteorShower;
import com.soulinchains.arcaneartifacts.glyph.EffectParasitism;
import com.soulinchains.arcaneartifacts.glyph.EffectTimeWarp;
import com.soulinchains.arcaneartifacts.glyph.EffectTornado;
import com.soulinchains.arcaneartifacts.glyph.EffectWeatherShift;
import com.soulinchains.arcaneartifacts.glyph.MethodBeam;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** The addon's glyph timelines, registered in Ars Nouveau's (synced) particle timeline registry. */
public final class ModTimelines {
    public static final DeferredRegister<IParticleTimelineType<?>> REGISTER =
            DeferredRegister.create(ParticleTimelineRegistry.PARTICLE_TIMELINE_REGISTRY, ArcaneArtifacts.MODID);

    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>> BEAM =
            register("beam", MethodBeam.INSTANCE, TimelineOption.TRAIL, ModTimelines::trailOptions, TrailMotion::new, true);
    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>> BLACK_HOLE =
            register("black_hole", EffectBlackHole.INSTANCE, TimelineOption.TICK, ModTimelines::tickOptions, () -> new SpiralMotion(new PropMap()), true);
    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>> TORNADO =
            register("tornado", EffectTornado.INSTANCE, TimelineOption.TRAIL, ModTimelines::trailOptions, TrailMotion::new, true);
    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>> METEOR_SHOWER =
            register("meteor_shower", EffectMeteorShower.INSTANCE, TimelineOption.TRAIL, ModTimelines::trailOptions, TrailMotion::new, true);
    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>> PARASITISM =
            register("parasitism", EffectParasitism.INSTANCE, TimelineOption.TRAIL, ModTimelines::trailOptions, TrailMotion::new, false);
    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>> TIME_WARP =
            register("time_warp", EffectTimeWarp.INSTANCE, TimelineOption.IMPACT, ModTimelines::impactOptions, BurstMotion::new, false);
    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>> COUNTERSPELL =
            register("counterspell", EffectCounterspell.INSTANCE, TimelineOption.IMPACT, ModTimelines::impactOptions, BurstMotion::new, false);
    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>> WEATHER_SHIFT =
            register("weather_shift", EffectWeatherShift.INSTANCE, TimelineOption.IMPACT, ModTimelines::impactOptions, BurstMotion::new, false);

    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>> ANVIL_DROP =
            register("anvil_drop", com.soulinchains.arcaneartifacts.glyph.EffectAnvilDrop.INSTANCE, TimelineOption.TRAIL, ModTimelines::trailOptions, TrailMotion::new, true);
    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>> SATIETY =
            register("satiety", com.soulinchains.arcaneartifacts.glyph.EffectSatiety.INSTANCE, TimelineOption.IMPACT, ModTimelines::impactOptions, BurstMotion::new, false);

    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>> PITY =
            register("pity", com.soulinchains.arcaneartifacts.glyph.EffectPity.INSTANCE, TimelineOption.IMPACT, ModTimelines::impactOptions, BurstMotion::new, false);
    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>> CUPID =
            register("cupid", com.soulinchains.arcaneartifacts.glyph.EffectCupid.INSTANCE, TimelineOption.IMPACT, ModTimelines::impactOptions, BurstMotion::new, false);

    public static final DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>> FEAR =
            register("fear", com.soulinchains.arcaneartifacts.glyph.EffectFear.INSTANCE, TimelineOption.IMPACT, ModTimelines::impactOptions, BurstMotion::new, false);

    private ModTimelines() {
    }

    private static DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>> register(
            String name, AbstractSpellPart part, ResourceLocation mainName, Supplier<List<IParticleMotionType<?>>> options,
            Supplier<ParticleMotion> mainDefault, boolean impact) {
        AtomicReference<DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>>> ref = new AtomicReference<>();
        GlyphTimeline.Spec spec = new GlyphTimeline.Spec(() -> ref.get().get(), mainName, options, mainDefault, impact);
        DeferredHolder<IParticleTimelineType<?>, IParticleTimelineType<GlyphTimeline>> holder = REGISTER.register(name,
                () -> new SimpleParticleTimelineType<>(part, GlyphTimeline.codec(spec), GlyphTimeline.streamCodec(spec), () -> new GlyphTimeline(spec),
                        (timeline, level) -> new com.hollingsworth.arsnouveau.client.particle.timeline_previews.EmitterTimelinePreview(
                                timeline.main, impact ? timeline.impact : null, level, new net.minecraft.world.phys.Vec3(0, -0.5, 0), 40, 30f, 3, 3)));
        ref.set(holder);
        return holder;
    }

    static List<IParticleMotionType<?>> trailOptions() {
        return List.of(ParticleMotionRegistry.TRAIL_TYPE.get(), ParticleMotionRegistry.SPIRAL_TYPE.get(), ParticleMotionRegistry.HELIX_TYPE.get(),
                ParticleMotionRegistry.WAVE_TYPE.get(), ParticleMotionRegistry.ZIGZAG_TYPE.get());
    }

    static List<IParticleMotionType<?>> tickOptions() {
        return List.of(ParticleMotionRegistry.SPIRAL_TYPE.get(), ParticleMotionRegistry.TORNADO_TYPE.get(), ParticleMotionRegistry.HELIX_TYPE.get(),
                ParticleMotionRegistry.BURST_TYPE.get(), ParticleMotionRegistry.UPWARD_FIELD_TYPE.get(), ParticleMotionRegistry.WISP_TYPE.get());
    }

    static List<IParticleMotionType<?>> impactOptions() {
        return List.of(ParticleMotionRegistry.BURST_TYPE.get());
    }
}
