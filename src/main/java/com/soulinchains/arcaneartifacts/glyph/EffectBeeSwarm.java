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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bee Swarm (Root Magic): three angry bees (+1 per Amplify) burst out and go for the creature the spell hit
 * (on a block: the nearest monster within 8 blocks). After 15 seconds (+5 per Extend Time) they fly off.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public class EffectBeeSwarm extends AreaEffect {
    public static final EffectBeeSwarm INSTANCE = new EffectBeeSwarm();
    public static final int BASE_TICKS = 300;
    /** Ticks left for a summoned bee, kept in its persistent data. */
    public static final String LIFE = ArcaneArtifacts.MODID + ":swarm_ticks";

    private EffectBeeSwarm() {
        super(ArcaneArtifacts.id("glyph_bee_swarm"), "Bee Swarm");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        int count = 3 + Math.max(0, (int) Math.round(stats.getAmpMultiplier()));
        int ticks = Math.max(60, BASE_TICKS + (int) Math.round(stats.getDurationMultiplier() * 100));
        LivingEntity victim = target instanceof LivingEntity living && living != shooter ? living : nearestMonster(level, point, shooter);
        summon(level, point, victim, count, ticks);
    }

    @Nullable
    private static LivingEntity nearestMonster(ServerLevel level, Vec3 point, LivingEntity shooter) {
        return level.getEntitiesOfClass(Monster.class, new AABB(point, point).inflate(8), m -> m.isAlive() && m != shooter).stream()
                .min(Comparator.comparingDouble(m -> m.distanceToSqr(point))).orElse(null);
    }

    /** Summons angry bees around a point, all set on the victim (if any). */
    public static List<Bee> summon(ServerLevel level, Vec3 at, @Nullable LivingEntity victim, int count, int ticks) {
        List<Bee> swarm = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Bee bee = EntityType.BEE.create(level);
            if (bee == null) {
                continue;
            }
            double angle = Math.PI * 2 * i / count;
            bee.moveTo(at.x + Math.cos(angle) * 0.8, at.y + 0.6, at.z + Math.sin(angle) * 0.8, level.random.nextFloat() * 360F, 0F);
            bee.getPersistentData().putInt(LIFE, ticks);
            level.addFreshEntity(bee);
            if (victim != null) {
                sic(bee, victim, ticks);
            }
            level.sendParticles(ParticleTypes.WAX_ON, bee.getX(), bee.getY(), bee.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
            swarm.add(bee);
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.PLAYERS, 1.0f, 1.0f);
        return swarm;
    }

    /** Sets a bee on a creature. */
    public static void sic(Bee bee, LivingEntity victim, int ticks) {
        bee.setPersistentAngerTarget(victim.getUUID());
        bee.setRemainingPersistentAngerTime(ticks);
        bee.setTarget(victim);
    }

    /** Summoned bees fly off when their time is up. */
    @SubscribeEvent
    public static void tickBee(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Bee bee) || bee.level().isClientSide || !bee.getPersistentData().contains(LIFE)) {
            return;
        }
        int left = bee.getPersistentData().getInt(LIFE) - 1;
        bee.getPersistentData().putInt(LIFE, left);
        if (left <= 0) {
            ((ServerLevel) bee.level()).sendParticles(ParticleTypes.WAX_OFF, bee.getX(), bee.getY(), bee.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
            bee.discard();
        }
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
        return augmentSetOf(AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentAmplify.INSTANCE, "One more bee.");
        map.put(AugmentExtendTime.INSTANCE, "The bees stay 5 seconds longer.");
        map.put(AugmentDurationDown.INSTANCE, "The bees leave sooner.");
    }

    @Override
    public String getBookDescription() {
        return "Three angry bees burst out and go for the creature the spell hit.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.CONJURATION);
    }
}
