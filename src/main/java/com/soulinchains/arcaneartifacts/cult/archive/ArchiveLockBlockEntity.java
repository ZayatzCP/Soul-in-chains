package com.soulinchains.arcaneartifacts.cult.archive;

import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.cult.RenegadeSealer;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The hidden lock of an Archive door (iron bars). Two kinds:
 * <ul>
 *     <li>{@link Mode#PLATES}: two Vow Plates far apart, pressed within 1 s of each other;</li>
 *     <li>{@link Mode#SEAL}: one player holds the Holding Seal (stays within 3 blocks of it for 10 s) while another lights the
 *     distant Vow Candle. A Renegade Sealer by the seal breaks the hold.</li>
 * </ul>
 * Opened, the bars vanish for good.
 */
public class ArchiveLockBlockEntity extends BlockEntity {
    public enum Mode { PLATES, SEAL }

    public static final int PLATE_WINDOW = 20;
    public static final int HOLD_TICKS = 200;
    public static final double HOLD_RANGE = 3;
    public static final int SEARCH = 24;
    private static final Map<BlockPos, BlockPos> CACHE = new HashMap<>();

    private Mode mode = Mode.PLATES;
    private BlockPos a = BlockPos.ZERO;
    private BlockPos b = BlockPos.ZERO;
    private BlockPos doorMin = BlockPos.ZERO;
    private BlockPos doorMax = BlockPos.ZERO;
    private boolean opened;
    private long pressA = -1000;
    private long pressB = -1000;
    @Nullable
    private Player holder;
    private long holdUntil = -1;

    public ArchiveLockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ARCHIVE_LOCK_ENTITY.get(), pos, state);
    }

    /** Set by the Archive when it is built (positions in the world). */
    public void configure(Mode newMode, BlockPos first, BlockPos second, BlockPos min, BlockPos max) {
        mode = newMode;
        a = first.immutable();
        b = second.immutable();
        doorMin = min.immutable();
        doorMax = max.immutable();
        opened = false;
        setChanged();
    }

    public boolean opened() {
        return opened;
    }

    /** The lock that a plate, seal or candle at {@code pos} belongs to. */
    @Nullable
    public static ArchiveLockBlockEntity find(Level level, BlockPos pos) {
        BlockPos known = CACHE.get(pos);
        if (known != null && level.getBlockEntity(known) instanceof ArchiveLockBlockEntity lock && lock.owns(pos)) {
            return lock;
        }
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-SEARCH, -6, -SEARCH), pos.offset(SEARCH, 6, SEARCH))) {
            if (level.getBlockEntity(p) instanceof ArchiveLockBlockEntity lock && lock.owns(pos)) {
                CACHE.put(pos.immutable(), p.immutable());
                return lock;
            }
        }
        return null;
    }

    public boolean owns(BlockPos pos) {
        return pos.equals(a) || pos.equals(b);
    }

    // ------------------------------------------------------------------ plates

    /** A player stands on one of the plates at {@code time}. Returns true when this opened the door. */
    public boolean press(BlockPos plate, long time) {
        if (opened || mode != Mode.PLATES) {
            return false;
        }
        if (plate.equals(a)) {
            pressA = time;
        } else if (plate.equals(b)) {
            pressB = time;
        } else {
            return false;
        }
        if (Math.abs(pressA - pressB) <= PLATE_WINDOW) {
            open();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ holding seal

    public boolean holding() {
        return holder != null && level != null && level.getGameTime() <= holdUntil && holder.isAlive()
                && holder.position().distanceTo(Vec3.atCenterOf(a)) <= HOLD_RANGE + 0.5 && !renegadeAtSeal();
    }

    private boolean renegadeAtSeal() {
        return level != null && !level.getEntitiesOfClass(RenegadeSealer.class, new AABB(a).inflate(HOLD_RANGE), RenegadeSealer::isAlive).isEmpty();
    }

    /** A player takes hold of the seal for 10 s. */
    public boolean beginHold(Player player) {
        if (opened || mode != Mode.SEAL || level == null) {
            return false;
        }
        holder = player;
        holdUntil = level.getGameTime() + HOLD_TICKS;
        player.displayClientMessage(Component.translatable("arcane_artifacts.cult.archive_holding"), true);
        level.playSound(null, a, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 0.8F);
        return true;
    }

    /** Someone lights the candle; it opens the door while another holds the seal. */
    public boolean lightCandle(Player player) {
        if (opened || mode != Mode.SEAL) {
            return false;
        }
        boolean alone = holder != null && holder.getUUID().equals(player.getUUID());
        if (!holding() || alone && !(player.isCreative() && Config.CULT_CREATIVE_SOLO.get())) {
            player.displayClientMessage(Component.translatable(renegadeAtSeal() ? "arcane_artifacts.cult.archive_renegade"
                    : "arcane_artifacts.cult.archive_need_holder"), true);
            return false;
        }
        open();
        return true;
    }

    // ------------------------------------------------------------------ the door

    private void open() {
        opened = true;
        holder = null;
        if (level != null) {
            for (BlockPos p : BlockPos.betweenClosed(doorMin, doorMax)) {
                if (level.getBlockState(p).is(Blocks.IRON_BARS)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    if (level instanceof ServerLevel server) {
                        server.sendParticles(ParticleTypes.ASH, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.01);
                    }
                }
            }
            level.playSound(null, doorMin, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.2F, 0.6F);
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Mode", mode.name());
        tag.put("A", NbtUtils.writeBlockPos(a));
        tag.put("B", NbtUtils.writeBlockPos(b));
        tag.put("DoorMin", NbtUtils.writeBlockPos(doorMin));
        tag.put("DoorMax", NbtUtils.writeBlockPos(doorMax));
        tag.putBoolean("Opened", opened);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try {
            mode = Mode.valueOf(tag.getString("Mode"));
        } catch (IllegalArgumentException e) {
            mode = Mode.PLATES;
        }
        a = NbtUtils.readBlockPos(tag, "A").orElse(BlockPos.ZERO);
        b = NbtUtils.readBlockPos(tag, "B").orElse(BlockPos.ZERO);
        doorMin = NbtUtils.readBlockPos(tag, "DoorMin").orElse(BlockPos.ZERO);
        doorMax = NbtUtils.readBlockPos(tag, "DoorMax").orElse(BlockPos.ZERO);
        opened = tag.getBoolean("Opened");
    }

    @Nullable
    public UUID holderId() {
        return holder == null ? null : holder.getUUID();
    }
}
