package com.soulinchains.arcaneartifacts.catto;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Catto's menu: equipment (tool, work item, two modules, bag), the bag itself and the player's inventory. Queue,
 * skills and reports are not slots: the server sends them in {@link CattoNetwork.SyncPayload}; buttons come back
 * through vanilla's container button packet.
 */
public class CattoMenu extends AbstractContainerMenu {
    public static final int BUTTON_PAUSE = 1;
    public static final int BUTTON_STOP = 2;
    public static final int BUTTON_CALL = 3;
    public static final int BUTTON_STAY = 4;
    public static final int BUTTON_CLEAR = 5;
    public static final int BUTTON_GIVE_ALL = 6;
    /** Queue row buttons: id = action * ROW_STRIDE + row. */
    public static final int ROW_STRIDE = 16;
    public static final int ROW_REMOVE = 1;
    public static final int ROW_UP = 2;
    public static final int ROW_MORE = 3;
    public static final int ROW_LESS = 4;

    public static final int EQUIPMENT_Y = 20;
    public static final int BAG_Y = 44;
    public static final int INVENTORY_Y = 112;
    public static final int[] EQUIPMENT_X = {8, 30, 56, 74, 100};

    @Nullable
    public final Catto cat;
    public final int catId;
    private int sentRevision = -1;
    private int ticks;

    /** Client side: the cat is found by its network id. */
    public CattoMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, inventory.player.level().getEntity(buf.readVarInt()) instanceof Catto c ? c : null, true);
    }

    public CattoMenu(int id, Inventory inventory, Catto cat) {
        this(id, inventory, cat, false);
    }

    private CattoMenu(int id, Inventory inventory, @Nullable Catto cat, boolean client) {
        super(CattoContent.MENU.get(), id);
        this.cat = cat;
        this.catId = cat == null ? -1 : cat.getId();
        ItemStackHandler equipment = cat != null ? cat.equipment : new ItemStackHandler(5);
        ItemStackHandler bag = cat != null ? cat.bag : new ItemStackHandler(Catto.BAG_SIZE);
        for (int i = 0; i < 5; i++) {
            final int slot = i;
            addSlot(new SlotItemHandler(equipment, i, EQUIPMENT_X[i], EQUIPMENT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return CattoMenu.this.cat == null || CattoMenu.this.cat.fitsSlot(slot, stack);
                }

                @Override
                public boolean mayPickup(Player player) {
                    return slot != Catto.SLOT_BAG || CattoMenu.this.cat == null || CattoMenu.this.cat.bagRemovable();
                }

                @Override
                public boolean isActive() {
                    return slot != Catto.SLOT_MODULE_2 || CattoMenu.this.cat == null || CattoMenu.this.cat.moduleSlotOpen(slot);
                }
            });
        }
        for (int i = 0; i < Catto.BAG_SIZE; i++) {
            final int slot = i;
            addSlot(new SlotItemHandler(bag, i, 8 + (i % 9) * 18, BAG_Y + (i / 9) * 18) {
                @Override
                public boolean isActive() {
                    return CattoMenu.this.cat == null || slot < CattoMenu.this.cat.capacity();
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isActive();
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
    }

    private static final int CAT_SLOTS = 5 + Catto.BAG_SIZE;

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < CAT_SLOTS) {
            if (!moveItemStackTo(stack, CAT_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            // Equipment first (a bag into the bag slot, a module into a module slot...), then the bag.
            for (int i = 0; i < 5 && !moved; i++) {
                Slot target = slots.get(i);
                if (target.isActive() && !target.hasItem() && target.mayPlace(stack) && i != Catto.SLOT_WORK) {
                    moved = moveItemStackTo(stack, i, i + 1, false);
                }
            }
            if (!moved) {
                int bagEnd = 5 + (cat == null ? Catto.BAG_SIZE : cat.capacity());
                if (!moveItemStackTo(stack, 5, bagEnd, false)) {
                    return ItemStack.EMPTY;
                }
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
        return cat != null && cat.isAlive() && player.distanceToSqr(cat) < 16 * 16 && player.getUUID().equals(cat.getOwnerUUID());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (cat != null && player instanceof ServerPlayer serverPlayer) {
            cat.button(serverPlayer, id);
            return true;
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (cat == null || cat.level().isClientSide()) {
            return;
        }
        ticks++;
        if (sentRevision != cat.revision || ticks % 40 == 0) {
            sentRevision = cat.revision;
            for (var listener : java.util.List.copyOf(cat.level().players())) {
                if (listener instanceof ServerPlayer sp && sp.containerMenu == this) {
                    CompoundTag data = cat.menuData();
                    PacketDistributor.sendToPlayer(sp, new CattoNetwork.SyncPayload(cat.getId(), data));
                }
            }
        }
    }
}
