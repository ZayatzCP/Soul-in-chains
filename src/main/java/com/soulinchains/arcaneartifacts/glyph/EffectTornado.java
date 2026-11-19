package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAccelerate;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectBurst;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectColdSnap;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectConjureWater;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectFlare;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectFreeze;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectIgnite;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectLinger;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectReset;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectWall;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import com.soulinchains.arcaneartifacts.phenomena.Phenomena;
import com.soulinchains.arcaneartifacts.phenomena.Tornado;

/**
 * Tornado: a whirlwind at the point of impact that spins creatures and items around and lifts them up.
 * <p>
 * Like Burst, it carries the rest of the spell: everything after the Tornado is cast on every creature caught in the
 * funnel (with Sensitive: on the ground under it) again and again while it lasts. Fire after it makes a fire tornado,
 * frost a frozen one, water a waterspout. Carrying a spell costs {@link #CARRY_SURCHARGE} more mana.
 */
public class EffectTornado extends AreaEffect {
    public static final EffectTornado INSTANCE = new EffectTornado();
    /** Extra mana when the Tornado carries the rest of the spell (70 + 180 = 250). */
    public static final int CARRY_SURCHARGE = 180;

    private EffectTornado() {
        super(ArcaneArtifacts.id("glyph_tornado"), "Tornado");
        EffectReset.RESET_LIMITS.add(this);
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        resolveAt(level, point, target, shooter, stats, context, null);
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context,
                             @Nullable SpellResolver resolver) {
        Vec3 base = target != null ? target.position() : point.subtract(0, 0.5, 0);
        Tornado.Settings settings = new Tornado.Settings();
        settings.radius = Math.max(2.0, Config.TORNADO_RADIUS.get() + 1.5 * stats.getAoeMultiplier());
        settings.durationTicks = Math.max(20, Config.TORNADO_SECONDS.get() * 20 + stats.getDurationInTicks() * 2);
        settings.strength = Math.max(0.2, 1.0 + 0.25 * stats.getAmpMultiplier());
        settings.sensitive = stats.isSensitive();
        settings.gentle = stats.hasBuff(AugmentDampen.INSTANCE);
        settings.maxHardness = (float) Math.max(0.5, 3.0 + 2.0 * stats.getAmpMultiplier());
        int accelerate = stats.getBuffCount(AugmentAccelerate.INSTANCE);
        if (accelerate > 0) {
            Vec3 look = shooter.getLookAngle().multiply(1, 0, 1);
            if (look.lengthSqr() > 1.0E-4) {
                settings.drift = look.normalize().scale(0.08 * accelerate);
            }
            settings.procInterval = Math.max(4, 10 - 2 * accelerate);
        }
        Spell rest = context.getRemainingSpell();
        if (resolver != null && hasEffect(rest)) {
            // Exactly like Burst: the rest of the spell goes with the tornado and does not also happen here.
            settings.carrier = resolver.getNewResolver(context.makeChildContext());
            settings.element = elementOf(rest);
            context.setCanceled(true);
        }
        var style = com.soulinchains.arcaneartifacts.fx.Fx.timeline(context, com.soulinchains.arcaneartifacts.fx.ModTimelines.TORNADO);
        settings.style = style.main;
        settings.impact = style.impact;
        Phenomena.add(new Tornado(level, shooter.getUUID(), base, settings));
        com.soulinchains.arcaneartifacts.fx.Fx.area(level, style.impact, base, settings.radius);
        level.playSound(null, base.x, base.y, base.z, net.minecraft.sounds.SoundEvents.ELYTRA_FLYING, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    private static boolean hasEffect(Spell spell) {
        for (AbstractSpellPart part : spell.recipe()) {
            if (part instanceof AbstractEffect) {
                return true;
            }
        }
        return false;
    }

    /** True when the Tornado in this spell is followed by another effect (it then carries it, at a surcharge). */
    public static boolean carries(Spell spell) {
        boolean after = false;
        for (AbstractSpellPart part : spell.recipe()) {
            if (part == INSTANCE) {
                after = true;
            } else if (after && part instanceof AbstractEffect) {
                return true;
            }
        }
        return false;
    }

    /** The element of the funnel, from the first elemental effect in the rest of the spell. */
    static Tornado.Element elementOf(Spell rest) {
        for (AbstractSpellPart part : rest.recipe()) {
            if (part == EffectIgnite.INSTANCE || part == EffectFlare.INSTANCE) {
                return Tornado.Element.FIRE;
            }
            if (part == EffectFreeze.INSTANCE || part == EffectColdSnap.INSTANCE) {
                return Tornado.Element.FROST;
            }
            if (part == EffectConjureWater.INSTANCE) {
                return Tornado.Element.WATER;
            }
        }
        return Tornado.Element.AIR;
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        super.buildConfig(builder);
        PER_SPELL_LIMIT = builder.comment("The maximum number of times this glyph may appear in a single spell").defineInRange("per_spell_limit", 1, 1, 1);
    }

    @Override
    public int getDefaultManaCost() {
        return 70;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAOE.INSTANCE, AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE, AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE,
                AugmentSensitive.INSTANCE, AugmentAccelerate.INSTANCE);
    }

    @Override
    protected void addDefaultAugmentLimits(Map<ResourceLocation, Integer> defaults) {
        super.addDefaultAugmentLimits(defaults);
        defaults.put(AugmentSensitive.INSTANCE.getRegistryName(), 1);
        defaults.put(AugmentAccelerate.INSTANCE.getRegistryName(), 3);
    }

    @Override
    protected void addDefaultInvalidCombos(Set<ResourceLocation> defaults) {
        super.addDefaultInvalidCombos(defaults);
        // Carriers inside carriers multiply without end.
        defaults.add(EffectBurst.INSTANCE.getRegistryName());
        defaults.add(EffectLinger.INSTANCE.getRegistryName());
        defaults.add(EffectWall.INSTANCE.getRegistryName());
        defaults.add(EffectBlackHole.INSTANCE.getRegistryName());
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAOE.INSTANCE, "A wider whirlwind.");
        map.put(AugmentAmplify.INSTANCE, "Spins faster, lifts higher and tears out harder blocks.");
        map.put(AugmentDampen.INSTANCE, "A gentler breeze; torn-out blocks are put down instead of flung.");
        map.put(AugmentExtendTime.INSTANCE, "The tornado lasts longer.");
        map.put(AugmentDurationDown.INSTANCE, "The tornado dies down sooner.");
        map.put(AugmentSensitive.INSTANCE, "Tears loose blocks out of the ground, and casts the rest of the spell on the ground instead of creatures.");
        map.put(AugmentAccelerate.INSTANCE, "The tornado crawls the way you look and casts the rest of the spell more often.");
    }

    @Override
    public String getBookDescription() {
        return "Raises a tornado where the spell lands. It spins creatures and items around and lifts them into the air; when it dies down, they fall. "
                + "Like Burst, it carries the rest of the spell to everything it catches, again and again: add Ignite for a fire tornado. "
                + "Carrying costs 180 more mana. Over lava it catches fire by itself. The caster is never caught.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.ELEMENTAL_AIR);
    }
}
