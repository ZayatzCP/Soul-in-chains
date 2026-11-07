package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.api.item.ArsNouveauCurio;
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
public abstract class ArcaneAccessoryItem extends ArsNouveauCurio {
    private final int descriptionLines;

    protected ArcaneAccessoryItem(Item.Properties properties, int descriptionLines) {
        super(properties.stacksTo(1));
        this.descriptionLines = descriptionLines;
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
        for (int i = 1; i <= descriptionLines; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".desc." + i).withStyle(ChatFormatting.GRAY));
        }
        appendState(stack, context, tooltip, flag);
    }

    /** Dynamic part of the tooltip (stored spell, charge, adaptations...). */
    protected void appendState(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    }
}
