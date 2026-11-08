package com.soulinchains.arcaneartifacts.research;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.common.capability.ANPlayerDataCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Server-side research rules. Every state change goes through here and is followed by a sync of the
 * player's progress. Nothing in this class trusts the client.
 */
public final class ResearchService {
    private ResearchService() {
    }

    public static PlayerResearch data(Player player) {
        return player.getData(ModAttachments.RESEARCH);
    }

    /**
     * Effective state ignoring glyph requirements: stored state, or DISCOVERED/UNKNOWN derived from the
     * required researches. Use {@link #state(PlayerResearch, ResourceLocation, Predicate)} where the
     * player's known glyphs matter (discovery).
     */
    public static ResearchState state(PlayerResearch data, ResourceLocation id) {
        return state(data, id, glyph -> true);
    }

    /** Effective state; {@code knowsGlyph} tells whether the player knows an Ars Nouveau glyph. */
    public static ResearchState state(PlayerResearch data, ResourceLocation id, Predicate<ResourceLocation> knowsGlyph) {
        PlayerResearch.Entry entry = data.entry(id);
        if (entry != null) {
            return entry.state();
        }
        ResearchDefinition def = ResearchManager.get(id);
        if (def == null) {
            return ResearchState.UNKNOWN;
        }
        for (ResourceLocation required : def.requires()) {
            PlayerResearch.Entry req = data.entry(required);
            if (req == null || !req.state().atLeast(ResearchState.PRINCIPLE)) {
                return ResearchState.UNKNOWN;
            }
        }
        for (ResourceLocation glyph : def.requiresGlyphs()) {
            if (!knowsGlyph.test(glyph)) {
                return ResearchState.UNKNOWN;
            }
        }
        return ResearchState.DISCOVERED;
    }

    public static ResearchState state(Player player, ResourceLocation id) {
        return state(data(player), id, glyphKnowledge(player));
    }

    /**
     * Whether the player knows a glyph: learned glyphs (Ars player data, synced to the client too),
     * starter glyphs, or every glyph in creative. Unknown glyph ids count as known so a datapack
     * referring to a missing addon does not lock a research forever.
     */
    public static Predicate<ResourceLocation> glyphKnowledge(Player player) {
        ANPlayerDataCap cap = player == null ? null : CapabilityRegistry.getPlayerDataCap(player);
        return glyphId -> {
            AbstractSpellPart part = GlyphRegistry.getSpellPart(glyphId);
            if (part == null || player == null || player.isCreative()) return true;
            return GlyphRegistry.getDefaultStartingSpells().contains(part) || (cap != null && cap.knowsGlyph(part));
        };
    }

    // ------------------------------------------------------------------ stage I: observation

