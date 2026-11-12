package com.soulinchains.arcaneartifacts.pseudo;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.SlotContext;

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

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> map = LinkedHashMultimap.create();
        if (!slotContext.cosmetic()) {
            map.put(FalseMana.MAX, new AttributeModifier(id.withSuffix("_false_mana"), falseMana, AttributeModifier.Operation.ADD_VALUE));
        }
        return map;
    }
}
