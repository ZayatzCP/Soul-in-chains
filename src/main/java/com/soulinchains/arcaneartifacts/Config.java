package com.soulinchains.arcaneartifacts;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side balance configuration. Glyph-specific values (mana cost, tier, duration of Arcane Mark,
 * etc.) live in the per-glyph config files that Ars Nouveau generates under
 * {@code serverconfig/arcane_artifacts/}.
 */
public final class Config {
    public static final ModConfigSpec SPEC;

    // Wreath of Manipulation
    public static final ModConfigSpec.IntValue WREATH_HITS_TO_ADAPT;
    public static final ModConfigSpec.IntValue WREATH_MIN_ADAPT_SECONDS;
    public static final ModConfigSpec.IntValue WREATH_MAX_ADAPT_SECONDS;
    public static final ModConfigSpec.IntValue WREATH_REGEN_DELAY_TICKS;
    public static final ModConfigSpec.IntValue WREATH_REGEN_DURATION_TICKS;
    public static final ModConfigSpec.IntValue WREATH_CLEANSE_THRESHOLD_TICKS;
    public static final ModConfigSpec.IntValue WREATH_CLEANSE_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue WREATH_MAGIC_REDUCTION;

    // Focusing Boots
    public static final ModConfigSpec.IntValue BOOTS_COOLDOWN_TICKS;

    // Focusing Gloves
    public static final ModConfigSpec.IntValue GLOVES_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue GLOVES_REPEAT_DELAY_TICKS;

    // Ring of Reversal
    public static final ModConfigSpec.IntValue RING_CHARGE_PER_HIT;
    public static final ModConfigSpec.IntValue RING_MAX_CHARGE;
    public static final ModConfigSpec.IntValue RING_MIN_TICKS_BETWEEN_CHARGES;
    public static final ModConfigSpec.DoubleValue RING_MIN_DAMAGE;
    public static final ModConfigSpec.IntValue RING_CHARGE_PER_CONVERSION;
    public static final ModConfigSpec.IntValue RING_MANA_PER_CONVERSION;
    public static final ModConfigSpec.IntValue RING_CONVERSION_INTERVAL_TICKS;

    // Mantle of Reflection
    public static final ModConfigSpec.DoubleValue MANTLE_REFLECT_CHANCE;
    public static final ModConfigSpec.IntValue MANTLE_COOLDOWN_TICKS;

    // Parasitism
    public static final ModConfigSpec.IntValue PARASITISM_DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue PARASITISM_RADIUS;
    public static final ModConfigSpec.IntValue PARASITISM_COOLDOWN_TICKS;

    // Arcane Missile / Pursuit / Splitting
    public static final ModConfigSpec.DoubleValue MISSILE_SEARCH_RADIUS;
    public static final ModConfigSpec.IntValue MISSILE_LIFETIME_TICKS;
    public static final ModConfigSpec.DoubleValue MISSILE_TURN_RATE;
    public static final ModConfigSpec.IntValue MISSILE_MAX_CHASE_TICKS;
    public static final ModConfigSpec.DoubleValue PURSUIT_RADIUS_BONUS;
    public static final ModConfigSpec.DoubleValue PURSUIT_TURN_BONUS;
    public static final ModConfigSpec.IntValue PURSUIT_LIFETIME_BONUS_TICKS;
    public static final ModConfigSpec.IntValue SPLIT_PROJECTILE_COUNT;
    public static final ModConfigSpec.DoubleValue SPLIT_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.IntValue SPLIT_CHILD_LIFETIME_TICKS;
    public static final ModConfigSpec.BooleanValue SPLIT_ALLOW_SAME_TARGET;

    // Research
    public static final ModConfigSpec.BooleanValue RESEARCH_GATES_GLYPHS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("wreath_of_manipulation");
        WREATH_HITS_TO_ADAPT = b.comment("Separate hits of one damage category needed to adapt to it.")
                .defineInRange("hits_to_adapt", 5, 1, 100);
        WREATH_MIN_ADAPT_SECONDS = b.comment("Minimum adaptation duration in seconds.")
                .defineInRange("min_adaptation_seconds", 180, 1, 36000);
        WREATH_MAX_ADAPT_SECONDS = b.comment("Maximum adaptation duration in seconds (actual duration is random between min and max).")
                .defineInRange("max_adaptation_seconds", 360, 1, 36000);
        WREATH_REGEN_DELAY_TICKS = b.comment("Ticks without taking damage before the wreath grants Regeneration.")
                .defineInRange("regeneration_delay_ticks", 80, 1, 12000);
        WREATH_REGEN_DURATION_TICKS = b.comment("Duration of the Regeneration I granted by the wreath, in ticks.")
                .defineInRange("regeneration_duration_ticks", 100, 1, 12000);
        WREATH_CLEANSE_THRESHOLD_TICKS = b.comment("How long a harmful effect must persist continuously before the wreath removes it.")
                .defineInRange("cleanse_threshold_ticks", 100, 1, 12000);
        WREATH_CLEANSE_COOLDOWN_TICKS = b.comment("Cooldown between two cleanses, in ticks.")
                .defineInRange("cleanse_cooldown_ticks", 400, 0, 72000);
        WREATH_MAGIC_REDUCTION = b.comment("Fraction of magic damage negated by the Arcane Adaptation effect (magic damage adaptation).")
                .defineInRange("magic_adaptation_reduction", 0.25, 0.0, 1.0);
        b.pop();

