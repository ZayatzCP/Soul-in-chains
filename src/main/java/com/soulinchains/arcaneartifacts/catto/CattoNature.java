package com.soulinchains.arcaneartifacts.catto;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.Locale;

/**
 * Catto's temperament, decided once when it appears. It sets the starting skills, how the bond grows, whether Catto
 * fights or backs off, how eagerly it picks things up, how fast it works, and how it talks.
 * <ul>
 * <li>Brawler: attacks monsters, bonds through fights; miner, explorer, fighter.</li>
 * <li>Gentle: always backs off, loves petting and fish; crafter, gatherer, alchemist, builder.</li>
 * <li>Curious: picks up everything in sight and wanders off to look; explorer, gatherer.</li>
 * <li>Lazy: works slower and naps whenever it can, but adores petting; alchemist, crafter.</li>
 * <li>Thrifty: takes only what is useful, sometimes saves a material; crafter, builder; fights back when hit.</li>
 * </ul>
 */
public enum CattoNature {
    BRAWLER, GENTLE, CURIOUS, LAZY, THRIFTY;

    public static CattoNature random(RandomSource random) {
        CattoNature[] all = values();
        return all[random.nextInt(all.length)];
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        return Component.translatable("arcane_artifacts.catto.nature." + key());
    }

    /** Starting experience per skill. */
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
            case CURIOUS -> switch (skill) {
                case EXPLORER -> 160;
                case GATHERER -> 80;
                default -> 0;
            };
            case LAZY -> switch (skill) {
                case ALCHEMIST -> 160;
                case CRAFTER -> 60;
                default -> 0;
            };
            case THRIFTY -> switch (skill) {
                case CRAFTER, BUILDER -> 100;
                case GATHERER -> 40;
                default -> 0;
            };
        };
    }

    /** Throws itself at monsters without being asked. */
    public boolean attacksMonsters() {
        return this == BRAWLER;
    }

    /** Hits back at whoever hurt it instead of running (unless badly hurt). */
    public boolean hitsBack() {
        return this == BRAWLER || this == THRIFTY;
    }

    /** Below this share of health Catto backs off, whatever it was doing. */
    public float retreatHealth() {
        return switch (this) {
            case BRAWLER -> 0.3F;
            case THRIFTY -> 0.5F;
            default -> 0.4F;
        };
    }

    public int petBond() {
        return switch (this) {
            case LAZY -> 6;
            case GENTLE -> 5;
            case CURIOUS -> 3;
            default -> 2;
        };
    }

    public int fishBond() {
        return switch (this) {
            case GENTLE, LAZY -> 3;
            default -> 2;
        };
    }

    public int fightBond() {
        return this == BRAWLER ? 2 : 0;
    }

    /** Work takes this much longer (lazy) or shorter. */
    public float workFactor() {
        return switch (this) {
            case LAZY -> 1.3F;
            case THRIFTY -> 0.9F;
            default -> 1.0F;
        };
    }

    /** How far Catto notices things lying around and goes to look at them. */
    public int pickupRadius() {
        return switch (this) {
            case CURIOUS -> 10;
            case LAZY -> 3;
            default -> 6;
        };
    }

    /** Takes even things nobody needs (to the storage if there is one). */
    public boolean takesEverything() {
        return this == CURIOUS;
    }

    /** Chance to keep the material of a craft or a placed block. */
    public float thrift() {
        return this == THRIFTY ? 0.2F : 0.0F;
    }
}
