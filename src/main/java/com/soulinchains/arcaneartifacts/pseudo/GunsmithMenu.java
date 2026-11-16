package com.soulinchains.arcaneartifacts.pseudo;

import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Farraday's Workshop: a gun slot and three part slots (barrel, cylinder, stock). The part slots show the
 * parts fitted in the gun; putting a part in or taking it out fits it or removes it at once. The gun goes
 * back to the player when the bench is closed.
 */
public class GunsmithMenu extends AbstractContainerMenu {
    public static final int GUN_X = 34;
    public static final int GUN_Y = 35;
    public static final int[] PART_X = {98, 120, 142};
    public static final int PART_Y = 35;

    private final ContainerLevelAccess access;
    private final Bench bench;

    /** Client side. */
    public GunsmithMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public GunsmithMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModBlocks.GUNSMITH_MENU.get(), id);
        this.access = access;
        this.bench = new Bench(access != ContainerLevelAccess.NULL);
        addSlot(new Slot(bench, 0, GUN_X, GUN_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof MagicGunItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (GunPartItem.Slot partSlot : GunPartItem.Slot.values()) {
            int index = partSlot.ordinal() + 1;
            addSlot(new Slot(bench, index, PART_X[partSlot.ordinal()], PART_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !bench.getItem(0).isEmpty() && GunParts.fits(stack, partSlot);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    /** The bench's four slots; on the server, part slots mirror the parts fitted in the gun. */
    static final class Bench extends SimpleContainer {
        private final boolean server;
        private boolean syncing;

        Bench(boolean server) {
            super(1 + GunParts.SLOTS);
            this.server = server;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            super.setItem(index, stack);
            if (!server || syncing) {
                return;
            }
            syncing = true;
            ItemStack gun = getItem(0);
            if (index == 0) {
                // A gun put in shows its parts; a gun taken out takes them along.
                var parts = gun.isEmpty() ? null : GunParts.all(gun);
                for (int i = 0; i < GunParts.SLOTS; i++) {
                    super.setItem(i + 1, parts == null ? ItemStack.EMPTY : parts.get(i));
                }
            } else if (!gun.isEmpty()) {
                GunParts.set(gun, GunPartItem.Slot.values()[index - 1], stack);
            }
            syncing = false;
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            ItemStack taken = super.removeItem(index, count);
            setItem(index, getItem(index));
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            ItemStack taken = super.removeItemNoUpdate(index);
            setItem(index, ItemStack.EMPTY);
            return taken;
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
        int benchSlots = 1 + GunParts.SLOTS;
        if (index < benchSlots) {
            if (!moveItemStackTo(stack, benchSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof MagicGunItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof GunPartItem part) {
            int target = part.part().slot.ordinal() + 1;
            if (!slots.get(target).mayPlace(stack) || !moveItemStackTo(stack, target, target + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
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
    public void removed(Player player) {
        super.removed(player);
        // Only the gun goes back: the parts are inside it.
        access.execute((level, pos) -> {
            ItemStack gun = bench.getItem(0);
            for (int i = 1; i < bench.getContainerSize(); i++) {
                bench.syncing = true;
                bench.setItem(i, ItemStack.EMPTY);
                bench.syncing = false;
            }
            if (!gun.isEmpty()) {
                bench.syncing = true;
                bench.setItem(0, ItemStack.EMPTY);
                bench.syncing = false;
                if (!player.addItem(gun)) {
                    player.drop(gun, false);
                }
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.GUNSMITH_BENCH.get());
    }
}
