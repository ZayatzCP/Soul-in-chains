package com.soulinchains.arcaneartifacts.pseudo;

import com.soulinchains.arcaneartifacts.registry.ModComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import javax.annotation.Nullable;

/** The parts fitted into a gun, kept on the gun itself: slot 0 barrel, 1 cylinder, 2 stock. */
public final class GunParts {
    public static final int SLOTS = 3;

    private GunParts() {
    }

    public static NonNullList<ItemStack> all(ItemStack gun) {
        NonNullList<ItemStack> parts = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ItemContainerContents contents = gun.get(ModComponents.GUN_PARTS);
        if (contents != null) {
            contents.copyInto(parts);
        }
        return parts;
    }

    public static ItemStack get(ItemStack gun, GunPartItem.Slot slot) {
        return all(gun).get(slot.ordinal());
    }

    public static void set(ItemStack gun, GunPartItem.Slot slot, ItemStack part) {
        NonNullList<ItemStack> parts = all(gun);
        parts.set(slot.ordinal(), part.copyWithCount(part.isEmpty() ? 0 : 1));
        if (parts.stream().allMatch(ItemStack::isEmpty)) {
            gun.remove(ModComponents.GUN_PARTS);
        } else {
            gun.set(ModComponents.GUN_PARTS, ItemContainerContents.fromItems(parts));
        }
    }

    @Nullable
    public static GunPartItem.Part part(ItemStack gun, GunPartItem.Slot slot) {
        return get(gun, slot).getItem() instanceof GunPartItem item ? item.part() : null;
    }

    public static boolean has(ItemStack gun, GunPartItem.Part part) {
        return part(gun, part.slot) == part;
    }

    /** True if the part may go into that slot. */
    public static boolean fits(ItemStack part, GunPartItem.Slot slot) {
        return part.getItem() instanceof GunPartItem item && item.part().slot == slot;
    }
}
