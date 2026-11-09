package com.soulinchains.arcaneartifacts.accessory;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.SlotContext;

/** Hop-n-Skip Slippers (feet slot): Jump Boost II, Luck II and soft landings. */
public class HopSlippersItem extends ArcaneAccessoryItem {
    public HopSlippersItem() {
        super(new Item.Properties().rarity(Rarity.UNCOMMON), 2);
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> map = LinkedHashMultimap.create();
        if (slotContext.cosmetic()) {
            return map;
        }
        map.put(Attributes.JUMP_STRENGTH, new AttributeModifier(id.withSuffix("_jump"), 0.2, AttributeModifier.Operation.ADD_VALUE));
        map.put(Attributes.LUCK, new AttributeModifier(id.withSuffix("_luck"), 2.0, AttributeModifier.Operation.ADD_VALUE));
        map.put(Attributes.SAFE_FALL_DISTANCE, new AttributeModifier(id.withSuffix("_safe_fall"), 4.0, AttributeModifier.Operation.ADD_VALUE));
        map.put(Attributes.FALL_DAMAGE_MULTIPLIER, new AttributeModifier(id.withSuffix("_fall"), -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return map;
    }
}
