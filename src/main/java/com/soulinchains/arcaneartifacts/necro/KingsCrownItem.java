package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * The Imprisoned King's Crown (head): everything the Bone Crown does, two servants instead of one, and Corruption grows
 * half as fast. Counts as the crown of the Shackled set.
 */
public class KingsCrownItem extends ArcaneAccessoryItem {
    public KingsCrownItem() {
        super(new Item.Properties().rarity(Rarity.EPIC).fireResistant(), 2);
    }
}
