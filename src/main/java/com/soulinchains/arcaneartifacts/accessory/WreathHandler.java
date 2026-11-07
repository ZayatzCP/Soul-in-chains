package com.soulinchains.arcaneartifacts.accessory;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModAttachments;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.registry.ModTags;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Counts damage instances for the Wreath of Manipulation.
 * <p>
 * Uses {@link LivingDamageEvent.Post}: it fires once per damage instance that was actually applied
 * (after invulnerability frames), so standing in lava counts at the vanilla hurt rate, and damage that
 * was fully redirected away (Parasitism) or cancelled never counts.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class WreathHandler {
    private WreathHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || event.getNewDamage() <= 0) {
            return;
        }
        ItemStack wreath = CurioHelper.findEquipped(player, ModItems.WREATH_OF_MANIPULATION.get()).orElse(ItemStack.EMPTY);
        if (wreath.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        player.getData(ModAttachments.WREATH_RUNTIME).onDamaged(now);

        DamageCategory category = DamageCategory.classify(event.getSource());
        if (category == null) {
            return;
        }
        WreathData data = WreathOfManipulationItem.data(wreath).expire(now);
        if (data.isAdapted(category, now)) {
            // Already adapted: the counter stays at zero until the adaptation ends.
            return;
        }
        int hits = data.hits(category) + 1;
        if (hits >= Config.WREATH_HITS_TO_ADAPT.get()) {
            int min = Config.WREATH_MIN_ADAPT_SECONDS.get();
            int max = Math.max(min, Config.WREATH_MAX_ADAPT_SECONDS.get());
            long duration = Mth.nextInt(player.getRandom(), min, max) * 20L;
            data = data.withAdaptation(category, now + duration);
            player.addEffect(new MobEffectInstance(category.grantedEffect(), 60, 0, true, false, true));
            announce(player, category);
        } else {
            data = data.withHits(category, hits);
        }
        wreath.set(ModComponents.WREATH_DATA, data);
    }

    private static void announce(Player player, DamageCategory category) {
        player.displayClientMessage(Component.translatable("arcane_artifacts.wreath.adapted_now",
                Component.translatable(category.translationKey())).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 2.0, player.getZ(), 30, 0.4, 0.2, 0.4, 0.5);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    /** Arcane Adaptation (the wreath's magic adaptation) reduces magic damage after armor. */
    @SubscribeEvent
    public static void onMagicDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().hasEffect(ModEffects.ARCANE_ADAPTATION) && event.getSource().is(ModTags.MAGIC_DAMAGE)) {
            event.setNewDamage(event.getNewDamage() * (1f - Config.WREATH_MAGIC_REDUCTION.get().floatValue()));
        }
    }
}
