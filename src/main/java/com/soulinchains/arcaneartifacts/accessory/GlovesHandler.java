package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The free second cast of the Focusing Gloves. The first cast goes through Ars Nouveau's normal
 * {@code SpellResolver#onCast} (validation, mana check and payment); only if it succeeded is the repeat
 * scheduled. The repeat calls the form's {@code onCast} directly: it neither checks nor spends mana and
 * fires no cast event, so it can never chain into a third cast.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class GlovesHandler {
    public static final String REPEAT_TAG = ArcaneArtifacts.MODID + ":gloves_repeat";

    private static final List<PendingRepeat> PENDING = new ArrayList<>();

    private record PendingRepeat(UUID player, Spell spell, ItemStack casterTool, long executeAt) {
    }

    private GlovesHandler() {
    }

    /** Drops the pending free repeats of a caster (Counterspell). */
    public static void cancelFor(UUID player) {
        PENDING.removeIf(repeat -> repeat.player().equals(player));
    }

    public static void scheduleRepeat(ServerPlayer player, Spell spell, ItemStack casterTool) {
        long executeAt = player.level().getGameTime() + Config.GLOVES_REPEAT_DELAY_TICKS.get();
        PENDING.add(new PendingRepeat(player.getUUID(), spell, casterTool.copy(), executeAt));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        List<PendingRepeat> due = new ArrayList<>();
        Iterator<PendingRepeat> it = PENDING.iterator();
        while (it.hasNext()) {
            PendingRepeat repeat = it.next();
            if (repeat.executeAt() <= now) {
                due.add(repeat);
                it.remove();
            }
        }
        for (PendingRepeat repeat : due) {
            ServerPlayer player = server.getPlayerList().getPlayer(repeat.player());
            if (player != null && player.isAlive() && !player.isSpectator()) {
                castRepeat(player, repeat);
            }
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
    }
}
