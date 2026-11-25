package com.soulinchains.arcaneartifacts.rite;

import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A ritual cage with a creature in it: the creature's saved data, its kind and its worth. */
public class RiteCageBlockEntity extends BlockEntity {
    private CompoundTag creature = new CompoundTag();
    private String kind = "";
    private int value;
    private boolean otherworldly;

    public RiteCageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RITE_CAGE_ENTITY.get(), pos, state);
    }

    public boolean occupied() {
        return !creature.isEmpty();
    }

    public CompoundTag creature() {
        return creature;
    }

    public String kind() {
        return kind;
    }

    public int value() {
        return value;
    }

    public boolean otherworldly() {
        return otherworldly;
    }

    public void fill(CompoundTag data) {
        apply(data);
        changed();
    }

    private void apply(CompoundTag data) {
        creature = data.getCompound(RiteCageItem.CREATURE).copy();
        kind = data.getString(RiteCageItem.KIND);
        value = data.getInt(RiteCageItem.VALUE);
        otherworldly = data.getBoolean(RiteCageItem.OTHERWORLDLY);
        shown = null;
        shownFailed = false;
    }

    /** Client side: the creature drawn inside (made from the saved data on first render). */
    @org.jetbrains.annotations.Nullable
    public net.minecraft.world.entity.Entity shown;
    public boolean shownFailed;

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    /** The data the cage item carries for this creature. */
    public CompoundTag asItemData() {
        CompoundTag tag = new CompoundTag();
        tag.put(RiteCageItem.CREATURE, creature.copy());
        tag.putString(RiteCageItem.KIND, kind);
        tag.putInt(RiteCageItem.VALUE, value);
        tag.putBoolean(RiteCageItem.OTHERWORLDLY, otherworldly);
        return tag;
    }

    /** The rite takes the creature: the cage is left empty. */
    public void consume() {
        creature = new CompoundTag();
        kind = "";
        value = 0;
        otherworldly = false;
        shown = null;
        changed();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Cage", asItemData());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        apply(tag.getCompound("Cage"));
    }
}
