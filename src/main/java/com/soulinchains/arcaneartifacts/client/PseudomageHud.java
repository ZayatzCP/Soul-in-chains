package com.soulinchains.arcaneartifacts.client;

import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.pseudo.FalseMana;
import com.soulinchains.arcaneartifacts.pseudo.ManaDispenserBlock;
import com.soulinchains.arcaneartifacts.pseudo.ManaGogglesItem;
import com.soulinchains.arcaneartifacts.pseudo.ManaTanks;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * The Pseudomage's HUD: a false mana bar split into divisions (one division = one round), shown only
 * while counterfeit accessories give false mana, and the goggles' exact Source readout.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PseudomageHud {
    private PseudomageHud() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ArcaneArtifacts.id("false_mana"), PseudomageHud::renderFalseMana);
        event.registerAboveAll(ArcaneArtifacts.id("goggles"), PseudomageHud::renderGoggles);
    }

    private static void renderFalseMana(GuiGraphics g, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()) {
            return;
        }
        float max = FalseMana.max(player);
        if (max <= 0) {
            return;
        }
        float current = Math.min(FalseMana.get(player), max);
        int width = 81;
        int x = g.guiWidth() / 2 + 10;
        int y = g.guiHeight() - 49 - (player.getArmorValue() > 0 || player.getAirSupply() < player.getMaxAirSupply() ? 10 : 0);
        g.fill(x - 1, y - 1, x + width + 1, y + 6, 0xFF1A1206);
        g.fill(x, y, x + width, y + 5, 0xFF4A3A20);
        int filled = Math.round(width * current / max);
        g.fill(x, y, x + filled, y + 5, 0xFFFF9A2E);
        g.fill(x, y, x + filled, y + 1, 0xFFFFD08A);
        // Divisions: one notch per round of false mana.
        int divisions = (int) (max / FalseMana.DIVISION);
        for (int i = 1; i < divisions; i++) {
            int notch = x + Math.round(width * i * FalseMana.DIVISION / max);
            g.fill(notch, y, notch + 1, y + 5, 0xFF1A1206);
        }
        String text = (int) current + "/" + (int) max;
        g.drawString(minecraft.font, text, x + width + 4, y - 1, 0xFFFF9A2E, true);
    }

    private static void renderGoggles(GuiGraphics g, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.options.hideGui || !ManaGogglesItem.isWearing(player)) {
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() == HitResult.Type.MISS) {
            return;
        }
        var level = minecraft.level;
        var pos = blockHit.getBlockPos();
        Component text = null;
        if (ManaTanks.isTank(level, pos)) {
            text = Component.translatable("arcane_artifacts.goggles.tank", ManaTanks.stored(level, pos), ManaTanks.capacity(level, pos),
                    ManaTanks.group(level, pos).size());
        } else if (level.getBlockEntity(pos) instanceof SourceJarTile jar) {
            text = Component.translatable("arcane_artifacts.goggles.jar", jar.getSource(), jar.getMaxSource());
        } else if (level.getBlockState(pos).getBlock() instanceof ManaDispenserBlock) {
            text = Component.translatable(level.getBlockState(pos).getValue(ManaDispenserBlock.DRAIN)
                    ? "arcane_artifacts.dispenser.drain" : "arcane_artifacts.dispenser.fill");
        }
        if (text != null) {
            int x = g.guiWidth() / 2 + 8;
            int y = g.guiHeight() / 2 + 6;
            g.drawString(minecraft.font, text, x, y, 0xFFFFC27A, true);
        }
    }
}
