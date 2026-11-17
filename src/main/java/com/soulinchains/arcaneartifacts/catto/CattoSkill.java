package com.soulinchains.arcaneartifacts.catto;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Catto's six crafts. Experience comes only from finished work (a block mined, an item crafted, a block placed...),
 * never from fighting. Each tier opens concrete actions; see {@link Catto} and {@link CattoPlanner}.
 */
public enum CattoSkill {
    CRAFTER, MINER, GATHERER, EXPLORER, BUILDER, ALCHEMIST;

    public static final int TIER_2 = 150;
    public static final int TIER_3 = 600;

    public static int tier(int xp) {
        return xp >= TIER_3 ? 3 : xp >= TIER_2 ? 2 : 1;
    }

    /** Experience still missing for the next tier, 0 at tier III. */
    public static int toNext(int xp) {
        return xp >= TIER_3 ? 0 : xp >= TIER_2 ? TIER_3 - xp : TIER_2 - xp;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        return Component.translatable("arcane_artifacts.catto.skill." + key());
    }

    /** What the given tier allows, one line. */
    public Component perk(int tier) {
        return Component.translatable("arcane_artifacts.catto.skill." + key() + ".tier" + tier);
    }
}
