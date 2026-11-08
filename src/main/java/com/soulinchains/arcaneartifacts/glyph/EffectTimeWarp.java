package com.soulinchains.arcaneartifacts.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;

/**
 * Time Warp: around the point of impact, time crawls for everyone but the caster. Creatures are slowed
 * heavily (and work slowly), and projectiles in flight lose most of their speed.
 */
public class EffectTimeWarp extends AreaEffect {
    public static final EffectTimeWarp INSTANCE = new EffectTimeWarp();

    private EffectTimeWarp() {
        super(ArcaneArtifacts.id("glyph_time_warp"), "Time Warp");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        double radius = Math.max(2.0, Config.TIME_WARP_RADIUS.get() + 2.0 * stats.getAoeMultiplier());
        int ticks = Math.max(20, Config.TIME_WARP_SECONDS.get() * 20 + stats.getDurationInTicks() * 2);
        int amplifier = Math.max(0, 3 + (int) Math.round(stats.getAmpMultiplier()));
        AABB area = new AABB(point, point).inflate(radius);
        for (Entity entity : level.getEntities(shooter, area, e -> e.isAlive() && e.position().distanceTo(point) <= radius)) {
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amplifier), shooter);
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 2), shooter);
            } else if (entity instanceof Projectile projectile) {
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.25));
                projectile.hurtMarked = true;
            }
        }
        com.soulinchains.arcaneartifacts.fx.Fx.area(level, com.soulinchains.arcaneartifacts.fx.Fx.timeline(context, com.soulinchains.arcaneartifacts.fx.ModTimelines.TIME_WARP).main, point, radius);
        level.playSound(null, point.x, point.y, point.z, net.minecraft.sounds.SoundEvents.BELL_RESONATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.5f);
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
        return augmentSetOf(AugmentAOE.INSTANCE, AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE, AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAOE.INSTANCE, "A wider bubble of slow time.");
        map.put(AugmentAmplify.INSTANCE, "Time crawls even slower.");
        map.put(AugmentDampen.INSTANCE, "Time is only a little slower.");
        map.put(AugmentExtendTime.INSTANCE, "The warp lasts longer.");
        map.put(AugmentDurationDown.INSTANCE, "The warp ends sooner.");
    }

    @Override
    public String getBookDescription() {
        return "Slows time around the place the spell lands for everyone except the caster: creatures become very slow, and projectiles in flight lose most of their speed.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.MANIPULATION);
    }
}
