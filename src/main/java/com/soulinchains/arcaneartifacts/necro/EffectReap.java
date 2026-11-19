package com.soulinchains.arcaneartifacts.necro;

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
import com.soulinchains.arcaneartifacts.glyph.AreaEffect;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Reap: marks a creature for 5 seconds (+2 per Extend Time). If it dies marked, its soul goes straight into the
 * caster's Soul Lantern, whoever dealt the last blow. With Amplify a weak soul comes out cursed.
 */
public class EffectReap extends AreaEffect {
    public static final EffectReap INSTANCE = new EffectReap();
    public static final int BASE_TICKS = 100;

    private EffectReap() {
        super(ArcaneArtifacts.id("glyph_reap"), "Reap");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        if (!(target instanceof LivingEntity living) || living == shooter || living instanceof Player) {
            return;
        }
        int ticks = Math.max(20, BASE_TICKS + (int) Math.round(stats.getDurationMultiplier() * 40));
        living.addEffect(new MobEffectInstance(ModEffects.REAP_MARK, ticks, 0, false, true, true), shooter);
        Souls.markForReaping(living, shooter.getUUID(), stats.getAmpMultiplier() > 0);
        level.sendParticles(ParticleTypes.SCULK_SOUL, living.getX(), living.getY() + living.getBbHeight(), living.getZ(), 6, 0.2, 0.1, 0.2, 0.02);
        level.playSound(null, living.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 0.5F);
    }

    @Override
    public int getDefaultManaCost() {
        return 20;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.ONE;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE, AugmentAmplify.INSTANCE);
    }

    @Override
    protected void addDefaultAugmentLimits(Map<net.minecraft.resources.ResourceLocation, Integer> defaults) {
        super.addDefaultAugmentLimits(defaults);
        defaults.put(AugmentAmplify.INSTANCE.getRegistryName(), 1);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentExtendTime.INSTANCE, "The mark lasts two seconds longer.");
        map.put(AugmentDurationDown.INSTANCE, "The mark fades sooner.");
        map.put(AugmentAmplify.INSTANCE, "A weak soul (wandering, beast, drowned) comes out cursed.");
    }

    @Override
    public String getBookDescription() {
        return "Marks a creature for 5 seconds. If it dies marked, its soul flies straight into your Soul Lantern, whoever dealt the last blow.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.NECROMANCY);
    }
}
