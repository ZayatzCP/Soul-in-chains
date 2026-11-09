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
import com.soulinchains.arcaneartifacts.fx.Fx;
import com.soulinchains.arcaneartifacts.fx.ModTimelines;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/** Pity: the target's own spells are weakened like two Dampen (four with Amplify) for a while. */
public class EffectPity extends AreaEffect {
    public static final EffectPity INSTANCE = new EffectPity();

    private EffectPity() {
        super(ArcaneArtifacts.id("glyph_pity"), "Pity");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        int ticks = Math.max(100, Config.PITY_SECONDS.get() * 20 + stats.getDurationInTicks() * 20);
        int amplifier = stats.getAmpMultiplier() > 0 ? 1 : 0;
        living.addEffect(new MobEffectInstance(ModEffects.PITY, ticks, amplifier, false, true, true), shooter);
        Fx.burst(level, Fx.timeline(context, ModTimelines.PITY).main, living.position().add(0, living.getBbHeight() / 2, 0));
    }

    @Override
    public int getDefaultManaCost() {
        return 50;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE, AugmentAmplify.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentExtendTime.INSTANCE, "Pity lasts longer.");
        map.put(AugmentDurationDown.INSTANCE, "Pity ends sooner.");
        map.put(AugmentAmplify.INSTANCE, "Deeper pity: like four Dampen instead of two.");
    }

    @Override
    public String getBookDescription() {
        return "Fills the target with pity: every spell it casts is weakened as if it carried two Dampen (four with Amplify) for 20 seconds.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.ABJURATION);
    }
}
