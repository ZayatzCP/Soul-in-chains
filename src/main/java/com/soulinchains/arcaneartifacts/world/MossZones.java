package com.soulinchains.arcaneartifacts.world;

import com.soulinchains.arcaneartifacts.mushroom.ArcaneMushroom;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/**
 * The colours of the mushroom moss drift through the caves in broad zones, one per kind of cave mushroom:
 * where the moss is green the green Toxcaps grow, where it is pink the Mendcaps, and so on. Used on both
 * sides: the client tints the moss, world generation picks the mushrooms.
 */
public final class MossZones {
    /** The zones in the order they follow each other, with the moss colour of each. */
    private static final ArcaneMushroom[] ZONES = {ArcaneMushroom.BLUE, ArcaneMushroom.TIDECAP, ArcaneMushroom.TOXCAP, ArcaneMushroom.LEAPCAP,
            ArcaneMushroom.SWIFTCAP, ArcaneMushroom.EMBERCAP, ArcaneMushroom.BRAWNCAP, ArcaneMushroom.MENDCAP, ArcaneMushroom.SOURCECAP,
            ArcaneMushroom.GHOSTCAP, ArcaneMushroom.STONECAP};
    private static final int[] COLORS = {0x8FD3F0, 0x7AE8E0, 0x7AD07A, 0xA8F08A, 0xF0E08A, 0xF0B07A, 0xF08A8A, 0xF0A0D0, 0xB58AF0,
            0xE8E8F2, 0xB4B4BE};

    private MossZones() {
    }

    /** Where in the cycle of zones {@code pos} is: an integer part (the zone) and a fraction (towards the next). */
    private static double position(BlockPos pos) {
        double wave = Math.sin(pos.getX() * 0.043) + Math.sin(pos.getZ() * 0.051 + 1.3) + 0.6 * Math.sin((pos.getX() + pos.getZ()) * 0.027 + 0.4)
                + 0.25 * Math.sin(pos.getY() * 0.09 + 0.7);
        double unit = (wave + 2.85) / 5.7;                 // about 0..1
        double cycles = unit * ZONES.length * 2;
        return ((cycles % ZONES.length) + ZONES.length) % ZONES.length;
    }

    /**
     * Whether the mushroom moss grows at {@code pos}: it covers the caves in broad patches, not all of them;
     * between the patches the stone shows, with dirt, mud and mycelium.
     */
    public static boolean mossy(BlockPos pos) {
        double noise = Math.sin(pos.getX() * 0.13 + 0.4) + Math.sin(pos.getZ() * 0.11 + 2.1) + Math.sin(pos.getY() * 0.19 + 1.1)
                + 0.5 * Math.sin((pos.getX() - pos.getZ()) * 0.07);
        return noise > 0.35;
    }

    /** The mushroom of the zone {@code pos} is in. */
    public static ArcaneMushroom zone(BlockPos pos) {
        return ZONES[(int) Math.floor(position(pos)) % ZONES.length];
    }

    /** The moss colour at {@code pos}: the zone's colour, blending into the next one near the border. */
    public static int color(BlockPos pos) {
        double p = position(pos);
        int i = (int) Math.floor(p) % ZONES.length;
        float t = (float) (p - Math.floor(p));
        float blend = Mth.clamp((t - 0.75F) / 0.25F, 0F, 1F);   // solid colour, then a soft border
        int a = COLORS[i];
        int b = COLORS[(i + 1) % COLORS.length];
        int r = (int) Mth.lerp(blend, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int g = (int) Mth.lerp(blend, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(blend, a & 0xFF, b & 0xFF);
        return (r << 16) | (g << 8) | bl;
    }
}
