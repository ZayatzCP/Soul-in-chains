package com.soulinchains.arcaneartifacts.belt;

import com.soulinchains.arcaneartifacts.registry.ModComponents;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * The belt's screen: the three pouches on top, the player inventory below. The pouches are not slots:
 * clicks on them are sent as menu buttons ({@code pouch * 4 + action}) and handled here on the server.
 * Actions: 0 put the carried stack in, 1 take a stack onto the cursor, 2 take a stack into the inventory,
 * 3 put every matching item from the inventory in.
 */
public class BeltMenu extends AbstractContainerMenu {
    public static final int PUT_CARRIED = 0, TAKE_TO_CURSOR = 1, TAKE_TO_INVENTORY = 2, PUT_ALL = 3;
    public static final int INVENTORY_X = 8, INVENTORY_Y = 84;

    private final Player player;

    public BeltMenu(int id, Inventory inventory) {
        super(BeltRegistry.MENU.get(), id);
        this.player = inventory.player;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        }
    }

    public Optional<ItemStack> belt() {
        return CurioHelper.findEquipped(player, ModItems.BOTTOMLESS_BELT.get());
    }

    public BeltContents contents() {
        return belt().map(BottomlessBeltItem::contents).orElse(BeltContents.EMPTY);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        Optional<ItemStack> beltStack = belt();
        if (beltStack.isEmpty()) {
            return false;
        }
        ItemStack belt = beltStack.get();
        int pouch = id / 4;
        int action = id % 4;
        if (pouch < 0 || pouch >= BeltContents.POUCHES) {
            return false;
        }
        BeltContents contents = BottomlessBeltItem.contents(belt);
        BeltContents.Pouch current = contents.get(pouch);
        switch (action) {
            case PUT_CARRIED -> {
                ItemStack carried = getCarried();
                if (carried.isEmpty() || !current.accepts(carried) || otherPouchHolds(contents, pouch, carried)) {
                    return false;
                }
                contents = contents.add(pouch, carried, carried.getCount());
                setCarried(ItemStack.EMPTY);
            }
            case TAKE_TO_CURSOR -> {
                if (current.isEmpty() || !getCarried().isEmpty()) {
                    return false;
                }
                int amount = (int) Math.min(current.count(), current.type().getMaxStackSize());
                setCarried(current.type().copyWithCount(amount));
                contents = contents.remove(pouch, amount);
            }
            case TAKE_TO_INVENTORY -> {
                if (current.isEmpty()) {
                    return false;
                }
                int amount = (int) Math.min(current.count(), current.type().getMaxStackSize());
                ItemStack out = current.type().copyWithCount(amount);
                player.getInventory().add(out);
                contents = contents.remove(pouch, amount - out.getCount());
            }
            case PUT_ALL -> {
                if (current.isEmpty()) {
                    return false;
                }
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.items.size(); i++) {
                    ItemStack stack = inventory.items.get(i);
                    if (ItemStack.isSameItemSameComponents(stack, current.type())) {
                        contents = contents.add(pouch, stack, stack.getCount());
                        inventory.items.set(i, ItemStack.EMPTY);
                    }
                }
            }
            default -> {
                return false;
            }
        }
        belt.set(ModComponents.BELT_CONTENTS, contents);
        broadcastChanges();
        return true;
    }

    private static boolean otherPouchHolds(BeltContents contents, int pouch, ItemStack stack) {
        int holder = contents.pouchFor(stack);
        return holder >= 0 && holder != pouch;
    }

    /** Shift-click in the inventory: into the pouch holding that item, or the first empty pouch. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        Optional<ItemStack> beltStack = belt();
        if (!slot.hasItem() || beltStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        BeltContents contents = BottomlessBeltItem.contents(beltStack.get());
        int pouch = contents.pouchFor(stack);
        if (pouch < 0) {
            pouch = contents.firstFree();
        }
        if (pouch < 0) {
            return ItemStack.EMPTY;
        }
        beltStack.get().set(ModComponents.BELT_CONTENTS, contents.add(pouch, stack, stack.getCount()));
        slot.set(ItemStack.EMPTY);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return belt().isPresent();
    }
}
