package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** The Shackled set pieces besides the Soul Lantern: Shackle Gloves (hands), Shroud (back), Bone Crown (head). */
public class ShackledGearItem extends ArcaneAccessoryItem {
    public enum Piece { GLOVES, SHROUD, CROWN }

    public final Piece piece;

    public ShackledGearItem(Piece piece) {
        super(new Item.Properties().rarity(Rarity.RARE), 2);
        this.piece = piece;
    }

    @Override
    protected int maxManaBonus() {
        return piece == Piece.SHROUD ? ShackledSet.SHROUD_MANA : 0;
    }
}
