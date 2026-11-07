package com.soulinchains.arcaneartifacts.accessory;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModAttachments;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Ring of Reversal charging. Anti-exploit rules:
 * <ul>
 *     <li>only damage that was really applied (after armor, i-frames, redirection) counts;</li>
 *     <li>hits below {@code min_damage} are ignored (no cactus/berry-bush farming with 0.5 damage);</li>
 *     <li>self-inflicted damage is ignored;</li>
 *     <li>at most one charging hit per {@code min_ticks_between_charges} (saved on the player, so relogging does not reset it);</li>
 *     <li>charge stops at the maximum.</li>
 * </ul>
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class RingHandler {
    private RingHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player) || player instanceof FakePlayer || player.level().isClientSide) {
            return;
        }
        if (event.getNewDamage() < Config.RING_MIN_DAMAGE.get() || event.getSource().getEntity() == player) {
            return;
        }
        ItemStack ring = CurioHelper.findEquipped(player, ModItems.RING_OF_REVERSAL.get()).orElse(ItemStack.EMPTY);
        if (ring.isEmpty()) {
            return;
        }
        int max = Config.RING_MAX_CHARGE.get();
        int charge = RingOfReversalItem.charge(ring);
        if (charge >= max) {
            return;
        }
        long now = player.level().getGameTime();
        long last = player.getData(ModAttachments.LAST_REVERSAL_CHARGE);
        if (now - last < Config.RING_MIN_TICKS_BETWEEN_CHARGES.get() && now >= last) {
            return;
        }
        player.setData(ModAttachments.LAST_REVERSAL_CHARGE, now);
        ring.set(ModComponents.REVERSAL_CHARGE, Math.min(max, charge + Config.RING_CHARGE_PER_HIT.get()));
    }
}
