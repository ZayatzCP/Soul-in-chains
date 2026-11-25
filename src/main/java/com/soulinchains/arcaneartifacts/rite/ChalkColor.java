package com.soulinchains.arcaneartifacts.rite;

import net.minecraft.util.StringRepresentable;

/** The colours of ritual chalk. Bone chalk stands in for any colour but amethyst; amethyst chalk only marks the boundary. */
public enum ChalkColor implements StringRepresentable {
    WHITE("white", 0xF1EEE6),
    CINNABAR("cinnabar", 0xD0473B),
    COAL("coal", 0x4A4650),
    AMETHYST("amethyst", 0xB48CF0),
    BONE("bone", 0xE6D9C3);

    public final String id;
    public final int rgb;

    ChalkColor(String id, int rgb) {
        this.id = id;
        this.rgb = rgb;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** Whether a line of this colour satisfies a sigil that asks for {@code wanted}. */
    public boolean serves(ChalkColor wanted) {
        return this == wanted || this == BONE && wanted != AMETHYST;
    }
}
