package com.soulinchains.arcaneartifacts.catto;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Catto's hands: its bag, the storages it is bound to, and breaking and placing blocks. Blocks are broken and placed
 * through a fake player in Catto's name, so claim and protection mods see (and may cancel) every action.
 */
public final class CattoWorld {
    private CattoWorld() {
    }

    // ------------------------------------------------------------------ bag

    public static int bagCount(Catto cat, Predicate<ItemStack> filter) {
        int n = 0;
        for (int i = 0; i < cat.capacity(); i++) {
            ItemStack stack = cat.bag.getStackInSlot(i);
            if (!stack.isEmpty() && filter.test(stack)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    public static boolean bagEmpty(Catto cat) {
        return bagCount(cat, s -> true) == 0;
    }

    /** True when no slot is free (Catto goes to empty the bag before picking up more). */
    public static boolean bagFull(Catto cat) {
        for (int i = 0; i < cat.capacity(); i++) {
            if (cat.bag.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Puts a stack into the usable part of the bag; returns what did not fit. */
    public static ItemStack bagInsert(Catto cat, ItemStack stack) {
        ItemStack rest = stack;
        for (int i = 0; i < cat.capacity() && !rest.isEmpty(); i++) {
            if (!cat.bag.getStackInSlot(i).isEmpty()) {
                rest = cat.bag.insertItem(i, rest, false);
            }
        }
        for (int i = 0; i < cat.capacity() && !rest.isEmpty(); i++) {
            rest = cat.bag.insertItem(i, rest, false);
        }
        return rest;
    }

    public static boolean bagFits(Catto cat, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = 0; i < cat.capacity() && !rest.isEmpty(); i++) {
            rest = cat.bag.insertItem(i, rest, true);
        }
        return rest.isEmpty();
    }

    /** Takes up to {@code amount} matching items out of the bag. */
    public static int bagRemove(Catto cat, Predicate<ItemStack> filter, int amount) {
        int left = amount;
        for (int i = 0; i < cat.capacity() && left > 0; i++) {
            ItemStack stack = cat.bag.getStackInSlot(i);
            if (!stack.isEmpty() && filter.test(stack)) {
                left -= cat.bag.extractItem(i, left, false).getCount();
            }
        }
        return amount - left;
    }

    /** Picks up item entities around a spot; returns how many items went into the bag. */
    public static int vacuum(Catto cat, BlockPos around, double radius, Predicate<ItemStack> filter) {
        int picked = 0;
        for (ItemEntity item : cat.level().getEntitiesOfClass(ItemEntity.class, new AABB(around).inflate(radius),
                e -> e.isAlive() && !e.getItem().isEmpty() && filter.test(e.getItem()))) {
            picked += pickUp(cat, item);
        }
        return picked;
    }

    public static int pickUp(Catto cat, ItemEntity item) {
        ItemStack stack = item.getItem();
        int before = stack.getCount();
        ItemStack rest = bagInsert(cat, stack.copy());
        int taken = before - rest.getCount();
        if (taken > 0) {
            cat.take(item, taken);
            if (rest.isEmpty()) {
                item.discard();
            } else {
                item.setItem(rest);
            }
        }
        return taken;
    }

    // ------------------------------------------------------------------ storages

    @Nullable
    public static IItemHandler storage(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
    }

    public static int count(IItemHandler handler, Predicate<ItemStack> filter) {
        int n = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && filter.test(stack)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    /** All matching items in all bound storages that are loaded. */
    public static int storageCount(Catto cat, Predicate<ItemStack> filter) {
        int n = 0;
        for (BlockPos pos : cat.storages) {
            IItemHandler handler = storage(cat.level(), pos);
            if (handler != null) {
                n += count(handler, filter);
            }
        }
        return n;
    }

    /** The closest bound storage that has a matching item. */
    @Nullable
    public static BlockPos storageWith(Catto cat, Predicate<ItemStack> filter) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : cat.storages) {
            IItemHandler handler = storage(cat.level(), pos);
            if (handler != null && count(handler, filter) > 0) {
                double d = pos.distSqr(cat.blockPosition());
                if (d < bestDist) {
                    bestDist = d;
                    best = pos;
                }
            }
        }
        return best;
    }

    /** The first bound storage (the main one first) that can take at least one of the given bag items. */
    @Nullable
    public static BlockPos storageFor(Catto cat, Predicate<ItemStack> what) {
        for (BlockPos pos : cat.storages) {
            IItemHandler handler = storage(cat.level(), pos);
            if (handler == null) {
                continue;
            }
            for (int i = 0; i < cat.capacity(); i++) {
                ItemStack stack = cat.bag.getStackInSlot(i);
                if (!stack.isEmpty() && what.test(stack)
                        && ItemHandlerHelper.insertItemStacked(handler, stack.copyWithCount(1), true).isEmpty()) {
                    return pos;
                }
            }
        }
        return null;
    }

    /** Moves matching items from a storage into the bag; returns how many moved. */
    public static int takeFrom(Catto cat, BlockPos pos, Predicate<ItemStack> filter, int amount) {
        IItemHandler handler = storage(cat.level(), pos);
        if (handler == null) {
            return 0;
        }
        int moved = 0;
        for (int i = 0; i < handler.getSlots() && moved < amount; i++) {
            ItemStack inSlot = handler.getStackInSlot(i);
            if (inSlot.isEmpty() || !filter.test(inSlot)) {
                continue;
            }
            ItemStack simulated = handler.extractItem(i, amount - moved, true);
            if (simulated.isEmpty()) {
                continue;
            }
            ItemStack rest = bagInsert(cat, simulated.copy());
            int fits = simulated.getCount() - rest.getCount();
            if (fits <= 0) {
                break;
            }
            handler.extractItem(i, fits, false);
            moved += fits;
        }
        return moved;
    }

    /** Puts matching bag items into a storage; returns how many went in. */
    public static int putInto(Catto cat, BlockPos pos, Predicate<ItemStack> what) {
        IItemHandler handler = storage(cat.level(), pos);
        if (handler == null) {
            return 0;
        }
        int moved = 0;
        for (int i = 0; i < cat.capacity(); i++) {
            ItemStack stack = cat.bag.getStackInSlot(i);
            if (stack.isEmpty() || !what.test(stack)) {
                continue;
            }
            ItemStack rest = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
            moved += stack.getCount() - rest.getCount();
            cat.bag.setStackInSlot(i, rest);
        }
        return moved;
    }

    // ------------------------------------------------------------------ blocks

    public static FakePlayer hands(Catto cat) {
        ServerLevel level = (ServerLevel) cat.level();
        UUID owner = cat.getOwnerUUID();
        String seed = "arcane_artifacts:catto:" + (owner == null ? cat.getUUID() : owner);
        FakePlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)), "[Catto]"));
        player.setPos(cat.getX(), cat.getY(), cat.getZ());
        return player;
    }

