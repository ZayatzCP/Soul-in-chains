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
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.soulinchains.arcaneartifacts.cult.RitualSealBlockEntity;
import net.minecraft.world.phys.HitResult;

/**
 * Second Voice: both members of a pair cast it during their rite, within 2 s of each other: the rite gains 8% quality and
 * every anchor 20 stability, once per rite. Needs two players.
 */
public class EffectSecondVoice extends AbstractEffect {
    public static final EffectSecondVoice INSTANCE = new EffectSecondVoice();

    private EffectSecondVoice() {
        super(ArcaneArtifacts.id("glyph_second_voice"), "Second Voice");
    }

    @Override
    public void onResolve(HitResult hit, Level level, @NotNull LivingEntity shooter, SpellStats stats, SpellContext context, SpellResolver resolver) {
        if (level.isClientSide || !(shooter instanceof Player player)) {
            return;
        }
        voice(player, stats.getBuffCount(AugmentHoldCircle.INSTANCE));
    }

    /** Returns true when this voice completed the pair's chord. */
    public static boolean voice(Player player, int holds) {
        RitualSealBlockEntity seal = RitualSealBlockEntity.ofConfessor(player);
        if (seal == null) {
            seal = RitualSealBlockEntity.ofWarden(player);
        }
        if (seal == null) {
            CultGlyphs.say(player, "voice_no_rite");
            return false;
        }
        CultGlyphs.hold(player, holds);
        boolean done = seal.secondVoice(player);
        CultGlyphs.say(player, done ? "voice_done" : "voice_waiting");
        return done;
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
        return augmentSetOf(AugmentHoldCircle.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentHoldCircle.INSTANCE, "Inside your rite's circle: its anchors gain 10 stability.");
    }

    @Override
    public String getBookDescription() {
        return "Needs both of the pair: the Confessor and the Warden cast it during their rite within 2 s of each other, and the rite gains 8% quality and every anchor 20 stability. Once per rite.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.ABJURATION);
    }
}
