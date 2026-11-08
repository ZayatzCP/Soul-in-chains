package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.common.capability.ManaCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Wreath of Manipulation (head slot). When the wearer's mana runs out, the wreath starts a Battle Rush:
 * for {@code rush_seconds} every spell costs no mana and the wearer takes no damage (see
 * {@link WreathHandler}). Afterwards the wreath needs {@code cooldown_seconds} to recover; the cooldown
 * is the Rush Exhaustion effect, counted from the start of the rush.
 */
public class WreathOfManipulationItem extends ArcaneAccessoryItem {
    public WreathOfManipulationItem() {
        super(new Item.Properties().rarity(Rarity.EPIC).component(ModComponents.RUSH_STATE, RushState.EMPTY), 3);
    }

    public static RushState state(ItemStack stack) {
        return stack.getOrDefault(ModComponents.RUSH_STATE, RushState.EMPTY);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        Level level = wearer.level();
        if (level.isClientSide || slotContext.cosmetic() || !(wearer instanceof Player player) || level.getGameTime() % 5 != 0) {
            return;
        }
        if (player.hasEffect(ModEffects.BATTLE_RUSH) || player.hasEffect(ModEffects.RUSH_EXHAUSTION) || !isManaDepleted(player)) {
            return;
        }
        startRush(player, stack);
    }

    /** Mana counts as depleted at or below {@code trigger_mana_fraction} of the maximum (and never above 1 point when the fraction is 0). */
    public static boolean isManaDepleted(Player player) {
        ManaCap mana = CapabilityRegistry.getMana(player);
        if (mana == null || mana.getMaxMana() <= 0) {
            return false;
        }
        double threshold = Math.max(1.0, mana.getMaxMana() * Config.WREATH_TRIGGER_FRACTION.get());
        return mana.getCurrentMana() <= threshold;
    }

    public static void startRush(Player player, ItemStack wreath) {
        long now = player.level().getGameTime();
        int rush = Config.WREATH_RUSH_SECONDS.get() * 20;
        int cooldown = Math.max(rush, Config.WREATH_COOLDOWN_SECONDS.get() * 20);
        player.addEffect(new MobEffectInstance(ModEffects.BATTLE_RUSH, rush, 0, false, true, true));
        player.addEffect(new MobEffectInstance(ModEffects.RUSH_EXHAUSTION, cooldown, 0, false, false, true));
        wreath.set(ModComponents.RUSH_STATE, new RushState(now + rush, now + cooldown));
        player.displayClientMessage(Component.translatable("arcane_artifacts.wreath.rush_started").withStyle(ChatFormatting.GOLD), true);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
            level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 2.0, player.getZ(), 30, 0.4, 0.2, 0.4, 0.5);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.6f, 1.6f);
        }
    }

    @Override
    protected void appendState(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        RushState state = state(stack);
        Level level = net.neoforged.fml.loading.FMLEnvironment.dist.isClient() ? ClientTime.level() : null;
        long now = level == null ? 0 : level.getGameTime();
        if (state.isActive(now)) {
            tooltip.add(Component.translatable("arcane_artifacts.wreath.rush_active",
                    StringUtil.formatTickDuration((int) (state.activeUntil() - now), 20f)).withStyle(ChatFormatting.GOLD));
        } else if (state.isRecovering(now)) {
            tooltip.add(Component.translatable("arcane_artifacts.wreath.recovering",
                    StringUtil.formatTickDuration((int) (state.readyAt() - now), 20f)).withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("arcane_artifacts.wreath.ready").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Isolates the client-only class reference so the item class stays loadable on dedicated servers. */
    private static final class ClientTime {
        static Level level() {
            return com.soulinchains.arcaneartifacts.client.ClientAccess.level();
        }
    }
}
