package com.soulinchains.arcaneartifacts.alchemy;

import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Alchemical Distiller's contents and brewing. Like a brewing stand, but every ingredient in the four
 * ingredient slots adds its own effect to the same potion. Each brew takes Source from Source Jars within
 * 6 blocks and one charge of nether wart blocks (one block gives {@link #CHARGES_PER_FUEL} charges).
 * Modifier slot: redstone (twice as long), glowstone (one level stronger, half as long), gunpowder
 * (splash), dragon's breath (lingering).
 */
public class AlchemicalDistillerBlockEntity extends BaseContainerBlockEntity {
    public static final int INGREDIENTS = 4;
    public static final int MODIFIER = 4;
    public static final int FUEL = 5;
    public static final int FIRST_BOTTLE = 6;
    public static final int BOTTLES = 3;
    public static final int SIZE = FIRST_BOTTLE + BOTTLES;
    public static final int CHARGES_PER_FUEL = 3;
    public static final int SOURCE_RANGE = 6;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int progress;
    private int charges;
    private boolean sourceNearby;

    /** 0: brew progress, 1: brew time, 2: fuel charges, 3: Source nearby (1/0). */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> brewTime();
                case 2 -> charges;
                case 3 -> sourceNearby ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 2 -> charges = value;
                case 3 -> sourceNearby = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public AlchemicalDistillerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ALCHEMICAL_DISTILLER_ENTITY.get(), pos, state);
    }

    public static int brewTime() {
        return Math.max(20, Config.DISTILLER_BREW_TICKS.get());
    }

    public static int sourceCost() {
        return Config.DISTILLER_SOURCE_COST.get();
    }

    public static boolean isModifier(ItemStack stack) {
        return stack.is(Items.REDSTONE) || stack.is(Items.GLOWSTONE_DUST) || stack.is(Items.GUNPOWDER) || stack.is(Items.DRAGON_BREATH);
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(Items.NETHER_WART_BLOCK);
    }

    /** Only plain bottles: water, awkward, mundane or thick potions (no effects yet). */
    public static boolean isBaseBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return !contents.hasEffects();
    }

    private boolean hasIngredients() {
        for (int i = 0; i < INGREDIENTS; i++) {
            if (DistillerIngredient.of(items.get(i)) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBottles() {
        for (int i = FIRST_BOTTLE; i < SIZE; i++) {
            if (isBaseBottle(items.get(i))) {
                return true;
            }
        }
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalDistillerBlockEntity distiller) {
        if (level.getGameTime() % 20 == 0) {
            distiller.sourceNearby = SourceUtil.hasSourceNearby(pos, level, SOURCE_RANGE, sourceCost());
        }
        boolean ready = distiller.hasIngredients() && distiller.hasBottles();
        if (!ready) {
            if (distiller.progress > 0) {
                distiller.progress = 0;
                distiller.setChanged();
            }
            return;
        }
        if (distiller.progress == 0) {
            // Starting a brew: one fuel charge and the Source are paid up front.
            if (distiller.charges <= 0) {
                ItemStack fuel = distiller.items.get(FUEL);
                if (!isFuel(fuel)) {
                    return;
                }
                fuel.shrink(1);
                distiller.charges = CHARGES_PER_FUEL;
            }
            if (SourceUtil.takeSourceMultipleWithParticles(pos, level, SOURCE_RANGE, sourceCost()) == null) {
                distiller.sourceNearby = false;
                return;
            }
            distiller.charges--;
            distiller.progress = 1;
            distiller.setChanged();
            return;
        }
        if (++distiller.progress >= brewTime()) {
            distiller.brew();
            distiller.progress = 0;
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 0.8f);
        }
        distiller.setChanged();
    }

    /** Brews right away, ignoring fuel, Source and time (used by tests). */
    public void brewNow() {
        brew();
    }

    /** Turns every plain bottle into a potion with all the ingredients' effects, then uses the ingredients up. */
    private void brew() {
        ItemStack modifier = items.get(MODIFIER);
        double durationScale = 1.0;
        int extraAmplifier = 0;
        Item potionItem = Items.POTION;
        if (modifier.is(Items.REDSTONE)) {
            durationScale = 2.0;
        } else if (modifier.is(Items.GLOWSTONE_DUST)) {
            durationScale = 0.5;
            extraAmplifier = 1;
        } else if (modifier.is(Items.GUNPOWDER)) {
            potionItem = Items.SPLASH_POTION;
        } else if (modifier.is(Items.DRAGON_BREATH)) {
            potionItem = Items.LINGERING_POTION;
        }
        List<MobEffectInstance> effects = new ArrayList<>();
        for (int i = 0; i < INGREDIENTS; i++) {
            DistillerIngredient ingredient = DistillerIngredient.of(items.get(i));
            if (ingredient != null) {
                effects.add(ingredient.instance(durationScale, extraAmplifier));
            }
        }
        PotionContents contents = new PotionContents(Optional.empty(), Optional.empty(), effects);
        for (int i = FIRST_BOTTLE; i < SIZE; i++) {
            if (isBaseBottle(items.get(i))) {
                ItemStack potion = new ItemStack(potionItem);
                potion.set(DataComponents.POTION_CONTENTS, contents);
                potion.set(DataComponents.CUSTOM_NAME, Component.translatable("item.arcane_artifacts.distilled_potion").withStyle(style -> style.withItalic(false)));
                items.set(i, potion);
            }
        }
        for (int i = 0; i < INGREDIENTS; i++) {
            if (DistillerIngredient.of(items.get(i)) != null) {
                ItemStack stack = items.get(i);
                ItemStack remainder = stack.getCraftingRemainingItem();
                stack.shrink(1);
                if (stack.isEmpty() && !remainder.isEmpty()) {
                    items.set(i, remainder);
                }
            }
        }
        if (isModifier(modifier)) {
            ItemStack remainder = modifier.getCraftingRemainingItem();
            modifier.shrink(1);
            if (modifier.isEmpty() && !remainder.isEmpty()) {
                items.set(MODIFIER, remainder);
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.arcane_artifacts.alchemical_distiller");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < INGREDIENTS) {
            return DistillerIngredient.of(stack) != null;
        }
        if (slot == MODIFIER) {
            return isModifier(stack);
        }
        if (slot == FUEL) {
            return isFuel(stack);
        }
        return isBaseBottle(stack);
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new AlchemicalDistillerMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Progress", progress);
        tag.putInt("Charges", charges);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = tag.getInt("Progress");
        charges = tag.getInt("Charges");
    }
}
