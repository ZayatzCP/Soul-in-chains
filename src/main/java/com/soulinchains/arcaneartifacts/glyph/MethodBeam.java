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
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Beam form: an instant ray from the caster's eyes along the look direction. It stops at the first
 * solid block (it never passes through blocks) and resolves the spell on the first creature in its way;
 * each Pierce lets it pass through one more creature. If it reaches no creature, it resolves on the
 * block it stopped at. Accelerate lengthens it.
 */
public class MethodBeam extends AbstractCastMethod {
    public static final MethodBeam INSTANCE = new MethodBeam();

    private MethodBeam() {
        super(ArcaneArtifacts.id("glyph_beam"), "Beam");
    }

    public void fire(Level level, LivingEntity caster, SpellStats stats, SpellResolver resolver) {
        double range = Config.BEAM_RANGE.get() + 8.0 * stats.getBuffCount(AugmentAccelerate.INSTANCE);
        Vec3 start = caster.getEyePosition();
        Vec3 end = start.add(caster.getLookAngle().scale(range));
        ClipContext.Block shape = stats.isSensitive() ? ClipContext.Block.OUTLINE : ClipContext.Block.COLLIDER;
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, shape, ClipContext.Fluid.NONE, caster));
        Vec3 stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

        int targets = 1 + stats.getBuffCount(AugmentPierce.INSTANCE);
        Set<Entity> hit = new HashSet<>();
        Vec3 from = start;
        boolean hitEntity = false;
        for (int i = 0; i < targets; i++) {
            AABB box = new AABB(from, stop).inflate(1.0);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, caster, from, stop, box,
                    e -> e.isAlive() && e.isPickable() && !e.isSpectator() && !hit.contains(e), 0.3f);
            if (entityHit == null) {
                break;
            }
            hit.add(entityHit.getEntity());
            hitEntity = true;
            resolver.onResolveEffect(level, entityHit);
            from = entityHit.getLocation();
        }
        if (!hitEntity && blockHit.getType() != HitResult.Type.MISS) {
            resolver.onResolveEffect(level, blockHit);
        }
        if (level instanceof ServerLevel server) {
            var style = com.soulinchains.arcaneartifacts.fx.Fx.timeline(resolver.spellContext, com.soulinchains.arcaneartifacts.fx.ModTimelines.BEAM);
            com.soulinchains.arcaneartifacts.fx.Fx.line(server, style.main, start.add(caster.getLookAngle().scale(0.6)), stop);
            com.soulinchains.arcaneartifacts.fx.Fx.burst(server, style.impact, stop);
            server.playSound(null, caster.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 1.8f);
        }
    }

    @Override
    public CastResolveType onCast(@Nullable ItemStack stack, LivingEntity caster, Level level, SpellStats stats, SpellContext context, SpellResolver resolver) {
        fire(level, caster, stats, resolver);
        return CastResolveType.SUCCESS;
    }

    @Override
    public CastResolveType onCastOnBlock(UseOnContext context, SpellStats stats, SpellContext spellContext, SpellResolver resolver) {
        LivingEntity caster = context.getPlayer() != null ? context.getPlayer() : spellContext.getUnwrappedCaster();
        fire(context.getLevel(), caster, stats, resolver);
        return CastResolveType.SUCCESS;
    }

    @Override
    public CastResolveType onCastOnBlock(BlockHitResult hit, LivingEntity caster, SpellStats stats, SpellContext spellContext, SpellResolver resolver) {
        fire(caster.level(), caster, stats, resolver);
        return CastResolveType.SUCCESS;
    }

    @Override
    public CastResolveType onCastOnEntity(@Nullable ItemStack stack, LivingEntity caster, Entity target, InteractionHand hand, SpellStats stats, SpellContext spellContext, SpellResolver resolver) {
        fire(caster.level(), caster, stats, resolver);
        return CastResolveType.SUCCESS;
    }

    @Override
    public int getDefaultManaCost() {
        return 30;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentPierce.INSTANCE, AugmentAccelerate.INSTANCE, AugmentSensitive.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentPierce.INSTANCE, "The beam passes through one more creature.");
        map.put(AugmentAccelerate.INSTANCE, "The beam reaches 8 blocks farther.");
        map.put(AugmentSensitive.INSTANCE, "The beam also stops at plants and other non-solid blocks.");
    }

    @Override
    public String getBookDescription() {
        return "An instant ray of magic from your eyes. It never passes through blocks: it stops at the first solid block and resolves on the first creature in its way (or on that block).";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.MANIPULATION);
    }
}
