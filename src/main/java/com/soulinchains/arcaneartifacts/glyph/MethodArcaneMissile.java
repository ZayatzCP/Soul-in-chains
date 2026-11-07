package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.CastResolveType;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAccelerate;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDecelerate;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.entity.ArcaneMissileEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Arcane Missile form: launches a magic missile that seeks the nearest valid creature within
 * 16 blocks (more with Pursuit), chases it and resolves the rest of the spell on impact. Without a
 * target it flies straight and fades when its lifetime ends.
 */
public class MethodArcaneMissile extends AbstractCastMethod {
    public static final MethodArcaneMissile INSTANCE = new MethodArcaneMissile();

    private MethodArcaneMissile() {
        super(ArcaneArtifacts.id("glyph_arcane_missile"), "Arcane Missile");
    }

    public void summonMissiles(Level level, LivingEntity shooter, SpellStats stats, SpellResolver resolver) {
        int count = 1 + stats.getBuffCount(AugmentSplit.INSTANCE);
        int pursuit = stats.getBuffCount(AugmentPursuit.INSTANCE);
        float velocity = Math.max(0.2f, 0.6f + (float) stats.getAccMultiplier() / 4f);
        int opposite = -1;
        for (int i = 0; i < count; i++) {
            ArcaneMissileEntity missile = new ArcaneMissileEntity(level, resolver);
            missile.configure(pursuit, false);
            missile.shoot(shooter, shooter.getXRot(), shooter.getYRot() + Math.round(i / 2.0) * 10 * opposite, 0.0F, velocity, 0.6f);
            opposite = -opposite;
            level.addFreshEntity(missile);
        }
    }

    @Override
    public CastResolveType onCast(@Nullable ItemStack stack, LivingEntity caster, Level level, SpellStats stats, SpellContext context, SpellResolver resolver) {
        summonMissiles(level, caster, stats, resolver);
        return CastResolveType.SUCCESS;
    }

    @Override
    public CastResolveType onCastOnBlock(UseOnContext context, SpellStats stats, SpellContext spellContext, SpellResolver resolver) {
        LivingEntity caster = context.getPlayer() != null ? context.getPlayer() : spellContext.getUnwrappedCaster();
        summonMissiles(context.getLevel(), caster, stats, resolver);
        return CastResolveType.SUCCESS;
    }

    @Override
    public CastResolveType onCastOnBlock(BlockHitResult hit, LivingEntity caster, SpellStats stats, SpellContext spellContext, SpellResolver resolver) {
        summonMissiles(caster.level(), caster, stats, resolver);
        return CastResolveType.SUCCESS;
    }

    @Override
    public CastResolveType onCastOnEntity(@Nullable ItemStack stack, LivingEntity caster, Entity target, InteractionHand hand, SpellStats stats, SpellContext spellContext, SpellResolver resolver) {
        summonMissiles(caster.level(), caster, stats, resolver);
        return CastResolveType.SUCCESS;
    }

    @Override
    public int getDefaultManaCost() {
        return 35;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @Override
    protected void addDefaultAugmentLimits(Map<ResourceLocation, Integer> defaults) {
        defaults.put(AugmentPursuit.INSTANCE.getRegistryName(), 3);
        defaults.put(AugmentSplitting.INSTANCE.getRegistryName(), 1);
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentPierce.INSTANCE, AugmentSplit.INSTANCE, AugmentAccelerate.INSTANCE,
                AugmentDecelerate.INSTANCE, AugmentSensitive.INSTANCE, AugmentPursuit.INSTANCE, AugmentSplitting.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentPierce.INSTANCE, "The missile passes through an additional target or block.");
        map.put(AugmentSplit.INSTANCE, "Launches an additional missile.");
        map.put(AugmentAccelerate.INSTANCE, "The missile flies faster.");
        map.put(AugmentDecelerate.INSTANCE, "The missile flies slower.");
        map.put(AugmentSensitive.INSTANCE, "The missile also collides with plants and other non-solid blocks.");
        map.put(AugmentPursuit.INSTANCE, "Wider search radius, sharper turns and a longer lifetime.");
        map.put(AugmentSplitting.INSTANCE, "On hitting a creature, splits into two half-damage missiles that seek other targets.");
    }

    @Override
    public String getBookDescription() {
        return "Launches an arcane missile that seeks the nearest hostile creature within 16 blocks, preferring creatures bearing an Arcane Mark, and chases it. It never targets its caster, the caster's team, tamed pets or summons, cannot pass through blocks and gives up a chase after a few seconds. Without a target it flies straight and fades away.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.MANIPULATION);
    }
}
