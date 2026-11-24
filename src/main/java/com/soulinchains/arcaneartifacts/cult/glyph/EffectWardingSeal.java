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
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;

/**
 * Warding Seal: a short protective circle around the point, radius 3 (+1 per AOE) for 4 s (+2 s per Extend Time). The caster,
 * their partner and the pair's rite-summoned creatures inside are Warded (60% less damage, no knockback). Once in 8 s.
 */
public class EffectWardingSeal extends AreaEffect {
    public static final EffectWardingSeal INSTANCE = new EffectWardingSeal();

    private EffectWardingSeal() {
        super(ArcaneArtifacts.id("glyph_warding_seal"), "Warding Seal");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        CultGlyphs.hold(shooter, stats.getBuffCount(AugmentHoldCircle.INSTANCE));
        Vec3 at = target != null ? target.position() : point;
        CultGlyphs.lay(level, at, shooter, stats.getBuffCount(AugmentAOE.INSTANCE), stats.getBuffCount(AugmentExtendTime.INSTANCE));
    }

    @Override
    public int getDefaultManaCost() {
        return 25;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.ONE;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAOE.INSTANCE, AugmentExtendTime.INSTANCE, AugmentHoldCircle.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAOE.INSTANCE, "The circle is a block wider.");
        map.put(AugmentExtendTime.INSTANCE, "The circle holds 2 s longer.");
        map.put(AugmentHoldCircle.INSTANCE, "Inside your rite's circle: its anchors gain 10 stability.");
    }

    @Override
    public String getBookDescription() {
        return "A short protective circle (radius 3, 4 s) around the point: you, your partner and your rite-summoned creatures inside take 60% less damage and no knockback. Once in 8 s.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.ABJURATION);
    }
}
