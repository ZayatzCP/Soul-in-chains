package com.soulinchains.arcaneartifacts.effect;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import top.theillusivec4.curios.api.CuriosApi;

/** Applying BONK!: never extends a running one, and the Hard Hat enchantment shortens it. */
public final class Bonk {
    public static final int DEFAULT_TICKS = 8 * 20;
    public static final ResourceKey<Enchantment> HARD_HAT = ResourceKey.create(Registries.ENCHANTMENT, ArcaneArtifacts.id("hard_hat"));

    private Bonk() {
    }

    /** The best Hard Hat level on the helmet or on anything worn in a Curios head slot. */
    public static int hardHatLevel(LivingEntity entity) {
        var holder = entity.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(HARD_HAT);
        if (holder.isEmpty()) {
            return 0;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(holder.get(), entity.getItemBySlot(EquipmentSlot.HEAD));
        var curios = CuriosApi.getCuriosInventory(entity).flatMap(inv -> inv.getStacksHandler("head"));
        if (curios.isPresent()) {
            var stacks = curios.get().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                level = Math.max(level, EnchantmentHelper.getItemEnchantmentLevel(holder.get(), stack));
            }
        }
        return level;
    }

    /** Duration after the Hard Hat: level 1 two thirds, level 2 one third, level 3 and above one second. */
    public static int durationFor(LivingEntity target, int ticks) {
        int hat = hardHatLevel(target);
        if (hat >= 3) {
            return 20;
        }
        return Math.max(20, ticks * (3 - hat) / 3);
    }

    /** Stuns the target. Returns false if it is already stunned (a BONK! is never extended). */
    public static boolean apply(LivingEntity target, int ticks) {
        if (target.hasEffect(ModEffects.BONK)) {
            return false;
        }
        var data = target.getPersistentData();
        data.putFloat(BonkEffect.YAW, target.getYHeadRot());
        data.putFloat(BonkEffect.PITCH, target.getXRot());
        target.addEffect(new MobEffectInstance(ModEffects.BONK, durationFor(target, ticks), 0, false, true, true));
        target.level().playSound(null, target.getX(), target.getEyeY(), target.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.8f, 1.8f);
        return true;
    }
}