    /** Researches the stack in a Research Table can start or is relevant to, in display order. */
    public static List<ResourceLocation> researchesFor(PlayerResearch data, ItemStack stack) {
        List<ResourceLocation> result = new ArrayList<>();
        for (Map.Entry<ResourceLocation, ResearchDefinition> e : ResearchManager.all().entrySet()) {
            if (e.getValue().isSubject(stack)) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    /**
     * Observes the item on the table: starts every DISCOVERED research whose subject it is. The item
     * is never consumed. Observing also fires the "observe" trigger, so experiments can ask the
     * player to compare several samples.
     */
    public static int observe(Player player, ItemStack stack) {
        if (stack.isEmpty() || !canResearch(player)) {
            return 0;
        }
        PlayerResearch data = data(player);
        Predicate<ResourceLocation> knows = glyphKnowledge(player);
        int started = 0;
        boolean locked = false;
        for (ResourceLocation id : researchesFor(data, stack)) {
            ResearchState state = state(data, id, knows);
            if (state == ResearchState.DISCOVERED) {
                data.setState(id, ResearchState.INVESTIGATING);
                started++;
            } else if (state == ResearchState.UNKNOWN) {
                locked = true;
            }
        }
        if (started > 0) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.research.started", started).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        } else if (locked) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.research.needs_knowledge").withStyle(ChatFormatting.GRAY), true);
        }
        fire(player, ResearchEvent.of(ResearchTriggers.OBSERVE).withItem(stack.getItem()));
        sync(player);
        return started;
    }

    // ------------------------------------------------------------------ stage II: experiments

    /** Matches a world event against every experiment of every research in progress. */
    public static void fire(Player player, ResearchEvent event) {
        if (!canResearch(player)) {
            return;
        }
        PlayerResearch data = data(player);
        boolean changed = false;
        for (Map.Entry<ResourceLocation, PlayerResearch.Entry> e : List.copyOf(data.entries().entrySet())) {
            if (e.getValue().state() != ResearchState.INVESTIGATING) {
                continue;
            }
            ResearchDefinition def = ResearchManager.get(e.getKey());
            if (def == null) {
                continue;
            }
            for (ResearchDefinition.ExperimentDefinition experiment : def.experiments()) {
                if (!e.getValue().done().contains(experiment.id()) && experiment.matches(event)) {
                    data.markDone(e.getKey(), experiment.id());
                    changed = true;
                    player.displayClientMessage(Component.translatable("arcane_artifacts.research.experiment_done",
                            Component.translatable(ResearchDefinition.langPrefix(e.getKey()) + ".experiment." + experiment.id())).withStyle(ChatFormatting.AQUA), true);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.4f);
                }
            }
            if (data.entry(e.getKey()).done().size() >= def.experiments().size()
                    && def.experiments().stream().allMatch(x -> data.entry(e.getKey()).done().contains(x.id()))) {
                data.setState(e.getKey(), ResearchState.PRINCIPLE);
                data.addNotes(def.category(), def.notes());
                changed = true;
                player.displayClientMessage(Component.translatable("arcane_artifacts.research.principle",
                        Component.translatable(ResearchDefinition.langPrefix(e.getKey()) + ".title")).withStyle(ChatFormatting.GOLD), false);
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.2f);
            }
        }
        if (changed) {
            sync(player);
        }
    }

    // ------------------------------------------------------------------ stage III: formulation

    public static boolean canFormulate(PlayerResearch data, ResourceLocation id) {
        ResearchDefinition def = ResearchManager.get(id);
        if (def == null || state(data, id) != ResearchState.PRINCIPLE) {
            return false;
        }
        for (Map.Entry<NoteCategory, Integer> req : def.notesRequired().entrySet()) {
            if (data.notes(req.getKey()) < req.getValue()) {
                return false;
            }
        }
        return true;
    }

    /** Turns a learned principle into an unlocked glyph recipe. Notes are checked, not spent. */
    public static boolean formulate(Player player, ResourceLocation id) {
        PlayerResearch data = data(player);
        if (!canResearch(player) || !canFormulate(data, id)) {
            return false;
        }
        data.setState(id, ResearchState.UNLOCKED);
        player.displayClientMessage(Component.translatable("arcane_artifacts.research.unlocked",
                Component.translatable(ResearchDefinition.langPrefix(id) + ".title")).withStyle(ChatFormatting.GOLD), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1f);
        refreshRecorded(player);
        sync(player);
        return true;
    }

    /** UNLOCKED -> RECORDED once the player knows every glyph the research unlocks. */
    public static boolean refreshRecorded(Player player) {
        PlayerResearch data = data(player);
        ANPlayerDataCap cap = CapabilityRegistry.getPlayerDataCap(player);
        boolean changed = false;
        for (Map.Entry<ResourceLocation, PlayerResearch.Entry> e : List.copyOf(data.entries().entrySet())) {
            if (e.getValue().state() != ResearchState.UNLOCKED) continue;
            ResearchDefinition def = ResearchManager.get(e.getKey());
            if (def == null || def.unlocks().isEmpty()) continue;
            boolean all = true;
            for (ResourceLocation glyphId : def.unlocks()) {
                AbstractSpellPart part = GlyphRegistry.getSpellPart(glyphId);
                if (part != null && (cap == null || !cap.knowsGlyph(part))) {
                    all = false;
                }
            }
            if (all) {
                data.setState(e.getKey(), ResearchState.RECORDED);
                changed = true;
            }
        }
        return changed;
    }

    // ------------------------------------------------------------------ glyph gating

    /** Creative players skip research only when the server allows it ({@code creative_bypasses_research}). */
    public static boolean bypasses(Player player) {
        return player.isCreative() && Config.RESEARCH_CREATIVE_BYPASS.get();
    }

    /** The first glyph of the spell that the player has not unlocked yet, or null. */
    public static @org.jetbrains.annotations.Nullable ResourceLocation firstLockedGlyph(Player player, com.hollingsworth.arsnouveau.api.spell.Spell spell) {
        for (var part : spell.unsafeList()) {
            if (part != null && !isGlyphAvailable(player, part.getRegistryName())) {
                return part.getRegistryName();
            }
        }
        return null;
    }

    /** True if research does not lock this glyph for the player (or locking is disabled). */
    public static boolean isGlyphAvailable(Player player, ResourceLocation glyphId) {
        if (!Config.RESEARCH_GATES_GLYPHS.get() || bypasses(player) || !ResearchManager.gatedGlyphs().contains(glyphId)) {
            return true;
        }
        PlayerResearch data = data(player);
        for (Map.Entry<ResourceLocation, ResearchDefinition> e : ResearchManager.all().entrySet()) {
            if (e.getValue().unlocks().contains(glyphId) && state(data, e.getKey()).atLeast(ResearchState.UNLOCKED)) {
                return true;
            }
        }
        return false;
    }

    /** True if research does not lock this item's apparatus recipe for the player. */
    public static boolean isItemAvailable(Player player, ResourceLocation itemId) {
        if (!Config.RESEARCH_GATES_GLYPHS.get() || bypasses(player) || !ResearchManager.gatedItems().contains(itemId)) {
            return true;
        }
        PlayerResearch data = data(player);
        for (Map.Entry<ResourceLocation, ResearchDefinition> e : ResearchManager.all().entrySet()) {
            if (e.getValue().unlocksItems().contains(itemId) && state(data, e.getKey()).atLeast(ResearchState.UNLOCKED)) {
                return true;
            }
        }
        return false;
    }

    /** Name of the research that unlocks the item, for messages. */
    public static Component itemResearchName(ResourceLocation itemId) {
        for (Map.Entry<ResourceLocation, ResearchDefinition> e : ResearchManager.all().entrySet()) {
            if (e.getValue().unlocksItems().contains(itemId)) {
                return Component.translatable(ResearchDefinition.langPrefix(e.getKey()) + ".title");
            }
        }
        return Component.literal(itemId.toString());
    }

    /** Name of the research that unlocks the glyph, for messages. */
    public static Component unlockingResearchName(ResourceLocation glyphId) {
        for (Map.Entry<ResourceLocation, ResearchDefinition> e : ResearchManager.all().entrySet()) {
            if (e.getValue().unlocks().contains(glyphId)) {
                return Component.translatable(ResearchDefinition.langPrefix(e.getKey()) + ".title");
            }
        }
        return Component.literal(glyphId.toString());
    }

    // ------------------------------------------------------------------ misc

    static boolean canResearch(Player player) {
        return !player.level().isClientSide && !(player instanceof FakePlayer) && !player.isSpectator();
    }

    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            ResearchNetwork.sendProgress(serverPlayer);
        }
    }
}
