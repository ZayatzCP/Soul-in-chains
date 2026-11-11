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
 * Crystal Ball: a casting focus with 3 spell slots. Every spell cast from it gets two AOE for free, its
 * projectiles are slow and heavy (they fall in an arc), and it casts at most once every 3 seconds
 * (see {@link CrystalBallHandler}).
 */
public class CrystalBallItem extends SpellBook implements ISpellModifierItem {
    public static final int SLOTS = 3;
    public static final int BUILT_IN_AOE = 2;
    public static final int COOLDOWN_TICKS = 60;

    public CrystalBallItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC)
                .component(DataComponentRegistry.SPELL_CASTER, new SpellCaster(SLOTS)), SpellTier.THREE);
    }

    public static boolean castFrom(@Nullable SpellContext context) {
        return context != null && context.getCasterTool().getItem() instanceof CrystalBallItem;
    }

    @Override
    public SpellStats.Builder applyItemModifiers(ItemStack stack, SpellStats.Builder builder, AbstractSpellPart spellPart, HitResult rayTraceResult,
                                                 Level world, @Nullable LivingEntity shooter, SpellContext spellContext) {
        if (spellContext != null && spellContext.getCasterTool().getItem() == this) {
            builder.addAOE(BUILT_IN_AOE);
            builder.addAccelerationModifier(-1.0f);
        }
        return builder;
    }

    /** A plain item model instead of the spell book's animated book model. */
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
    }

    @Override
    public void appendHoverText(final @NotNull ItemStack stack, final @NotNull TooltipContext context, final @NotNull List<Component> tooltip, final @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.arcane_artifacts.crystal_ball.lore").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.arcane_artifacts.crystal_ball.desc.1").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.arcane_artifacts.crystal_ball.desc.2").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
