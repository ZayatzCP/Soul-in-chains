package com.soulinchains.arcaneartifacts.cult;

import com.google.common.collect.Multimap;
import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.SlotContext;

/**
 * The Cultist's two sets. Warden, "Keeper of the Circle": Seal-Keeper's Helm, Cuirass of the Outer Circle, Belt of Anchors,
 * Seal-Wright's Gauntlets, Sentry Boots - heavy (each piece 3% slower). Confessor, "Voice of the Rite": Face of Dogma,
 * Mantle of Communion, Book of Arrears, Hands of the Inscriber, Shoes of the Quiet Step - frail, all mana and precision.
 * What each piece does lives in {@link CultGear} (inside the rite) and {@link CultGearEvents} (outside).
 */
public class CultGearItem extends ArcaneAccessoryItem {
    public enum Piece {
        HELM(true, "head"), CUIRASS(true, "body"), ANCHOR_BELT(true, "belt"), GAUNTLETS(true, "hands"), SENTRY_BOOTS(true, "feet"),
        FACE(false, "head"), MANTLE(false, "body"), ARREARS(false, "belt"), INSCRIBER_HANDS(false, "hands"), QUIET_SHOES(false, "feet");

        public final boolean warden;
        public final String slot;

        Piece(boolean warden, String slot) {
            this.warden = warden;
            this.slot = slot;
        }
    }

    public final Piece piece;

    public CultGearItem(Piece piece) {
        super(new Item.Properties().rarity(Rarity.RARE), 2);
        this.piece = piece;
    }

    @Override
    protected int maxManaBonus() {
        return switch (piece) {
            case FACE -> 60;
            case MANTLE, INSCRIBER_HANDS -> 30;
            default -> 0;
        };
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        var map = super.getAttributeModifiers(slotContext, id, stack);
        if (slotContext.cosmetic()) {
            return map;
        }
        if (piece.warden) {
            map.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(id.withSuffix("_weight"), -0.03, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        switch (piece) {
            case HELM -> map.put(Attributes.ARMOR, new AttributeModifier(id.withSuffix("_armor"), 3, AttributeModifier.Operation.ADD_VALUE));
            case CUIRASS -> {
                map.put(Attributes.ARMOR, new AttributeModifier(id.withSuffix("_armor"), 6, AttributeModifier.Operation.ADD_VALUE));
                map.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id.withSuffix("_toughness"), 2, AttributeModifier.Operation.ADD_VALUE));
            }
            case SENTRY_BOOTS -> map.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id.withSuffix("_knockback"), 0.6,
                    AttributeModifier.Operation.ADD_VALUE));
            default -> {
            }
        }
        return map;
    }

}
