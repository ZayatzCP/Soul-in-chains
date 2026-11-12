package com.soulinchains.arcaneartifacts.pseudo;

import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * A counterfeit accessory of the Pseudomage: it gives false mana (never real mana or mana
 * regeneration), shown as a divided bar over the hotbar.
 */
public class CounterfeitManaItem extends ArcaneAccessoryItem {
    private final double falseMana;

    public CounterfeitManaItem(double falseMana, int descriptionLines) {
        super(new Item.Properties().rarity(Rarity.UNCOMMON), descriptionLines);
        this.falseMana = falseMana;
    }

    public double falseMana() {
        return falseMana;
    }

}
