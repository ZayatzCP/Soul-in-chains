package com.soulinchains.arcaneartifacts.item;

import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Bottle of Source: a crafting resource from the Mudzyas. Poured into a Source Jar it gives back 100 Source. */
public class BottleOfSourceItem extends Item {
    public static final int SOURCE = 100;

    public BottleOfSourceItem() {
        super(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16).craftRemainder(Items.GLASS_BOTTLE));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof SourceJarTile jar)
                || jar.getMaxSource() - jar.getSource() < SOURCE) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide) {
            jar.addSource(SOURCE);
            var player = context.getPlayer();
            ItemStack stack = context.getItemInHand();
            if (player != null && !player.getAbilities().instabuild) {
                stack.shrink(1);
                if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
                    player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
                }
            }
            context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1f, 1f);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.arcane_artifacts.bottle_of_source.desc.1").withStyle(ChatFormatting.GRAY));
    }
}
