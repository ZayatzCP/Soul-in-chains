package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.network.chat.Component;

/** The rites a seal can be set to with the Book of Formulas (more come with the ranks). */
public enum RiteScheme {
    ASH_WITNESS("ash_witness");

    public final String id;

    RiteScheme(String id) {
        this.id = id;
    }

    public RiteScheme next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static RiteScheme byName(String name) {
        for (RiteScheme scheme : values()) {
            if (scheme.name().equals(name)) {
                return scheme;
            }
        }
        return ASH_WITNESS;
    }

    public Component displayName() {
        return Component.translatable("arcane_artifacts.cult.scheme." + id);
    }
}
