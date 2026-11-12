package com.soulinchains.arcaneartifacts.accessory;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Researcher's Secret Notebook: the notes of the wizard "???". Opens the Arcane Artifacts section of the
 * Ars Nouveau book interface (descriptions, lore and recipes of every artifact).
 */
public class ResearcherNotebookItem extends Item {
    private final boolean pseudomage;

    public ResearcherNotebookItem() {
        this(false);
    }

    /** {@code pseudomage}: the False Mage's Guide, the Pseudomage's own book. */
    public ResearcherNotebookItem(boolean pseudomage) {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        this.pseudomage = pseudomage;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (level.isClientSide) {
            if (pseudomage) {
                com.soulinchains.arcaneartifacts.client.ClientAccess.openFalseMageGuide();
            } else {
                com.soulinchains.arcaneartifacts.client.ClientAccess.openNotebook();
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc.1").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
