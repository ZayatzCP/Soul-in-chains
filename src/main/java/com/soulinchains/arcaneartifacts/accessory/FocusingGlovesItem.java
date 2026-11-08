package com.soulinchains.arcaneartifacts.accessory;

import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.items.ModItem;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import com.soulinchains.arcaneartifacts.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Focusing Gloves: a spell-casting tool held in the hand, like a spell book, a wand or the spell bow.
 * A spell is inscribed at the Scribes Table; using the gloves casts it twice for the price of one
 * (the second cast is free and follows a few ticks later, see {@link GlovesHandler}).
 * <p>
 * Like an Ars Nouveau wand, an inscribed spell without a form is cast as a Projectile, so the
 * "default form" of the gloves is Projectile; a spell that starts with its own form keeps it.
 */
public class FocusingGlovesItem extends ModItem implements ICasterTool {
    public FocusingGlovesItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).component(DataComponentRegistry.SPELL_CASTER, new SpellCaster()));
    }

    public static Spell storedSpell(ItemStack stack) {
        SpellCaster caster = stack.get(DataComponentRegistry.SPELL_CASTER);
        return caster == null ? new Spell() : caster.getSpell();
    }

    /** The spell the gloves actually cast: Projectile is prepended when the inscribed spell has no form. */
    public static Spell castableSpell(ItemStack stack) {
        Spell stored = storedSpell(stack);
        if (stored.isEmpty() || stored.getCastMethod() != null) {
            return stored.isEmpty() ? new Spell(MethodProjectile.INSTANCE) : stored;
        }
        List<AbstractSpellPart> recipe = new ArrayList<>();
        recipe.add(MethodProjectile.INSTANCE);
        recipe.addAll(stored.unsafeList());
        return stored.mutable().setRecipe(recipe).immutable();
    }

    private static boolean hasEffect(Spell spell) {
        return spell.unsafeList().stream().anyMatch(part -> part instanceof AbstractEffect);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        Spell spell = castableSpell(stack);
        if (!hasEffect(spell)) {
            PortUtil.sendMessageNoSpam(player, Component.translatable("arcane_artifacts.gloves.empty"));
            return InteractionResultHolder.fail(stack);
        }
        SpellContext context = SpellContext.fromEntity(spell, player, stack);
        if (new SpellResolver(context).onCast(stack, level)) {
            if (player instanceof ServerPlayer serverPlayer) {
                GlovesHandler.scheduleRepeat(serverPlayer, spell, stack);
            }
            player.getCooldowns().addCooldown(this, Math.max(1, Config.GLOVES_COOLDOWN_TICKS.get()));
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public boolean isScribedSpellValid(AbstractCaster<?> caster, Player player, InteractionHand hand, ItemStack stack, Spell spell) {
        return spell.isValid();
    }

    @Override
    public void sendInvalidMessage(Player player) {
        PortUtil.sendMessageNoSpam(player, Component.translatable("arcane_artifacts.gloves.invalid"));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        for (int i = 1; i <= 3; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".desc." + i).withStyle(ChatFormatting.GRAY));
        }
        Spell stored = storedSpell(stack);
        if (stored.isEmpty()) {
            tooltip.add(Component.translatable("arcane_artifacts.gloves.default_pattern").withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("arcane_artifacts.caster.stored", castableSpell(stack).getDisplayString()).withStyle(ChatFormatting.AQUA));
        }
    }
}
