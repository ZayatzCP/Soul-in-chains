package com.soulinchains.arcaneartifacts.cult;

import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A ritual anchor: one quarter of the circle's stability. The Warden lights it with the Oath Shield (stability 100) and
 * mends it during the rite; the Torn Acolytes strike at it. At 0 it breaks and goes dark until lit again.
 */
public class RitualAnchorBlockEntity extends BlockEntity {
    public static final int MAX = 100;
    private boolean active;
    private int stability;
    @Nullable
    private UUID activator;
    /** Who lit it, while they are around (not saved). */
    @Nullable
    private Player activatorRef;
    private boolean broken;

    public RitualAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RITUAL_ANCHOR_ENTITY.get(), pos, state);
    }

    public boolean active() {
        return active && !broken;
    }

    public int stability() {
        return active ? stability : 0;
    }

    public boolean broken() {
        return broken;
    }

    @Nullable
    public UUID activator() {
        return activator;
    }

    @Nullable
    public Player activatorRef() {
        return activatorRef;
    }

    /** Lit by a player (the Warden, if the pair works as it should). */
    public void activate(Player player, int startStability) {
        active = true;
        broken = false;
        stability = Math.max(stability, Math.min(MAX, startStability));
        activator = player.getUUID();
        activatorRef = player;
        changed(true);
    }

    /** Mended: returns how much stability it gained. */
    public int repair(int amount) {
        if (!active || broken) {
            return 0;
        }
        int before = stability;
        stability = Math.min(MAX, stability + amount);
        changed(false);
        return stability - before;
    }

    /** Struck: returns true when this blow broke it. */
    public boolean damage(int amount) {
        if (!active || broken) {
            return false;
        }
        stability = Math.max(0, stability - amount);
        if (stability == 0) {
            broken = true;
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, 20, 0.2, 0.3, 0.2, 0.02);
                server.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0F, 0.8F);
            }
        }
        changed(true);
        return broken;
    }

    /** The rite is over: the anchor goes dark. */
    public void reset() {
        active = false;
        broken = false;
        stability = 0;
        activatorRef = null;
        changed(true);
    }

    private void changed(boolean visual) {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            boolean lit = active();
            if (visual && state.hasProperty(RitualAnchorBlock.LIT) && state.getValue(RitualAnchorBlock.LIT) != lit) {
                level.setBlock(worldPosition, state.setValue(RitualAnchorBlock.LIT, lit), 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Active", active);
        tag.putBoolean("Broken", broken);
        tag.putInt("Stability", stability);
        if (activator != null) {
            tag.putUUID("Activator", activator);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        active = tag.getBoolean("Active");
        broken = tag.getBoolean("Broken");
        stability = tag.getInt("Stability");
        activator = tag.hasUUID("Activator") ? tag.getUUID("Activator") : null;
    }
}
