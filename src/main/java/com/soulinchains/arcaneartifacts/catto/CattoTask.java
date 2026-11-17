package com.soulinchains.arcaneartifacts.catto;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * One errand in Catto's queue. Plain data: what to do, where and how much, plus the progress made so far.
 * It is saved in the entity, written onto Errand Scrolls and sent to the owner's screen as NBT.
 */
public final class CattoTask {
    public enum Type {
        CRAFT(Items.CRAFTING_TABLE),
        MINE(Items.IRON_PICKAXE),
        COLLECT(Items.WHEAT),
        FETCH(Items.CHEST),
        BUILD(Items.BRICKS),
        EXPLORE(Items.COMPASS);

        public final Item icon;

        Type(Item icon) {
            this.icon = icon;
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component title() {
            return Component.translatable("arcane_artifacts.catto.task." + key());
        }
    }

    /** Count used when the player does not say how much: "until the area is empty" for gathering, 64 otherwise. */
    public static final int DEFAULT_COUNT = 64;

    public final Type type;
    /** Craft/fetch target, build material, gathering filter (empty = anything). */
    public ItemStack item = ItemStack.EMPTY;
    /** Block to mine. */
    @Nullable
    public ResourceLocation block;
    /** Centre, first corner, or the origin of a blueprint. */
    public BlockPos a = BlockPos.ZERO;
    /** Second corner of an area (null: an area around {@link #a}). */
    @Nullable
    public BlockPos b;
    /** How many: blocks, items or crafts. 0 for gathering = until nothing is left. */
    public int count;
    public int done;
    /** 0 = working, 1 = bringing the result. */
    public int phase;
    /** Exploring: index of the next waypoint. */
    public int waypoint;
    /** Added by Catto itself (Crafter III crafting a missing ingredient): never splits again. */
    public boolean sub;
    /** Building from a Blueprint Parchment instead of filling an area. */
    @Nullable
    public CompoundTag blueprint;
    /** Exploring: what was found (block id -> count) and the closest spot of each. */
    public final Map<ResourceLocation, Integer> found = new LinkedHashMap<>();
    public final Map<ResourceLocation, BlockPos> nearest = new LinkedHashMap<>();

    public CattoTask(Type type) {
        this.type = type;
    }

    // ------------------------------------------------------------------ factories

    public static CattoTask craft(ItemStack target, int count) {
        CattoTask task = new CattoTask(Type.CRAFT);
        task.item = target.copyWithCount(1);
        task.count = Math.max(1, count);
        return task;
    }

    public static CattoTask fetch(ItemStack target, int count) {
        CattoTask task = new CattoTask(Type.FETCH);
        task.item = target.copyWithCount(1);
        task.count = Math.max(1, count);
        return task;
    }

    public static CattoTask mine(Block block, BlockPos centre, int count) {
        CattoTask task = new CattoTask(Type.MINE);
        task.block = BuiltInRegistries.BLOCK.getKey(block);
        task.item = new ItemStack(block.asItem());
        task.a = centre.immutable();
        task.count = Math.max(1, count);
        return task;
    }

    public static CattoTask collect(BlockPos a, @Nullable BlockPos b, ItemStack filter) {
        CattoTask task = new CattoTask(Type.COLLECT);
        task.a = a.immutable();
        task.b = b == null ? null : b.immutable();
        task.item = filter.isEmpty() ? ItemStack.EMPTY : filter.copyWithCount(1);
        return task;
    }

    public static CattoTask fill(BlockPos a, BlockPos b, ItemStack material) {
        CattoTask task = new CattoTask(Type.BUILD);
        task.a = a.immutable();
        task.b = b.immutable();
        task.item = material.copyWithCount(1);
        task.count = volume(a, b);
        return task;
    }

    public static CattoTask blueprint(BlockPos origin, CompoundTag blueprint, int blocks) {
        CattoTask task = new CattoTask(Type.BUILD);
        task.a = origin.immutable();
        task.blueprint = blueprint.copy();
        task.item = new ItemStack(com.soulinchains.arcaneartifacts.catto.CattoContent.BLUEPRINT.get());
        task.count = blocks;
        return task;
    }

    public static CattoTask explore(BlockPos centre) {
        CattoTask task = new CattoTask(Type.EXPLORE);
        task.a = centre.immutable();
        return task;
    }

