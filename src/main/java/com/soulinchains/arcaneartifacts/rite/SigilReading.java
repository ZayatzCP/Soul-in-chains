package com.soulinchains.arcaneartifacts.rite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What is drawn on one 3x3 place of the field: the sigil it matches (or null), the colour it is drawn in, and whether the
 * colour is right for that sigil. An empty place reads as {@link #EMPTY}.
 */
public record SigilReading(@Nullable Sigil sigil, @Nullable ChalkColor color, int marks, boolean colorOk) {
    public static final SigilReading EMPTY = new SigilReading(null, null, 0, false);

    public boolean empty() {
        return marks == 0;
    }

    /** A sigil drawn right: a known figure in a colour that serves it. */
    public boolean valid() {
        return sigil != null && colorOk;
    }

    public boolean is(Sigil wanted) {
        return valid() && sigil == wanted;
    }

    /** Reads the place whose north-west cell is {@code corner} (y is the chalk layer). */
    public static SigilReading read(BlockGetter level, BlockPos corner) {
        List<int[]> lines = new ArrayList<>();
        ChalkColor color = null;
        boolean mixed = false;
        int marks = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                BlockPos pos = corner.offset(c, 0, r);
                ChalkColor here = ChalkMarkBlock.color(level, pos);
                if (here == null || here == ChalkColor.AMETHYST) {
                    continue;
                }
                marks++;
                if (here != ChalkColor.BONE) {
                    if (color != null && color != here) {
                        mixed = true;
                    }
                    color = here;
                }
                if (c < 2 && ChalkMarkBlock.linked(level, pos, Direction.EAST)) {
                    lines.add(new int[]{r * 3 + c, r * 3 + c + 1});
                }
                if (r < 2 && ChalkMarkBlock.linked(level, pos, Direction.SOUTH)) {
                    lines.add(new int[]{r * 3 + c, (r + 1) * 3 + c});
                }
            }
        }
        if (marks == 0) {
            return EMPTY;
        }
        Sigil sigil = Sigil.match(lines.toArray(new int[0][]));
        ChalkColor drawn = color == null ? ChalkColor.BONE : color;
        boolean ok = sigil != null && !mixed && drawn.serves(sigil.color);
        return new SigilReading(sigil, drawn, marks, ok);
    }
}
