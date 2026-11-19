package com.soulinchains.arcaneartifacts.accessory;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.hollingsworth.arsnouveau.api.item.ArsNouveauCurio;
import com.hollingsworth.arsnouveau.api.mana.IManaDiscountEquipment;
import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

/**
 * Base for every accessory of the addon: equipped through Curios, one copy per wearer (equipping a
 * second identical accessory in another slot is refused, so effects never stack), and a tooltip made
 * of {@code item.arcane_artifacts.<id>.desc.N} lines.
 */
public abstract class ArcaneAccessoryItem extends ArsNouveauCurio implements IManaDiscountEquipment {
    private final int descriptionLines;

    protected ArcaneAccessoryItem(Item.Properties properties, int descriptionLines) {
        super(properties.stacksTo(1));
        this.descriptionLines = descriptionLines;
    }

    /** Extra maximum mana while worn (Ars Nouveau's max mana attribute, so the mana bar and other addons see it). */
    protected int maxManaBonus() {
        return 0;
    }

    /** Extra mana regeneration per second while worn (Ars Nouveau's mana regen attribute). */
    protected double manaRegenBonus() {
        return 0.0;
    }

    /** Flat discount on every spell while worn, counted by Ars Nouveau itself. */
    protected int manaDiscount() {
        return 0;
    }

    @Override
    public int getManaDiscount(ItemStack stack, Spell spell) {
        return manaDiscount();
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> map = LinkedHashMultimap.create(super.getAttributeModifiers(slotContext, id, stack));
        if (!slotContext.cosmetic()) {
            addManaModifiers(map, id);
        }
        return map;
    }

    /** Subclasses that build their own attribute map call this so their mana bonuses are kept. */
    protected final void addManaModifiers(Multimap<Holder<Attribute>, AttributeModifier> map, ResourceLocation id) {
        if (maxManaBonus() != 0) {
            map.put(PerkAttributes.MAX_MANA, new AttributeModifier(id.withSuffix("_max_mana"), maxManaBonus(), AttributeModifier.Operation.ADD_VALUE));
        }
        if (manaRegenBonus() != 0.0) {
            map.put(PerkAttributes.MANA_REGEN_BONUS, new AttributeModifier(id.withSuffix("_mana_regen"), manaRegenBonus(), AttributeModifier.Operation.ADD_VALUE));
        }
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return CurioHelper.findSlot(slotContext.entity(), this)
                .map(result -> result.slotContext().identifier().equals(slotContext.identifier())
                        && result.slotContext().index() == slotContext.index())
                .orElse(true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        String lore = getDescriptionId() + ".lore";
        if (net.minecraft.locale.Language.getInstance().has(lore)) {
            tooltip.add(Component.translatable(lore).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }
        for (int i = 1; i <= descriptionLines; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".desc." + i).withStyle(ChatFormatting.GRAY));
        }
        appendState(stack, context, tooltip, flag);
    }

    /** Dynamic part of the tooltip (stored spell, charge, adaptations...). */
    protected void appendState(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    }
}
