package com.soulinchains.arcaneartifacts.catto;

import java.util.Locale;

/** The magic modules Catto wears in the module slots. Each one spends Source while Catto works. */
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
    CAT_BOOTS;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
