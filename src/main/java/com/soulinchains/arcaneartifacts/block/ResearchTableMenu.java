package com.soulinchains.arcaneartifacts.block;

import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.research.ResearchManager;
import com.soulinchains.arcaneartifacts.research.ResearchService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu of the Research Table. Actions are vanilla menu button clicks, validated here on the server:
 * {@link #BUTTON_OBSERVE} observes the sample; {@code BUTTON_FORMULATE + index} formulates the research
 * at that index of {@link ResearchManager#all()} (same order on both sides).
 */
public class ResearchTableMenu extends AbstractContainerMenu {
    public static final int BUTTON_OBSERVE = 0;
    public static final int BUTTON_FORMULATE = 100;

    // Screen layout, shared with ResearchTableScreen.
    public static final int SAMPLE_X = 82, SAMPLE_Y = 46;
    public static final int INVENTORY_X = 8, INVENTORY_Y = 148;

    private final ContainerLevelAccess access;
    private final ItemStackHandler sample;
    private final BlockPos pos;

    /** Client constructor (position sent by the server when opening). */
    public ResearchTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new ItemStackHandler(1));
    }

    public ResearchTableMenu(int id, Inventory inventory, ResearchTableBlockEntity table) {
        this(id, inventory, table.getBlockPos(), table.inventory());
    }

    private ResearchTableMenu(int id, Inventory inventory, BlockPos pos, ItemStackHandler sample) {
        super(ModBlocks.RESEARCH_TABLE_MENU.get(), id);
        this.pos = pos;
        this.sample = sample;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        addSlot(new SlotItemHandler(sample, 0, SAMPLE_X, SAMPLE_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        }
    }

    public ItemStack sample() {
        return getSlot(0).getItem();
    }

    public BlockPos pos() {
        return pos;
    }

    public static List<ResourceLocation> researchOrder() {
        return new ArrayList<>(ResearchManager.all().keySet());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_OBSERVE) {
            return ResearchService.observe(player, sample()) >= 0;
        }
        if (id >= BUTTON_FORMULATE) {
            List<ResourceLocation> order = researchOrder();
            int index = id - BUTTON_FORMULATE;
            return index < order.size() && ResearchService.formulate(player, order.get(index));
        }
        return false;
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
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.RESEARCH_TABLE.get());
    }
}
