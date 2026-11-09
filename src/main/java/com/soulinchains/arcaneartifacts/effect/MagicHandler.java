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

    /** How many Harmless effects are resolving right now (on the server thread). */
    private static int harmlessDepth;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEffectPre(com.hollingsworth.arsnouveau.api.event.EffectResolveEvent.Pre event) {
        if (!event.isCanceled() && !event.world.isClientSide && isHarmless(event.context)) {
            harmlessDepth++;
        }
    }

    @SubscribeEvent
    public static void onEffectPost(com.hollingsworth.arsnouveau.api.event.EffectResolveEvent.Post event) {
        if (!event.world.isClientSide && isHarmless(event.context) && harmlessDepth > 0) {
            harmlessDepth--;
        }
    }

    /** Safety net: never let the counter leak into the next tick. */
    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        harmlessDepth = 0;
    }

    /** A Harmless spell breaks no blocks... */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (harmlessDepth > 0) {
            event.setCanceled(true);
        }
    }

    /** ...and its explosions destroy nothing and hurt nobody. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExplosion(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate event) {
        if (harmlessDepth > 0) {
            event.getAffectedBlocks().clear();
            event.getAffectedEntities().clear();
        }
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
        if (event.getEffect().is(ModEffects.MAGIC_ABSORPTION)) {
            event.getEntity().getPersistentData().remove(MAGIC_SHIELD);
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect().is(ModEffects.MAGIC_ABSORPTION)) {
            event.getEntity().getPersistentData().remove(MAGIC_SHIELD);
        }
    }

    /** True when the spell carries the Harmless augment: it deals no damage at all. */
    public static boolean isHarmless(@Nullable SpellContext context) {
        return context != null && context.getSpell().unsafeList().contains(AugmentHarmless.INSTANCE);
    }
}
