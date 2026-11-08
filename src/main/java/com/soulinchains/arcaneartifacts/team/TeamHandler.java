package com.soulinchains.arcaneartifacts.team;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Objects;
import java.util.UUID;

/** Sworn companions (same company on their worn suit charms) cannot hurt each other or each other's pets. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class TeamHandler {
    private TeamHandler() {
    }

    /** The company of the suit charm the entity wears, or null. */
    public static @Nullable UUID teamOf(@Nullable Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        return CuriosApi.getCuriosInventory(living)
                .flatMap(inventory -> inventory.findFirstCurio(stack -> stack.getItem() instanceof SuitCharmItem && TeamBond.of(stack) != null))
                .map(result -> Objects.requireNonNull(TeamBond.of(result.stack())).team())
                .orElse(null);
    }

    /** The player behind an entity: itself, or the owner of a pet. */
    private static @Nullable Entity controller(@Nullable Entity entity) {
        if (entity instanceof OwnableEntity ownable && ownable.getOwner() != null) {
            return ownable.getOwner();
        }
        return entity;
    }

    /** True when both belong (directly or through their pets) to the same sworn company. */
    public static boolean sworn(@Nullable Entity a, @Nullable Entity b) {
        Entity ca = controller(a);
        Entity cb = controller(b);
        if (!(ca instanceof Player) || !(cb instanceof Player) || ca == cb) {
            return false;
        }
        UUID ta = teamOf(ca);
        return ta != null && ta.equals(teamOf(cb));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && attacker != event.getEntity() && sworn(attacker, event.getEntity())) {
            event.setCanceled(true);
        }
    }

}
