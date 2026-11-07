package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.IPotionEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * Arcane Mark: places a visible arcane mark on the creature for 8 seconds. A creature carries at most
 * one mark: re-marking only refreshes the duration. Marked creatures are the preferred targets of
 * Arcane Missile and of split projectiles.
 */
public class EffectArcaneMark extends AbstractEffect implements IPotionEffect {
    public static final EffectArcaneMark INSTANCE = new EffectArcaneMark();

    private EffectArcaneMark() {
        super(ArcaneArtifacts.id("glyph_arcane_mark"), "Arcane Mark");
    }

    @Override
    public void onResolveEntity(EntityHitResult hit, Level level, @NotNull LivingEntity shooter, SpellStats stats, SpellContext context, SpellResolver resolver) {
        if (!(hit.getEntity() instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        int ticks = Math.max(20, getBaseDuration() * 20 + getExtendTimeDuration() * stats.getDurationInTicks());
        // amplifier is always 0: one mark per creature, a new mark refreshes the old one.
        MobEffectInstance existing = living.getEffect(ModEffects.ARCANE_MARK);
        if (existing != null && existing.getDuration() >= ticks) {
            return;
        }
        living.addEffect(new MobEffectInstance(ModEffects.ARCANE_MARK, ticks, 0, false, true, true), shooter);
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        super.buildConfig(builder);
        addPotionConfig(builder, 8);
        addExtendTimeConfig(builder, 4);
    }

    @Override
    public int getBaseDuration() {
        return POTION_TIME == null ? 8 : POTION_TIME.get();
    }

    @Override
    public int getExtendTimeDuration() {
        return EXTEND_TIME == null ? 4 : EXTEND_TIME.get();
    }

    @Override
    public int getDefaultManaCost() {
        return 15;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.ONE;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentExtendTime.INSTANCE, "Increases the duration of the mark.");
        map.put(AugmentDurationDown.INSTANCE, "Decreases the duration of the mark.");
    }

    @Override
    public String getBookDescription() {
        return "Brands the target with an arcane mark for 8 seconds, visible to every player. A creature can bear only one mark; marking it again refreshes the mark. Arcane Missiles and split projectiles prefer marked creatures.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.MANIPULATION);
    }
}
