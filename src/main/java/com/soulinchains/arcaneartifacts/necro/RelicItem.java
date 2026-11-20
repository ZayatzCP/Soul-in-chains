package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** A Shackled relic, worn in the single Relic slot: only one at a time. See {@link Relics}. */
public class RelicItem extends ArcaneAccessoryItem {
    public final Relics.Kind kind;

    public RelicItem(Relics.Kind kind) {
        super(new Item.Properties().rarity(Rarity.EPIC), 2);
        this.kind = kind;
    }
}
