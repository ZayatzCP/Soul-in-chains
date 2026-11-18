package com.soulinchains.arcaneartifacts.catto;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.Locale;

/**
 * What kind of cat Catto is, decided once when it appears. A brawler loves a fight: it attacks monsters, retreats
 * only when badly hurt, grows attached through battles and starts as a miner, explorer and fighter. A gentle one
 * always backs off from monsters, loves petting and food, and starts as a crafter, gatherer, builder and alchemist.
 */
public enum CattoNature {
    BRAWLER, GENTLE;

    public static CattoNature random(RandomSource random) {
        return random.nextBoolean() ? BRAWLER : GENTLE;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        return Component.translatable("arcane_artifacts.catto.nature." + key());
    }

    /** Starting experience per skill (indexed by {@link CattoSkill#ordinal()}). */
    public int startXp(CattoSkill skill) {
        return switch (this) {
            case BRAWLER -> switch (skill) {
                case FIGHTER -> 160;
                case MINER, EXPLORER -> 80;
                default -> 0;
            };
            case GENTLE -> switch (skill) {
                case CRAFTER, GATHERER, ALCHEMIST -> 80;
                case BUILDER -> 50;
                default -> 0;
            };
        };
    }

    /** Bond from petting, fish and a won fight. */
    public int petBond() {
        return this == GENTLE ? 5 : 2;
    }

    public int fishBond() {
        return this == GENTLE ? 3 : 2;
    }

    public int fightBond() {
        return this == BRAWLER ? 2 : 0;
    }
}
