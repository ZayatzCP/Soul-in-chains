package com.soulinchains.arcaneartifacts.necro;

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
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
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
 * Chain (form): a chain flies from the caster; the first creature it hooks gets the rest of the spell and is reeled in.
 * With Dampen the caster is reeled to the creature or to the block instead.
 */
public class MethodChain extends AbstractCastMethod {
    public static final MethodChain INSTANCE = new MethodChain();

    private MethodChain() {
        super(ArcaneArtifacts.id("glyph_chain"), "Chain");
    }

    private CastResolveType throwChain(LivingEntity caster, Level level, SpellStats stats, SpellResolver resolver) {
        if (!level.isClientSide) {
            float speed = 1.6F + 0.4F * stats.getBuffCount(AugmentAccelerate.INSTANCE);
            level.addFreshEntity(new ChainBolt(level, caster, resolver, stats.hasBuff(AugmentDampen.INSTANCE), speed));
        }
        return CastResolveType.SUCCESS;
    }

    @Override
    public CastResolveType onCast(@Nullable ItemStack stack, LivingEntity caster, Level level, SpellStats stats, SpellContext context, SpellResolver resolver) {
        return throwChain(caster, level, stats, resolver);
    }

    @Override
    public CastResolveType onCastOnBlock(UseOnContext context, SpellStats stats, SpellContext spellContext, SpellResolver resolver) {
        return context.getPlayer() == null ? CastResolveType.FAILURE : throwChain(context.getPlayer(), context.getLevel(), stats, resolver);
    }

    @Override
    public CastResolveType onCastOnBlock(BlockHitResult hit, LivingEntity caster, SpellStats stats, SpellContext spellContext, SpellResolver resolver) {
        return throwChain(caster, caster.level(), stats, resolver);
    }

    @Override
    public CastResolveType onCastOnEntity(@Nullable ItemStack stack, LivingEntity caster, Entity target, InteractionHand hand, SpellStats stats,
                                          SpellContext spellContext, SpellResolver resolver) {
        return throwChain(caster, caster.level(), stats, resolver);
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
        return augmentSetOf(AugmentDampen.INSTANCE, AugmentAccelerate.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentDampen.INSTANCE, "Reels you to the creature or block instead of pulling the creature.");
        map.put(AugmentAccelerate.INSTANCE, "The chain flies faster.");
    }

    @Override
    public String getBookDescription() {
        return "A chain flies from your hand; the first creature it hooks gets the rest of the spell and is reeled in to you. Bosses are too heavy to pull.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.NECROMANCY);
    }
}
