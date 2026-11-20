package com.soulinchains.arcaneartifacts.necro.throne;

import com.soulinchains.arcaneartifacts.necro.SoulType;
import com.soulinchains.arcaneartifacts.necro.Souls;
import com.soulinchains.arcaneartifacts.necro.mob.ImprisonedKing;
import com.soulinchains.arcaneartifacts.necro.mob.Restless;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Throne of Chains, the Imprisoned King's summoning build (checked block by block, like the Wither's T):
 * <pre>
 *   y+3   L . . . L        L  Great Soul Lantern, hung one by one: north-west, north-east, south-east, south-west
 *   y+0..2 P . . . P       P  Chain Pillar, three high, on each corner
 *   y+0   . . T . .        T  Empty Throne in the middle
 *   y-1   S S S S S        S  Shackle Stone, a full 5x5 floor
 * </pre>
 * Hanging a lantern costs a great soul. A lantern hung out of order puts all of them out (their souls come back as
 * wisps) and the throne spits out Restless. With all four lit, a Shackle Key used on the throne at night (any time
 * outside the Overworld) wakes the King, chained to the pillars: their tops become locks.
 */
public final class ThroneOfChains {
    /** Corner offsets from the throne, in the order the lanterns must be hung: NW, NE, SE, SW. */
    public static final int[][] CORNERS = {{-2, -2}, {2, -2}, {2, 2}, {-2, 2}};
    public static final int PILLAR = 3;
    /** Set while the build itself removes lanterns, so they do not drop their souls. */
    static boolean consuming;

    private ThroneOfChains() {
    }

    public static BlockPos pillarTop(BlockPos throne, int corner) {
        return throne.offset(CORNERS[corner][0], PILLAR - 1, CORNERS[corner][1]);
    }

    public static BlockPos lantern(BlockPos throne, int corner) {
        return pillarTop(throne, corner).above();
    }

