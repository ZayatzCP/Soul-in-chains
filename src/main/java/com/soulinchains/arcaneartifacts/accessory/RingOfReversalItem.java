package com.soulinchains.arcaneartifacts.accessory;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Ring of Reversal (ring slot). A single hit stronger than half of the wearer's maximum health is
 * negated and the attacker is thrown back by as many blocks as the hit would have dealt - see
 * {@link RingHandler}.
 */
public class RingOfReversalItem extends ArcaneAccessoryItem {
    public RingOfReversalItem() {
        super(new Item.Properties().rarity(Rarity.RARE), 3);
    }
}
