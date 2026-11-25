package com.soulinchains.arcaneartifacts.rite;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A ritual site around a Heart of the Circle. The Heart sits in the floor with the offering pedestal on it; a closed
 * square of amethyst chalk marks the field around it. The field is 9x9, 15x15 or 21x21 blocks of deepslate floor under a
 * roof, cut into 3x3 places: the middle one is the altar, the four around it hold the rite's sigils (north - the main one,
 * south, west, east), every other place may hold a modifier.
 */
public final class RiteSite {
    public static final TagKey<Block> FLOOR = TagKey.create(Registries.BLOCK, ArcaneArtifacts.id("rite_floor"));
    public static final int[] SIZES = {9, 15, 21};

    /** A place on the field, in places from the altar (x east, z south). */
    public record Place(int x, int z) {
        public static final Place MAIN = new Place(0, -1);
        public static final Place SOUTH = new Place(0, 1);
        public static final Place WEST = new Place(-1, 0);
        public static final Place EAST = new Place(1, 0);

        public boolean altar() {
            return x == 0 && z == 0;
        }

        public boolean rite() {
            return equals(MAIN) || equals(SOUTH) || equals(WEST) || equals(EAST);
        }
    }

    public final BlockPos heart;
    /** Field size in blocks (9, 15 or 21), 0 when no closed boundary was found. */
    public final int size;
    public final List<String> problems = new ArrayList<>();
    public final Map<Place, SigilReading> places = new LinkedHashMap<>();
    public int candlesLit;
    public int candlesTotal;
    public int lanterns;
    public boolean censer;

    private RiteSite(BlockPos heart, int size) {
        this.heart = heart.immutable();
        this.size = size;
    }

    /** Half the field, from the altar to its last row (4, 7 or 10). */
    public int half() {
        return (size - 1) / 2;
    }

    /** Players the field needs: 2, 3 or 4. */
    public int participants() {
        return size <= 9 ? 2 : size <= 15 ? 3 : 4;
    }

    public boolean ok() {
        return size > 0 && problems.isEmpty();
    }

    public SigilReading at(Place place) {
        return places.getOrDefault(place, SigilReading.EMPTY);
    }

    /** Whether a position (any height) lies over the field, boundary excluded. */
    public boolean inside(BlockPos pos) {
        return size > 0 && Math.abs(pos.getX() - heart.getX()) <= half() && Math.abs(pos.getZ() - heart.getZ()) <= half();
    }

    /** The place a chalk position belongs to, or null outside the field. */
    public Place placeOf(BlockPos pos) {
        if (!inside(pos)) {
            return null;
        }
        return new Place(Math.floorDiv(pos.getX() - heart.getX() + 1, 3), Math.floorDiv(pos.getZ() - heart.getZ() + 1, 3));
    }

    /** The north-west chalk cell of a place. */
    public BlockPos corner(Place place) {
        return heart.offset(place.x * 3 - 1, 1, place.z * 3 - 1);
    }

    /** Scans the site around a Heart: boundary, floor, roof, sigils, candles, lanterns and the censer. */
    public static RiteSite scan(Level level, BlockPos heart) {
        int size = 0;
        for (int s : SIZES) {
            if (boundary(level, heart, (s + 1) / 2)) {
                size = s;
                break;
            }
        }
        RiteSite site = new RiteSite(heart, size);
        if (size == 0) {
            site.problems.add("no_boundary");
            return site;
        }
        int h = site.half();
        boolean floorOk = true;
        boolean roofOk = true;
        for (int dx = -h; dx <= h; dx++) {
            for (int dz = -h; dz <= h; dz++) {
                BlockPos floor = heart.offset(dx, 0, dz);
                if ((dx != 0 || dz != 0) && !level.getBlockState(floor).is(FLOOR)) {
                    floorOk = false;
                }
                if (level.canSeeSky(floor.above())) {
                    roofOk = false;
                }
            }
        }
        if (!floorOk) {
            site.problems.add("floor");
        }
        if (!roofOk) {
            site.problems.add("roof");
        }
        if (!level.getBlockState(heart.above()).is(ModBlocks.OFFERING_PEDESTAL.get())) {
            site.problems.add("pedestal");
        }
        int k = size / 3;
        int r = (k - 1) / 2;
        for (int px = -r; px <= r; px++) {
            for (int pz = -r; pz <= r; pz++) {
                Place place = new Place(px, pz);
                if (!place.altar()) {
                    site.places.put(place, SigilReading.read(level, site.corner(place)));
                }
            }
        }
        // candles on the field and on the boundary ring, lanterns on the walls around it, the censer over the pedestal
        for (BlockPos pos : BlockPos.betweenClosed(heart.offset(-h - 1, 1, -h - 1), heart.offset(h + 1, 4, h + 1))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CandleBlock) {
                int n = state.getValue(CandleBlock.CANDLES);
                site.candlesTotal += n;
                if (state.getValue(CandleBlock.LIT)) {
                    site.candlesLit += n;
                }
            }
        }
        for (BlockPos pos : BlockPos.betweenClosed(heart.offset(-h - 3, 0, -h - 3), heart.offset(h + 3, 6, h + 3))) {
            if (level.getBlockState(pos).is(Blocks.SOUL_LANTERN)) {
                site.lanterns++;
            }
        }
        for (BlockPos pos : BlockPos.betweenClosed(heart.offset(-1, 2, -1), heart.offset(1, 5, 1))) {
            if (level.getBlockState(pos).is(ModBlocks.RITUAL_CENSER.get())) {
                site.censer = true;
            }
        }
        return site;
    }

    /** A closed square of amethyst chalk {@code d} blocks from the Heart, on the chalk layer. */
    static boolean boundary(Level level, BlockPos heart, int d) {
        for (int i = -d; i <= d; i++) {
            for (BlockPos p : new BlockPos[]{heart.offset(i, 1, -d), heart.offset(i, 1, d), heart.offset(-d, 1, i), heart.offset(d, 1, i)}) {
                if (ChalkMarkBlock.color(level, p) != ChalkColor.AMETHYST) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Places that hold something which is not a valid sigil (counted against the quality). */
    public int wrongPlaces() {
        int wrong = 0;
        for (SigilReading reading : places.values()) {
            if (!reading.empty() && !reading.valid()) {
                wrong++;
            }
        }
        return wrong;
    }
}
