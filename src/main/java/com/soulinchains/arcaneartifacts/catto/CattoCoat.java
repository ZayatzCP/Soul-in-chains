package com.soulinchains.arcaneartifacts.catto;

import net.minecraft.util.RandomSource;

import java.util.Locale;

/** Catto's coat. A cat bound by the ritual keeps the closest one to its own. */
public enum CattoCoat {
    SIAMESE, GINGER, BLACK, TUXEDO, WHITE, CALICO, GRAY;

    public static CattoCoat random(RandomSource random) {
        CattoCoat[] all = values();
        return all[random.nextInt(all.length)];
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The coat closest to a vanilla cat variant ("minecraft:tabby", ...). */
    public static CattoCoat fromVanilla(String variant) {
        String name = variant.contains(":") ? variant.substring(variant.indexOf(':') + 1) : variant;
        return switch (name) {
            case "tabby", "red", "persian" -> GINGER;
            case "black", "all_black" -> BLACK;
            case "british_shorthair", "jellie" -> GRAY;
            case "calico" -> CALICO;
            case "white" -> WHITE;
            case "ragdoll", "siamese" -> SIAMESE;
            default -> TUXEDO;
        };
    }
}
