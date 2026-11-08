package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Spell Quiver (quiver slot). Inscribe a spell at the Scribes Table (only its effects matter, a form is
 * ignored). While worn, every arrow the wearer shoots carries that spell and releases it where it hits,
 * weakened by two Dampen after every effect. Each enchanted arrow costs the spell's mana when it is shot;
 * without enough mana the arrow is a plain arrow.
 */
public class SpellQuiverItem extends ArcaneAccessoryItem implements ICasterTool {
    public static final int DAMPEN = 2;

    public SpellQuiverItem() {
        super(new Item.Properties().rarity(Rarity.RARE).component(DataComponentRegistry.SPELL_CASTER, new SpellCaster()), 3);
    }

    public static Spell storedSpell(ItemStack stack) {
        SpellCaster caster = stack.get(DataComponentRegistry.SPELL_CASTER);
        return caster == null ? new Spell() : caster.getSpell();
    }

    /** The spell carried by arrows: forms removed, two Dampen after every effect (and its augments). */
    public static Spell arrowSpell(Spell stored) {
        List<AbstractSpellPart> parts = new ArrayList<>();
        for (AbstractSpellPart part : stored.unsafeList()) {
            if (!(part instanceof AbstractCastMethod)) {
                parts.add(part);
            }
        }
        List<AbstractSpellPart> out = new ArrayList<>();
        boolean inEffect = false;
        for (int i = 0; i < parts.size(); i++) {
            AbstractSpellPart part = parts.get(i);
            if (part instanceof AbstractEffect) {
                inEffect = true;
            }
            out.add(part);
            boolean chainEnds = i + 1 == parts.size() || parts.get(i + 1) instanceof AbstractEffect;
            if (inEffect && chainEnds) {
                for (int d = 0; d < DAMPEN; d++) {
                    out.add(AugmentDampen.INSTANCE);
                }
            }
        }
        return stored.mutable().setRecipe(out).immutable();
    }

    public static boolean hasEffect(Spell spell) {
        return spell.unsafeList().stream().anyMatch(part -> part instanceof AbstractEffect);
    }

    @Override
    public boolean isScribedSpellValid(AbstractCaster<?> caster, Player player, InteractionHand hand, ItemStack stack, Spell spell) {
        return hasEffect(spell);
    }

    @Override
    public void sendInvalidMessage(Player player) {
        PortUtil.sendMessageNoSpam(player, Component.translatable("arcane_artifacts.quiver.invalid"));
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
