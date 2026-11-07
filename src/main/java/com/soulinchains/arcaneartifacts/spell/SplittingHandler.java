package com.soulinchains.arcaneartifacts.spell;

import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import com.hollingsworth.arsnouveau.api.event.SpellProjectileHitEvent;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.entity.ArcaneMissileEntity;
import com.soulinchains.arcaneartifacts.glyph.AugmentPursuit;
import com.soulinchains.arcaneartifacts.glyph.AugmentSplitting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Splitting: when a projectile carrying the Splitting augment hits a creature, two child missiles are
 * spawned at the impact point. Children:
 * <ul>
 *     <li>are marked through {@link SpellContext#tag} so their spell damage is scaled by
 *     {@code damage_multiplier} (50% by default) in {@link #onSpellDamage};</li>
 *     <li>are flagged as split children and are never split again (no exponential growth);</li>
 *     <li>exclude the creature the parent hit, unless the config allows re-hits;</li>
 *     <li>cost no mana: they are part of the already paid cast, not new casts.</li>
 * </ul>
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class SplittingHandler {
    public static final String SPLIT_CHILD_TAG = ArcaneArtifacts.MODID + ":split_child";

    private SplittingHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onProjectileHit(SpellProjectileHitEvent event) {
        if (event.isCanceled()) {
            return;
        }
        EntityProjectileSpell projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level) || !(event.getHitResult() instanceof EntityHitResult entityHit)) {
            return;
        }
        Entity hit = entityHit.getEntity();
        if (!(hit instanceof LivingEntity) || hit.equals(projectile.getOwner())) {
            return;
        }
        if (projectile instanceof ArcaneMissileEntity missile && missile.isSplitChild()) {
            return;
        }
        SpellResolver resolver = projectile.resolver();
        if (resolver == null || resolver.spellContext.tag.getBoolean(SPLIT_CHILD_TAG)) {
            return;
        }
        var caster = resolver.spellContext.getUnwrappedCaster();
        int splitting = resolver.spell.getBuffsAtIndex(0, caster, AugmentSplitting.INSTANCE);
        if (splitting <= 0) {
            return;
        }
        int pursuit = resolver.spell.getBuffsAtIndex(0, caster, AugmentPursuit.INSTANCE);
        spawnChildren(level, projectile, resolver, hit, pursuit);
    }

    private static void spawnChildren(ServerLevel level, EntityProjectileSpell parent, SpellResolver resolver, Entity hit, int pursuit) {
        int count = Config.SPLIT_PROJECTILE_COUNT.get();
        Vec3 origin = parent.position();
        Vec3 parentVelocity = parent.getDeltaMovement();
        Vec3 forward = parentVelocity.lengthSqr() > 1.0E-4 ? parentVelocity.normalize() : new Vec3(0, 0, 1);
        double speed = Math.max(0.35, Math.min(parentVelocity.length(), 0.8));
        for (int i = 0; i < count; i++) {
            SpellResolver childResolver = resolver.clone();
            childResolver.spellContext.tag.putBoolean(SPLIT_CHILD_TAG, true);
            ArcaneMissileEntity child = new ArcaneMissileEntity(level, childResolver);
            child.configure(pursuit, true);
            child.setOwner(parent.getOwner());
            if (!Config.SPLIT_ALLOW_SAME_TARGET.get()) {
                child.exclude(hit.getUUID());
            }
            // Fan the children out sideways (+/-50 degrees) and slightly upward from the impact point.
            double angle = Math.toRadians(count == 1 ? 0 : -50 + 100.0 * i / (count - 1));
            Vec3 dir = forward.yRot((float) angle).add(0, 0.25, 0).normalize();
            child.setPos(origin.x + dir.x * 0.6, origin.y + 0.2, origin.z + dir.z * 0.6);
            child.shoot(dir.x, dir.y, dir.z, (float) speed, 0.0f);
            level.addFreshEntity(child);
        }
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, origin.x, origin.y, origin.z, 12, 0.2, 0.2, 0.2, 0.15);
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.6f, 1.6f);
    }

    /** Split children deal reduced damage. Only damage is scaled; other effects resolve normally. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onSpellDamage(SpellDamageEvent.Pre event) {
        if (event.context != null && event.context.tag.getBoolean(SPLIT_CHILD_TAG)) {
            event.damage *= Config.SPLIT_DAMAGE_MULTIPLIER.get().floatValue();
        }
    }
}
