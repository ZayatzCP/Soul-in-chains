package com.soulinchains.arcaneartifacts.enchant;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.effect.Bonk;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Optional;

/**
 * Heavy Projectile (crossbows). The charge time (10 s) lives in the enchantment's data; here: bolts
 * deal 20 damage and stun with BONK!, and the crossbow keeps no enchantment but Heavy Projectile,
 * Mending, Unbreaking and Curse of Vanishing, whichever mod they come from.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class HeavyProjectile {
    public static final ResourceKey<Enchantment> KEY = ResourceKey.create(Registries.ENCHANTMENT, ArcaneArtifacts.id("heavy_projectile"));
    public static final float DAMAGE = 20f;

    private HeavyProjectile() {
    }

    private static Optional<Holder.Reference<Enchantment>> holder(Level level) {
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(KEY);
    }

    public static boolean has(Level level, ItemStack stack) {
        return !stack.isEmpty() && holder(level).map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack) > 0).orElse(false);
    }

    private static boolean allowed(Holder<Enchantment> enchantment) {
        return enchantment.is(KEY) || enchantment.is(Enchantments.MENDING) || enchantment.is(Enchantments.UNBREAKING)
                || enchantment.is(Enchantments.VANISHING_CURSE);
    }

    /** Removes every enchantment the heavy crossbow does not tolerate. */
    public static void strip(Level level, ItemStack stack) {
        if (has(level, stack)) {
            EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.removeIf(e -> !allowed(e)));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || !has(arrow.level(), weapon)) {
            return;
        }
        event.setAmount(Math.max(event.getAmount(), DAMAGE));
        LivingEntity target = event.getEntity();
        Bonk.apply(target, Bonk.DEFAULT_TICKS);
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!event.getEntity().level().isClientSide) {
            strip(event.getEntity().level(), event.getItem());
        }
    }

    /** No other enchantment can be added to a heavy crossbow on the anvil. */
    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        var player = event.getPlayer();
        if (player == null) {
            return;
        }
        Level level = player.level();
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        boolean heavy = has(level, left) || has(level, right);
        if (!heavy) {
            return;
        }
        var stored = EnchantmentHelper.getEnchantmentsForCrafting(right);
        for (var entry : stored.entrySet()) {
            if (!allowed(entry.getKey())) {
                event.setCanceled(true);
                return;
            }
        }
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(left).entrySet()) {
            if (!allowed(entry.getKey())) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
