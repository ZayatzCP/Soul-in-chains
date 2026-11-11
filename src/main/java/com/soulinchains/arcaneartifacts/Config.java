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
    public static final ModConfigSpec.IntValue WREATH_RUSH_SECONDS;
    public static final ModConfigSpec.IntValue WREATH_COOLDOWN_SECONDS;
    public static final ModConfigSpec.DoubleValue WREATH_TRIGGER_FRACTION;

    // Focusing Boots
    public static final ModConfigSpec.IntValue BOOTS_COOLDOWN_TICKS;

    // Focusing Gloves
    public static final ModConfigSpec.IntValue GLOVES_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue GLOVES_REPEAT_DELAY_TICKS;

    // Ring of Reversal
    public static final ModConfigSpec.DoubleValue RING_BIG_HIT_FRACTION;
    public static final ModConfigSpec.DoubleValue RING_BLOCKS_PER_DAMAGE;
    public static final ModConfigSpec.DoubleValue RING_MAX_KNOCKBACK_BLOCKS;
    public static final ModConfigSpec.IntValue RING_COOLDOWN_TICKS;

    // Amulet of Opposites
    public static final ModConfigSpec.DoubleValue AMULET_ATTRACT_RADIUS;
    public static final ModConfigSpec.DoubleValue AMULET_REPEL_RADIUS;

    // Mantle of Reflection
    public static final ModConfigSpec.DoubleValue MANTLE_REFLECT_CHANCE;
    public static final ModConfigSpec.IntValue MANTLE_COOLDOWN_TICKS;

    // Parasitism
    public static final ModConfigSpec.IntValue PARASITISM_DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue PARASITISM_RADIUS;
    public static final ModConfigSpec.IntValue PARASITISM_COOLDOWN_TICKS;

    // Arcane Missile / Pursuit / Shrapnel
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

    // New glyphs
    public static final ModConfigSpec.DoubleValue BEAM_RANGE;
    public static final ModConfigSpec.DoubleValue BLACK_HOLE_RADIUS;
    public static final ModConfigSpec.IntValue BLACK_HOLE_SECONDS;
    public static final ModConfigSpec.DoubleValue BLACK_HOLE_DAMAGE;
    public static final ModConfigSpec.DoubleValue TORNADO_RADIUS;
    public static final ModConfigSpec.IntValue TORNADO_SECONDS;
    public static final ModConfigSpec.IntValue WEATHER_SECONDS;
    public static final ModConfigSpec.IntValue COUNTERSPELL_SILENCE_SECONDS;
    public static final ModConfigSpec.DoubleValue COUNTERSPELL_RADIUS;
    public static final ModConfigSpec.IntValue METEOR_COUNT;
    public static final ModConfigSpec.DoubleValue METEOR_DAMAGE;
    public static final ModConfigSpec.DoubleValue TIME_WARP_RADIUS;
    public static final ModConfigSpec.IntValue TIME_WARP_SECONDS;
    public static final ModConfigSpec.DoubleValue ANVIL_DAMAGE;
    public static final ModConfigSpec.DoubleValue ANVIL_HEAVY_DAMAGE;
    public static final ModConfigSpec.IntValue SATIETY_SECONDS;
    public static final ModConfigSpec.IntValue PITY_SECONDS;
    public static final ModConfigSpec.IntValue CALM_RING_SECONDS;
    public static final ModConfigSpec.IntValue FEAR_SECONDS;
    public static final ModConfigSpec.IntValue CALM_RING_REPEAT_SECONDS;
    public static final ModConfigSpec.DoubleValue CASTER_RECOIL_FRACTION;
    public static final ModConfigSpec.DoubleValue ANVIL_BONK_DAMAGE;

    // Research
    public static final ModConfigSpec.BooleanValue RESEARCH_GATES_GLYPHS;
    public static final ModConfigSpec.BooleanValue RESEARCH_CREATIVE_BYPASS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("wreath_of_manipulation");
        WREATH_RUSH_SECONDS = b.comment("Duration of the Battle Rush (free spells, no damage taken), in seconds.")
                .defineInRange("rush_seconds", 30, 1, 3600);
        WREATH_COOLDOWN_SECONDS = b.comment("Time before the next Battle Rush can start, counted from the start of the previous one, in seconds.")
                .defineInRange("cooldown_seconds", 300, 1, 36000);
        WREATH_TRIGGER_FRACTION = b.comment("The rush starts when the wearer's mana is at or below this fraction of the maximum (at least 1 point).")
                .defineInRange("trigger_mana_fraction", 0.02, 0.0, 1.0);
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
        RING_BIG_HIT_FRACTION = b.comment("A hit dealing more than this fraction of the wearer's maximum health is negated.")
                .defineInRange("big_hit_fraction", 0.5, 0.0, 10.0);
        RING_BLOCKS_PER_DAMAGE = b.comment("The attacker is thrown back about this many blocks per point of negated damage.")
                .defineInRange("blocks_per_damage", 1.0, 0.0, 10.0);
        RING_MAX_KNOCKBACK_BLOCKS = b.defineInRange("max_knockback_blocks", 48.0, 0.0, 256.0);
        RING_COOLDOWN_TICKS = b.comment("Cooldown after a negated hit, in ticks (0 = none).")
                .defineInRange("cooldown_ticks", 0, 0, 72000);
        b.pop();

        b.push("amulet_of_opposites");
        AMULET_ATTRACT_RADIUS = b.defineInRange("attract_radius", 10.0, 1.0, 64.0);
        AMULET_REPEL_RADIUS = b.defineInRange("repel_radius", 5.0, 1.0, 64.0);
        b.pop();

        b.push("mantle_of_reflection");
        MANTLE_REFLECT_CHANCE = b.defineInRange("reflect_chance", 0.5, 0.0, 1.0);
        MANTLE_COOLDOWN_TICKS = b.comment("Cooldown after a reflection, in ticks (0 = none).")
                .defineInRange("cooldown_ticks", 0, 0, 72000);
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

        b.push("shrapnel");
        SPLIT_PROJECTILE_COUNT = b.defineInRange("projectile_count", 2, 1, 8);
        SPLIT_DAMAGE_MULTIPLIER = b.defineInRange("damage_multiplier", 0.5, 0.0, 1.0);
        SPLIT_CHILD_LIFETIME_TICKS = b.defineInRange("child_lifetime_ticks", 80, 10, 1200);
        SPLIT_ALLOW_SAME_TARGET = b.comment("Allow split projectiles to hit the entity the parent projectile hit.")
                .define("allow_same_target", false);
        b.pop();

        b.push("glyphs");
        BEAM_RANGE = b.defineInRange("beam_range", 24.0, 1.0, 128.0);
        BLACK_HOLE_RADIUS = b.defineInRange("black_hole_radius", 6.0, 1.0, 64.0);
        BLACK_HOLE_SECONDS = b.defineInRange("black_hole_seconds", 5, 1, 120);
        BLACK_HOLE_DAMAGE = b.comment("Damage dealt twice per second in the core.").defineInRange("black_hole_damage", 3.0, 0.0, 1000.0);
        TORNADO_RADIUS = b.defineInRange("tornado_radius", 4.0, 1.0, 64.0);
        TORNADO_SECONDS = b.defineInRange("tornado_seconds", 6, 1, 120);
        WEATHER_SECONDS = b.comment("How long the new weather lasts, in seconds.").defineInRange("weather_seconds", 600, 60, 72000);
        COUNTERSPELL_SILENCE_SECONDS = b.defineInRange("counterspell_silence_seconds", 5, 0, 600);
        COUNTERSPELL_RADIUS = b.comment("Radius of a Counterspell resolved on a block.").defineInRange("counterspell_radius", 4.0, 0.5, 64.0);
        METEOR_COUNT = b.defineInRange("meteor_count", 5, 1, 64);
        METEOR_DAMAGE = b.defineInRange("meteor_damage", 6.0, 0.0, 1000.0);
        TIME_WARP_RADIUS = b.defineInRange("time_warp_radius", 6.0, 1.0, 64.0);
        TIME_WARP_SECONDS = b.defineInRange("time_warp_seconds", 6, 1, 600);
        ANVIL_DAMAGE = b.defineInRange("anvil_damage", 60.0, 0.0, 10000.0);
        ANVIL_HEAVY_DAMAGE = b.comment("Anvil Drop damage with Amplify.").defineInRange("anvil_heavy_damage", 120.0, 0.0, 10000.0);
        SATIETY_SECONDS = b.defineInRange("satiety_seconds", 60, 10, 36000);
        PITY_SECONDS = b.defineInRange("pity_seconds", 20, 1, 3600);
        CASTER_RECOIL_FRACTION = b.comment("Every damaging spell also hurts its caster by this fraction of the damage (once per tick, the strongest hit). 0 turns it off.").defineInRange("caster_recoil_fraction", 0.5, 0.0, 10.0);
        FEAR_SECONDS = b.defineInRange("fear_seconds", 6, 1, 600);
        CALM_RING_REPEAT_SECONDS = b.comment("Ring of Calm: the same kind of creature is announced again only after this many seconds.").defineInRange("calm_ring_repeat_seconds", 300, 0, 86400);
        CALM_RING_SECONDS = b.comment("Ring of Calm: cooldown in seconds after a hostile creature shows up; the bonuses return once it has passed with no hostile creatures nearby.").defineInRange("calm_ring_cooldown_seconds", 60, 0, 3600);
        ANVIL_BONK_DAMAGE = b.comment("Anvil Drop damage with Extend Time (it stuns with BONK! instead).").defineInRange("anvil_bonk_damage", 12.0, 0.0, 10000.0);
        b.pop();

        b.push("research");
        RESEARCH_GATES_GLYPHS = b.comment("Glyphs listed in a research's \"unlocks\" can only be crafted/learned after that research is formulated. Creative players are blocked too unless creative_bypasses_research is true.")
                .define("research_gates_glyphs", true);
        RESEARCH_CREATIVE_BYPASS = b.comment("Let creative-mode players craft, learn and cast locked glyphs and artifacts without research.")
                .define("creative_bypasses_research", false);
        b.pop();

        SPEC = b.build();
    }

    private Config() {
    }
}
