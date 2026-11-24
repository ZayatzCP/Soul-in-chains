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
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAccelerate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Inscribe (form): the Confessor draws the ritual sign on the creature they look at (16 blocks, +8 per Accelerate): it is
 * Inscribed for 30 s (Red Acolytes go for it first, Retribution strengthens the summons against it) and gets the rest of
 * the spell. With no creature in sight the spell resolves on the block looked at.
 */
public class MethodInscribe extends AbstractCastMethod {
    public static final MethodInscribe INSTANCE = new MethodInscribe();
    public static final double RANGE = 16;

    private MethodInscribe() {
        super(ArcaneArtifacts.id("glyph_inscribe"), "Inscribe");
    }

    private CastResolveType draw(Level level, LivingEntity caster, SpellStats stats, SpellResolver resolver) {
        CultGlyphs.hold(caster, stats.getBuffCount(AugmentHoldCircle.INSTANCE));
        double range = RANGE + 8.0 * stats.getBuffCount(AugmentAccelerate.INSTANCE);
        Vec3 start = caster.getEyePosition();
        Vec3 end = start.add(caster.getLookAngle().scale(range));
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, caster, start, stop, new AABB(start, stop).inflate(1.0),
                e -> e.isAlive() && e.isPickable() && !e.isSpectator(), 0.3F);
        if (entityHit != null) {
            if (entityHit.getEntity() instanceof LivingEntity living && !level.isClientSide) {
                CultGlyphs.inscribe(living, caster);
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.ENCHANT, living.getX(), living.getY() + living.getBbHeight() + 0.3, living.getZ(), 20, 0.3, 0.2, 0.3, 0.5);
                    server.playSound(null, living.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.6F);
                }
            }
            resolver.onResolveEffect(level, entityHit);
            return CastResolveType.SUCCESS;
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            resolver.onResolveEffect(level, blockHit);
            return CastResolveType.SUCCESS;
        }
        return CastResolveType.FAILURE;
    }

    @Override
    public CastResolveType onCast(@Nullable ItemStack stack, LivingEntity caster, Level level, SpellStats stats, SpellContext context, SpellResolver resolver) {
        return draw(level, caster, stats, resolver);
    }

    @Override
    public CastResolveType onCastOnBlock(UseOnContext context, SpellStats stats, SpellContext spellContext, SpellResolver resolver) {
        LivingEntity caster = context.getPlayer() != null ? context.getPlayer() : spellContext.getUnwrappedCaster();
        return draw(context.getLevel(), caster, stats, resolver);
    }

    @Override
    public CastResolveType onCastOnBlock(BlockHitResult hit, LivingEntity caster, SpellStats stats, SpellContext spellContext, SpellResolver resolver) {
        return draw(caster.level(), caster, stats, resolver);
    }

    @Override
    public CastResolveType onCastOnEntity(@Nullable ItemStack stack, LivingEntity caster, Entity target, InteractionHand hand, SpellStats stats,
                                          SpellContext spellContext, SpellResolver resolver) {
        return draw(caster.level(), caster, stats, resolver);
    }

    @Override
    public int getDefaultManaCost() {
        return 20;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.ONE;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAccelerate.INSTANCE, AugmentHoldCircle.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAccelerate.INSTANCE, "Reaches 8 blocks further.");
        map.put(AugmentHoldCircle.INSTANCE, "Inside your rite's circle: its anchors gain 10 stability.");
    }

    @Override
    public String getBookDescription() {
        return "The ritual sign lands on the creature you look at (16 blocks): Inscribed for 30 s, and the rest of the spell follows it. Red Acolytes go for the Inscribed first.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.ABJURATION);
    }
}