    public static int volume(BlockPos a, BlockPos b) {
        return (Math.abs(a.getX() - b.getX()) + 1) * (Math.abs(a.getY() - b.getY()) + 1) * (Math.abs(a.getZ() - b.getZ()) + 1);
    }

    // ------------------------------------------------------------------ text

    /** The target in words: an item, a block or a place. */
    public Component target() {
        return switch (type) {
            case MINE -> block == null ? Component.literal("?") : BuiltInRegistries.BLOCK.get(block).getName();
            case COLLECT -> item.isEmpty() ? Component.translatable("arcane_artifacts.catto.anything") : item.getHoverName();
            case EXPLORE -> Component.literal(a.getX() + " " + a.getY() + " " + a.getZ());
            case BUILD -> blueprint != null ? Component.translatable("arcane_artifacts.catto.blueprint_target") : item.getHoverName();
            default -> item.getHoverName();
        };
    }

    /** One line for the queue: "Mine: Iron Ore (12/64)". */
    public Component describe() {
        Component progress = switch (type) {
            case COLLECT -> count > 0 ? Component.literal(done + "/" + count) : Component.literal(String.valueOf(done));
            case EXPLORE -> Component.literal(waypoint + "");
            default -> Component.literal(done + "/" + count);
        };
        return Component.translatable("arcane_artifacts.catto.task_line", type.title(), target(), progress);
    }

    public ItemStack icon() {
        if (type == Type.MINE && block != null) {
            Item blockItem = BuiltInRegistries.BLOCK.get(block).asItem();
            if (blockItem != Items.AIR) {
                return new ItemStack(blockItem);
            }
        }
        return item.isEmpty() ? new ItemStack(type.icon) : item.copyWithCount(1);
    }

    public CattoTask copy(HolderLookup.Provider registries) {
        return load(save(registries), registries);
    }

    // ------------------------------------------------------------------ NBT

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.name());
        if (!item.isEmpty()) {
            tag.put("Item", item.save(registries));
        }
        if (block != null) {
            tag.putString("Block", block.toString());
        }
        tag.putLong("A", a.asLong());
        if (b != null) {
            tag.putLong("B", b.asLong());
        }
        tag.putInt("Count", count);
        tag.putInt("Done", done);
        tag.putInt("Phase", phase);
        tag.putInt("Waypoint", waypoint);
        tag.putBoolean("Sub", sub);
        if (blueprint != null) {
            tag.put("Blueprint", blueprint);
        }
        if (!found.isEmpty()) {
            ListTag list = new ListTag();
            found.forEach((id, n) -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("Id", id.toString());
                entry.putInt("N", n);
                BlockPos pos = nearest.get(id);
                if (pos != null) {
                    entry.putLong("Pos", pos.asLong());
                }
                list.add(entry);
            });
            tag.put("Found", list);
        }
        return tag;
    }

    @Nullable
    public static CattoTask load(CompoundTag tag, HolderLookup.Provider registries) {
        Type type;
        try {
            type = Type.valueOf(tag.getString("Type"));
        } catch (IllegalArgumentException e) {
            return null;
        }
        CattoTask task = new CattoTask(type);
        if (tag.contains("Item")) {
            task.item = ItemStack.parseOptional(registries, tag.getCompound("Item"));
        }
        if (tag.contains("Block")) {
            task.block = ResourceLocation.tryParse(tag.getString("Block"));
        }
        task.a = BlockPos.of(tag.getLong("A"));
        task.b = tag.contains("B") ? BlockPos.of(tag.getLong("B")) : null;
        task.count = tag.getInt("Count");
        task.done = tag.getInt("Done");
        task.phase = tag.getInt("Phase");
        task.waypoint = tag.getInt("Waypoint");
        task.sub = tag.getBoolean("Sub");
        if (tag.contains("Blueprint")) {
            task.blueprint = tag.getCompound("Blueprint");
        }
        for (Tag t : tag.getList("Found", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Id"));
            if (id != null) {
                task.found.put(id, entry.getInt("N"));
                if (entry.contains("Pos")) {
                    task.nearest.put(id, BlockPos.of(entry.getLong("Pos")));
                }
            }
        }
        return task;
    }

    /** A fresh copy for a scroll program: no progress. */
    public CattoTask restart(HolderLookup.Provider registries) {
        CattoTask copy = copy(registries);
        copy.done = 0;
        copy.phase = 0;
        copy.waypoint = 0;
        copy.found.clear();
        copy.nearest.clear();
        return copy;
    }
}
