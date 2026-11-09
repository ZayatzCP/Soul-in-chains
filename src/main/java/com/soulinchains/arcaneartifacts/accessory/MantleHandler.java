package com.soulinchains.arcaneartifacts.accessory;

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
import org.jetbrains.annotations.Nullable;

/**
 * Mantle of Reflection: any projectile about to hit the wearer - arrows, tridents, fireballs, potions,
 * Ars spell projectiles - has a {@code reflect_chance} (default 50%) to be sent back toward whoever
 * launched it (or straight back along its path when it has no living owner). Optional cooldown
 * (default none), shown on the slot.
 * <p>
 * Uses {@link ProjectileImpactEvent}, fired by vanilla and Ars projectiles before they apply their hit,
 * so cancelling it cleanly prevents the hit. Never reflected: projectiles in
 * {@code arcane_artifacts:unreflectable_projectiles} and projectiles shot by the wearer's allies.
 * The reflected projectile keeps its speed and effects; the wearer becomes its owner.
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
        LivingEntity caster = projectile.getOwner() instanceof LivingEntity owner && owner != wearer && owner.isAlive()
                && owner.level() == level ? owner : null;
        if (projectile.getOwner() == wearer || (caster != null && Allies.areAllied(caster, wearer))) {
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
        if (Config.MANTLE_COOLDOWN_TICKS.get() > 0) {
            wearer.getCooldowns().addCooldown(mantle.getItem(), Config.MANTLE_COOLDOWN_TICKS.get());
        }
        level.sendParticles(ParticleTypes.END_ROD, projectile.getX(), projectile.getY(), projectile.getZ(), 14, 0.25, 0.25, 0.25, 0.08);
        level.playSound(null, wearer.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.0f, 0.6f);
        wearer.displayClientMessage(Component.translatable("arcane_artifacts.mantle.reflected"), true);
    }

    private static boolean isReflectable(Projectile projectile) {
        return !projectile.getType().is(ModTags.UNREFLECTABLE_PROJECTILES);
    }

    public static void reflect(Projectile projectile, Player wearer, @Nullable LivingEntity caster) {
        Vec3 velocity = projectile.getDeltaMovement();
        double speed = Math.max(velocity.length(), 0.3);
        Vec3 back = velocity.lengthSqr() > 1.0E-6 ? velocity.scale(-1).normalize() : wearer.getLookAngle();
        Vec3 aim = caster == null ? Vec3.ZERO : caster.getBoundingBox().getCenter().subtract(projectile.position());
        Vec3 dir = aim.lengthSqr() > 1.0E-6 ? aim.normalize() : back;
        // The wearer becomes the owner: the projectile can no longer hit the wearer and can now hit the caster.
        projectile.setOwner(wearer);
        projectile.setPos(projectile.position().add(dir.scale(0.5)));
        projectile.setDeltaMovement(dir.scale(speed));
        projectile.hasImpulse = true;
        if (projectile instanceof ArcaneMissileEntity missile && caster != null) {
            missile.forceTarget(caster);
        }
    }
}
