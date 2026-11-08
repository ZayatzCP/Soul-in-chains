package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.event.SpellCastEvent;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Silenced creatures (Counterspell) cannot cast Ars Nouveau spells. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class SilenceHandler {
    private SilenceHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCast(SpellCastEvent event) {
        if (event.getEntity().hasEffect(ModEffects.SILENCED)) {
            event.setCanceled(true);
            if (event.getEntity() instanceof Player player) {
                player.displayClientMessage(Component.translatable("arcane_artifacts.silenced").withStyle(ChatFormatting.DARK_GRAY), true);
            }
        }
    }
}
