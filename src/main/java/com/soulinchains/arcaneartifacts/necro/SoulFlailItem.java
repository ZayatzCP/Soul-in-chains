package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * Soul Flail (Shackled, tier 2): a heavy iron ball on a chain. 9 damage, slow, reaches a block farther and knocks a
 * raised shield down like an axe. A blow on a soul wisp catches it; every {@link #SHARDS_PER_SOUL} blows on living
 * creatures chip enough soul shards together to make a Wandering Soul in the lantern.
 */
public class SoulFlailItem extends SwordItem {
    public static final int SHARDS_PER_SOUL = 10;

    public SoulFlailItem() {
        super(Tiers.IRON, new Properties().rarity(Rarity.UNCOMMON).component(ModComponents.SOUL_SHARDS, 0).attributes(attributes()));
    }

    private static ItemAttributeModifiers attributes() {
        ItemAttributeModifiers base = SwordItem.createAttributes(Tiers.IRON, 5, -3.2F);
        return base.withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(ArcaneArtifacts.id("soul_flail_reach"), 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (attacker instanceof Player player && !player.level().isClientSide && SoulType.of(target) != null) {
            int shards = stack.getOrDefault(ModComponents.SOUL_SHARDS, 0) + 1;
            if (shards >= SHARDS_PER_SOUL && Souls.store(player, SoulType.WANDERING)) {
                shards = 0;
                player.displayClientMessage(Component.translatable("arcane_artifacts.soul_flail.shards_soul"), true);
            }
            stack.set(ModComponents.SOUL_SHARDS, Math.min(shards, SHARDS_PER_SOUL));
        }
        return result;
    }

    /** Knocks a raised shield down, like an axe. */
    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        for (int i = 1; i <= 3; i++) {
            tooltip.add(Component.translatable(getDescriptionId() + ".desc." + i).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("arcane_artifacts.soul_flail.shards", stack.getOrDefault(ModComponents.SOUL_SHARDS, 0), SHARDS_PER_SOUL)
                .withStyle(ChatFormatting.AQUA));
    }
}
