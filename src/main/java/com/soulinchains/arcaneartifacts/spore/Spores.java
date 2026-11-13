package com.soulinchains.arcaneartifacts.spore;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.entity.Gribushka;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Mycelinoid spores. Living things with a nose and lungs catch the Spore Infection and sneeze; a sneeze
 * passes the infection to other hostile creatures close by. Creatures without a nose or lungs (skeletons,
 * blazes, golems, slimes... the entity type tag {@code arcane_artifacts:cannot_sneeze}) cannot catch it,
 * and the Mycelinoids themselves never do: the spores are theirs.
 */
public final class Spores {
    public static final TagKey<EntityType<?>> CANNOT_SNEEZE = TagKey.create(Registries.ENTITY_TYPE, ArcaneArtifacts.id("cannot_sneeze"));
    public static final int INFECTION_TICKS = 240;
    private static final double SPREAD_RANGE = 3.0;

    private Spores() {
    }

    /** The Mycelinoid folk: immune to their own spores, and friends of the spore cloud. */
    public static boolean isMycelial(LivingEntity entity) {
        return entity instanceof Gribushka;
    }

    /** Whether it has a nose and lungs to catch the spores with. */
    public static boolean canCatch(LivingEntity entity) {
        return entity.isAlive() && !isMycelial(entity) && !(entity instanceof ArmorStand) && !entity.getType().is(CANNOT_SNEEZE)
                && !(entity instanceof Player player && (player.isCreative() || player.isSpectator()));
    }

    /** Infects it (if it can catch the spores); returns whether it is now infected. */
    public static boolean infect(LivingEntity entity, int ticks) {
        if (!canCatch(entity)) {
            return false;
        }
        MobEffectInstance current = entity.getEffect(ModEffects.SPORE_INFECTION);
        if (current == null || current.getDuration() < ticks) {
            entity.addEffect(new MobEffectInstance(ModEffects.SPORE_INFECTION, ticks, 0, false, true, true));
        }
        return true;
    }

    /** Whether the cloud of {@code owner} should treat {@code entity} as a friend. */
    public static boolean isFriend(LivingEntity owner, LivingEntity entity) {
        return entity == owner || isMycelial(entity) || entity instanceof Player || (owner == null && entity instanceof Player);
    }

    /** Whether the cloud of {@code owner} should hurt {@code entity}: monsters and whoever is after a friend. */
    public static boolean isFoe(LivingEntity owner, LivingEntity entity) {
        if (isFriend(owner, entity) || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof Enemy) {
            return true;
        }
        if (owner != null && (owner.getLastHurtByMob() == entity || (owner instanceof Mob mob && mob.getTarget() == entity))) {
            return true;
        }
        return entity instanceof Mob mob && mob.getTarget() != null && isFriend(owner, mob.getTarget());
    }

    /**
     * A sneeze: a puff in front of the face, a moment of being stunned and a little hurt, and the spores fly
     * to other hostile creatures nearby.
     */
    public static void sneeze(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 look = entity.getLookAngle();
        Vec3 nose = entity.getEyePosition().add(look.scale(0.4));
        level.sendParticles(ParticleTypes.SNEEZE, nose.x, nose.y, nose.z, 6, 0.1, 0.05, 0.1, 0.05);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, nose.x, nose.y, nose.z, 8, 0.4, 0.3, 0.4, 0.01);
        float pitch = 0.9F + entity.getRandom().nextFloat() * 0.3F + (entity.getBbHeight() < 1.0F ? 0.4F : 0F);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PANDA_SNEEZE, SoundSource.HOSTILE, 1.0F, pitch);
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 4, false, false, false));
        entity.push(-look.x * 0.25, 0.05, -look.z * 0.25);
        entity.hurt(level.damageSources().magic(), 1.0F);
        MobEffectInstance infection = entity.getEffect(ModEffects.SPORE_INFECTION);
        int passed = Math.max(100, infection == null ? INFECTION_TICKS / 2 : infection.getDuration() / 2);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(SPREAD_RANGE),
                other -> other != entity && other instanceof Enemy && !other.hasEffect(ModEffects.SPORE_INFECTION))) {
            infect(other, passed);
        }
    }
}
