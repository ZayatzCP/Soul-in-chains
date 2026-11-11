package com.soulinchains.arcaneartifacts.spell;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.glyph.AugmentAura;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Moving auras of spell projectiles carrying the Aura augment. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class AuraHandler {
    private static final int PERIOD = 5;

    private record Aura(EntityProjectileSpell projectile, double radius, boolean blocks, Set<UUID> touched, Set<BlockPos> touchedBlocks) {
    }

    private static final List<Aura> ACTIVE = new ArrayList<>();

    private AuraHandler() {
    }

    public static double radiusFor(int aoe) {
        return 2.0 + 1.0 * Math.max(0, aoe);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof EntityProjectileSpell projectile) || projectile.spellResolver == null) {
            return;
        }
        var parts = projectile.spellResolver.spellContext.getSpell().unsafeList();
        if (!parts.contains(AugmentAura.INSTANCE)) {
            return;
        }
        int aoe = (int) parts.stream().filter(p -> p == AugmentAOE.INSTANCE).count();
        boolean blocks = parts.contains(AugmentSensitive.INSTANCE);
        ACTIVE.add(new Aura(projectile, radiusFor(aoe), blocks, new HashSet<>(), new HashSet<>()));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Aura> it = ACTIVE.iterator();
        List<Runnable> work = new ArrayList<>();
        while (it.hasNext()) {
            Aura aura = it.next();
            EntityProjectileSpell projectile = aura.projectile();
            if (projectile.isRemoved() || !projectile.isAlive()) {
                it.remove();
                continue;
            }
            if (projectile.level() != level || projectile.tickCount % PERIOD != 0) {
                continue;
            }
            work.add(() -> pulse(level, aura));
        }
        work.forEach(Runnable::run);
    }

    private static void pulse(ServerLevel level, Aura aura) {
        EntityProjectileSpell projectile = aura.projectile();
        SpellResolver base = projectile.spellResolver;
        Vec3 at = projectile.position();
        double r = aura.radius();
        level.sendParticles(ParticleTypes.WITCH, at.x, at.y, at.z, 6, r * 0.4, r * 0.4, r * 0.4, 0.0);
        UUID owner = projectile.getOwner() == null ? null : projectile.getOwner().getUUID();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && e.distanceToSqr(at) <= r * r && !e.getUUID().equals(owner))) {
            if (aura.touched().add(living.getUUID())) {
                resolveCopy(level, base, new EntityHitResult(living));
            }
        }
        if (aura.blocks()) {
            BlockPos under = BlockPos.containing(at);
            for (int dy = 0; dy <= (int) Math.ceil(r); dy++) {
                BlockPos pos = under.below(dy);
                if (!level.getBlockState(pos).isAir()) {
                    if (aura.touchedBlocks().add(pos)) {
                        resolveCopy(level, base, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                    }
                    break;
                }
            }
        }
    }

    /** Resolves the projectile's spell on a copy of its context, so the projectile itself keeps flying intact. */
    private static void resolveCopy(ServerLevel level, SpellResolver base, net.minecraft.world.phys.HitResult hit) {
        try {
            SpellResolver copy = base.getNewResolver(base.spellContext.clone());
            copy.onResolveEffect(level, hit);
        } catch (RuntimeException e) {
            ArcaneArtifacts.LOGGER.error("Aura failed to resolve", e);
        }
    }
}
