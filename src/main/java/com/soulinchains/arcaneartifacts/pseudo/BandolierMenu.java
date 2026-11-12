package com.soulinchains.arcaneartifacts.pseudo;

import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The bandolier's eight pouches (magic cartridges only, 16 each) and the player's inventory. */
public class BandolierMenu extends AbstractContainerMenu {
    public static final int POUCH_Y = 32;
    public static final int[] POUCH_X = {17, 35, 53, 71, 89, 107, 125, 143};

    private final ItemStack bandolier;
    private final SimpleContainer pouches;

    /** Client side: the contents arrive through slot sync. */
    public BandolierMenu(int id, Inventory inventory) {
        this(id, inventory, ItemStack.EMPTY);
    }

    public BandolierMenu(int id, Inventory inventory, ItemStack bandolier) {
        super(PseudomageRegistry.BANDOLIER_MENU.get(), id);
        this.bandolier = bandolier;
        this.pouches = new SimpleContainer(BandolierItem.POUCHES) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (!BandolierMenu.this.bandolier.isEmpty()) {
                    BandolierItem.setPouches(BandolierMenu.this.bandolier, getItems());
                }
            }

            @Override
            public int getMaxStackSize() {
                return BandolierItem.STACK;
            }
        };
        if (!bandolier.isEmpty()) {
            var items = BandolierItem.pouches(bandolier);
            for (int i = 0; i < items.size(); i++) {
                pouches.setItem(i, items.get(i));
            }
        }
        for (int i = 0; i < BandolierItem.POUCHES; i++) {
            addSlot(new Slot(pouches, i, POUCH_X[i], POUCH_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ModItems.MAGIC_CARTRIDGE.get());
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            int index = col;
            addSlot(new Slot(inventory, col, 8 + col * 18, 142) {
                @Override
                public boolean mayPickup(Player player) {
                    // The open bandolier itself stays in hand.
                    return getItem() != bandolier || bandolier.isEmpty();
                }
            });
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < BandolierItem.POUCHES) {
            if (!moveItemStackTo(stack, BandolierItem.POUCHES, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!stack.is(ModItems.MAGIC_CARTRIDGE.get()) || !moveItemStackTo(stack, 0, BandolierItem.POUCHES, false)) {
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
        return bandolier.isEmpty() || player.getInventory().contains(bandolier) || FalseMana.bandolier(player) == bandolier;
    }
}
