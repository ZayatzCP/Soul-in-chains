package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;


/**
 * Relics of the Shackled (the Relic slot holds one):
 * <ul>
 *     <li>Tooth Necklace - every fifth soul you spend adds no Corruption.</li>
 *     <li>Executioner's Ring - the Executioner's Axe executes at 25% health instead of 15%.</li>
 *     <li>Jailer's Eye - your servants and loose souls within 32 blocks glow through walls.</li>
 *     <li>Caged Heart - a killing blow leaves you at 1 health and puts three souls into your lantern; then it sleeps for
 *     ten minutes.</li>
 * </ul>
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class Relics {
    public enum Kind {
        TOOTH_NECKLACE("tooth_necklace"),
        EXECUTIONER_RING("executioner_ring"),
        JAILER_EYE("jailer_eye"),
        CAGED_HEART("caged_heart");

        public final String id;

        Kind(String id) {
            this.id = id;
        }

        public Item item() {
            return switch (this) {
                case TOOTH_NECKLACE -> ModItems.TOOTH_NECKLACE.get();
                case EXECUTIONER_RING -> ModItems.EXECUTIONER_RING.get();
                case JAILER_EYE -> ModItems.JAILER_EYE.get();
                case CAGED_HEART -> ModItems.CAGED_HEART.get();
            };
        }
    }

    public static final int NECKLACE_EVERY = 5;
    public static final float RING_EXECUTE_SHARE = 0.25F;
    public static final int HEART_COOLDOWN_TICKS = 12000;
    public static final int HEART_SOULS = 3;
    private static final String NECKLACE_COUNT = ArcaneArtifacts.MODID + ":necklace_count";
    private static final String HEART_READY_AT = ArcaneArtifacts.MODID + ":caged_heart_ready";

    private Relics() {
    }

    public static boolean wears(Player player, Kind kind) {
        return CurioHelper.findEquipped(player, kind.item()).isPresent();
    }

    /** Tooth Necklace: true when this spend is the free fifth one. */
    public static boolean toothNecklaceSpares(Player player) {
        if (!wears(player, Kind.TOOTH_NECKLACE)) {
            return false;
        }
        int count = player.getPersistentData().getInt(NECKLACE_COUNT) + 1;
        if (count >= NECKLACE_EVERY) {
            player.getPersistentData().putInt(NECKLACE_COUNT, 0);
            return true;
        }
        player.getPersistentData().putInt(NECKLACE_COUNT, count);
        return false;
    }

    public static float executeShare(Player player) {
        return wears(player, Kind.EXECUTIONER_RING) ? RING_EXECUTE_SHARE : ExecutionerAxeItem.EXECUTE_SHARE;
    }

    /** A random relic, for the Imprisoned King and the Jailer's chest. */
    public static ItemStack random(net.minecraft.util.RandomSource random) {
        Kind[] kinds = Kind.values();
        return new ItemStack(kinds[random.nextInt(kinds.length)].item());
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 20 != 0 || !wears(player, Kind.JAILER_EYE)) {
            return;
        }
        AABB box = player.getBoundingBox().inflate(32);
        for (Servant servant : level.getEntitiesOfClass(Servant.class, box, s -> player.getUUID().equals(s.getOwnerUUID()))) {
            servant.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false, false));
        }
        for (SoulWisp wisp : level.getEntitiesOfClass(SoulWisp.class, box)) {
            wisp.setGlowingTag(true);
        }
    }

    /** Caged Heart: runs before the Phylactery, so the cheaper save is spent first. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)
                || event.getSource().is(DamageTypes.GENERIC_KILL) || !wears(player, Kind.CAGED_HEART)) {
            return;
        }
        long now = level.getGameTime();
        if (player.getPersistentData().getLong(HEART_READY_AT) > now) {
            return;
        }
        event.setCanceled(true);
        player.setHealth(1F);
        player.getPersistentData().putLong(HEART_READY_AT, now + HEART_COOLDOWN_TICKS);
        for (int i = 0; i < HEART_SOULS; i++) {
            if (!Souls.store(player, SoulType.WANDERING)) {
                Souls.spawnWisp(level, player, SoulType.WANDERING);
            }
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3));
        level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0F, 0.8F);
        player.displayClientMessage(Component.translatable("arcane_artifacts.relic.caged_heart.beat"), true);
    }

}
