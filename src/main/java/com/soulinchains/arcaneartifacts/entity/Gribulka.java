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
    /** Ticks left for a Gribulka summoned by the Druid's Swarm (a decoy); 0 for a wild one. */
    private int decoyTicks;

    public Gribulka(EntityType<? extends Gribulka> type, Level level) {
        super(type, level);
    }

    /** Turns this Gribulka into a decoy that draws monsters for the given time, then leaves. */
    public void makeDecoy(int ticks) {
        decoyTicks = ticks;
    }

    public boolean isDecoy() {
        return decoyTicks > 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (decoyTicks > 0 && level() instanceof net.minecraft.server.level.ServerLevel server) {
            if (--decoyTicks <= 0) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 0.3, getZ(), 10, 0.3, 0.3, 0.3, 0);
                discard();
                return;
            }
            if (tickCount % 10 == 0) {
                taunt(server);
            }
        }
    }

    /** Every monster within 10 blocks turns on the decoy. */
    public int taunt(net.minecraft.server.level.ServerLevel level) {
        int taunted = 0;
        for (net.minecraft.world.entity.monster.Monster monster : level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                getBoundingBox().inflate(10))) {
            if (monster.getTarget() == null || !(monster.getTarget() instanceof Gribulka g && g.isDecoy())) {
                monster.setTarget(this);
                taunted++;
            }
        }
        return taunted;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isDecoy() && super.removeWhenFarAway(distance);
    }

    @Override
    public boolean shouldDropExperience() {
        return !isDecoy() && super.shouldDropExperience();
    }

    @Override
    protected boolean shouldDropLoot() {
        return !isDecoy() && super.shouldDropLoot();
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DecoyTicks", decoyTicks);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        decoyTicks = tag.getInt("DecoyTicks");
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
