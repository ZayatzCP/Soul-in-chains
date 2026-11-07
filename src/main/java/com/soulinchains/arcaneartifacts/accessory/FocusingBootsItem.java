package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Focusing Boots (feet slot). Inscribed at the Scribes Table like any Ars caster (place the boots on the
 * table, use a spell book on it). Only spells whose form is Self are accepted; the new spell replaces
 * the old one. Crouching casts it - see {@link BootsHandler}.
 */
public class FocusingBootsItem extends ArcaneAccessoryItem implements ICasterTool {
    public FocusingBootsItem() {
        super(new Item.Properties().rarity(Rarity.RARE).component(DataComponentRegistry.SPELL_CASTER, new SpellCaster()), 3);
    }

    public static Spell storedSpell(ItemStack stack) {
        SpellCaster caster = stack.get(DataComponentRegistry.SPELL_CASTER);
        return caster == null ? new Spell() : caster.getSpell();
    }

    @Override
    public boolean isScribedSpellValid(AbstractCaster<?> caster, Player player, InteractionHand hand, ItemStack stack, Spell spell) {
        return spell.isValid() && spell.getCastMethod() == MethodSelf.INSTANCE;
    }

    @Override
    public void sendInvalidMessage(Player player) {
        PortUtil.sendMessageNoSpam(player, Component.translatable("arcane_artifacts.boots.invalid"));
    }

    @Override
    protected void appendState(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Spell spell = storedSpell(stack);
        if (spell.isEmpty()) {
            tooltip.add(Component.translatable("arcane_artifacts.caster.empty").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("arcane_artifacts.caster.stored", spell.getDisplayString()).withStyle(ChatFormatting.AQUA));
        }
    }
}
