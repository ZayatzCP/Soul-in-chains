package com.soulinchains.arcaneartifacts.necro;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.glyph.AreaEffect;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * Bond (Overseer): chains the target to the nearest other creature within {@link #RANGE} blocks for 8 seconds (+2 per
 * Extend Time). Damage to either one is split in half between both. On your own servant or ally it shares their pain.
 */
public class EffectBond extends AreaEffect {
    public static final EffectBond INSTANCE = new EffectBond();
    public static final double RANGE = 6.0;
    public static final int BASE_TICKS = 160;

    private EffectBond() {
        super(ArcaneArtifacts.id("glyph_bond"), "Bond");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        if (shooter instanceof Player player && !Necro.requireRank(player, Necro.OVERSEER)) {
            return;
        }
        if (!(target instanceof LivingEntity first) || first == shooter) {
            return;
        }
        LivingEntity second = level.getEntitiesOfClass(LivingEntity.class, first.getBoundingBox().inflate(RANGE),
                        e -> e != first && e != shooter && e.isAlive() && !e.isSpectator()).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(first))).orElse(null);
        if (second == null) {
            return;
        }
        int ticks = Math.max(40, BASE_TICKS + (int) Math.round(stats.getDurationMultiplier() * 40));
        bond(level, first, second, ticks);
    }

    public static void bond(ServerLevel level, LivingEntity first, LivingEntity second, int ticks) {
        BondLink link = new BondLink(ModEntities.BOND_LINK.get(), level);
        link.link(first, second, ticks);
        level.addFreshEntity(link);
        level.playSound(null, first.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.5F);
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
        return augmentSetOf(AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentExtendTime.INSTANCE, "The bond lasts two seconds longer.");
        map.put(AugmentDurationDown.INSTANCE, "The bond snaps sooner.");
    }

    @Override
    public String getBookDescription() {
        return "Overseer rank. Chains the target to the nearest other creature: for 8 seconds damage to either one is split between both.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.NECROMANCY);
    }
}
