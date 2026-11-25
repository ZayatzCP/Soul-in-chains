package com.soulinchains.arcaneartifacts.rite;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * What a caged creature is worth to a rite. The base comes from two lists ({@code arcane_artifacts:rite_rare} 3 and
 * {@code arcane_artifacts:rite_otherworldly} 5), every other creature is worth 1. On top: +1 per piece of gear it carries
 * (+2 if enchanted), +1 for a baby or a named creature, and a creature riding or ridden counts the whole stack twice.
 */
public final class CageValue {
    public static final TagKey<EntityType<?>> RARE = TagKey.create(Registries.ENTITY_TYPE, ArcaneArtifacts.id("rite_rare"));
    public static final TagKey<EntityType<?>> OTHERWORLDLY = TagKey.create(Registries.ENTITY_TYPE, ArcaneArtifacts.id("rite_otherworldly"));
    public static final TagKey<EntityType<?>> FORBIDDEN = TagKey.create(Registries.ENTITY_TYPE, ArcaneArtifacts.id("rite_uncageable"));

    private CageValue() {
    }

    public static boolean otherworldly(EntityType<?> type) {
        return type.is(OTHERWORLDLY);
    }

    /** One creature, without its riders. */
    public static int single(Entity entity) {
        int value = entity.getType().is(OTHERWORLDLY) ? 5 : entity.getType().is(RARE) ? 3 : 1;
        if (entity instanceof LivingEntity living) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot == EquipmentSlot.BODY) {
                    continue;
                }
                ItemStack stack = living.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    value += stack.isEnchanted() ? 2 : 1;
                }
            }
            if (living.isBaby()) {
                value++;
            }
        }
        if (entity.hasCustomName()) {
            value++;
        }
        return value;
    }

    /** The creature together with everything riding it (or that it rides): twice the sum when there is more than one. */
    public static int of(Entity entity) {
        Entity root = entity.getRootVehicle();
        int sum = 0;
        int count = 0;
        for (Entity part : root.getSelfAndPassengers().toList()) {
            sum += single(part);
            count++;
        }
        return count > 1 ? sum * 2 : sum;
    }

    /** Whether the creature can be caged at all: not a player, not a boss, not on the forbidden list, worn down to half. */
    public static boolean cageable(LivingEntity target, boolean creative) {
        if (target instanceof net.minecraft.world.entity.player.Player || target.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES)
                || target.getType().is(FORBIDDEN) || !(target instanceof Mob)) {
            return false;
        }
        return creative || target.getHealth() <= target.getMaxHealth() / 2;
    }
}
