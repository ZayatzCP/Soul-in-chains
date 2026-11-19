package com.soulinchains.arcaneartifacts.necro;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.glyph.AreaEffect;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Shackle: chains rise from the ground and hold the target on the spot for 3 seconds (+1 per Extend Time): it can
 * neither walk, jump nor teleport. Bosses are held half as long and at most once every 20 seconds.
 */
public class EffectShackle extends AreaEffect {
    public static final EffectShackle INSTANCE = new EffectShackle();
    public static final int BASE_TICKS = 60;
    public static final int BOSS_COOLDOWN_TICKS = 400;
    private static final String BOSS_COOLDOWN_TAG = ArcaneArtifacts.MODID + ":shackle_cooldown";

    private EffectShackle() {
        super(ArcaneArtifacts.id("glyph_shackle"), "Shackle");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        int ticks = Math.max(20, BASE_TICKS + (int) Math.round(stats.getDurationMultiplier() * 20));
        if (target instanceof LivingEntity living && living != shooter) {
            shackle(level, living, ticks, shooter);
        }
        if (stats.getAoeMultiplier() > 0) {
            double radius = 1.5 + stats.getAoeMultiplier();
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius))) {
                if (living != shooter && living != target) {
                    shackle(level, living, ticks, shooter);
                }
            }
        }
    }

    /** Chains the creature to the ground. Returns false when a boss is still shaking off the last shackle. */
    public static boolean shackle(ServerLevel level, LivingEntity living, int ticks, @Nullable LivingEntity source) {
        if (living.getType().is(Tags.EntityTypes.BOSSES)) {
            long now = level.getGameTime();
            if (living.getPersistentData().getLong(BOSS_COOLDOWN_TAG) > now) {
                return false;
            }
            living.getPersistentData().putLong(BOSS_COOLDOWN_TAG, now + BOSS_COOLDOWN_TICKS);
            ticks = Math.max(10, ticks / 2);
        }
        living.addEffect(new MobEffectInstance(ModEffects.SHACKLED, ticks, 0, false, true, true), source);
        living.setDeltaMovement(0, Math.min(0, living.getDeltaMovement().y), 0);
        living.hurtMarked = true;
        ShackleAnchor anchor = new ShackleAnchor(ModEntities.SHACKLE_ANCHOR.get(), level);
        anchor.hold(living, ticks);
        level.addFreshEntity(anchor);
        level.playSound(null, living.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    @Override
    public int getDefaultManaCost() {
        return 30;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.ONE;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE, AugmentAOE.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentExtendTime.INSTANCE, "The chains hold one second longer.");
        map.put(AugmentDurationDown.INSTANCE, "The chains let go sooner.");
        map.put(AugmentAOE.INSTANCE, "Chains every creature around the point.");
    }

    @Override
    public String getBookDescription() {
        return "Chains rise from the ground and hold the target on the spot for 3 seconds: it can neither walk, jump nor teleport. Bosses are held half as long, at most once every 20 seconds.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.NECROMANCY);
    }
}
