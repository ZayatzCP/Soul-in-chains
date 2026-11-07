package com.soulinchains.arcaneartifacts.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

public final class CurioHelper {
    private CurioHelper() {
    }

    /** The first equipped stack of {@code item} in any Curios slot. The returned stack is the live slot stack. */
    public static Optional<ItemStack> findEquipped(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(inv -> inv.findFirstCurio(item))
                .map(SlotResult::stack);
    }

    public static Optional<SlotResult> findSlot(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity).flatMap(inv -> inv.findFirstCurio(item));
    }
}
