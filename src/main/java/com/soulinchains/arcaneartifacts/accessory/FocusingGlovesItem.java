package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
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
 * Focusing Gloves (hands slot). The gloves hold a spell "pattern"; any spell the wearer casts normally
 * (spell book, wand, staff...) whose glyph sequence starts with that pattern is cast a second time for
 * free - see {@link GlovesHandler}. With nothing inscribed the pattern is the Projectile form. A spell
 * inscribed at the Scribes Table replaces the pattern; it must start with a form.
 */
public class FocusingGlovesItem extends ArcaneAccessoryItem implements ICasterTool {
    public FocusingGlovesItem() {
        super(new Item.Properties().rarity(Rarity.RARE).component(DataComponentRegistry.SPELL_CASTER, new SpellCaster()), 3);
    }

    /** The pattern used for matching: the inscribed spell, or the Projectile form when empty. */
    public static List<AbstractSpellPart> pattern(ItemStack stack) {
        SpellCaster caster = stack.get(DataComponentRegistry.SPELL_CASTER);
        Spell spell = caster == null ? new Spell() : caster.getSpell();
        if (spell.isEmpty()) {
            return List.of(MethodProjectile.INSTANCE);
        }
        return List.copyOf(spell.unsafeList());
    }

    public static boolean matches(List<AbstractSpellPart> pattern, Spell spell) {
        List<AbstractSpellPart> recipe = spell.unsafeList();
        if (pattern.isEmpty() || recipe.size() < pattern.size()) {
            return false;
        }
        for (int i = 0; i < pattern.size(); i++) {
            if (!pattern.get(i).getRegistryName().equals(recipe.get(i).getRegistryName())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isScribedSpellValid(AbstractCaster<?> caster, Player player, InteractionHand hand, ItemStack stack, Spell spell) {
        return spell.isValid() && spell.getCastMethod() != null;
    }

    @Override
    public void sendInvalidMessage(Player player) {
        PortUtil.sendMessageNoSpam(player, Component.translatable("arcane_artifacts.gloves.invalid"));
    }

    @Override
    protected void appendState(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SpellCaster caster = stack.get(DataComponentRegistry.SPELL_CASTER);
        Spell spell = caster == null ? new Spell() : caster.getSpell();
        if (spell.isEmpty()) {
            tooltip.add(Component.translatable("arcane_artifacts.gloves.default_pattern").withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("arcane_artifacts.gloves.pattern", spell.getDisplayString()).withStyle(ChatFormatting.AQUA));
        }
    }
}
