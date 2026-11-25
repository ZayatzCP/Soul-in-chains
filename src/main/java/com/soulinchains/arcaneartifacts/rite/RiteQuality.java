package com.soulinchains.arcaneartifacts.rite;

/** The quality a site starts a rite with, from what was prepared: candles, lanterns, the censer, sigils and modifiers. */
public final class RiteQuality {
    public static final float BASE = 30F;
    public static final int CANDLE_CAP = 20;
    public static final float LANTERN = 3F;
    public static final int LANTERN_CAP = 4;
    public static final float CENSER = 8F;
    public static final float MAIN_SIGIL = 10F;
    public static final float SIDE_SIGIL = 5F;
    public static final float WRONG_SIGIL = 5F;

    private RiteQuality() {
    }

    /** The starting quality for {@code rite} on {@code site}. */
    public static float start(RiteSite site, RiteModifiers mods) {
        float q = BASE;
        q += Math.min(CANDLE_CAP, site.candlesLit);
        q += LANTERN * Math.min(LANTERN_CAP, site.lanterns);
        if (site.censer) {
            q += CENSER;
        }
        if (site.at(RiteSite.Place.MAIN).valid()) {
            q += MAIN_SIGIL;
        }
        for (RiteSite.Place side : new RiteSite.Place[]{RiteSite.Place.SOUTH, RiteSite.Place.WEST, RiteSite.Place.EAST}) {
            if (site.at(side).valid()) {
                q += SIDE_SIGIL;
            }
        }
        q -= WRONG_SIGIL * site.wrongPlaces();
        q += mods.quality();
        return Math.max(0F, Math.min(100F, q));
    }
}
