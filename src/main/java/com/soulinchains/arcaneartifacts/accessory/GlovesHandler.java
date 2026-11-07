package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.api.event.SpellCastEvent;
import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Focusing Gloves: "cast twice, pay once".
 * <ol>
 *     <li>{@link SpellCastEvent} (lowest priority, so cancelled casts never get here) marks the cast as a
 *     candidate when the wearer is a real player, the spell matches the gloves' pattern and the gloves
 *     are off cooldown. The event is only fired after Ars validated the spell and checked mana.</li>
 *     <li>{@link SpellCostCalcEvent.Post} is fired by Ars only when the cast actually succeeded and mana
 *     is being spent. Only then is the free repeat scheduled and the gloves put on cooldown.</li>
 *     <li>A few ticks later the repeat calls the form's {@code onCast} directly with a fresh context and
 *     resolver for the same spell. It bypasses {@code SpellResolver#onCast}, so it does not check or spend
 *     mana and does not fire {@link SpellCastEvent} again: it can never chain into a third cast.</li>
 * </ol>
 * The repeat is cast from the wearer's position and facing at that moment ("cast on air"), which is
 * what projectile, missile and self forms do anyway. Casts made by the Focusing Boots are never repeated.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class GlovesHandler {
    public static final String REPEAT_TAG = ArcaneArtifacts.MODID + ":gloves_repeat";

    private static final Map<SpellContext, Boolean> CANDIDATES = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final List<PendingRepeat> PENDING = new ArrayList<>();

    private record PendingRepeat(UUID player, Spell spell, ItemStack casterTool, long executeAt) {
    }

    private GlovesHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCast(SpellCastEvent event) {
        SpellContext context = event.context;
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer || context == null) {
            return;
        }
        if (context.getCaster().getCasterType() != SpellContext.CasterType.PLAYER
                || context.tag.getBoolean(REPEAT_TAG) || context.tag.getBoolean(BootsHandler.BOOTS_TAG)) {
            return;
        }
        ItemStack gloves = CurioHelper.findEquipped(player, ModItems.FOCUSING_GLOVES.get()).orElse(ItemStack.EMPTY);
        if (gloves.isEmpty() || player.getCooldowns().isOnCooldown(gloves.getItem())) {
            return;
        }
        if (FocusingGlovesItem.matches(FocusingGlovesItem.pattern(gloves), event.spell)) {
            CANDIDATES.put(context, Boolean.TRUE);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onManaSpent(SpellCostCalcEvent.Post event) {
        SpellContext context = event.context;
        if (context == null || CANDIDATES.remove(context) == null) {
            return;
        }
        if (!(context.getUnwrappedCaster() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack gloves = CurioHelper.findEquipped(player, ModItems.FOCUSING_GLOVES.get()).orElse(ItemStack.EMPTY);
        if (gloves.isEmpty() || player.getCooldowns().isOnCooldown(gloves.getItem())) {
            return;
        }
        player.getCooldowns().addCooldown(gloves.getItem(), Math.max(1, Config.GLOVES_COOLDOWN_TICKS.get()));
        long executeAt = player.level().getGameTime() + Config.GLOVES_REPEAT_DELAY_TICKS.get();
        PENDING.add(new PendingRepeat(player.getUUID(), context.getSpell(), context.getCasterTool().copy(), executeAt));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Casts resolve synchronously inside a tick: a candidate that did not spend mana by now failed.
        CANDIDATES.clear();
        if (PENDING.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        Iterator<PendingRepeat> it = PENDING.iterator();
        List<PendingRepeat> due = new ArrayList<>();
        while (it.hasNext()) {
            PendingRepeat repeat = it.next();
            if (repeat.executeAt() <= now) {
                due.add(repeat);
                it.remove();
            }
        }
        for (PendingRepeat repeat : due) {
            ServerPlayer player = server.getPlayerList().getPlayer(repeat.player());
            if (player == null || !player.isAlive() || player.isSpectator()) {
                continue;
            }
            castRepeat(player, repeat);
        }
    }

    private static void castRepeat(ServerPlayer player, PendingRepeat repeat) {
        SpellContext context = SpellContext.fromEntity(repeat.spell(), player, repeat.casterTool());
        context.tag.putBoolean(REPEAT_TAG, true);
        SpellResolver resolver = new SpellResolver(context);
        AbstractCastMethod method = resolver.castType;
        if (method == null) {
            return;
        }
        try {
            method.onCast(repeat.casterTool(), player, player.level(), resolver.getCastStats(), context, resolver);
        } catch (RuntimeException e) {
            ArcaneArtifacts.LOGGER.error("Focusing Gloves repeat cast failed for {}", player.getGameProfile().getName(), e);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PENDING.clear();
        CANDIDATES.clear();
    }
}
