package com.soulinchains.arcaneartifacts.rite;

import java.util.EnumMap;
import java.util.Map;

/**
 * The modifier sigils drawn on a site (outside the rite's own four places) and what they add up to. A second copy of the
 * same modifier gives half, a third a quarter; its price is paid in full each time. Sonm and Venec cancel each other's
 * gain but both prices stay.
 */
public final class RiteModifiers {
    private final Map<Sigil, Integer> counts = new EnumMap<>(Sigil.class);

    public static RiteModifiers of(RiteSite site) {
        RiteModifiers mods = new RiteModifiers();
        site.places.forEach((place, reading) -> {
            if (!place.rite() && reading.valid() && reading.sigil().modifier) {
                mods.add(reading.sigil());
            }
        });
        return mods;
    }

    public RiteModifiers add(Sigil sigil) {
        counts.merge(sigil, 1, Integer::sum);
        return this;
    }

    public int count(Sigil sigil) {
        return counts.getOrDefault(sigil, 0);
    }

    /** 1 + 1/2 + 1/4 ... for n copies. */
    static float stacked(int n) {
        float sum = 0;
        float step = 1;
        for (int i = 0; i < n; i++) {
            sum += step;
            step /= 2;
        }
        return sum;
    }

    private boolean cancelled() {
        return count(Sigil.SONM) > 0 && count(Sigil.VENEC) > 0;
    }

    /** What the modifiers add to the quality (prices included). */
    public float quality() {
        float q = -10F * count(Sigil.SONM) - 5F * count(Sigil.KLYATVA) - 5F * count(Sigil.TISH);
        if (!cancelled()) {
            q += 10F * stacked(count(Sigil.VENEC));
        }
        q += 15F * stacked(count(Sigil.KROV));
        return q;
    }

    /** Extra creatures (Sonm) minus the one Venec takes. */
    public float extraCount() {
        float extra = cancelled() ? 0 : stacked(count(Sigil.SONM));
        if (count(Sigil.VENEC) > 0) {
            extra -= 1;
        }
        return extra;
    }

    public boolean empowered() {
        return count(Sigil.VENEC) > 0 && !cancelled();
    }

    public float eliteChance() {
        return 0.15F * stacked(count(Sigil.ROG));
    }

    public float lifeFactor() {
        return 1F + 0.5F * stacked(count(Sigil.VITOK));
    }

    public int extraChannelTicks() {
        return 200 * count(Sigil.VITOK);
    }

    public float extraLoot() {
        return 0.25F * stacked(count(Sigil.ZHILA));
    }

    /** Extra items Zhila burns from the first offering. */
    public int extraOffering() {
        return count(Sigil.ZHILA);
    }

    public boolean loyal() {
        return count(Sigil.KLYATVA) > 0;
    }

    public int fewerEchoes() {
        return count(Sigil.TISH);
    }

    /** Hearts of health each participant pays at the start (Krov). */
    public float bloodPrice() {
        return 6F * count(Sigil.KROV);
    }

    public int total() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }
}
