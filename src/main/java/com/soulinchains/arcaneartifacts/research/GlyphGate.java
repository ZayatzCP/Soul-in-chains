package com.soulinchains.arcaneartifacts.research;

import com.hollingsworth.arsnouveau.api.event.SpellCastEvent;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Last line of the research lock: a player cannot cast a spell containing a glyph whose research is not
 * formulated, however the glyph got into the spell (creative spell book, copied spell, old save...).
 * Crafting at the Scribes Table and learning the glyph item are refused earlier.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class GlyphGate {
    private GlyphGate() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCast(SpellCastEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        ResourceLocation locked = ResearchService.firstLockedGlyph(player, event.spell);
        if (locked != null) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("arcane_artifacts.research.glyph_locked",
                    ResearchService.unlockingResearchName(locked)).withStyle(ChatFormatting.RED), true);
        }
    }
}
