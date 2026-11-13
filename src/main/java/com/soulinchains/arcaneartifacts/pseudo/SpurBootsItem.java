package com.soulinchains.arcaneartifacts.pseudo;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.accessory.ArcaneAccessoryItem;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

/**
 * Pseudomage's Boots: worn-in riding boots with spurs. +30% movement speed, magma blocks do not burn, and
 * the spurs make whatever the wearer rides 30% faster too.
 */
public class SpurBootsItem extends ArcaneAccessoryItem {
    public static final double SPEED = 0.30;
    /** The modifier the spurs put on a mount while the wearer rides it. */
    public static final ResourceLocation MOUNT_SPEED = ArcaneArtifacts.id("spurs_mount_speed");

    public SpurBootsItem() {
        super(new Item.Properties().rarity(Rarity.RARE), 3);
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> map = LinkedHashMultimap.create();
        if (!slotContext.cosmetic()) {
            map.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(id.withSuffix("_speed"), SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        return map;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (!slotContext.cosmetic() && !wearer.level().isClientSide && wearer.tickCount % 10 == 0) {
            spur(wearer.getVehicle());
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        super.onUnequip(slotContext, newStack, stack);
        unspur(slotContext.entity().getVehicle());
    }

    public static boolean isWearing(LivingEntity entity) {
        return CurioHelper.findEquipped(entity, ModItems.PSEUDOMAGE_BOOTS.get()).isPresent();
    }

    /** Spurs the mount on: +30% speed while ridden by the wearer. */
    public static void spur(Entity vehicle) {
        if (vehicle instanceof LivingEntity mount) {
            AttributeInstance speed = mount.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && !speed.hasModifier(MOUNT_SPEED)) {
                speed.addTransientModifier(new AttributeModifier(MOUNT_SPEED, SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }

    public static void unspur(Entity vehicle) {
        if (vehicle instanceof LivingEntity mount) {
            AttributeInstance speed = mount.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(MOUNT_SPEED);
            }
        }
    }

    @EventBusSubscriber(modid = ArcaneArtifacts.MODID)
    public static final class Events {
        private Events() {
        }

        /** Magma blocks ("hot floor") do not hurt the wearer. */
        @SubscribeEvent
        public static void onDamage(LivingIncomingDamageEvent event) {
            if (event.getSource().is(DamageTypes.HOT_FLOOR) && isWearing(event.getEntity())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onMount(EntityMountEvent event) {
            if (event.getLevel().isClientSide() || !(event.getEntityMounting() instanceof LivingEntity rider)) {
                return;
            }
            if (event.isDismounting()) {
                unspur(event.getEntityBeingMounted());
            } else if (isWearing(rider)) {
                spur(event.getEntityBeingMounted());
            }
        }
    }
}
