package com.soulinchains.arcaneartifacts.catto;

import java.util.Locale;

/** Effects of Catto's gear (headwear and armor, see {@link CattoGear}). Each one spends a little Source while Catto works. */
public enum CattoModule {
    /** Picks up items from 4 blocks away and keeps the bag tidy while mining and gathering. */
    MAGNET_WHISKER,
    /** Smells ore through stone: mining searches 8 blocks further and digs a tunnel to buried blocks. */
    KEEN_NOSE,
    /** Places torches from the work slot wherever Catto works in the dark. */
    FIREFLY,
    /** Slowly makes Source by itself. */
    SOURCE_FUR,
    /** Faster steps, no fall damage, no burning. */
    CAT_BOOTS,
    /** Harvests like a Gatherer one tier higher and gathers 4 blocks further. */
    GREEN_THUMB,
    /** Slowly heals. */
    REGROWTH;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