        b.push("focusing_boots");
        BOOTS_COOLDOWN_TICKS = b.comment("Internal cooldown between two crouch-casts, in ticks.")
                .defineInRange("cooldown_ticks", 20, 0, 12000);
        b.pop();

        b.push("focusing_gloves");
        GLOVES_COOLDOWN_TICKS = b.comment("Cooldown after a free repeat cast, in ticks.")
                .defineInRange("cooldown_ticks", 60, 0, 12000);
        GLOVES_REPEAT_DELAY_TICKS = b.comment("Delay between the paid cast and the free repeat, in ticks.")
                .defineInRange("repeat_delay_ticks", 3, 1, 40);
        b.pop();

        b.push("ring_of_reversal");
        RING_CHARGE_PER_HIT = b.defineInRange("charge_per_hit", 3, 0, 1000);
        RING_MAX_CHARGE = b.defineInRange("max_charge", 100, 1, 100000);
        RING_MIN_TICKS_BETWEEN_CHARGES = b.comment("Minimum ticks between two hits that generate charge (anti-exploit).")
                .defineInRange("min_ticks_between_charges", 20, 0, 1200);
        RING_MIN_DAMAGE = b.comment("Hits dealing less damage than this are ignored (anti-exploit).")
                .defineInRange("min_damage", 1.0, 0.0, 1000.0);
        RING_CHARGE_PER_CONVERSION = b.defineInRange("charge_per_conversion", 10, 1, 100000);
        RING_MANA_PER_CONVERSION = b.defineInRange("mana_per_conversion", 2, 0, 100000);
        RING_CONVERSION_INTERVAL_TICKS = b.defineInRange("conversion_interval_ticks", 20, 1, 1200);
        b.pop();

        b.push("mantle_of_reflection");
        MANTLE_REFLECT_CHANCE = b.defineInRange("reflect_chance", 0.15, 0.0, 1.0);
        MANTLE_COOLDOWN_TICKS = b.defineInRange("cooldown_ticks", 240, 0, 72000);
        b.pop();

        b.push("parasitism");
        PARASITISM_DURATION_TICKS = b.defineInRange("link_duration_ticks", 160, 1, 72000);
        PARASITISM_RADIUS = b.comment("The link breaks when the host is farther than this many blocks.")
                .defineInRange("link_radius", 32.0, 1.0, 256.0);
        PARASITISM_COOLDOWN_TICKS = b.comment("Cooldown, counted from the moment a link is created.")
                .defineInRange("cooldown_ticks", 400, 0, 72000);
        b.pop();

        b.push("arcane_missile");
        MISSILE_SEARCH_RADIUS = b.defineInRange("search_radius", 16.0, 1.0, 128.0);
        MISSILE_LIFETIME_TICKS = b.defineInRange("lifetime_ticks", 200, 20, 72000);
        MISSILE_TURN_RATE = b.comment("Fraction of the velocity that is steered toward the target each tick.")
                .defineInRange("turn_rate", 0.18, 0.01, 1.0);
        MISSILE_MAX_CHASE_TICKS = b.comment("A missile gives up a target it has chased for this long without hitting it.")
                .defineInRange("max_chase_ticks", 100, 10, 72000);
        PURSUIT_RADIUS_BONUS = b.defineInRange("pursuit_radius_bonus", 8.0, 0.0, 64.0);
        PURSUIT_TURN_BONUS = b.defineInRange("pursuit_turn_rate_bonus", 0.08, 0.0, 1.0);
        PURSUIT_LIFETIME_BONUS_TICKS = b.defineInRange("pursuit_lifetime_bonus_ticks", 100, 0, 72000);
        b.pop();

        b.push("splitting");
        SPLIT_PROJECTILE_COUNT = b.defineInRange("projectile_count", 2, 1, 8);
        SPLIT_DAMAGE_MULTIPLIER = b.defineInRange("damage_multiplier", 0.5, 0.0, 1.0);
        SPLIT_CHILD_LIFETIME_TICKS = b.defineInRange("child_lifetime_ticks", 80, 10, 1200);
        SPLIT_ALLOW_SAME_TARGET = b.comment("Allow split projectiles to hit the entity the parent projectile hit.")
                .define("allow_same_target", false);
        b.pop();

        b.push("research");
        RESEARCH_GATES_GLYPHS = b.comment("Glyphs listed in a research's \"unlocks\" can only be crafted/learned after that research is formulated. Creative players are never blocked.")
                .define("research_gates_glyphs", true);
        b.pop();

        SPEC = b.build();
    }

    private Config() {
    }
}
