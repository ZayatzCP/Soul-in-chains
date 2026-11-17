package com.soulinchains.arcaneartifacts.catto;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One Catto per player: owner -> where the Catto is (alive in the world, hidden in a bell, or fallen and waiting for the
 * bonding ritual). Kept with the overworld's data so it survives restarts.
 */
public final class CattoSavedData extends SavedData {
    private static final String NAME = "arcane_artifacts_catto";

    public enum Status { ALIVE, IN_BELL, FALLEN }

    public static final class Entry {
        public Status status = Status.ALIVE;
        @Nullable
        public UUID catId;
        public ResourceLocation dimension = ResourceLocation.withDefaultNamespace("overworld");
        public BlockPos lastPos = BlockPos.ZERO;
        /** The fallen Catto, ready to be brought back. */
        @Nullable
        public CompoundTag snapshot;
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    public static CattoSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CattoSavedData::new, CattoSavedData::load, null), NAME);
    }

    @Nullable
    public Entry entry(UUID owner) {
        return entries.get(owner);
    }

    public Entry getOrCreate(UUID owner) {
        return entries.computeIfAbsent(owner, k -> new Entry());
    }

    public void remove(UUID owner) {
        if (entries.remove(owner) != null) {
            setDirty();
        }
    }

    public void alive(UUID owner, Catto cat) {
        Entry entry = getOrCreate(owner);
        entry.status = Status.ALIVE;
        entry.catId = cat.getUUID();
        entry.dimension = cat.level().dimension().location();
        entry.lastPos = cat.blockPosition();
        entry.snapshot = null;
        setDirty();
    }

    /** Called every few seconds by a living Catto: where to look for it. */
    public void moved(UUID owner, Catto cat) {
        Entry entry = entries.get(owner);
        if (entry == null || !cat.getUUID().equals(entry.catId) || entry.status != Status.ALIVE) {
            alive(owner, cat);
            return;
        }
        if (!entry.lastPos.equals(cat.blockPosition()) || !entry.dimension.equals(cat.level().dimension().location())) {
            entry.lastPos = cat.blockPosition();
            entry.dimension = cat.level().dimension().location();
            setDirty();
        }
    }

    public void hidden(UUID owner) {
        Entry entry = getOrCreate(owner);
        entry.status = Status.IN_BELL;
        entry.snapshot = null;
        setDirty();
    }

    public void fallen(UUID owner, CompoundTag snapshot) {
        Entry entry = getOrCreate(owner);
        entry.status = Status.FALLEN;
        entry.snapshot = snapshot;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag all = new CompoundTag();
        entries.forEach((owner, entry) -> {
            CompoundTag e = new CompoundTag();
            e.putString("Status", entry.status.name());
            if (entry.catId != null) {
                e.putUUID("Cat", entry.catId);
            }
            e.putString("Dim", entry.dimension.toString());
            e.putLong("Pos", entry.lastPos.asLong());
            if (entry.snapshot != null) {
                e.put("Snapshot", entry.snapshot);
            }
            all.put(owner.toString(), e);
        });
        tag.put("Owners", all);
        return tag;
    }

    private static CattoSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CattoSavedData data = new CattoSavedData();
        CompoundTag all = tag.getCompound("Owners");
        for (String key : all.getAllKeys()) {
            try {
                UUID owner = UUID.fromString(key);
                CompoundTag e = all.getCompound(key);
                Entry entry = new Entry();
                try {
                    entry.status = Status.valueOf(e.getString("Status"));
                } catch (IllegalArgumentException ignored) {
                    entry.status = Status.ALIVE;
                }
                entry.catId = e.hasUUID("Cat") ? e.getUUID("Cat") : null;
                ResourceLocation dim = ResourceLocation.tryParse(e.getString("Dim"));
                if (dim != null) {
                    entry.dimension = dim;
                }
                entry.lastPos = BlockPos.of(e.getLong("Pos"));
                entry.snapshot = e.contains("Snapshot") ? e.getCompound("Snapshot") : null;
                data.entries.put(owner, entry);
            } catch (IllegalArgumentException ignored) {
                // a broken key: skip it
            }
        }
        return data;
    }
}
