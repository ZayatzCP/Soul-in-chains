package com.soulinchains.arcaneartifacts.research;

import com.hollingsworth.arsnouveau.api.event.EffectResolveEvent;
import com.hollingsworth.arsnouveau.api.event.SpellCastEvent;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.common.items.Glyph;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Turns real, server-side game events into {@link ResearchEvent}s. Trigger ids usable in research JSON:
 * <ul>
 *     <li>{@code arcane_artifacts:spell_effect} - a glyph effect resolves on an entity or block, cast by the player
 *     (fields: glyph, entity, block). Fired before the effect applies, so "fire on ice" still sees the ice;</li>
 *     <li>{@code arcane_artifacts:spell_cast} - the player casts a spell (glyph: any glyph of the spell);</li>
 *     <li>{@code arcane_artifacts:effect_applied} - the player applies a mob effect to an entity (effect, entity);</li>
 *     <li>{@code arcane_artifacts:observe} - an item is observed at a Research Table (item);</li>
 *     <li>{@code arcane_artifacts:damage_taken} - the player really took damage (damage_type, entity = attacker);</li>
 *     <li>custom ids fired by this mod's own glyphs and blocks (see {@link #fire}).</li>
 * </ul>
 * Only researches already in the INVESTIGATING state listen, so nothing done before a research was
 * started is counted retroactively.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class ResearchTriggers {
    public static final ResourceLocation SPELL_EFFECT = ArcaneArtifacts.id("spell_effect");
    public static final ResourceLocation SPELL_CAST = ArcaneArtifacts.id("spell_cast");
    public static final ResourceLocation EFFECT_APPLIED = ArcaneArtifacts.id("effect_applied");
    public static final ResourceLocation OBSERVE = ArcaneArtifacts.id("observe");
    public static final ResourceLocation DAMAGE_TAKEN = ArcaneArtifacts.id("damage_taken");

    private ResearchTriggers() {
    }

    /** Entry point for this mod's own mechanics (custom trigger ids). */
    public static void fire(Player player, ResearchEvent event) {
        ResearchService.fire(player, event);
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ResearchManager());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(player -> {
            com.soulinchains.arcaneartifacts.command.ArcaneCommands.forgetStarterGlyphs(player);
            ResearchNetwork.sendDefinitions(player);
            ResearchNetwork.sendProgress(player);
        });
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResearchNetwork.sendProgress(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResearchNetwork.sendProgress(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEffectResolve(EffectResolveEvent.Pre event) {
        if (!(event.shooter instanceof ServerPlayer player) || event.resolveEffect == null) {
            return;
        }
        ResearchEvent research = ResearchEvent.of(SPELL_EFFECT).withGlyphs(Set.of(event.resolveEffect.getRegistryName()));
        if (event.rayTraceResult instanceof EntityHitResult hit) {
            research = research.withEntity(hit.getEntity().getType());
        } else if (event.rayTraceResult instanceof BlockHitResult hit) {
            research = research.withBlock(event.world.getBlockState(hit.getBlockPos()).getBlock());
        }
        ResearchService.fire(player, research);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCast(SpellCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.spell == null) {
            return;
        }
        Set<ResourceLocation> glyphs = new LinkedHashSet<>();
        for (AbstractSpellPart part : event.spell.recipe()) {
            glyphs.add(part.getRegistryName());
        }
        ResearchService.fire(player, ResearchEvent.of(SPELL_CAST).withGlyphs(glyphs));
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEffectSource() instanceof ServerPlayer player && event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel) {
            ResearchService.fire(player, ResearchEvent.of(EFFECT_APPLIED)
                    .withEffect(event.getEffectInstance().getEffect())
                    .withEntity(event.getEntity().getType()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamageTaken(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getNewDamage() > 0) {
            var attacker = event.getSource().getEntity();
            ResearchService.fire(player, ResearchEvent.of(DAMAGE_TAKEN)
                    .withDamage(event.getSource().typeHolder())
                    .withEntity(attacker == null ? null : attacker.getType()));
        }
    }

    /** Detects glyphs written into the spell book (UNLOCKED -> RECORDED). Cheap: every 2 seconds. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 40 == 0 && ResearchService.refreshRecorded(player)) {
            ResearchService.sync(player);
        }
    }

    /**
     * Backup gate: a glyph item whose research is not finished cannot be learned (the main gate is the
     * Scribes Table, see ScribesTileMixin). Glyphs the player already knows are not affected.
     */
    @SubscribeEvent
    public static void onUseGlyph(PlayerInteractEvent.RightClickItem event) {
        // Server only: the client has no authoritative research state, and the server's refusal is enough.
        if (event.getLevel().isClientSide || !(event.getItemStack().getItem() instanceof Glyph glyph) || glyph.spellPart == null) {
            return;
        }
        ResourceLocation id = glyph.spellPart.getRegistryName();
        if (!ResearchService.isGlyphAvailable(event.getEntity(), id)) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(Component.translatable("arcane_artifacts.research.glyph_locked",
                    ResearchService.unlockingResearchName(id)).withStyle(ChatFormatting.RED), true);
        }
    }
}
