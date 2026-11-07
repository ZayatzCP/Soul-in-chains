package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.entity.ArcaneMissileEntity;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.registry.ModTags;
import com.soulinchains.arcaneartifacts.util.Allies;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/**
 * Mantle of Reflection: a hostile magical projectile that is about to hit the wearer has a 15% chance
 * to be sent back toward whoever launched it, followed by a 12 s cooldown (item cooldown, visible on
 * the slot).
 * <p>
 * Uses {@link ProjectileImpactEvent}, which both vanilla projectiles and Ars spell projectiles fire
 * before applying their hit, so cancelling it cleanly prevents the hit. Eligible projectiles:
 * every Ars spell projectile ({@link EntityProjectileSpell}, including Arcane Missiles) plus the
 * entity tag {@code arcane_artifacts:reflectable_projectiles}. Never reflected: arrows and other
 * physical projectiles (not in the tag), melee, explosions (not projectiles), projectiles in
 * {@code arcane_artifacts:unreflectable_projectiles}, projectiles without a living owner (nothing to
 * aim at) and projectiles from allies. The reflected projectile keeps its spell and speed: it is not
 * amplified.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class MantleHandler {
    private MantleHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level) || !(event.getRayTraceResult() instanceof EntityHitResult hit)) {
            return;
        }
        if (!(hit.getEntity() instanceof Player wearer) || !isReflectable(projectile)) {
            return;
        }
        if (!(projectile.getOwner() instanceof LivingEntity caster) || caster == wearer || !caster.isAlive()
                || caster.level() != level || Allies.areAllied(caster, wearer)) {
            return;
        }
        ItemStack mantle = CurioHelper.findEquipped(wearer, ModItems.MANTLE_OF_REFLECTION.get()).orElse(ItemStack.EMPTY);
        if (mantle.isEmpty() || wearer.getCooldowns().isOnCooldown(mantle.getItem())) {
            return;
        }
        if (wearer.getRandom().nextDouble() >= Config.MANTLE_REFLECT_CHANCE.get()) {
            return;
        }
        event.setCanceled(true);
        reflect(projectile, wearer, caster);
        wearer.getCooldowns().addCooldown(mantle.getItem(), Config.MANTLE_COOLDOWN_TICKS.get());
        level.sendParticles(ParticleTypes.END_ROD, projectile.getX(), projectile.getY(), projectile.getZ(), 14, 0.25, 0.25, 0.25, 0.08);
        level.playSound(null, wearer.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.0f, 0.6f);
        wearer.displayClientMessage(Component.translatable("arcane_artifacts.mantle.reflected"), true);
    }

    private static boolean isReflectable(Projectile projectile) {
        if (projectile.getType().is(ModTags.UNREFLECTABLE_PROJECTILES)) {
            return false;
        }
        return projectile instanceof EntityProjectileSpell || projectile.getType().is(ModTags.REFLECTABLE_PROJECTILES);
    }

    private static void reflect(Projectile projectile, Player wearer, LivingEntity caster) {
        Vec3 velocity = projectile.getDeltaMovement();
        double speed = Math.max(velocity.length(), 0.3);
        Vec3 aim = caster.getBoundingBox().getCenter().subtract(projectile.position());
        Vec3 dir = aim.lengthSqr() > 1.0E-6 ? aim.normalize() : velocity.scale(-1).normalize();
        // The wearer becomes the owner: the projectile can no longer hit the wearer and can now hit the caster.
        projectile.setOwner(wearer);
        projectile.setPos(projectile.position().add(dir.scale(0.5)));
        projectile.setDeltaMovement(dir.scale(speed));
        projectile.hasImpulse = true;
        if (projectile instanceof ArcaneMissileEntity missile) {
            missile.forceTarget(caster);
        }
    }
}
