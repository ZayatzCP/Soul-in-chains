package com.soulinchains.arcaneartifacts.pseudo;

import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * False mana: the Pseudomage's counterfeit of real mana is nothing but the magic cartridges carried in the
 * worn bandolier. It never refills by itself; only cartridges put into the bandolier fill it. The HUD
 * bar has one division per bandolier pouch.
 */
public final class FalseMana {
    /** Cartridges per bandolier pouch: one division of the bar. */
    public static final int DIVISION = BandolierItem.STACK;

    private FalseMana() {
    }

    public static ItemStack bandolier(Player player) {
        return CurioHelper.findEquipped(player, ModItems.BANDOLIER.get()).orElse(ItemStack.EMPTY);
    }

    public static int max(Player player) {
        return bandolier(player).isEmpty() ? 0 : BandolierItem.CAPACITY;
    }

    public static int get(Player player) {
        ItemStack bandolier = bandolier(player);
        return bandolier.isEmpty() ? 0 : BandolierItem.cartridges(bandolier);
    }
}
