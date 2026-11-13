package com.soulinchains.arcaneartifacts.druid;

import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * The Druid's gear (mantle, hat, gloves, boots): placeholders while the Druid is being written. They can be
 * worn in their Curios slots but do nothing yet.
 */
public class DruidGearItem extends ArcaneAccessoryItem {
    public DruidGearItem() {
        super(new Item.Properties().rarity(Rarity.UNCOMMON), 1);
    }
}
