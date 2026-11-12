package com.soulinchains.arcaneartifacts.pseudo;

import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** One block of a Mana Tank: its share of the Source (filled from the bottom of the tank up). */
public class ManaTankBlockEntity extends BlockEntity {
    private int source;

    public ManaTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MANA_TANK_ENTITY.get(), pos, state);
    }

    public static int capacityPerBlock() {
        return Config.MANA_TANK_CAPACITY.get();
    }

    public int getSource() {
        return source;
    }

    void setSource(int value) {
        int clamped = Math.max(0, Math.min(capacityPerBlock(), value));
        if (clamped != source) {
            source = clamped;
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }
    }

    /** Fill level of this block, 0..1, for the renderer. */
    public float fill() {
        return capacityPerBlock() <= 0 ? 0 : (float) source / capacityPerBlock();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Source", source);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        source = tag.getInt("Source");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Source", source);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
