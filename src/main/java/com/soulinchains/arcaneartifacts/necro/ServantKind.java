package com.soulinchains.arcaneartifacts.necro;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The seven servants a Shackled raises from souls. Each one costs souls, holds a share of the master's maximum mana
 * while it exists and has its own trade.
 */
public enum ServantKind {
    /** Infantry: hits with its chain from 3 blocks. */
    SKELETON("chained_skeleton", 20, 4, 0.27, 2, 0.06F, false, Map.of(SoulType.WANDERING, 3)),
    /** Fast, catches the target and holds it (Slowness). */
    HOUND("ghost_hound", 16, 3, 0.36, 0, 0.06F, false, Map.of(SoulType.BEAST, 2)),
    /** Tank with a sword, sets what it hits on fire. */
    KNIGHT("burning_knight", 40, 6, 0.24, 8, 0.12F, true, ordered(SoulType.BLAZING, 1, SoulType.WANDERING, 3)),
    /** Steps through shadows behind its target, hits hard. */
    ASSASSIN("shadow_assassin", 18, 8, 0.32, 2, 0.12F, false, ordered(SoulType.VOID, 1, SoulType.WANDERING, 2)),
    /** Carries its master over water and drowns enemies. */
    ROWER("drowned_rower", 24, 3, 0.25, 2, 0.08F, false, Map.of(SoulType.DROWNED, 2)),
    /** Does not fight: carries 27 stacks and picks up loot. */
    PORTER("bone_porter", 20, 0, 0.27, 4, 0.05F, false, Map.of(SoulType.WANDERING, 4)),
    /** Heals its master and fellow servants, weakens enemies. */
    PLAGUE_DOCTOR("plague_doctor", 22, 3, 0.26, 2, 0.10F, false, ordered(SoulType.CURSED, 1, SoulType.WANDERING, 3));

    public final String id;
    public final double health;
    public final double damage;
    public final double speed;
    public final double armor;
    /** Share of the master's maximum mana held while this servant exists. */
    public final float reserve;
    public final boolean fireImmune;
    /** Souls spent to raise it; the first one is the soul given back when the chain breaks. */
    public final Map<SoulType, Integer> cost;

    ServantKind(String id, double health, double damage, double speed, double armor, float reserve, boolean fireImmune, Map<SoulType, Integer> cost) {
        this.id = id;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.armor = armor;
        this.reserve = reserve;
        this.fireImmune = fireImmune;
        this.cost = cost;
    }

    public boolean fights() {
        return this != PORTER;
    }

    /** The soul a crumbling servant gives back: its rarest soul. */
    public SoulType keySoul() {
        return cost.keySet().iterator().next();
    }

    private static Map<SoulType, Integer> ordered(SoulType a, int na, SoulType b, int nb) {
        Map<SoulType, Integer> map = new LinkedHashMap<>();
        map.put(a, na);
        map.put(b, nb);
        return map;
    }
}
