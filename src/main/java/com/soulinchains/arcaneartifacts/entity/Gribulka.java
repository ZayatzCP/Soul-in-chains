package com.soulinchains.arcaneartifacts.entity;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Gribulka: a little mushroom on stubby legs that waddles around the Mushroom Caves, shy of players,
 * usually not far from the walking hive it came out of.
 */
public class Gribulka extends PathfinderMob {
    /** A young Gribulka (the designer's Gribulka Jr): it grows up after {@link #GROW_TICKS}. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> JUNIOR =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Gribulka.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    public static final int GROW_TICKS = 12000;
    private int growTicks;

    public Gribulka(EntityType<? extends Gribulka> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(JUNIOR, false);
    }

    public boolean isJunior() {
        return entityData.get(JUNIOR);
    }

    public void setJunior(boolean junior) {
        entityData.set(JUNIOR, junior);
        growTicks = junior ? GROW_TICKS : 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && isJunior() && --growTicks <= 0) {
            setJunior(false);   // grown up
        }
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
                                                                  MobSpawnType spawnType, @Nullable net.minecraft.world.entity.SpawnGroupData data) {
        if (spawnType != MobSpawnType.SPAWN_EGG && level.getRandom().nextInt(4) == 0) {
            setJunior(true);
        }
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Junior", isJunior());
        tag.putInt("GrowTicks", growTicks);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(JUNIOR, tag.getBoolean("Junior"));
        growTicks = tag.getInt("GrowTicks");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.28);
    }

    public static boolean checkSpawnRules(EntityType<Gribulka> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return checkMobSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 5.0F, 1.0, 1.4, player -> !player.isCrouching()));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public float getVoicePitch() {
        return 1.8F + getRandom().nextFloat() * 0.2F;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.FROG_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }
}
