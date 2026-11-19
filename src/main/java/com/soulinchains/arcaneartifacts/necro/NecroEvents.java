package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import com.hollingsworth.arsnouveau.api.event.MaxManaCalcEvent;
import com.soulinchains.arcaneartifacts.Config;
import net.minecraft.world.entity.player.Player;

/**
 * Shackled creatures cannot teleport away (endermen, chorus fruit, ender pearls); Bond splits damage between two
 * creatures; servants hold a share of their master's maximum mana.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class NecroEvents {
    private NecroEvents() {
    }

    @SubscribeEvent
    public static void onEnderTeleport(EntityTeleportEvent.EnderEntity event) {
        cancelIfShackled(event);
    }

    @SubscribeEvent
    public static void onChorus(EntityTeleportEvent.ChorusFruit event) {
        cancelIfShackled(event);
    }

    @SubscribeEvent
    public static void onPearl(EntityTeleportEvent.EnderPearl event) {
        cancelIfShackled(event);
    }

    /** A phantom cannot be hurt (except by the void or /kill) and cannot attack. */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onPhantomHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity().hasEffect(ModEffects.PHANTOM) && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPhantomAttack(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity().hasEffect(ModEffects.PHANTOM)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPhantomTargeted(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() != null && event.getNewAboutToBeSetTarget().hasEffect(ModEffects.PHANTOM)) {
            event.setCanceled(true);
        }
    }

    /** A Shackled who dies with a full lantern: half the souls burst out as Restless (up to five). */
    @SubscribeEvent
    public static void onPlayerDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        int total = Souls.total(player);
        if (total < RELEASE_THRESHOLD) {
            return;
        }
        int lost = total / 2;
        int released = 0;
        for (SoulType type : SoulType.values()) {
            while (released < lost && Souls.take(player, type)) {
                released++;
            }
        }
        for (int i = 0; i < Math.min(released, 5); i++) {
            var ghost = com.soulinchains.arcaneartifacts.registry.ModEntities.RESTLESS.get().create(level);
            if (ghost != null) {
                ghost.moveTo(player.getX() + level.random.nextGaussian(), player.getY() + 1, player.getZ() + level.random.nextGaussian(), 0, 0);
                ghost.shortLived();
                level.addFreshEntity(ghost);
            }
        }
    }

    public static final int RELEASE_THRESHOLD = 10;

    /** At night a Soul Eater may come for a Shackled with Corruption 50 or more. */
    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level) || player.tickCount % 200 != 0 || player.isCreative() || player.isSpectator()
                || level.isDay() || Corruption.stage(player) < 2 || Souls.total(player) <= 0 || level.random.nextInt(EATER_CHANCE) != 0
                || !level.getEntitiesOfClass(com.soulinchains.arcaneartifacts.necro.mob.SoulEater.class, player.getBoundingBox().inflate(64)).isEmpty()) {
            return;
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double distance = 16 + level.random.nextInt(9);
            int x = net.minecraft.util.Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = net.minecraft.util.Mth.floor(player.getZ() + Math.sin(angle) * distance);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - player.getY()) > 12) {
                continue;
            }
            var eater = com.soulinchains.arcaneartifacts.registry.ModEntities.SOUL_EATER.get().create(level);
            if (eater == null) {
                return;
            }
            eater.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360, 0);
            if (level.noCollision(eater)) {
                eater.setTarget(player);
                level.addFreshEntity(eater);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("arcane_artifacts.soul_eater.coming"), true);
                return;
            }
        }
    }

    /** One in this many checks (every 10 seconds) at night brings a Soul Eater: about once per 25 minutes. */
    public static final int EATER_CHANCE = 150;

    private static boolean splitting;

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (splitting || event.getAmount() <= 0) {
            return;
        }
        LivingEntity partner = BondLink.partner(event.getEntity());
        if (partner == null) {
            return;
        }
        float half = event.getAmount() / 2.0F;
        event.setAmount(half);
        splitting = true;
        try {
            partner.invulnerableTime = 0;
            partner.hurt(event.getEntity().level().damageSources().magic(), half);
        } finally {
            splitting = false;
        }
    }

    @SubscribeEvent
    public static void onMaxMana(MaxManaCalcEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            float reserve = Math.min(Config.SERVANT_MAX_RESERVE.get().floatValue(), Servant.reserveOf(player.getUUID()));
            if (reserve > 0) {
                event.setReserve(Math.min(1.0F, event.getReserve() + reserve));
            }
        }
    }

    private static void cancelIfShackled(EntityTeleportEvent event) {
        if (event.getEntity() instanceof LivingEntity living && living.hasEffect(ModEffects.SHACKLED)) {
            event.setCanceled(true);
        }
    }
}
