package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.network.chat.Component;

/** The five grades of a finished rite and the loot multiplier of what it summons. */
public enum RitualQuality {
    BROKEN("broken", 0, 1.00F),
    WEAK("weak", 40, 1.00F),
    STEADY("steady", 60, 1.15F),
    FLAWLESS("flawless", 80, 1.30F),
    PERFECT("perfect", 95, 1.50F);

    public final String id;
    public final int from;
    public final float lootMultiplier;

    RitualQuality(String id, int from, float lootMultiplier) {
        this.id = id;
        this.from = from;
        this.lootMultiplier = lootMultiplier;
    }

    public static RitualQuality of(float quality) {
        RitualQuality best = BROKEN;
        for (RitualQuality q : values()) {
            if (quality >= q.from) {
                best = q;
            }
        }
        return best;
    }

    public Component displayName() {
        return Component.translatable("arcane_artifacts.cult.quality." + id);
    }
}
