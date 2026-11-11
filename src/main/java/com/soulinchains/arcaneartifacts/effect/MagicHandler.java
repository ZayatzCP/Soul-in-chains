package com.soulinchains.arcaneartifacts.effect;

import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import com.hollingsworth.arsnouveau.api.event.SpellModifierEvent;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.glyph.AugmentHarmless;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;

/** Pity (every spell of the creature is weakened like two Dampen per level) and Harmless spells. */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class MagicHandler {
    public static final int PITY_DAMPEN = 2;

    private MagicHandler() {
    }

    @SubscribeEvent
    public static void onSpellModifiers(SpellModifierEvent event) {
        if (event.caster == null) {
            return;
        }
        MobEffectInstance pity = event.caster.getEffect(ModEffects.PITY);
        if (pity != null) {
            event.builder.addAmplification(-PITY_DAMPEN * (pity.getAmplifier() + 1));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSpellDamage(SpellDamageEvent.Pre event) {
        if (isHarmless(event.context)) {
            event.damage = 0;
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ recoil: spells hurt their caster too

    /** Recoil collected this tick per caster: only the strongest hit of the tick comes back. */
    private static final java.util.Map<java.util.UUID, Float> RECOIL = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, net.minecraft.world.entity.LivingEntity> RECOIL_CASTER = new java.util.HashMap<>();

    /** A spell whose form is Self already targets the caster: no extra recoil. */
    public static boolean selfCast(@Nullable SpellContext context) {
        return context != null && context.getSpell().getCastMethod() instanceof com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
    }

    /**
     * Every spell that deals damage hurts its caster as well ({@code caster_recoil_fraction} of the
     * damage). Many hits in the same tick (an area spell) recoil once, with the strongest of them.
     */
    public static void recoil(@Nullable net.minecraft.world.entity.LivingEntity caster, @Nullable net.minecraft.world.entity.Entity target, float damage) {
        if (caster == null || target == caster || damage <= 0 || caster.level().isClientSide) {
            return;
        }
        float amount = (float) (damage * com.soulinchains.arcaneartifacts.Config.CASTER_RECOIL_FRACTION.get());
        if (amount <= 0) {
            return;
        }
        RECOIL.merge(caster.getUUID(), amount, Math::max);
        RECOIL_CASTER.put(caster.getUUID(), caster);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellDamageDealt(SpellDamageEvent.Post event) {
        if (!selfCast(event.context) && !isHarmless(event.context)) {
            recoil(event.caster, event.target, event.damage);
        }
    }

    @SubscribeEvent
    public static void onTickEnd(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (RECOIL.isEmpty()) {
            return;
        }
        var pending = new java.util.HashMap<>(RECOIL);
        RECOIL.clear();
        pending.forEach((id, amount) -> {
            var caster = RECOIL_CASTER.get(id);
            if (caster != null && caster.isAlive()) {
                caster.invulnerableTime = 0;
                caster.hurt(caster.damageSources().magic(), amount);
            }
        });
        RECOIL_CASTER.clear();
    }

    // ------------------------------------------------------------------ Magic Absorption

    public static final String MAGIC_SHIELD = "arcane_artifacts.magic_absorption";

    /** A fresh Magic Absorption fills the magic shield: 4 points per level. */
    @SubscribeEvent
    public static void onEffectAdded(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added event) {
        if (event.getEffectInstance().getEffect().is(ModEffects.MAGIC_ABSORPTION)) {
            float pool = 4.0f * (event.getEffectInstance().getAmplifier() + 1);
            var data = event.getEntity().getPersistentData();
            data.putFloat(MAGIC_SHIELD, Math.max(pool, data.getFloat(MAGIC_SHIELD)));
        }
    }

    public static boolean isMagic(net.minecraft.world.damagesource.DamageSource source) {
        if (source.is(net.neoforged.neoforge.common.Tags.DamageTypes.IS_MAGIC)
                || source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                || source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)) {
            return true;
        }
        String namespace = source.typeHolder().unwrapKey().map(key -> key.location().getNamespace()).orElse("");
        return namespace.equals("ars_nouveau") || namespace.equals(ArcaneArtifacts.MODID);
    }

    /** Magic damage is soaked up by the shield first; when the shield is gone, so is the effect. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onMagicDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide || !entity.hasEffect(ModEffects.MAGIC_ABSORPTION) || !isMagic(event.getSource())) {
            return;
        }
        var data = entity.getPersistentData();
        float pool = data.getFloat(MAGIC_SHIELD);
        float absorbed = Math.min(pool, event.getAmount());
        event.setAmount(event.getAmount() - absorbed);
        pool -= absorbed;
        data.putFloat(MAGIC_SHIELD, pool);
        if (pool <= 0.01f) {
            entity.removeEffect(ModEffects.MAGIC_ABSORPTION);
            data.remove(MAGIC_SHIELD);
        }
        if (event.getAmount() <= 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEffectGone(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove event) {
        if (event.getEffect().is(ModEffects.FEAR)) {
            FearEffect.forget(event.getEntity());
        }
        if (event.getEffect().is(ModEffects.MAGIC_ABSORPTION)) {
            event.getEntity().getPersistentData().remove(MAGIC_SHIELD);
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect().is(ModEffects.FEAR)) {
            FearEffect.forget(event.getEntity());
        }
        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect().is(ModEffects.MAGIC_ABSORPTION)) {
            event.getEntity().getPersistentData().remove(MAGIC_SHIELD);
        }
    }

    /** A frightened creature attacks nobody. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFrightenedAttack(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                && event.getSource().getDirectEntity() == attacker && attacker.hasEffect(ModEffects.FEAR)) {
            event.setCanceled(true);
        }
    }

    /** True when the spell carries the Harmless augment: it deals no damage at all. */
    public static boolean isHarmless(@Nullable SpellContext context) {
        return context != null && context.getSpell().unsafeList().contains(AugmentHarmless.INSTANCE);
    }
}
