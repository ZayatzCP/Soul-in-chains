package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Weather Shift: changes the weather of the dimension. No Amplify: clear sky. One Amplify: rain (snow
 * where it is cold). Two or more: thunderstorm. Only works in dimensions with a sky.
 */
public class EffectWeatherShift extends AreaEffect {
    public static final EffectWeatherShift INSTANCE = new EffectWeatherShift();

    public enum Weather { CLEAR, RAIN, THUNDER }

    private EffectWeatherShift() {
        super(ArcaneArtifacts.id("glyph_weather_shift"), "Weather Shift");
    }

    public static Weather weatherFor(SpellStats stats) {
        int amplify = stats.getBuffCount(AugmentAmplify.INSTANCE);
        return amplify <= 0 ? Weather.CLEAR : amplify == 1 ? Weather.RAIN : Weather.THUNDER;
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) {
            if (shooter instanceof Player player) {
                player.displayClientMessage(Component.translatable("arcane_artifacts.weather.no_sky"), true);
            }
            return;
        }
        int ticks = Math.max(1200, Config.WEATHER_SECONDS.get() * 20 + stats.getDurationInTicks() * 30);
        Weather weather = weatherFor(stats);
        apply(level, weather, ticks);
        com.soulinchains.arcaneartifacts.fx.Fx.area(level, com.soulinchains.arcaneartifacts.fx.Fx.timeline(context, com.soulinchains.arcaneartifacts.fx.ModTimelines.WEATHER_SHIFT).main, shooter.position().add(0, 0.2, 0), 3.0);
        level.playSound(null, shooter.blockPosition(), weather == Weather.THUNDER ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.WEATHER, 0.8f, 1.0f);
        if (shooter instanceof Player player) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.weather." + weather.name().toLowerCase(java.util.Locale.ROOT)), true);
        }
    }

    public static void apply(ServerLevel level, Weather weather, int ticks) {
        switch (weather) {
            case CLEAR -> level.setWeatherParameters(ticks, 0, false, false);
            case RAIN -> level.setWeatherParameters(0, ticks, true, false);
            case THUNDER -> level.setWeatherParameters(0, ticks, true, true);
        }
    }

    @Override
    public int getDefaultManaCost() {
        return 100;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "One: rain. Two: thunderstorm. None: clear sky.");
        map.put(AugmentExtendTime.INSTANCE, "The new weather lasts longer.");
        map.put(AugmentDurationDown.INSTANCE, "The new weather passes sooner.");
    }

    @Override
    public String getBookDescription() {
        return "Changes the weather. Without Amplify the sky clears, with one Amplify it rains (or snows), with two a thunderstorm breaks out. Only works where there is a sky.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.ELEMENTAL_AIR);
    }
}
