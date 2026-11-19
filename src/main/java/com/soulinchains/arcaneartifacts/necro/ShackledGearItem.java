package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Shackled set pieces: Shackle Gloves (hands), Shroud (back), Bone Crown (head), Shackled Boots (feet). The Soul
 * Lantern and the Phylactery Amulet have their own classes.
 */
public class ShackledGearItem extends ArcaneAccessoryItem {
    public enum Piece { GLOVES, SHROUD, CROWN, BOOTS }

    public final Piece piece;

    public ShackledGearItem(Piece piece) {
        super(new Item.Properties().rarity(Rarity.RARE), 2);
        this.piece = piece;
    }

    @Override
    protected int maxManaBonus() {
        return piece == Piece.SHROUD ? ShackledSet.SHROUD_MANA : 0;
    }

    /** Boots: heavy - half the knockback. */
    @Override
    public com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>,
            net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(top.theillusivec4.curios.api.SlotContext slotContext,
                                                                                               net.minecraft.resources.ResourceLocation id,
                                                                                               net.minecraft.world.item.ItemStack stack) {
        var map = super.getAttributeModifiers(slotContext, id, stack);
        if (piece == Piece.BOOTS && !slotContext.cosmetic()) {
            map.put(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(id.withSuffix("_knockback"), ShackledSet.BOOTS_KNOCKBACK,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        }
        return map;
    }

    /** Boots: soul sand, soul soil and graveyard soil speed you up instead of slowing you down. */
    @Override
    public void curioTick(top.theillusivec4.curios.api.SlotContext slotContext, net.minecraft.world.item.ItemStack stack) {
        var wearer = slotContext.entity();
        if (piece != Piece.BOOTS || wearer.level().isClientSide || slotContext.cosmetic() || wearer.tickCount % 10 != 0) {
            return;
        }
        var below = wearer.level().getBlockState(wearer.getBlockPosBelowThatAffectsMyMovement());
        if (below.is(net.minecraft.tags.BlockTags.SOUL_SPEED_BLOCKS) || below.getBlock() instanceof GraveSoilBlock) {
            wearer.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 30, 1, true, false, true));
        }
    }
}
