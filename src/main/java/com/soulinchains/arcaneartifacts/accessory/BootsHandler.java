package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Focusing Boots: the moment the wearer starts crouching (rising edge, so holding sneak casts once),
 * the inscribed Self spell is cast through a regular Ars {@link SpellResolver}. That means the usual
 * validation, mana check, "not enough mana" message, mana discounts, cast events and mana expenditure
 * all apply exactly once. A short internal cooldown (item cooldown, visible on the slot) prevents
 * crouch-spamming.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class BootsHandler {
    /** Marks contexts created by the boots so the gloves never duplicate them. */
    public static final String BOOTS_TAG = ArcaneArtifacts.MODID + ":boots_cast";

    private static final Set<UUID> SNEAKING = new HashSet<>();

    private BootsHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        boolean sneaking = player.isShiftKeyDown() && !player.isSpectator();
        boolean wasSneaking = sneaking ? !SNEAKING.add(player.getUUID()) : !SNEAKING.remove(player.getUUID());
        if (!sneaking || wasSneaking || !player.isAlive()) {
            return;
        }
        ItemStack boots = CurioHelper.findEquipped(player, ModItems.FOCUSING_BOOTS.get()).orElse(ItemStack.EMPTY);
        if (boots.isEmpty() || player.getCooldowns().isOnCooldown(boots.getItem())) {
            return;
        }
        Spell spell = FocusingBootsItem.storedSpell(boots);
        if (spell.isEmpty()) {
            return;
        }
        if (spell.getCastMethod() != MethodSelf.INSTANCE) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.boots.invalid"), true);
            return;
        }
        SpellContext context = SpellContext.fromEntity(spell, player, boots);
        context.tag.putBoolean(BOOTS_TAG, true);
        SpellResolver resolver = new SpellResolver(context);
        if (resolver.onCast(boots, player.level())) {
            player.getCooldowns().addCooldown(boots.getItem(), Config.BOOTS_COOLDOWN_TICKS.get());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SNEAKING.remove(event.getEntity().getUUID());
    }
}
