package com.soulinchains.arcaneartifacts.grove;

import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** What a Light in a Jar keeps: the light itself (its friend, its name), to be let out again. */
public class LightJarBlockEntity extends BlockEntity {
    public static final String LIGHT = "light";
    private CompoundTag light = new CompoundTag();

    public LightJarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.LIGHT_JAR_ENTITY.get(), pos, state);
    }

    public CompoundTag light() {
        return light;
    }

    public void setLight(CompoundTag light) {
        this.light = light;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(LIGHT, light);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        light = tag.getCompound(LIGHT);
    }
}
