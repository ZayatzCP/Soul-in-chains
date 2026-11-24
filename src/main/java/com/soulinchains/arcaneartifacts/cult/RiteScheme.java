package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.network.chat.Component;

/**
 * The rites a seal can be set to with the Book of Formulas: what each needs (reagents, lit anchors, rank), how long it
 * takes, and the special rule it plays by.
 * <ul>
 *     <li>Three Voices: at the end all three anchors must hold 50+, or −10%.</li>
 *     <li>Red Veil: one more Torn Acolyte per wave; the Confessor leaving the inner zone costs twice as much.</li>
 *     <li>Abundance: an extra inscription window halfway through.</li>
 *     <li>Second Coming: the knife's close and the shield's seal must fall within a second of each other, or −15%.</li>
 * </ul>
 */
public enum RiteScheme {
    ASH_WITNESS("ash_witness", 1, 4, 2, 1, 0, 2, 500),
    THREE_VOICES("three_voices", 2, 4, 3, 3, 1, 3, 700),
    RED_VEIL("red_veil", 2, 2, 4, 1, 2, 2, 800),
    ABUNDANCE("abundance", 3, 6, 2, 2, 3, 4, 900),
    SECOND_COMING("second_coming", 3, 4, 4, 2, 4, 4, 1000);

    public final String id;
    public final int rank;
    public final int ash;
    public final int shards;
    public final int candles;
    public final int dust;
    public final int anchors;
    public final int ticks;

    RiteScheme(String id, int rank, int ash, int shards, int candles, int dust, int anchors, int ticks) {
        this.id = id;
        this.rank = rank;
        this.ash = ash;
        this.shards = shards;
        this.candles = candles;
        this.dust = dust;
        this.anchors = anchors;
        this.ticks = ticks;
    }

    public RiteScheme next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** The next scheme after this one that a player of this rank may draw (this one when none). */
    public RiteScheme nextFor(int rank) {
        RiteScheme next = this;
        for (int i = 0; i < values().length; i++) {
            next = next.next();
            if (next.rank <= rank) {
                return next;
            }
        }
        return this;
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
