package com.soulinchains.arcaneartifacts.cult;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

/**
 * The Cultist gear outside the rite's numbers:
 * <ul>
 *     <li>Belt of Anchors: 4 charges of +35 on a shield mend, one back for every blow blocked; a block within half a
 *     second of raising the shield is a parry - half the blow goes back to the attacker.</li>
 *     <li>Sentry Boots: in the outer ring of your rite (4+ blocks from the seal) Resistance I; crouching still for 2 s -
 *     the stance, 30% less damage until you move.</li>
 *     <li>Cuirass: 20% less from projectiles. Helm: creatures hunting your partner glow.</li>
 *     <li>Seal-Wright's Gauntlets: a blow strips the creature's projectiles within 4 blocks out of the air.</li>
 *     <li>Keeper of the Circle (full Warden set): 10% less from creatures while your Confessor is within 8; once a minute
 *     a killing blow on your Confessor falls on you instead.</li>
 *     <li>Mantle: below 30% health - 3 s of smoke veil (invisible, 50% less damage), once every 90 s.</li>
 *     <li>Hands of the Inscriber: a Ritual Knife blow marks the creature (Inscribed, 10 s), once every 5 s. Face of Dogma:
 *     the Inscribed around you glow.</li>
 *     <li>Shoes of the Quiet Step: crouching, your steps make no vibrations (sculk and the Warden do not hear).</li>
 * </ul>
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class CultGearEvents {
    public static final int PARRY_TICKS = 10;
    public static final float PARRY_SHARE = 0.5F;
    public static final int STANCE_TICKS = 40;
    public static final float STANCE_REDUCTION = 0.3F;
    public static final float CUIRASS_PROJECTILE = 0.2F;
    public static final float KEEPER_REDUCTION = 0.1F;
    public static final double KEEPER_RANGE = 8;
    public static final int KEEPER_COOLDOWN = 1200;
    public static final int VEIL_TICKS = 60;
    public static final int VEIL_COOLDOWN = 1800;
    public static final int INSCRIBE_TICKS = 200;
    public static final int INSCRIBE_COOLDOWN = 100;
    private static final String STANCE = ArcaneArtifacts.MODID + ":stance";
    private static final String KEEPER_READY = ArcaneArtifacts.MODID + ":keeper_ready";
    private static final String VEIL_UNTIL = ArcaneArtifacts.MODID + ":veil_until";
    private static final String VEIL_READY = ArcaneArtifacts.MODID + ":veil_ready";
    private static final String INSCRIBE_READY = ArcaneArtifacts.MODID + ":inscribe_ready";

    private CultGearEvents() {
    }

    private static long now(Player player) {
        return player.level().getGameTime();
    }

    // ------------------------------------------------------------------ Belt of Anchors

    private static Optional<ItemStack> belt(Player player) {
        return CurioHelper.findEquipped(player, ModItems.ANCHOR_BELT.get());
    }

    public static int charges(Player player) {
        return belt(player).map(s -> s.getOrDefault(ModComponents.ANCHOR_CHARGES, CultGear.BELT_CHARGES)).orElse(0);
    }

    /** A shield mend spends a charge for +35; returns the extra. */
    public static int useAnchorCharge(Player player) {
        Optional<ItemStack> belt = belt(player);
        if (belt.isEmpty()) {
            return 0;
        }
        int charges = belt.get().getOrDefault(ModComponents.ANCHOR_CHARGES, CultGear.BELT_CHARGES);
        if (charges <= 0) {
            return 0;
        }
        belt.get().set(ModComponents.ANCHOR_CHARGES, charges - 1);
        return CultGear.BELT_CHARGE_REPAIR;
    }

    @SubscribeEvent
    public static void onBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || !event.getBlocked()) {
            return;
        }
        belt(player).ifPresent(belt -> {
            int charges = belt.getOrDefault(ModComponents.ANCHOR_CHARGES, CultGear.BELT_CHARGES);
            if (charges < CultGear.BELT_CHARGES) {
                belt.set(ModComponents.ANCHOR_CHARGES, charges + 1);
            }
            if (player.getTicksUsingItem() <= PARRY_TICKS && event.getDamageSource().getEntity() instanceof LivingEntity attacker && attacker != player) {
                attacker.hurt(player.damageSources().thorns(player), event.getBlockedDamage() * PARRY_SHARE);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 1.6F);
            }
        });
    }

    // ------------------------------------------------------------------ ticking pieces

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (CultGear.wears(player, ModItems.SENTRY_BOOTS.get())) {
            boolean still = player.isShiftKeyDown() && player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4;
            int stance = still ? player.getPersistentData().getInt(STANCE) + 1 : 0;
            player.getPersistentData().putInt(STANCE, Math.min(stance, 1000));
            if (stance == STANCE_TICKS) {
                level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.2, player.getZ(), 12, 0.4, 0.05, 0.4, 0.05);
            }
            RitualSealBlockEntity seal = player.tickCount % 20 == 0 ? RitualSealBlockEntity.ofWarden(player) : null;
            if (seal != null && player.position().distanceTo(Vec3.atCenterOf(seal.getBlockPos())) > 4) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, true, false, true));
            }
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        if (CultGear.wears(player, ModItems.SEAL_KEEPER_HELM.get())) {
            Player partner = Cult.get(player).partner().map(level::getPlayerByUUID).orElse(null);
            if (partner != null) {
                for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16), m -> m.getTarget() == partner)) {
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
                }
            }
        }
        if (CultGear.wears(player, ModItems.FACE_OF_DOGMA.get())) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(24), e -> e.hasEffect(ModEffects.INSCRIBED))) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
            }
        }
    }

    public static boolean inStance(Player player) {
        return CultGear.wears(player, ModItems.SENTRY_BOOTS.get()) && player.getPersistentData().getInt(STANCE) >= STANCE_TICKS;
    }

    // ------------------------------------------------------------------ damage

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0) {
            return;
        }
        LivingEntity victim = event.getEntity();
        // Hands of the Inscriber and Seal-Wright's Gauntlets work on the attacker's blow
        if (event.getSource().getEntity() instanceof Player attacker && event.getSource().getDirectEntity() == attacker && !attacker.level().isClientSide) {
            if (attacker.getMainHandItem().getItem() instanceof RitualKnifeItem && CultGear.wears(attacker, ModItems.INSCRIBER_HANDS.get())
                    && attacker.getPersistentData().getLong(INSCRIBE_READY) <= now(attacker)) {
                inscribe(victim);
                attacker.getPersistentData().putLong(INSCRIBE_READY, now(attacker) + INSCRIBE_COOLDOWN);
            }
            if (CultGear.wears(attacker, ModItems.SEAL_GAUNTLETS.get())) {
                stripProjectiles(victim);
            }
        }
        if (!(victim instanceof Player player) || player.level().isClientSide) {
            return;
        }
        float amount = event.getAmount();
        if (inStance(player)) {
            amount *= 1F - STANCE_REDUCTION;
        }
        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE) && CultGear.wears(player, ModItems.OUTER_CIRCLE_CUIRASS.get())) {
            amount *= 1F - CUIRASS_PROJECTILE;
        }
        if (!(event.getSource().getEntity() instanceof Player) && CultGear.wardenSet(player)) {
            Player confessor = Cult.get(player).partner().map(player.level()::getPlayerByUUID).orElse(null);
            if (confessor != null && confessor.distanceTo(player) <= KEEPER_RANGE) {
                amount *= 1F - KEEPER_REDUCTION;
            }
        }
        if (player.getPersistentData().getLong(VEIL_UNTIL) > now(player)) {
            amount *= 0.5F;
        } else if (CultGear.wears(player, ModItems.COMMUNION_MANTLE.get()) && player.getHealth() - amount < player.getMaxHealth() * 0.3F
                && player.getPersistentData().getLong(VEIL_READY) <= now(player)) {
            veil(player);
            amount *= 0.5F;
        }
        event.setAmount(amount);
    }

    /** The mantle's smoke veil. */
    public static void veil(Player player) {
        player.getPersistentData().putLong(VEIL_UNTIL, now(player) + VEIL_TICKS);
        player.getPersistentData().putLong(VEIL_READY, now(player) + VEIL_COOLDOWN);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, VEIL_TICKS, 0, false, false, true));
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 0.8, 0.5, 0.02);
            level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
    }

    public static void inscribe(LivingEntity target) {
        target.addEffect(new MobEffectInstance(ModEffects.INSCRIBED, INSCRIBE_TICKS, 0, false, true, true));
    }

    /** The gauntlets: the creature's arrows and fireballs around it fall out of the air. Returns how many. */
    public static int stripProjectiles(LivingEntity victim) {
        int stripped = 0;
        for (Projectile projectile : victim.level().getEntitiesOfClass(Projectile.class, victim.getBoundingBox().inflate(4), p -> p.getOwner() == victim)) {
            projectile.discard();
            stripped++;
        }
        return stripped;
    }

    // ------------------------------------------------------------------ Keeper of the Circle: the last blow

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player confessor) || confessor.level().isClientSide || event.getSource().is(DamageTypes.GENERIC_KILL)
                || Cult.role(confessor) != CultRole.CONFESSOR) {
            return;
        }
        Player warden = Cult.get(confessor).partner().map(confessor.level()::getPlayerByUUID).orElse(null);
        if (warden != null && takeTheBlow(confessor, warden)) {
            event.setCanceled(true);
        }
    }

    /** The Warden takes the Confessor's killing blow (once a minute, within 8 blocks, full set). */
    public static boolean takeTheBlow(Player confessor, Player warden) {
        if (!CultGear.wardenSet(warden) || !warden.isAlive() || warden.distanceTo(confessor) > KEEPER_RANGE
                || warden.getPersistentData().getLong(KEEPER_READY) > now(warden)) {
            return false;
        }
        warden.getPersistentData().putLong(KEEPER_READY, now(warden) + KEEPER_COOLDOWN);
        confessor.setHealth(2F);
        warden.setHealth(Math.max(2F, warden.getHealth() - 10F));
        warden.displayClientMessage(Component.translatable("arcane_artifacts.cult.keeper_took"), true);
        confessor.displayClientMessage(Component.translatable("arcane_artifacts.cult.keeper_saved"), true);
        if (confessor.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, confessor.getX(), confessor.getY() + 1, confessor.getZ(), 30, 0.4, 0.6, 0.4, 0.2);
        }
        return true;
    }

    // ------------------------------------------------------------------ Shoes of the Quiet Step

    @SubscribeEvent
    public static void onVibration(VanillaGameEvent event) {
        if (event.getCause() instanceof Player player && player.isShiftKeyDown() && CultGear.wears(player, ModItems.QUIET_SHOES.get())) {
            event.setCanceled(true);
        }
    }
}
