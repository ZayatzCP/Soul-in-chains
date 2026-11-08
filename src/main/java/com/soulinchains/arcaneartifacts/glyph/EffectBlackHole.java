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

/**
 * Black Hole: opens a singularity at the point of impact that pulls in creatures, items and projectiles
 * and grinds whatever reaches its core with magic damage. The caster is never pulled. Blocks are safe.
 */
public class EffectBlackHole extends AreaEffect {
    public static final EffectBlackHole INSTANCE = new EffectBlackHole();

    private EffectBlackHole() {
        super(ArcaneArtifacts.id("glyph_black_hole"), "Black Hole");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        double radius = Math.max(2.0, Config.BLACK_HOLE_RADIUS.get() + 2.0 * stats.getAoeMultiplier());
        int duration = Math.max(20, Config.BLACK_HOLE_SECONDS.get() * 20 + stats.getDurationInTicks() * 2);
        float damage = (float) Math.max(0.0, Config.BLACK_HOLE_DAMAGE.get() + 1.5 * stats.getAmpMultiplier());
        double strength = Math.max(0.05, 0.22 + 0.05 * stats.getAmpMultiplier());
        var style = com.soulinchains.arcaneartifacts.fx.Fx.timeline(context, com.soulinchains.arcaneartifacts.fx.ModTimelines.BLACK_HOLE);
        Phenomena.add(new Vortex(level, shooter.getUUID(), Vortex.Kind.BLACK_HOLE, point, radius, duration, damage, strength, style.main, style.impact));
        com.soulinchains.arcaneartifacts.fx.Fx.area(level, style.impact, point, radius);
        level.playSound(null, point.x, point.y, point.z, net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 0.6f);
    }

    @Override
    public int getDefaultManaCost() {
        return 120;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.THREE;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAOE.INSTANCE, AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE, AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAOE.INSTANCE, "Widens the pull by 2 blocks.");
        map.put(AugmentAmplify.INSTANCE, "Stronger pull and more damage in the core.");
        map.put(AugmentDampen.INSTANCE, "Weaker pull and less damage.");
        map.put(AugmentExtendTime.INSTANCE, "The singularity lasts longer.");
        map.put(AugmentDurationDown.INSTANCE, "The singularity collapses sooner.");
    }

    @Override
    public String getBookDescription() {
        return "Opens a small black hole where the spell lands. It pulls in creatures, items and projectiles for a few seconds and damages whatever reaches its core. The caster is never pulled, and blocks are never broken.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.MANIPULATION);
    }
}
