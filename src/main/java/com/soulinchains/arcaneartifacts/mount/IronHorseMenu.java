package com.soulinchains.arcaneartifacts.mount;

import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;

/** The Iron Horse's coal bunker (anything a furnace burns) and the boiler's fuel gauge. */
public class IronHorseMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 80;
    public static final int SLOT_Y = 44;

    private final Container bunker;
    private final ContainerData data;
    @Nullable
    private final IronHorse horse;

    /** Client side: contents and gauge arrive through sync. */
    public IronHorseMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(1), new SimpleContainerData(2), null);
    }

    public IronHorseMenu(int id, Inventory inventory, Container bunker, ContainerData data, @Nullable IronHorse horse) {
        super(ModEntities.IRON_HORSE_MENU.get(), id);
        this.bunker = bunker;
        this.data = data;
        this.horse = horse;
        addSlot(new Slot(bunker, 0, SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isFuel(stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    /** Fuel in the boiler, 0..1. */
    public float fuelFraction() {
        int max = data.get(1);
        return max <= 0 ? 0F : Math.min(1F, data.get(0) / (float) max);
    }

    public int fuelSeconds() {
        return data.get(0) / 20;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!isFuel(stack) || !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return horse == null || horse.isAlive() && player.distanceToSqr(horse) < 64.0;
    }
}
