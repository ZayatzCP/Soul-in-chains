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
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.CastResolveType;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.cult.RitualServant;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Witness Call (form): a small summon that answers only a bound pair. With the partner online and within the link range,
 * a faint Ash Witness rises where you look (12 blocks) for 30 s and the rest of the spell resolves on it. Amplify: a common
 * witness; Census: one more faint witness each instead; Extend Time: +10 s; Abundance: a better loot roll. At most three
 * called witnesses per caster, once in 5 s.
 */
public class MethodWitnessCall extends AbstractCastMethod {
    public static final MethodWitnessCall INSTANCE = new MethodWitnessCall();
    public static final double REACH = 12;

    private MethodWitnessCall() {
        super(ArcaneArtifacts.id("glyph_witness_call"), "Witness Call");
    }

    private CastResolveType callUp(Level level, LivingEntity caster, SpellStats stats, SpellResolver resolver) {
        if (!(level instanceof ServerLevel server) || !(caster instanceof Player player)) {
            return CastResolveType.FAILURE;
        }
        Player partner = CultGlyphs.partnerInRange(player);
        if (partner == null && !(player.isCreative() && Config.CULT_CREATIVE_SOLO.get())) {
            CultGlyphs.say(player, "call_no_partner");
            return CastResolveType.FAILURE;
        }
        if (!CultGlyphs.ready(player, "call", CultGlyphs.CALL_COOLDOWN)) {
            CultGlyphs.say(player, "call_cooldown");
            return CastResolveType.FAILURE;
        }
        CultGlyphs.hold(player, stats.getBuffCount(AugmentHoldCircle.INSTANCE));
        Vec3 start = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(start, start.add(player.getLookAngle().scale(REACH)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 at = hit.getType() == HitResult.Type.MISS
                ? player.position().add(player.getLookAngle().x * 2, 0, player.getLookAngle().z * 2)
                : Vec3.atBottomCenterOf(hit.getBlockPos().relative(hit.getDirection()));
        List<RitualServant> risen = CultGlyphs.call(server, at, player, partner, stats.getBuffCount(AugmentAmplify.INSTANCE),
                stats.getBuffCount(AugmentCensus.INSTANCE), stats.getBuffCount(AugmentRiteAbundance.INSTANCE), stats.getBuffCount(AugmentExtendTime.INSTANCE));
        for (RitualServant servant : risen) {
            resolver.onResolveEffect(level, new EntityHitResult(servant));
        }
        return risen.isEmpty() ? CastResolveType.FAILURE : CastResolveType.SUCCESS;
    }

    @Override
    public CastResolveType onCast(@Nullable ItemStack stack, LivingEntity caster, Level level, SpellStats stats, SpellContext context, SpellResolver resolver) {
        return callUp(level, caster, stats, resolver);
    }

    @Override
    public CastResolveType onCastOnBlock(UseOnContext context, SpellStats stats, SpellContext spellContext, SpellResolver resolver) {
        LivingEntity caster = context.getPlayer() != null ? context.getPlayer() : spellContext.getUnwrappedCaster();
        return callUp(context.getLevel(), caster, stats, resolver);
    }

    @Override
    public CastResolveType onCastOnBlock(BlockHitResult hit, LivingEntity caster, SpellStats stats, SpellContext spellContext, SpellResolver resolver) {
        return callUp(caster.level(), caster, stats, resolver);
    }

    @Override
    public CastResolveType onCastOnEntity(@Nullable ItemStack stack, LivingEntity caster, Entity target, InteractionHand hand, SpellStats stats,
                                          SpellContext spellContext, SpellResolver resolver) {
        return callUp(caster.level(), caster, stats, resolver);
    }

    @Override
    public int getDefaultManaCost() {
        return 55;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE, AugmentCensus.INSTANCE, AugmentRiteAbundance.INSTANCE,
                AugmentHoldCircle.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "A common witness instead of a faint one.");
        map.put(AugmentExtendTime.INSTANCE, "The witnesses last 10 s longer.");
        map.put(AugmentCensus.INSTANCE, "One more faint witness instead of a stronger one.");
        map.put(AugmentRiteAbundance.INSTANCE, "+0.25 to their loot multiplier (up to x1.5).");
        map.put(AugmentHoldCircle.INSTANCE, "Inside your rite's circle: its anchors gain 10 stability.");
    }

    @Override
    public String getBookDescription() {
        return "Only for a bound pair: with your partner within the link range, a faint Ash Witness rises where you look for 30 s, and the rest of the spell resolves on it.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.CONJURATION);
    }
}
