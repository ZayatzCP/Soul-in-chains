package com.soulinchains.arcaneartifacts.spore;

import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/**
 * A cloud of Mycelinoid spores (Mushroom Shield). Every half second: friends (the owner, players,
 * Mycelinoids) get Speed; foes (monsters and whoever is after a friend) take spore damage and catch the
 * Spore Infection.
 */
public class SporeCloud extends AreaEffectCloud {
    public static final float RADIUS = 3.0F;
    public static final int DURATION = 80;
    public static final float DAMAGE = 2.0F;

    public SporeCloud(EntityType<? extends SporeCloud> type, Level level) {
        super(type, level);
    }

    public static SporeCloud spawn(ServerLevel level, LivingEntity owner) {
        SporeCloud cloud = new SporeCloud(ModEntities.SPORE_CLOUD.get(), level);
        cloud.setPos(owner.getX(), owner.getY(), owner.getZ());
        cloud.setOwner(owner);
        cloud.setRadius(RADIUS);
        cloud.setRadiusPerTick(0F);
        cloud.setDuration(DURATION);
        cloud.setWaitTime(0);
        cloud.setParticle(new DustParticleOptions(new Vector3f(0.72F, 0.6F, 0.32F), 1.6F));
        level.addFreshEntity(cloud);
        return cloud;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && this.tickCount % 10 == 1) {
            pulse();
        }
    }

    /** One breath of the cloud over everything inside it. */
    public void pulse() {
        LivingEntity owner = getOwner();
        float radius = getRadius();
        AABB area = getBoundingBox().inflate(radius, 1.5, radius);
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, area)) {
            double dx = entity.getX() - getX();
            double dz = entity.getZ() - getZ();
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            if (Spores.isFriend(owner, entity)) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, true, true));
            } else if (Spores.isFoe(owner, entity)) {
                entity.hurt(damageSources().indirectMagic(this, owner), DAMAGE);
                Spores.infect(entity, Spores.INFECTION_TICKS);
            }
        }
    }
}
