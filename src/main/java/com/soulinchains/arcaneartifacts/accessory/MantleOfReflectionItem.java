package com.soulinchains.arcaneartifacts.accessory;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** Mantle of Reflection (back slot). Behaviour: {@link MantleHandler}. */
public class MantleOfReflectionItem extends ArcaneAccessoryItem {
    public MantleOfReflectionItem() {
        super(new Item.Properties().rarity(Rarity.EPIC), 4);
    }
}
