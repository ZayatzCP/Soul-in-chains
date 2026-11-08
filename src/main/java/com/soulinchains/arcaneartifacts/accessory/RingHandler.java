package com.soulinchains.arcaneartifacts.accessory;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Ring of Reversal: an incoming hit larger than {@code big_hit_fraction} (default half) of the wearer's
 * maximum health is cancelled, and the creature responsible for it (the shooter, for projectiles) is
 * thrown back roughly {@code blocks_per_damage} blocks per point of the cancelled damage (capped).
 * Damage that bypasses invulnerability (/kill, the void) is never cancelled. An optional cooldown
 * (default 0) is shown on the ring's slot.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class RingHandler {
    /** Horizontal travel of a launched mob is roughly velocity × this factor (air drag 0.91 per tick, ~1 s of flight). */
    private static final double TRAVEL_PER_VELOCITY = 9.0;

    private RingHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float damage = event.getAmount();
        if (damage <= player.getMaxHealth() * Config.RING_BIG_HIT_FRACTION.get()) {
            return;
        }
        ItemStack ring = CurioHelper.findEquipped(player, ModItems.RING_OF_REVERSAL.get()).orElse(ItemStack.EMPTY);
        if (ring.isEmpty() || player.getCooldowns().isOnCooldown(ring.getItem())) {
            return;
        }
        event.setCanceled(true);
        if (Config.RING_COOLDOWN_TICKS.get() > 0) {
            player.getCooldowns().addCooldown(ring.getItem(), Config.RING_COOLDOWN_TICKS.get());
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && attacker != player && attacker.isAlive()) {
            throwBack(attacker, player, damage);
        }
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7f, 1.4f);
        }
        player.displayClientMessage(Component.translatable("arcane_artifacts.ring.reversed", Math.round(damage)), true);
    }

    /** Launches the attacker away from the wearer so that it lands about {@code damage} blocks away. */
    public static void throwBack(Entity attacker, Player wearer, float damage) {
        double blocks = Math.min(Config.RING_MAX_KNOCKBACK_BLOCKS.get(), damage * Config.RING_BLOCKS_PER_DAMAGE.get());
        Vec3 away = attacker.position().subtract(wearer.position());
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < 1.0E-4) {
            away = wearer.getLookAngle().multiply(1, 0, 1);
        }
        Vec3 horizontal = away.normalize().scale(blocks / TRAVEL_PER_VELOCITY);
        double vertical = 0.45 + Math.min(0.6, blocks / 60.0);
        attacker.setDeltaMovement(horizontal.x, vertical, horizontal.z);
        attacker.hasImpulse = true;
        attacker.hurtMarked = true;
    }
}
