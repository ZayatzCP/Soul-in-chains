package com.soulinchains.arcaneartifacts.pseudo;

import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Pseudomage's Goggles (head, or a Curios head slot): looking at a Mana Tank or a Source Jar shows exactly how much Source is inside. They give no mana and no mana regeneration.
 */
public class ManaGogglesItem extends ArcaneAccessoryItem implements Equipable {
    public ManaGogglesItem() {
        super(new Item.Properties().rarity(Rarity.UNCOMMON), 2);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    public static boolean isWearing(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MANA_GOGGLES.get())
                || CurioHelper.findEquipped(player, ModItems.MANA_GOGGLES.get()).isPresent()
                || CurioHelper.findEquipped(player, ModItems.SOMBRERO.get()).isPresent();
    }
}
