package com.soulinchains.arcaneartifacts.rite;

import net.minecraft.network.chat.Component;

/** How well a rite went, by its quality: how many it brings, how strong, how much loot. Below 20% it is broken. */
public enum RiteTier {
    BROKEN("broken", 0, 0F, 1.0F),
    WEAK("weak", 20, 0.5F, 1.0F),
    STEADY("steady", 45, 1.0F, 1.5F),
    FLAWLESS("flawless", 70, 1.5F, 2.0F),
    PERFECT("perfect", 90, 2.0F, 3.0F);

    public final String id;
    public final int from;
    /** Multiplier on how many creatures or items the rite brings. */
    public final float count;
    public final float loot;

    RiteTier(String id, int from, float count, float loot) {
        this.id = id;
        this.from = from;
        this.count = count;
        this.loot = loot;
    }

    public static RiteTier of(float quality) {
        RiteTier tier = BROKEN;
        for (RiteTier t : values()) {
            if (quality >= t.from) {
                tier = t;
            }
        }
        return tier;
    }

    public Component displayName() {
        return Component.translatable("arcane_artifacts.rite.tier." + id);
    }
}
