package com.soulinchains.arcaneartifacts.necro.mob;

import com.soulinchains.arcaneartifacts.necro.EffectShackle;
import com.soulinchains.arcaneartifacts.necro.Necro;
import com.soulinchains.arcaneartifacts.necro.SoulType;
import com.soulinchains.arcaneartifacts.necro.Souls;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The Jailer: the keeper of a Soul Dungeon. Every few seconds he shackles whoever he fights and hits shackled prey twice
 * as hard; at half health he calls Chain Ghosts. Slain, he leaves a Great Soul and the Shackle Key, and every Overseer
 * who fought him rises to Chainlord.
 */
public class Jailer extends Monster {
    public static final int SHACKLE_TICKS = 160;
    public static final float SHACKLED_BONUS = 2.0F;
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.arcane_artifacts.jailer"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int shackleCooldown = 60;
    private boolean calledGhosts;

    public Jailer(EntityType<? extends Jailer> type, Level level) {
        super(type, level);
        xpReward = 80;
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        setDropChance(EquipmentSlot.MAINHAND, 0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 160)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.ATTACK_DAMAGE, 9)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        bossBar.setProgress(getHealth() / getMaxHealth());
        LivingEntity target = getTarget();
        if (--shackleCooldown <= 0 && target != null && target.isAlive() && distanceTo(target) < 12) {
            EffectShackle.shackle(level, target, 50, this);
            level.playSound(null, blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1.5F, 0.4F);
            shackleCooldown = SHACKLE_TICKS;
        }
        if (!calledGhosts && getHealth() < getMaxHealth() / 2) {
            calledGhosts = true;
            for (int i = 0; i < 2; i++) {
                ChainGhost ghost = ModEntities.CHAIN_GHOST.get().create(level);
                if (ghost != null) {
                    ghost.moveTo(getX() + (i == 0 ? 2 : -2), getY() + 2, getZ(), 0, 0);
                    ghost.setTarget(target);
                    level.addFreshEntity(ghost);
                }
            }
            level.playSound(null, blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.2F, 0.6F);
        }
    }

    /** Shackled prey is hit twice as hard. */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity living && living.hasEffect(ModEffects.SHACKLED)) {
            float base = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
            living.hurt(damageSources().mobAttack(this), base * (SHACKLED_BONUS - 1F));
        }
        return super.doHurtTarget(target);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Souls.spawnWisp(level, this, SoulType.GREAT);
        level.addFreshEntity(new ItemEntity(level, getX(), getY() + 1, getZ(), new ItemStack(ModItems.SHACKLE_KEY.get())));
        if (random.nextFloat() < 0.35F) {
            level.addFreshEntity(new ItemEntity(level, getX(), getY() + 1, getZ(), new ItemStack(ModItems.EXECUTIONER_AXE.get())));
        }
        java.util.Set<Player> witnesses = new java.util.LinkedHashSet<>(level.getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(32)));
        if (source.getEntity() instanceof Player killer) {
            witnesses.add(killer);
        }
        for (Player player : witnesses) {
            if (Necro.rank(player) == Necro.OVERSEER) {
                Necro.setRank(player, Necro.CHAINLORD);
                player.sendSystemMessage(Component.translatable("arcane_artifacts.necro.chainlord"));
            }
        }
        level.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1.5, getZ(), 60, 0.8, 1.0, 0.8, 0.08);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        calledGhosts = tag.getBoolean("CalledGhosts");
        if (hasCustomName()) {
            bossBar.setName(getDisplayName());
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("CalledGhosts", calledGhosts);
    }
}
