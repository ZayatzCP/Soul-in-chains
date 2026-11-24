package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.api.event.SpellCastEvent;
import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Hat of Mysteries (head, Focusing set): the archmage's pointed hat. Echo - a spell has a {@link #ECHO_CHANCE} chance
 * to cost no mana at all, {@link #LOW_MANA_ECHO_CHANCE} while you have less than half your mana. +50 maximum mana.
 * <p>
 * The roll is made once per cast (on {@link SpellCastEvent}) and the cost of that cast, computed right after, is then
 * zero; a cast always needs the full mana to start.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public class MysteryHatItem extends ArcaneAccessoryItem {
    public static final float ECHO_CHANCE = 0.20F;
    public static final float LOW_MANA_ECHO_CHANCE = 0.40F;
    private static final String ECHO_TICK = ArcaneArtifacts.MODID + ":echo_tick";

    public MysteryHatItem() {
        super(new Item.Properties().rarity(Rarity.RARE), 3);
    }

    @Override
    protected int maxManaBonus() {
        return 50;
    }

    public static boolean wears(LivingEntity entity) {
        return CurioHelper.findEquipped(entity, ModItems.MYSTERY_HAT.get()).isPresent();
    }

    /** The Echo chance for this caster right now. */
    public static float chance(Player player) {
        var mana = CapabilityRegistry.getMana(player);
        boolean low = mana != null && mana.getMaxMana() > 0 && mana.getCurrentMana() < mana.getMaxMana() / 2.0;
        return low ? LOW_MANA_ECHO_CHANCE : ECHO_CHANCE;
    }

    /** Marks this tick's cast as an echo: its cost becomes zero. */
    public static void echo(Player player) {
        player.getPersistentData().putLong(ECHO_TICK, player.level().getGameTime());
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.8, player.getZ(), 24, 0.4, 0.3, 0.4, 0.6);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.6F);
        }
        player.displayClientMessage(Component.translatable("arcane_artifacts.mystery_hat.echo"), true);
    }

    public static boolean echoing(Player player) {
        return player.getPersistentData().getLong(ECHO_TICK) == player.level().getGameTime() && player.getPersistentData().contains(ECHO_TICK);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCast(SpellCastEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Player player) || player.level().isClientSide || !wears(player)) {
            return;
        }
        if (player.getRandom().nextFloat() < chance(player)) {
            echo(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCost(SpellCostCalcEvent.Post event) {
        if (event.context.getUnwrappedCaster() instanceof Player player && !player.level().isClientSide && echoing(player)) {
            event.currentCost = 0;
        }
    }
}
