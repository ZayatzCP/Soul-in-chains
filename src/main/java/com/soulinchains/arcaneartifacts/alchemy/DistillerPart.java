package com.soulinchains.arcaneartifacts.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/** The four blocks of the 2×2 Alchemical Distiller. "Left/right" are as seen by the player facing its front. */
public enum DistillerPart implements StringRepresentable {
    BOTTOM_LEFT("bottom_left", 0, 0),
    BOTTOM_RIGHT("bottom_right", 1, 0),
    TOP_LEFT("top_left", 0, 1),
    TOP_RIGHT("top_right", 1, 1);

    private final String name;
    private final int side;
    private final int up;

    DistillerPart(String name, int side, int up) {
        this.name = name;
        this.side = side;
        this.up = up;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** The direction that goes from the left column to the right one, for a distiller facing {@code facing}. */
    public static Direction rightOf(Direction facing) {
        return facing.getCounterClockWise();
    }

    /** Position of this part when the main (bottom-left) block is at {@code main}. */
    public BlockPos from(BlockPos main, Direction facing) {
        return main.relative(rightOf(facing), side).above(up);
    }

    /** Position of the main block, given the position of this part. */
    public BlockPos main(BlockPos pos, Direction facing) {
        return pos.relative(rightOf(facing), -side).below(up);
    }

    public boolean isMain() {
        return this == BOTTOM_LEFT;
    }
}
