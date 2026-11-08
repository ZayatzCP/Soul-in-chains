package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import com.soulinchains.arcaneartifacts.phenomena.Phenomena;
import com.soulinchains.arcaneartifacts.phenomena.Vortex;

/** Tornado: a whirlwind at the point of impact that spins creatures and items around and lifts them up. */
public class EffectTornado extends AreaEffect {
    public static final EffectTornado INSTANCE = new EffectTornado();

    private EffectTornado() {
        super(ArcaneArtifacts.id("glyph_tornado"), "Tornado");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        Vec3 base = target != null ? target.position() : point.subtract(0, 0.5, 0);
        double radius = Math.max(2.0, Config.TORNADO_RADIUS.get() + 1.5 * stats.getAoeMultiplier());
        int duration = Math.max(20, Config.TORNADO_SECONDS.get() * 20 + stats.getDurationInTicks() * 2);
        double strength = Math.max(0.2, 1.0 + 0.25 * stats.getAmpMultiplier());
        var style = com.soulinchains.arcaneartifacts.fx.Fx.timeline(context, com.soulinchains.arcaneartifacts.fx.ModTimelines.TORNADO);
        Phenomena.add(new Vortex(level, shooter.getUUID(), Vortex.Kind.TORNADO, base, radius, duration, 0f, strength, style.main));
        com.soulinchains.arcaneartifacts.fx.Fx.area(level, style.impact, base, radius);
        level.playSound(null, base.x, base.y, base.z, net.minecraft.sounds.SoundEvents.ELYTRA_FLYING, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    @Override
    public int getDefaultManaCost() {
        return 70;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAOE.INSTANCE, AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE, AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAOE.INSTANCE, "A wider whirlwind.");
        map.put(AugmentAmplify.INSTANCE, "Spins faster and lifts higher.");
        map.put(AugmentDampen.INSTANCE, "A gentler breeze.");
        map.put(AugmentExtendTime.INSTANCE, "The tornado lasts longer.");
        map.put(AugmentDurationDown.INSTANCE, "The tornado dies down sooner.");
    }

    @Override
    public String getBookDescription() {
        return "Raises a tornado where the spell lands. It spins creatures and items around and lifts them into the air; when it dies down, they fall. The caster is never caught.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.ELEMENTAL_AIR);
    }
}
