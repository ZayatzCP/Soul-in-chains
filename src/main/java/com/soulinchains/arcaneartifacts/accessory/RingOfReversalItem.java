package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.common.capability.ManaCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

/**
 * Ring of Reversal (ring slot). Hits taken add charge ({@link RingHandler}); while worn the charge is
 * converted into mana: every {@code conversion_interval_ticks}, 10 charge become 2 mana, as long as
 * the wearer's mana is not full. Charge is stored on the ring and shown as its item bar.
 */
public class RingOfReversalItem extends ArcaneAccessoryItem {
    public RingOfReversalItem() {
        super(new Item.Properties().rarity(Rarity.RARE).component(ModComponents.REVERSAL_CHARGE, 0), 3);
    }

    public static int charge(ItemStack stack) {
        return stack.getOrDefault(ModComponents.REVERSAL_CHARGE, 0);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (wearer.level().isClientSide || slotContext.cosmetic()
                || wearer.level().getGameTime() % Config.RING_CONVERSION_INTERVAL_TICKS.get() != 0) {
            return;
        }
        int charge = charge(stack);
        int cost = Config.RING_CHARGE_PER_CONVERSION.get();
        if (charge < cost) {
            return;
        }
        ManaCap mana = CapabilityRegistry.getMana(wearer);
        if (mana == null || mana.getCurrentMana() >= mana.getMaxMana()) {
            return;
        }
        stack.set(ModComponents.REVERSAL_CHARGE, charge - cost);
        mana.addMana(Config.RING_MANA_PER_CONVERSION.get());
        if (wearer instanceof ServerPlayer player) {
            mana.syncToClient(player);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return charge(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int max = Config.SPEC.isLoaded() ? Config.RING_MAX_CHARGE.get() : 100;
        return Mth.clamp(Math.round(13f * charge(stack) / max), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x9B59F5;
    }

    @Override
    protected void appendState(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int max = Config.SPEC.isLoaded() ? Config.RING_MAX_CHARGE.get() : 100;
        tooltip.add(Component.translatable("arcane_artifacts.ring.charge", charge(stack), max).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
