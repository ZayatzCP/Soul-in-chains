package com.soulinchains.arcaneartifacts.alchemy;

import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Alchemical Distiller menu: 4 ingredients, 1 modifier, 1 fuel, 3 bottles, then the player inventory. */
public class AlchemicalDistillerMenu extends AbstractContainerMenu {
    public static final int[][] SLOT_POS = {
            {44, 18}, {62, 18}, {80, 18}, {98, 18}, // ingredients
            {140, 18},                               // modifier
            {17, 18},                                // fuel
            {58, 70}, {80, 74}, {102, 70}            // bottles
    };
    private final Container container;
    private final ContainerData data;

    public AlchemicalDistillerMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(AlchemicalDistillerBlockEntity.SIZE), new SimpleContainerData(4));
    }

    public AlchemicalDistillerMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModBlocks.ALCHEMICAL_DISTILLER_MENU.get(), id);
        checkContainerSize(container, AlchemicalDistillerBlockEntity.SIZE);
        this.container = container;
        this.data = data;
        for (int i = 0; i < AlchemicalDistillerBlockEntity.SIZE; i++) {
            final int index = i;
            addSlot(new Slot(container, i, SLOT_POS[i][0], SLOT_POS[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(index, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return index >= AlchemicalDistillerBlockEntity.FIRST_BOTTLE ? 1 : super.getMaxStackSize();
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 162));
        }
        addDataSlots(data);
    }

    public int progress() {
        return data.get(0);
    }

    public int brewTime() {
        return Math.max(1, data.get(1));
    }

    public int charges() {
        return data.get(2);
    }

    public boolean sourceNearby() {
        return data.get(3) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int size = AlchemicalDistillerBlockEntity.SIZE;
        if (index < size) {
            if (!moveItemStackTo(stack, size, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < size && !stack.isEmpty(); i++) {
                if (container.canPlaceItem(i, stack) && moveItemStackTo(stack, i, i + 1, false)) {
                    moved = true;
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
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
        return container.stillValid(player);
    }
}
