package com.soulinchains.arcaneartifacts.team;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

/** One of the four suit charms of a sworn company (charm slot). Only one suit charm can be worn at a time. */
public class SuitCharmItem extends ArcaneAccessoryItem {
    private final Suit suit;

    public SuitCharmItem(Suit suit) {
        super(new Item.Properties().rarity(Rarity.RARE), 3);
        this.suit = suit;
    }

    public Suit suit() {
        return suit;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        if (!super.canEquip(slotContext, stack)) {
            return false;
        }
        return CuriosApi.getCuriosInventory(slotContext.entity())
                .flatMap(inventory -> inventory.findFirstCurio(other -> other.getItem() instanceof SuitCharmItem))
                .map(result -> result.slotContext().identifier().equals(slotContext.identifier())
                        && result.slotContext().index() == slotContext.index())
                .orElse(true);
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = LinkedHashMultimap.create();
        if (TeamBond.of(stack) != null && !slotContext.cosmetic()) {
            modifiers.put(suit.attribute(), new AttributeModifier(id, suit.amount(), AttributeModifier.Operation.ADD_VALUE));
        }
        return modifiers;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return TeamBond.of(stack) != null;
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(suit.color());
    }

    @Override
    protected void appendState(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        TeamBond bond = TeamBond.of(stack);
        tooltip.add(bond == null
                ? Component.translatable("arcane_artifacts.oath.unsworn").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                : Component.translatable("arcane_artifacts.oath.company", bond.name()).withStyle(ChatFormatting.GOLD));
    }
}
