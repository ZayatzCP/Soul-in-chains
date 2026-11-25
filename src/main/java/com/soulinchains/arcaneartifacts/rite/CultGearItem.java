package com.soulinchains.arcaneartifacts.rite;

import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import com.soulinchains.arcaneartifacts.accessory.ArcaneSets;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

/**
 * The Cultist's vestments: each piece makes one role's work easier, the full set lifts the rite.
 * <ul>
 *     <li>Mask of Dogma (head) - the Reader reads half again as fast.</li>
 *     <li>Rite Robe (body) - Echoes hit for half; Blood costs half as much.</li>
 *     <li>Offering Belt (belt) - the Sacrificer has 5 s longer to bring an offering or open the cage.</li>
 *     <li>Inscriber's Gloves (hands) - the Tracer has 1 s longer to retrace; chalk is spent only half the time.</li>
 *     <li>Silent Sandals (feet) - with the Keeper wearing them, the wind puts candles out half as often.</li>
 * </ul>
 * The full set: +3% to a rite's quality for every participant wearing it all (up to +15%).
 */
public class CultGearItem extends ArcaneAccessoryItem {
    public enum Piece {
        MASK(() -> ModItems.CULT_MASK.get()), ROBE(() -> ModItems.CULT_ROBE.get()), BELT(() -> ModItems.OFFERING_BELT.get()),
        GLOVES(() -> ModItems.INSCRIBER_GLOVES.get()), SANDALS(() -> ModItems.SILENT_SANDALS.get());

        final Supplier<Item> item;

        Piece(Supplier<Item> item) {
            this.item = item;
        }
    }

    public static final float SET_QUALITY = 3F;
    public static final int BELT_EXTRA = 100;
    public static final int GLOVES_EXTRA = 20;

    public CultGearItem() {
        super(new Item.Properties().rarity(Rarity.UNCOMMON), 2);
    }

    public static boolean wears(LivingEntity entity, Piece piece) {
        return entity != null && CurioHelper.findEquipped(entity, piece.item.get()).isPresent();
    }

    public static boolean fullSet(LivingEntity entity) {
        return ArcaneSets.Set.CULTIST.complete(entity);
    }
}
