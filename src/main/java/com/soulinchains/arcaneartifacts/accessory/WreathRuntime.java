package com.soulinchains.arcaneartifacts.accessory;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.EffectCures;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-player, non-persistent state of the wreath (a NeoForge data attachment without serializer:
 * it disappears with the player entity on logout/death, which is the desired behaviour for timers).
 */
public final class WreathRuntime {
    private long lastDamageTime = -1;
    private boolean regenerationGranted = true;
    private long cleanseReadyAt;
    private final Map<Holder<MobEffect>, Long> harmfulSince = new HashMap<>();

    public void onDamaged(long now) {
        lastDamageTime = now;
        regenerationGranted = false;
    }

    public void reset() {
        lastDamageTime = -1;
        regenerationGranted = true;
        harmfulSince.clear();
    }

    void tickRegeneration(Player player, long now, int delay, int duration) {
        if (regenerationGranted || lastDamageTime < 0 || now - lastDamageTime < delay) {
            return;
        }
        regenerationGranted = true;
        if (player.getHealth() < player.getMaxHealth()) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, true, true, true));
        }
    }

    /**
     * A harmful effect that has been active continuously for {@code threshold} ticks is removed, if it
     * is normally curable (milk-curable, i.e. not an effect a mod declared incurable) and the cleanse is
     * off cooldown. One effect per cleanse.
     */
    void tickCleanse(Player player, long now, int threshold, int cooldown) {
        Set<Holder<MobEffect>> present = new HashSet<>();
        Holder<MobEffect> toRemove = null;
        for (MobEffectInstance instance : player.getActiveEffects()) {
            Holder<MobEffect> effect = instance.getEffect();
            if (effect.value().getCategory() != MobEffectCategory.HARMFUL || !instance.getCures().contains(EffectCures.MILK)) {
                continue;
            }
            present.add(effect);
            long since = harmfulSince.computeIfAbsent(effect, e -> now);
            if (toRemove == null && now - since >= threshold) {
                toRemove = effect;
            }
        }
        harmfulSince.keySet().retainAll(present);
        if (toRemove != null && now >= cleanseReadyAt && player.removeEffect(toRemove)) {
            harmfulSince.remove(toRemove);
            cleanseReadyAt = now + cooldown;
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.8, player.getZ(), 10, 0.3, 0.2, 0.3, 0.02);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.7f, 1.4f);
            }
            player.displayClientMessage(Component.translatable("arcane_artifacts.wreath.cleansed",
                    toRemove.value().getDisplayName()), true);
        }
    }
}
