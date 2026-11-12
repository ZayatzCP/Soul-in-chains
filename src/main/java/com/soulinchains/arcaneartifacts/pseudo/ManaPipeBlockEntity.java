package com.soulinchains.arcaneartifacts.pseudo;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Registers its pipe with Ars Nouveau as a Source provider while it is loaded: anything of Ars that looks
 * for Source Jars near it finds the tanks of the pipe network instead (no jars needed).
 */
public class ManaPipeBlockEntity extends BlockEntity implements ISpecialSourceProvider {
    public ManaPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MANA_PIPE_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            SourceManager.INSTANCE.addInterface(level, this);
        }
    }

    @Override
    public ISourceTile getSource() {
        return new ManaNetwork(level, worldPosition);
    }

    @Override
    public boolean isValid() {
        return !isRemoved() && level != null && level.isLoaded(worldPosition) && level.getBlockEntity(worldPosition) == this;
    }

    @Override
    public BlockPos getCurrentPos() {
        return worldPosition;
    }
}