    /** Is the floor, the pillars and the throne all there? */
    public static boolean complete(Level level, BlockPos throne) {
        if (!level.getBlockState(throne).is(ModBlocks.EMPTY_THRONE.get())) {
            return false;
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (!level.getBlockState(throne.offset(x, -1, z)).is(ModBlocks.SHACKLE_STONE.get())) {
                    return false;
                }
            }
        }
        for (int corner = 0; corner < 4; corner++) {
            for (int y = 0; y < PILLAR; y++) {
                if (!level.getBlockState(throne.offset(CORNERS[corner][0], y, CORNERS[corner][1])).is(ModBlocks.CHAIN_PILLAR.get())) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The throne whose pillar this is the top of, and which corner; null when the pillar belongs to none. */
    @Nullable
    public static int[] find(Level level, BlockPos pillarTop) {
        for (int corner = 0; corner < 4; corner++) {
            BlockPos throne = pillarTop.offset(-CORNERS[corner][0], -(PILLAR - 1), -CORNERS[corner][1]);
            if (complete(level, throne)) {
                return new int[]{throne.getX(), throne.getY(), throne.getZ(), corner};
            }
        }
        return null;
    }

    public static int lit(Level level, BlockPos throne) {
        int count = 0;
        for (int corner = 0; corner < 4; corner++) {
            if (level.getBlockState(lantern(throne, corner)).is(ModBlocks.GREAT_SOUL_LANTERN.get())) {
                count++;
            }
        }
        return count;
    }

    /** True when the lanterns that are lit are exactly the first {@code n} of the order. */
    private static boolean inOrder(Level level, BlockPos throne, int n) {
        for (int corner = 0; corner < 4; corner++) {
            boolean litHere = level.getBlockState(lantern(throne, corner)).is(ModBlocks.GREAT_SOUL_LANTERN.get());
            if (litHere != corner < n) {
                return false;
            }
        }
        return true;
    }

    public enum HangResult { NOT_A_THRONE, NO_SOUL, OCCUPIED, START_NORTH_WEST, WRONG_ORDER, HUNG }

    /** A player hangs a lantern on the top of a pillar. Costs a great soul from the Soul Lantern. */
    public static HangResult hang(ServerLevel level, BlockPos pillarTop, Player player) {
        int[] found = find(level, pillarTop);
        if (found == null) {
            return HangResult.NOT_A_THRONE;
        }
        BlockPos throne = new BlockPos(found[0], found[1], found[2]);
        int corner = found[3];
        if (!level.getBlockState(pillarTop.above()).isAir()) {
            return HangResult.OCCUPIED;
        }
        int lit = lit(level, throne);
        if (lit == 0 && corner != 0) {
            // Nothing lit yet: no punishment, just the first rule.
            return HangResult.START_NORTH_WEST;
        }
        if (corner != lit || !inOrder(level, throne, lit)) {
            putOut(level, throne);
            for (int i = 0; i < 3; i++) {
                Restless ghost = ModEntities.RESTLESS.get().create(level);
                if (ghost != null) {
                    ghost.moveTo(throne.getX() + 0.5, throne.getY() + 1, throne.getZ() + 0.5, level.random.nextFloat() * 360, 0);
                    ghost.shortLived();
                    level.addFreshEntity(ghost);
                }
            }
            level.playSound(null, throne, SoundEvents.WITHER_HURT, SoundSource.BLOCKS, 1.0F, 0.5F);
            return HangResult.WRONG_ORDER;
        }
        if (Souls.count(player, SoulType.GREAT) <= 0 || !Souls.spend(player, SoulType.GREAT)) {
            return HangResult.NO_SOUL;
        }
        level.setBlock(pillarTop.above(), ModBlocks.GREAT_SOUL_LANTERN.get().defaultBlockState(), 3);
        level.sendParticles(ParticleTypes.SCULK_SOUL, pillarTop.getX() + 0.5, pillarTop.getY() + 1.5, pillarTop.getZ() + 0.5, 20, 0.2, 0.3, 0.2, 0.03);
        level.playSound(null, pillarTop, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 0.5F + 0.2F * lit);
        return HangResult.HUNG;
    }

    /** Every lantern goes out and falls; the great souls come back as wisps. */
    public static void putOut(ServerLevel level, BlockPos throne) {
        for (int corner = 0; corner < 4; corner++) {
            BlockPos pos = lantern(throne, corner);
            if (level.getBlockState(pos).is(ModBlocks.GREAT_SOUL_LANTERN.get())) {
                level.removeBlock(pos, false);
            }
        }
    }

    /** Ready to summon: built, all four lit, and night (any time outside the Overworld). */
    public static boolean ready(Level level, BlockPos throne) {
        return complete(level, throne) && lit(level, throne) == 4 && inOrder(level, throne, 4);
    }

    public static boolean rightTime(Level level) {
        return !level.dimensionType().hasSkyLight() || level.isNight();
    }

    /** Consumes the lanterns, turns the pillar tops into locks and raises the King. */
    @Nullable
    public static ImprisonedKing summon(ServerLevel level, BlockPos throne) {
        if (!ready(level, throne)) {
            return null;
        }
        List<BlockPos> locks = new ArrayList<>();
        consuming = true;
        try {
            for (int corner = 0; corner < 4; corner++) {
                level.removeBlock(lantern(throne, corner), false);
                BlockPos top = pillarTop(throne, corner);
                level.setBlock(top, ModBlocks.CHAIN_LOCK.get().defaultBlockState(), 3);
                locks.add(top);
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(top.getX() + 0.5, top.getY() + 1, top.getZ() + 0.5);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
            }
        } finally {
            consuming = false;
        }
        ImprisonedKing king = ModEntities.IMPRISONED_KING.get().create(level);
        if (king == null) {
            return null;
        }
        king.moveTo(throne.getX() + 0.5, throne.getY() + 1, throne.getZ() + 0.5, 180F, 0F);
        king.chainTo(locks);
        king.setPersistenceRequired();
        level.addFreshEntity(king);
        king.scaleForPlayers();
        level.playSound(null, throne, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0F, 0.6F);
        return king;
    }

    static void tell(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("arcane_artifacts.throne." + key, args), true);
    }

    static boolean isAir(Level level, BlockPos pos) {
        return level.getBlockState(pos).isAir() || level.getBlockState(pos).is(Blocks.CAVE_AIR);
    }
}
