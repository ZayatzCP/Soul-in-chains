package com.soulinchains.arcaneartifacts.accessory;

import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModAttachments;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

/**
 * Wreath of Manipulation (head slot). Adaptation counting happens in {@link WreathHandler} (damage
 * events); this class keeps the granted effects alive while worn and runs regeneration and cleansing.
 */
public class WreathOfManipulationItem extends ArcaneAccessoryItem {
    /** Effects are re-applied for this long and refreshed before they run out, so removing the wreath ends them quickly. */
    private static final int EFFECT_PULSE_TICKS = 60;

    public WreathOfManipulationItem() {
        super(new Item.Properties().rarity(Rarity.EPIC).component(ModComponents.WREATH_DATA, WreathData.EMPTY), 4);
    }

    public static WreathData data(ItemStack stack) {
        return stack.getOrDefault(ModComponents.WREATH_DATA, WreathData.EMPTY);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        Level level = wearer.level();
        if (level.isClientSide || slotContext.cosmetic()) {
            return;
        }
        long now = level.getGameTime();

        // 1. Expire finished adaptations (their hit counters were reset when they were granted).
        WreathData data = data(stack);
        WreathData expired = data.expire(now);
        if (expired != data) {
            stack.set(ModComponents.WREATH_DATA, expired);
            data = expired;
        }

        // 2. Keep the granted effects alive while the adaptation lasts.
        if (now % 20 == 0) {
            for (DamageCategory category : DamageCategory.values()) {
                if (!data.isAdapted(category, now)) continue;
                MobEffectInstance current = wearer.getEffect(category.grantedEffect());
                if (current == null || current.getDuration() < EFFECT_PULSE_TICKS / 2) {
                    int duration = (int) Math.min(EFFECT_PULSE_TICKS, data.remainingTicks(category, now));
                    if (duration > 0) {
                        wearer.addEffect(new MobEffectInstance(category.grantedEffect(), duration, 0, true, false, true));
                    }
                }
            }
        }

        if (!(wearer instanceof Player player)) {
            return;
        }
        WreathRuntime runtime = player.getData(ModAttachments.WREATH_RUNTIME);

        // 3. Regeneration after a calm period.
        runtime.tickRegeneration(player, now, Config.WREATH_REGEN_DELAY_TICKS.get(), Config.WREATH_REGEN_DURATION_TICKS.get());

        // 4. Cleansing of persistent harmful effects.
        if (now % 5 == 0) {
            runtime.tickCleanse(player, now, Config.WREATH_CLEANSE_THRESHOLD_TICKS.get(), Config.WREATH_CLEANSE_COOLDOWN_TICKS.get());
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof Player player && !player.level().isClientSide) {
            player.getData(ModAttachments.WREATH_RUNTIME).reset();
        }
    }

    @Override
    protected void appendState(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        WreathData data = data(stack);
        Level level = net.neoforged.fml.loading.FMLEnvironment.dist.isClient() ? ClientTime.level() : null;
        long now = level == null ? 0 : level.getGameTime();
        int needed = Config.SPEC.isLoaded() ? Config.WREATH_HITS_TO_ADAPT.get() : 5;
        boolean any = false;
        for (DamageCategory category : DamageCategory.values()) {
            Component name = Component.translatable(category.translationKey());
            if (data.isAdapted(category, now)) {
                any = true;
                String time = StringUtil.formatTickDuration((int) data.remainingTicks(category, now), 20f);
                tooltip.add(Component.translatable("arcane_artifacts.wreath.adapted", name, time).withStyle(ChatFormatting.LIGHT_PURPLE));
            } else if (data.hits(category) > 0) {
                any = true;
                tooltip.add(Component.translatable("arcane_artifacts.wreath.progress", name, data.hits(category), needed).withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        if (!any) {
            tooltip.add(Component.translatable("arcane_artifacts.wreath.none").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** Isolates the client-only class reference so the item class stays loadable on dedicated servers. */
    private static final class ClientTime {
        static Level level() {
            return com.soulinchains.arcaneartifacts.client.ClientAccess.level();
        }
    }
}
