package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.effect.FearEffect;
import com.soulinchains.arcaneartifacts.fx.Fx;
import com.soulinchains.arcaneartifacts.fx.ModTimelines;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/** Fear: the target runs away from the caster and cannot act on its own. Amplify makes it run faster. */
public class EffectFear extends AreaEffect {
    public static final EffectFear INSTANCE = new EffectFear();

    private EffectFear() {
        super(ArcaneArtifacts.id("glyph_fear"), "Fear");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        if (!(target instanceof LivingEntity living) || living == shooter) {
            return;
        }
        int ticks = Math.max(60, Config.FEAR_SECONDS.get() * 20 + stats.getDurationInTicks() * 20);
        int amplifier = Math.max(0, (int) Math.round(stats.getAmpMultiplier()));
        FearEffect.setSource(living, shooter);
        living.addEffect(new MobEffectInstance(ModEffects.FEAR, ticks, amplifier, false, true, true), shooter);
        level.playSound(null, living.blockPosition(), SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 0.5f, 1.6f);
        Fx.burst(level, Fx.timeline(context, ModTimelines.FEAR).main, living.position().add(0, living.getBbHeight() / 2, 0));
    }

    @Override
    public int getDefaultManaCost() {
        return 60;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "The frightened creature runs away faster.");
        map.put(AugmentExtendTime.INSTANCE, "Fear lasts longer.");
        map.put(AugmentDurationDown.INSTANCE, "Fear passes sooner.");
    }

    @Override
    public String getBookDescription() {
        return "Frightens the target: it runs away from the caster and cannot act on its own for 6 seconds. Amplify makes it run faster.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.MANIPULATION);
    }
}
