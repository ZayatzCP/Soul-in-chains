package com.soulinchains.arcaneartifacts.item;

import com.hollingsworth.arsnouveau.api.event.SpellCastEvent;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Crystal Ball: one cast every 3 seconds, and its projectile is a heavy ball thrown in an arc. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class CrystalBallHandler {
    private CrystalBallHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCast(SpellCastEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide || !CrystalBallItem.castFrom(event.context)) {
            return;
        }
        var ball = event.context.getCasterTool().getItem();
        if (event.getEntity() instanceof Player player) {
            if (player.getCooldowns().isOnCooldown(ball)) {
                event.setCanceled(true);
                player.displayClientMessage(Component.translatable("arcane_artifacts.crystal_ball.recharging").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            player.getCooldowns().addCooldown(ball, CrystalBallItem.COOLDOWN_TICKS);
        }
    }

    @SubscribeEvent
    public static void onProjectile(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof EntityProjectileSpell projectile
                && projectile.spellResolver != null && CrystalBallItem.castFrom(projectile.spellResolver.spellContext)) {
            // The Ars projectile flies straight; the Crystal Ball throws a real ball instead, which arcs under
            // gravity and resolves the same spell where it lands.
            if (projectile.getOwner() instanceof net.minecraft.world.entity.LivingEntity shooter) {
                event.setCanceled(true);
                CrystalOrbEntity orb = new CrystalOrbEntity(event.getLevel(), shooter, projectile.spellResolver);
                orb.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
                orb.throwFrom(shooter);
                event.getLevel().addFreshEntity(orb);
            }
        }
    }
}
