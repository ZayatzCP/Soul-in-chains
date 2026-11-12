package com.soulinchains.arcaneartifacts.pseudo;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.SlotContext;

/**
 * The Pseudomage's counterfeit trinkets: they look like mana jewellery but give no mana at all, only
 * comfort on the road. The ring helps walking (faster, steps up a whole block), the pendant helps
 * exploring (night vision).
 */
public class CounterfeitManaItem extends ArcaneAccessoryItem {
    public enum Kind { RING, PENDANT }

    private final Kind kind;

    public CounterfeitManaItem(Kind kind) {
        super(new Item.Properties().rarity(Rarity.UNCOMMON), 2);
        this.kind = kind;
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> map = LinkedHashMultimap.create();
        if (kind == Kind.RING && !slotContext.cosmetic()) {
            map.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(id.withSuffix("_speed"), 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            map.put(Attributes.STEP_HEIGHT, new AttributeModifier(id.withSuffix("_step"), 0.5, AttributeModifier.Operation.ADD_VALUE));
        }
        return map;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        var wearer = slotContext.entity();
        if (kind == Kind.PENDANT && !slotContext.cosmetic() && !wearer.level().isClientSide && wearer.tickCount % 40 == 0) {
            wearer.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
        }
    }
}
