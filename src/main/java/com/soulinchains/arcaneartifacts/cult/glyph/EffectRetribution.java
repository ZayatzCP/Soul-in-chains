package com.soulinchains.arcaneartifacts.cult.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import com.soulinchains.arcaneartifacts.glyph.AreaEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;

/**
 * Retribution: the pair's rite-summoned creatures within 24 blocks of the target turn on it. Against an Inscribed target
 * they gain Strength I (+1 per Amplify) for 10 s. Abundance: +0.1 loot multiplier for each of them, once.
 */
public class EffectRetribution extends AreaEffect {
    public static final EffectRetribution INSTANCE = new EffectRetribution();

    private EffectRetribution() {
        super(ArcaneArtifacts.id("glyph_retribution"), "Retribution");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        CultGlyphs.hold(shooter, stats.getBuffCount(AugmentHoldCircle.INSTANCE));
        if (target instanceof LivingEntity living) {
            CultGlyphs.command(level, shooter, living, stats.getBuffCount(AugmentAmplify.INSTANCE), stats.getBuffCount(AugmentRiteAbundance.INSTANCE));
        }
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
        return augmentSetOf(AugmentAmplify.INSTANCE, AugmentRiteAbundance.INSTANCE, AugmentHoldCircle.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "Strength one level higher against the Inscribed.");
        map.put(AugmentRiteAbundance.INSTANCE, "+0.1 to the loot multiplier of each summon that obeys.");
        map.put(AugmentHoldCircle.INSTANCE, "Inside your rite's circle: its anchors gain 10 stability.");
    }

    @Override
    public String getBookDescription() {
        return "Your rite-summoned creatures within 24 blocks attack the target. Against an Inscribed target they gain Strength for 10 s.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.CONJURATION);
    }
}
