package com.soulinchains.arcaneartifacts.entity;

import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Gribulka Hive: an old hollow stump overgrown with shelf fungi that walks the Mushroom Caves on four legs.
 * Gribulkas live inside: now and then one comes out, and when the hive is hit, two run out at once.
 * It never fights back. It is very hard to push.
 */
public class GribulkaHive extends PathfinderMob {
    public static final int RELEASE_INTERVAL = 600;
    public static final int MAX_AROUND = 4;
    private static final int RELEASE_ANIMATION = 20;
    private static final EntityDataAccessor<Integer> RELEASED_AT = SynchedEntityData.defineId(GribulkaHive.class, EntityDataSerializers.INT);

    public GribulkaHive(EntityType<? extends GribulkaHive> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.12)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public static boolean checkSpawnRules(EntityType<GribulkaHive> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return checkMobSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RELEASED_AT, -1000);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    /** 0..1: the trunk leans as a Gribulka climbs out (client animation). */
    public float releaseAnimation(float partialTick) {
        float age = this.tickCount + partialTick - this.entityData.get(RELEASED_AT);
        return age < 0 || age > RELEASE_ANIMATION ? 0F : (float) Math.sin(age / RELEASE_ANIMATION * Math.PI);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % RELEASE_INTERVAL == 0 && level() instanceof ServerLevel server
                && server.getEntitiesOfClass(Gribulka.class, getBoundingBox().inflate(12.0)).size() < MAX_AROUND) {
            release(server);
        }
    }

    /** One Gribulka climbs out of the hive. */
    public @Nullable Gribulka release(ServerLevel server) {
        Gribulka gribulka = ModEntities.GRIBULKA.get().create(server);
        if (gribulka == null) {
            return null;
        }
        double angle = getRandom().nextDouble() * Math.PI * 2;
        gribulka.moveTo(getX() + Math.cos(angle) * 1.2, getY() + 0.5, getZ() + Math.sin(angle) * 1.2, getRandom().nextFloat() * 360F, 0F);
        gribulka.finalizeSpawn(server, server.getCurrentDifficultyAt(blockPosition()), MobSpawnType.BREEDING, null);
        server.addFreshEntity(gribulka);
        server.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1.5, getZ(), 12, 0.5, 0.5, 0.5, 0.02);
        playSound(SoundEvents.BEEHIVE_EXIT, 1.0F, 0.6F);
        this.entityData.set(RELEASED_AT, this.tickCount);
        return gribulka;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && level() instanceof ServerLevel server && getRandom().nextInt(3) == 0) {
            release(server);
            release(server);
        }
        return hurt;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.BEEHIVE_WORK;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
