package com.soulinchains.arcaneartifacts.grove;

import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import org.joml.Vector3f;

/** The colours of the Thicket Lights, how often each is met in the Dark Forest, and its glow. */
public enum LightColor implements StringRepresentable {
    AMBER(0xFFC45C, 35),
    MOSS(0xA6E36B, 35),
    DUSK(0xC9A2FF, 15),
    FROST(0xA2E6FF, 10),
    ROSE(0xFF8FB4, 5);

    public final int rgb;
    public final int weight;

    LightColor(int rgb, int weight) {
        this.rgb = rgb;
        this.weight = weight;
    }

    public Vector3f vector() {
        return new Vector3f((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static LightColor byId(int id) {
        LightColor[] all = values();
        return all[Math.floorMod(id, all.length)];
    }

    public static LightColor random(RandomSource random) {
        int total = 0;
        for (LightColor c : values()) {
            total += c.weight;
        }
        int roll = random.nextInt(total);
        for (LightColor c : values()) {
            roll -= c.weight;
            if (roll < 0) {
                return c;
            }
        }
        return AMBER;
    }
}
