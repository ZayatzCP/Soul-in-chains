package com.soulinchains.arcaneartifacts.cult;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** A blow on the Confessor while reading costs the rite some quality. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class CultEvents {
    private CultEvents() {
    }

    /** Seal of Shelter (60% less) and the Interception (the covering Warden takes a share of the Confessor's blows). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCover(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0 || !(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        if (CultTools.warded(player)) {
            event.setAmount(event.getAmount() * (1F - CultTools.WARD_REDUCTION));
        }
        if (Cult.role(player) == CultRole.CONFESSOR) {
            Player warden = Cult.get(player).partner().map(player.level()::getPlayerByUUID).orElse(null);
            if (warden != null && warden != player && Cult.role(warden) == CultRole.WARDEN) {
                event.setAmount(CultTools.intercept(player, warden, event.getSource(), event.getAmount()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0 || !(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        RitualSealBlockEntity seal = RitualSealBlockEntity.ofConfessor(player);
        if (seal != null) {
            seal.onConfessorHurt();
        }
    }

    /** A pair broken while one of the two was away is cleared when they come back. */
    @SubscribeEvent
    public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        Cult.migrate(player);
        Cult.get(player).partner().ifPresent(id -> {
            Player partner = player.level().getServer() == null ? null : player.level().getServer().getPlayerList().getPlayer(id);
            if (partner != null) {
                Cult.reconcile(player, partner);
            }
        });
    }
}
