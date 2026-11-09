package com.soulinchains.arcaneartifacts.shield;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.accessory.MantleHandler;
import com.soulinchains.arcaneartifacts.entity.ArcaneMissileEntity;
import net.minecraft.core.particles.ParticleTypes;
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
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/** The Mirror Shield sends spells back; the Focusing Shield answers every block with its spell. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class ShieldHandler {
    private ShieldHandler() {
    }

    /** The raised shield of the player, or empty when not blocking with an item of this type. */
    private static ItemStack raised(Player player, Class<?> type) {
        if (!player.isBlocking()) {
            return ItemStack.EMPTY;
        }
        ItemStack using = player.getUseItem();
        return type.isInstance(using.getItem()) ? using : ItemStack.EMPTY;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level) || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof Player player)) {
            return;
        }
        if (!(projectile instanceof EntityProjectileSpell) && !(projectile instanceof ArcaneMissileEntity)) {
            return;
        }
        ItemStack shield = raised(player, MirrorShieldItem.class);
        if (shield.isEmpty() || projectile.getOwner() == player) {
            return;
        }
        // Only what comes from the front is caught by the mirror.
        Vec3 toProjectile = projectile.position().subtract(player.position()).multiply(1, 0, 1);
        Vec3 look = player.getViewVector(1.0f).multiply(1, 0, 1);
        if (toProjectile.lengthSqr() > 1.0E-4 && look.lengthSqr() > 1.0E-4 && toProjectile.normalize().dot(look.normalize()) < 0) {
            return;
        }
        LivingEntity caster = projectile.getOwner() instanceof LivingEntity owner && owner.isAlive() && owner.level() == level ? owner : null;
        event.setCanceled(true);
        MantleHandler.reflect(projectile, player, caster);
        shield.hurtAndBreak(1, player, player.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        level.sendParticles(ParticleTypes.END_ROD, projectile.getX(), projectile.getY(), projectile.getZ(), 16, 0.25, 0.25, 0.25, 0.1);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.3f);
    }

    @SubscribeEvent
    public static void onBlock(LivingShieldBlockEvent event) {
        if (!event.getBlocked() || !(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        ItemStack shield = raised(player, FocusingShieldItem.class);
        if (shield.isEmpty() || player.getCooldowns().isOnCooldown(shield.getItem())) {
            return;
        }
        Spell spell = FocusingShieldItem.storedSpell(shield);
        if (!FocusingShieldItem.isAllowed(spell)) {
            return;
        }
        player.getCooldowns().addCooldown(shield.getItem(), 20);
        SpellResolver resolver = new SpellResolver(SpellContext.fromEntity(spell, player, shield));
        try {
            resolver.onCast(shield, player.level());
        } catch (RuntimeException e) {
            ArcaneArtifacts.LOGGER.error("Focusing Shield cast failed for {}", player.getGameProfile().getName(), e);
        }
    }
}
