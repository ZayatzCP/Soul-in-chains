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
import com.soulinchains.arcaneartifacts.entity.Gribulka;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gribulka Swarm (Root Magic): two Gribulkas (+1 per Amplify) pop out of the ground. For 15 seconds (+5 per
 * Extend Time) every monster nearby wants to stamp on them instead of on you; then they go back underground.
 */
public class EffectSwarm extends AreaEffect {
    public static final EffectSwarm INSTANCE = new EffectSwarm();
    public static final int BASE_TICKS = 300;

    private EffectSwarm() {
        super(ArcaneArtifacts.id("glyph_gribulka_swarm"), "Gribulka Swarm");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        int count = 2 + Math.max(0, (int) Math.round(stats.getAmpMultiplier()));
        int ticks = Math.max(60, BASE_TICKS + (int) Math.round(stats.getDurationMultiplier() * 100));
        Vec3 at = target != null ? target.position() : point;
        summon(level, at, count, ticks);
    }

    /** Summons decoy Gribulkas around a point. */
    public static List<Gribulka> summon(ServerLevel level, Vec3 at, int count, int ticks) {
        List<Gribulka> swarm = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Gribulka gribulka = ModEntities.GRIBULKA.get().create(level);
            if (gribulka == null) {
                continue;
            }
            double angle = Math.PI * 2 * i / count;
            gribulka.moveTo(at.x + Math.cos(angle) * 1.2, at.y, at.z + Math.sin(angle) * 1.2, level.random.nextFloat() * 360F, 0F);
            gribulka.makeDecoy(ticks);
            level.addFreshEntity(gribulka);
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, gribulka.getX(), gribulka.getY() + 0.3, gribulka.getZ(), 12, 0.3, 0.3, 0.3, 0);
            swarm.add(gribulka);
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.0f, 1.4f);
        return swarm;
    }

    @Override
    public int getDefaultManaCost() {
        return 50;
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
        map.put(AugmentAmplify.INSTANCE, "One more Gribulka.");
        map.put(AugmentExtendTime.INSTANCE, "The Gribulkas stay 5 seconds longer.");
        map.put(AugmentDurationDown.INSTANCE, "The Gribulkas leave sooner.");
    }

    @Override
    public String getBookDescription() {
        return "Two Gribulkas pop out of the ground and draw every monster nearby to themselves for 15 seconds.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.CONJURATION);
    }
}
