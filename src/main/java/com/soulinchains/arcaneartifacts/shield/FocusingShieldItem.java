package com.soulinchains.arcaneartifacts.shield;

import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHeal;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Focusing Shield: inscribe a spell at the Scribes Table (from a spell book or the Archmage Staff).
 * Every successful block casts it (mana is paid as usual), at most once a second. Healing spells are
 * refused: the shield is for answering blows, not for shrugging them off.
 */
public class FocusingShieldItem extends ShieldItem implements ICasterTool {
    public FocusingShieldItem() {
        super(new Item.Properties().durability(640).rarity(Rarity.RARE).component(DataComponentRegistry.SPELL_CASTER, new SpellCaster()));
    }

    public static Spell storedSpell(ItemStack stack) {
        SpellCaster caster = stack.get(DataComponentRegistry.SPELL_CASTER);
        return caster == null ? new Spell() : caster.getSpell();
    }

    public static boolean isAllowed(Spell spell) {
        return !spell.isEmpty() && !spell.unsafeList().contains(EffectHeal.INSTANCE);
    }

    @Override
    public boolean isScribedSpellValid(AbstractCaster<?> caster, Player player, InteractionHand hand, ItemStack stack, Spell spell) {
        return isAllowed(spell);
    }

    @Override
    public void sendInvalidMessage(Player player) {
        PortUtil.sendMessageNoSpam(player, Component.translatable("arcane_artifacts.focusing_shield.invalid"));
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(ModItems.ROSE_GOLD_INGOT.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.2").withStyle(ChatFormatting.GRAY));
        Spell spell = storedSpell(stack);
        tooltip.add(spell.isEmpty()
                ? Component.translatable("arcane_artifacts.caster.empty").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("arcane_artifacts.caster.stored", spell.getDisplayString()).withStyle(ChatFormatting.AQUA));
    }
}