    /** Can Catto (at this Miner tier) break the block at all? */
    public static boolean minerAllows(BlockState state, int tier) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return tier >= 3;
        }
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
            return tier >= 2;
        }
        return true;
    }

    public static boolean toolHarvests(Catto cat, BlockState state) {
        return !state.requiresCorrectToolForDrops() || cat.tool().isCorrectToolForDrops(state);
    }

    /** Never next to lava or water (no flooding, no burning), never what Catto or its owner stands on. */
    public static boolean safeToBreak(Catto cat, BlockPos pos) {
        Level level = cat.level();
        for (Direction dir : Direction.values()) {
            if (!level.getFluidState(pos.relative(dir)).isEmpty()) {
                return false;
            }
        }
        if (pos.equals(cat.blockPosition())) {
            return false;
        }
        // Under its own paws only if there is solid ground right below: Catto drops one block, never into a hole.
        if (pos.equals(cat.blockPosition().below()) && !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return false;
        }
        var owner = cat.ownerPlayer();
        return owner == null || !pos.equals(owner.blockPosition().below());
    }

    /** At least one side open: Catto can reach it without digging. */
    public static boolean exposed(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState next = level.getBlockState(pos.relative(dir));
            if (next.isAir() || next.canBeReplaced() || !next.isCollisionShapeFullBlock(level, pos.relative(dir))) {
                return true;
            }
        }
        return false;
    }

    /** Ticks it takes Catto to break the block with its tool (faster at higher Miner tiers). */
    public static int breakTicks(Catto cat, BlockState state, BlockPos pos) {
        float hardness = state.getDestroySpeed(cat.level(), pos);
        float speed = Math.max(1.0F, cat.tool().getDestroySpeed(state));
        float tierFactor = switch (cat.tier(CattoSkill.MINER)) {
            case 3 -> 0.5F;
            case 2 -> 0.75F;
            default -> 1.0F;
        };
        return Math.max(5, Math.min(200, Math.round(hardness * 30.0F / speed * tierFactor)));
    }

    /** Breaks a block as Catto; the drops land in the world (Catto picks them up right after). */
    public static boolean breakBlock(Catto cat, BlockPos pos) {
        ServerLevel level = (ServerLevel) cat.level();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        FakePlayer hands = hands(cat);
        ItemStack tool = cat.tool();
        hands.setItemInHand(InteractionHand.MAIN_HAND, tool);
        boolean broken;
        try {
            broken = hands.gameMode.destroyBlock(pos);
        } finally {
            ItemStack after = hands.getMainHandItem();
            hands.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            cat.equipment.setStackInSlot(Catto.SLOT_TOOL, after);
        }
        level.destroyBlockProgress(cat.getId(), pos, -1);
        return broken;
    }

    /** Places a block state as Catto if the place is free; the item comes out of the bag. */
    public static boolean place(Catto cat, BlockPos pos, BlockState state) {
        ServerLevel level = (ServerLevel) cat.level();
        BlockState current = level.getBlockState(pos);
        if (!current.canBeReplaced() || !state.canSurvive(level, pos) || !level.isUnobstructed(state, pos, CollisionContext.empty())) {
            return false;
        }
        if (!(state.getBlock().asItem() instanceof BlockItem blockItem)) {
            return false;
        }
        if (bagCount(cat, s -> s.is(blockItem)) <= 0) {
            return false;
        }
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, state, 3)) {
            return false;
        }
        if (EventHooks.onBlockPlace(hands(cat), snapshot, Direction.UP)) {
            snapshot.restore();
            return false;
        }
        bagRemove(cat, s -> s.is(blockItem), 1);
        var sound = state.getSoundType(level, pos, cat);
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        return true;
    }

    /** Firefly module: a torch from the work slot where Catto stands, if it is dark there. */
    public static void lightUp(Catto cat) {
        Level level = cat.level();
        BlockPos at = cat.blockPosition();
        ItemStack work = cat.workItem();
        if (!work.is(Items.TORCH) || level.getMaxLocalRawBrightness(at) >= 7 || !level.getBlockState(at).canBeReplaced()
                || !Blocks.TORCH.defaultBlockState().canSurvive(level, at) || !level.getFluidState(at).isEmpty()) {
            return;
        }
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, at);
        if (level.setBlock(at, Blocks.TORCH.defaultBlockState(), 3)) {
            if (EventHooks.onBlockPlace(hands(cat), snapshot, Direction.UP)) {
                snapshot.restore();
            } else {
                work.shrink(1);
                cat.equipment.setStackInSlot(Catto.SLOT_WORK, work);
            }
        }
    }
}
