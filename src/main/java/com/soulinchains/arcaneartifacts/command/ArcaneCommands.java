package com.soulinchains.arcaneartifacts.command;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.common.capability.ANPlayerDataCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.research.PlayerResearch;
import com.soulinchains.arcaneartifacts.research.ResearchManager;
import com.soulinchains.arcaneartifacts.research.ResearchService;
import com.soulinchains.arcaneartifacts.research.ResearchState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;

/**
 * {@code /arcane_artifacts unlock_all [players]}: completes every research of the addon and teaches
 * every glyph (Ars Nouveau's and the addon's). For testing and for servers that skip the research game.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class ArcaneCommands {
    /**
     * Removes Ars Nouveau's starting glyphs (Projectile, Touch, Harm, Break) from the glyphs a player
     * "knows": Ars always lists them in the spell book, so a player who also knows them sees each twice.
     * Older versions of the unlock command taught them; this repairs such players when they join.
     */
    public static void forgetStarterGlyphs(ServerPlayer player) {
        ANPlayerDataCap cap = CapabilityRegistry.getPlayerDataCap(player);
        if (cap == null) {
            return;
        }
        List<AbstractSpellPart> starters = GlyphRegistry.getDefaultStartingSpells();
        if (cap.getKnownGlyphs().stream().anyMatch(starters::contains)) {
            List<AbstractSpellPart> kept = cap.getKnownGlyphs().stream().filter(part -> !starters.contains(part)).toList();
            cap.setKnownGlyphs(kept);
            CapabilityRegistry.EventHandler.syncPlayerCap(player);
        }
    }

    private ArcaneCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(ArcaneArtifacts.MODID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("unlock_all")
                        .executes(ArcaneCommands::unlockSelf)
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> unlock(context.getSource(), EntityArgument.getPlayers(context, "targets"))))));
    }

    private static int unlockSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return unlock(context.getSource(), List.of(context.getSource().getPlayerOrException()));
    }

    private static int unlock(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            unlockAll(player);
            source.sendSuccess(() -> Component.translatable("arcane_artifacts.command.unlock_all", player.getDisplayName()), true);
        }
        return players.size();
    }

    /** Every research becomes unlocked and every registered glyph becomes known. */
    public static void unlockAll(ServerPlayer player) {
        PlayerResearch data = ResearchService.data(player);
        for (ResourceLocation id : ResearchManager.all().keySet()) {
            PlayerResearch.Entry entry = data.entry(id);
            if (entry == null || entry.state() != ResearchState.RECORDED) {
                data.setState(id, ResearchState.UNLOCKED);
            }
        }
        ANPlayerDataCap cap = CapabilityRegistry.getPlayerDataCap(player);
        if (cap != null) {
            List<AbstractSpellPart> starters = GlyphRegistry.getDefaultStartingSpells();
            for (AbstractSpellPart part : GlyphRegistry.getSpellpartMap().values()) {
                // Ars adds the starting glyphs to the spell book itself: knowing them too shows them twice.
                if (!starters.contains(part)) {
                    cap.unlockGlyph(part);
                }
            }
            CapabilityRegistry.EventHandler.syncPlayerCap(player);
        }
        ResearchService.refreshRecorded(player);
        ResearchService.sync(player);
    }
}
