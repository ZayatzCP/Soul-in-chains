package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
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

/** Satiety: the target is fed and stays full (Well Fed) for a while. */
public class EffectSatiety extends AreaEffect {
    public static final EffectSatiety INSTANCE = new EffectSatiety();

    private EffectSatiety() {
        super(ArcaneArtifacts.id("glyph_satiety"), "Satiety");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        LivingEntity fed = target instanceof LivingEntity living ? living : shooter;
        int ticks = Math.max(200, Config.SATIETY_SECONDS.get() * 20 + stats.getDurationInTicks() * 30);
        fed.addEffect(new MobEffectInstance(ModEffects.WELL_FED, ticks, 0, false, true, true), shooter);
        Fx.burst(level, Fx.timeline(context, ModTimelines.SATIETY).main, fed.position().add(0, fed.getBbHeight() / 2, 0));
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
        return augmentSetOf(AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentExtendTime.INSTANCE, "Stay full for longer.");
        map.put(AugmentDurationDown.INSTANCE, "Hunger returns sooner.");
    }

    @Override
    public String getBookDescription() {
        return "Fills the target's hunger and keeps it full (Well Fed) for a minute. Cast on a block, it feeds the caster.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.CONJURATION);
    }
}
