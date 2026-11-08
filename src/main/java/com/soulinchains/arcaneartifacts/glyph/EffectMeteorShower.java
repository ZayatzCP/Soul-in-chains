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
import com.soulinchains.arcaneartifacts.phenomena.Meteor;
import com.soulinchains.arcaneartifacts.phenomena.Phenomena;

/** Meteor Shower: calls burning meteors down around the point of impact. Never breaks or ignites blocks. */
public class EffectMeteorShower extends AreaEffect {
    public static final EffectMeteorShower INSTANCE = new EffectMeteorShower();

    private EffectMeteorShower() {
        super(ArcaneArtifacts.id("glyph_meteor_shower"), "Meteor Shower");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        int count = Math.max(1, Config.METEOR_COUNT.get() + 2 * (int) Math.round(stats.getAoeMultiplier()));
        double spread = 2.5 + 1.5 * Math.max(0, stats.getAoeMultiplier());
        float damage = (float) Math.max(1.0, Config.METEOR_DAMAGE.get() + 2.0 * stats.getAmpMultiplier());
        var style = com.soulinchains.arcaneartifacts.fx.Fx.timeline(context, com.soulinchains.arcaneartifacts.fx.ModTimelines.METEOR_SHOWER);
        Phenomena.add(new com.soulinchains.arcaneartifacts.phenomena.AreaMarker(level, shooter.getUUID(), point.subtract(0, 0.4, 0), spread + 2.5, count * 6 + 30, style.impact));
        for (int i = 0; i < count; i++) {
            Vec3 spot = i == 0 ? point : point.add(level.random.nextGaussian() * spread, 0, level.random.nextGaussian() * spread);
            Phenomena.add(new Meteor(level, shooter.getUUID(), spot, i * 6, damage, 2.5, 22.0, style.main, style.impact));
        }
    }

    @Override
    public int getDefaultManaCost() {
        return 150;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.THREE;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAOE.INSTANCE, AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAOE.INSTANCE, "Two more meteors over a wider area.");
        map.put(AugmentAmplify.INSTANCE, "Heavier meteors.");
        map.put(AugmentDampen.INSTANCE, "Lighter meteors.");
    }

    @Override
    public String getBookDescription() {
        return "Calls a shower of burning meteors down around the place the spell lands. Each one bursts on impact, burning and throwing back the creatures around it. The caster is spared; blocks are not broken or set alight.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.ELEMENTAL_FIRE);
    }
}
