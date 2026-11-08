package com.soulinchains.arcaneartifacts.item;

import com.hollingsworth.arsnouveau.api.item.ISpellModifierItem;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellCaster;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.List;
import java.util.function.Consumer;

/**
 * Archmage's Staff: a spell book in staff form. It works exactly like an Ars Nouveau spell book (same
 * crafting screen on the book key, same radial menu, archmage tier glyphs) but holds 5 spells instead
 * of 10, and every spell cast from it gets two levels of Amplify for free, as if two Amplify glyphs were
 * carved into the wood.
 */
public class SpellStaffItem extends SpellBook implements ISpellModifierItem {
    public static final int SLOTS = 5;
    public static final int BUILT_IN_AMPLIFY = 2;

    public SpellStaffItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC)
                .component(DataComponentRegistry.SPELL_CASTER, new SpellCaster(SLOTS)), SpellTier.THREE);
    }

    /** Amplifies only spells cast from this staff, not spells from other casters while the staff is carried. */
    @Override
    public SpellStats.Builder applyItemModifiers(ItemStack stack, SpellStats.Builder builder, AbstractSpellPart spellPart, HitResult rayTraceResult,
                                                 Level world, @Nullable LivingEntity shooter, SpellContext spellContext) {
        if (spellContext != null && spellContext.getCasterTool().getItem() == this) {
            builder.addAmplification(BUILT_IN_AMPLIFY);
        }
        return builder;
    }

    /** A plain item model instead of the spell book's animated book model. */
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
    }

    @Override
    public void appendHoverText(final @NotNull ItemStack stack, final @NotNull TooltipContext context, final @NotNull List<Component> tooltip, final @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.arcane_artifacts.spell_staff.desc.1").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.arcane_artifacts.spell_staff.desc.2").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
