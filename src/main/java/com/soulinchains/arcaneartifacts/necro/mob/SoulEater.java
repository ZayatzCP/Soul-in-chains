package com.soulinchains.arcaneartifacts.necro.mob;

import com.soulinchains.arcaneartifacts.necro.SoulType;
import com.soulinchains.arcaneartifacts.necro.Souls;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Soul Eater: it comes at night for a Shackled whose Corruption runs high. Each hit tears a soul out of the lantern;
 * with souls in its belly it runs, blinks away and soon vanishes for good. Kill it first and every stolen soul comes
 * back twice over.
 */
public class SoulEater extends Monster {
    public static final int MAX_STOLEN = 3;
    /** Ticks it lingers after its first theft before it is gone with the souls. */
    public static final int ESCAPE_TICKS = 600;
    private final List<SoulType> stolen = new ArrayList<>();
    private int escapeLeft = -1;
    private int blinkCooldown;

    public SoulEater(EntityType<? extends SoulEater> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 16.0F, 1.2, 1.5, p -> hasLoot()));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false) {
            @Override
            public boolean canUse() {
                return !hasLoot() && super.canUse();
            }
        });
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean hasLoot() {
        return !stolen.isEmpty();
    }

    public List<SoulType> stolen() {
        return List.copyOf(stolen);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof Player player && stolen.size() < MAX_STOLEN) {
            SoulType taken = steal(player);
            if (taken != null) {
                if (escapeLeft < 0) {
                    escapeLeft = ESCAPE_TICKS;
                }
                player.displayClientMessage(Component.translatable("arcane_artifacts.soul_eater.stole", Component.translatable(taken.translationKey())), true);
                level().playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.0F, 0.5F);
            }
        }
        return hit;
    }

    /** Tears one random soul out of the player's lantern. */
    private SoulType steal(Player player) {
        List<SoulType> carried = new ArrayList<>();
        for (SoulType type : SoulType.values()) {
            if (Souls.count(player, type) > 0) {
                carried.add(type);
            }
        }
        if (carried.isEmpty()) {
            return null;
        }
        SoulType type = carried.get(random.nextInt(carried.size()));
        if (!Souls.take(player, type)) {
            return null;
        }
        stolen.add(type);
        return type;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (hasLoot() && random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.SOUL, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0.03, 0);
            }
            return;
        }
        if (escapeLeft > 0) {
            escapeLeft--;
            if (blinkCooldown-- <= 0 && getTarget() != null && distanceTo(getTarget()) < 8) {
                // Blinks away from whoever chases it.
                for (int i = 0; i < 8; i++) {
                    if (randomTeleport(getX() + (random.nextDouble() - 0.5) * 24, getY() + random.nextInt(8) - 4,
                            getZ() + (random.nextDouble() - 0.5) * 24, true)) {
                        level().playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.6F);
                        break;
                    }
                }
                blinkCooldown = 60;
            }
            if (escapeLeft == 0) {
                // Gone with the souls.
                ((ServerLevel) level()).sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.1);
                discard();
            }
        }
    }

    /** Killed: every stolen soul comes back twice. */
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            for (SoulType type : stolen) {
                Souls.spawnWisp(level, this, type);
                Souls.spawnWisp(level, this, type);
            }
            stolen.clear();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !hasLoot() && super.removeWhenFarAway(distance);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_AMBIENT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Stolen", new IntArrayTag(stolen.stream().mapToInt(Enum::ordinal).toArray()));
        tag.putInt("EscapeLeft", escapeLeft);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        stolen.clear();
        for (int id : tag.getIntArray("Stolen")) {
            stolen.add(SoulType.byOrdinal(id));
        }
        escapeLeft = tag.contains("EscapeLeft") ? tag.getInt("EscapeLeft") : -1;
    }
}
