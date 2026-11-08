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
 * table, use a spell book on it). The Self form is built in: write only the effects (a spell that already
 * starts with Self is accepted too; other forms are refused). Crouching casts it - see {@link BootsHandler}.
 */
public class FocusingBootsItem extends ArcaneAccessoryItem implements ICasterTool {
    public FocusingBootsItem() {
        super(new Item.Properties().rarity(Rarity.RARE).component(DataComponentRegistry.SPELL_CASTER, new SpellCaster()), 3);
    }

    public static Spell storedSpell(ItemStack stack) {
        SpellCaster caster = stack.get(DataComponentRegistry.SPELL_CASTER);
        return caster == null ? new Spell() : caster.getSpell();
    }

    /** The spell as it is cast: the built-in Self form followed by the inscribed effects. */
    public static Spell castableSpell(ItemStack stack) {
        return withSelf(storedSpell(stack));
    }

    public static Spell withSelf(Spell spell) {
        if (spell.isEmpty() || spell.getCastMethod() != null) {
            return spell;
        }
        java.util.List<com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart> recipe = new java.util.ArrayList<>();
        recipe.add(MethodSelf.INSTANCE);
        recipe.addAll(spell.unsafeList());
        return spell.mutable().setRecipe(recipe).immutable();
    }

    @Override
    public boolean isScribedSpellValid(AbstractCaster<?> caster, Player player, InteractionHand hand, ItemStack stack, Spell spell) {
        Spell cast = withSelf(spell);
        return !cast.isEmpty() && cast.isValid() && cast.getCastMethod() == MethodSelf.INSTANCE;
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
