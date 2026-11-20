package com.soulinchains.arcaneartifacts.necro.mob;

import com.soulinchains.arcaneartifacts.necro.ChainBolt;
import com.soulinchains.arcaneartifacts.necro.EffectShackle;
import com.soulinchains.arcaneartifacts.necro.Relics;
import com.soulinchains.arcaneartifacts.necro.Servant;
import com.soulinchains.arcaneartifacts.necro.ShackledSet;
import com.soulinchains.arcaneartifacts.necro.SoulType;
import com.soulinchains.arcaneartifacts.necro.Souls;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Imprisoned King, raised on a Throne of Chains. 600 health (+50% for each more player near), armour 10.
 * <ol>
 *     <li>Chained: while any lock on the pillars stands he cannot be hurt or move; every 5 s his chains lash everyone
 *     within 10 blocks and drag them in, every 12 s he calls wither skeletons.</li>
 *     <li>Half-free (locks broken): walks and fights; every 6 s drags his target to him and shackles it.</li>
 *     <li>Free (30% health): flies, throws chains, tears your servants' chains apart, and executes the arena - at once
 *     and every 15 s: 10 damage and shackles on everyone within 24 blocks.</li>
 * </ol>
 * Slain: the Scythe of the Reaper, the King's Crown, two great souls and a relic.
 */
public class ImprisonedKing extends Monster {
    public static final int RISING_TICKS = 200;
    public static final float FREE_SHARE = 0.3F;
    public static final float LASH_DAMAGE = 6F;
    public static final float EXECUTION_DAMAGE = 10F;
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.arcane_artifacts.imprisoned_king"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_20);
    private final List<BlockPos> locks = new ArrayList<>();
    private int rising;
    private int lashCooldown = 100;
    private int callCooldown = 160;
    private int dragCooldown = 80;
    private int executeCooldown;
    private int throwCooldown = 40;
    private int tearCooldown = 100;
    private boolean freed;

    public ImprisonedKing(EntityType<? extends ImprisonedKing> type, Level level) {
        super(type, level);
        xpReward = 250;
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        setDropChance(EquipmentSlot.MAINHAND, 0F);
        setDropChance(EquipmentSlot.HEAD, 0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.ATTACK_DAMAGE, 12)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Called by the throne: the pillar tops he is chained to. */
    public void chainTo(List<BlockPos> lockPositions) {
        locks.clear();
        locks.addAll(lockPositions);
        rising = RISING_TICKS;
    }

    /** +50% health for each player beyond the first within 32 blocks. */
    public void scaleForPlayers() {
        int players = level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(32), p -> !p.isSpectator()).size();
        var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null && players > 1) {
            health.addOrReplacePermanentModifier(new AttributeModifier(com.soulinchains.arcaneartifacts.ArcaneArtifacts.id("king_party"),
                    0.5 * (players - 1), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        setHealth(getMaxHealth());
    }

    public int locksLeft() {
        int left = 0;
        for (BlockPos lock : locks) {
            if (level().getBlockState(lock).is(ModBlocks.CHAIN_LOCK.get())) {
                left++;
            }
        }
        return left;
    }

    /** 0 rising, 1 chained, 2 half-free, 3 free. */
    public int phase() {
        if (rising > 0) {
            return 0;
        }
        if (locksLeft() > 0) {
            return 1;
        }
        return freed || getHealth() <= getMaxHealth() * FREE_SHARE ? 3 : 2;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && phase() <= 1) {
            if (level() instanceof ServerLevel level && source.getEntity() != null) {
                level.playSound(null, blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.HOSTILE, 1.5F, 0.5F);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 2, getZ(), 10, 0.5, 0.8, 0.5, 0.1);
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        bossBar.setProgress(getHealth() / getMaxHealth());
        int phase = phase();
        if (phase <= 1) {
            getNavigation().stop();
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        }
        if (phase == 0) {
            rising--;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + level.random.nextDouble() * 4, getZ(), 6, 0.6, 0.2, 0.6, 0.02);
            if (rising == 0) {
                level.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0F, 0.5F);
            }
            return;
        }
        LivingEntity target = getTarget();
        if (phase == 1) {
            if (tickCount % 10 == 0) {
                drawChains(level);
            }
            if (--lashCooldown <= 0) {
                lash(level);
                lashCooldown = 100;
            }
            if (--callCooldown <= 0) {
                callGuards(level);
                callCooldown = 240;
            }
            return;
        }
        if (phase == 2) {
            if (--dragCooldown <= 0 && target != null && target.isAlive() && distanceTo(target) < 20) {
                drag(target, 1.0);
                EffectShackle.shackle(level, target, 40, this);
                dragCooldown = 120;
            }
            return;
        }
        // phase 3: free
        if (!freed) {
            freed = true;
            setNoGravity(true);
            bossBar.setColor(BossEvent.BossBarColor.RED);
            level.playSound(null, blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, 0.5F);
            executeCooldown = 0;
        }
        hover(target);
        if (--executeCooldown <= 0) {
            executeArena(level);
            executeCooldown = 300;
        }
        if (--throwCooldown <= 0 && target != null && target.isAlive() && hasLineOfSight(target)) {
            throwChain(target);
            throwCooldown = 50;
        }
        if (--tearCooldown <= 0) {
            tearServants(level);
            tearCooldown = 200;
        }
    }

    private void drawChains(ServerLevel level) {
        Vec3 from = position().add(0, 2.5, 0);
        for (BlockPos lock : locks) {
            if (!level.getBlockState(lock).is(ModBlocks.CHAIN_LOCK.get())) {
                continue;
            }
            Vec3 to = Vec3.atCenterOf(lock);
            for (int i = 1; i < 12; i++) {
                Vec3 at = from.lerp(to, i / 12.0);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private static double pullOf(LivingEntity target) {
        return ShackledSet.wearsBoots(target) ? 0.4 : 1.0;
    }

    private void drag(LivingEntity target, double strength) {
        Vec3 pull = position().subtract(target.position()).normalize().scale(1.4 * strength * pullOf(target));
        target.setDeltaMovement(target.getDeltaMovement().add(pull.x, 0.4 * strength, pull.z));
        target.hurtMarked = true;
        level().playSound(null, target.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 1.2F, 0.5F);
    }

    /** Chained: the chains whip around him. */
    public int lash(ServerLevel level) {
        List<Player> hit = level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(10), p -> p.isAlive() && !p.isCreative() && !p.isSpectator());
        for (Player player : hit) {
            player.hurt(damageSources().indirectMagic(this, this), LASH_DAMAGE);
            drag(player, 0.6);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1, getZ(), 12, 4, 0.5, 4, 0);
        level.playSound(null, blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 2.0F, 0.4F);
        return hit.size();
    }

    private void callGuards(ServerLevel level) {
        int near = level.getEntitiesOfClass(WitherSkeleton.class, getBoundingBox().inflate(16)).size();
        for (int i = 0; i < 2 && near + i < 6; i++) {
            WitherSkeleton guard = EntityType.WITHER_SKELETON.create(level);
            if (guard != null) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                guard.moveTo(getX() + Math.cos(angle) * 4, getY(), getZ() + Math.sin(angle) * 4, level.random.nextFloat() * 360, 0);
                guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
                guard.setTarget(getTarget());
                level.addFreshEntity(guard);
                level.sendParticles(ParticleTypes.SOUL, guard.getX(), guard.getY() + 1, guard.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
            }
        }
    }

    private void hover(LivingEntity target) {
        Vec3 goal = target != null ? target.position().add(0, 4, 0) : position();
        Vec3 delta = goal.subtract(position());
        Vec3 move = delta.lengthSqr() > 1 ? delta.normalize().scale(0.18) : Vec3.ZERO;
        setDeltaMovement(getDeltaMovement().scale(0.6).add(move.scale(0.4)));
        if (target != null) {
            getLookControl().setLookAt(target, 30F, 30F);
        }
    }

    /** Free: every player within 24 blocks takes the chains' judgement. */
    public int executeArena(ServerLevel level) {
        List<Player> hit = level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(24), p -> p.isAlive() && !p.isCreative() && !p.isSpectator());
        for (Player player : hit) {
            player.invulnerableTime = 0;
            player.hurt(damageSources().indirectMagic(this, this), EXECUTION_DAMAGE);
            EffectShackle.shackle(level, player, 60, this);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 20, 0.3, 0.6, 0.3, 0.2);
        }
        level.playSound(null, blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2.0F, 0.4F);
        return hit.size();
    }

    private void throwChain(LivingEntity target) {
        double dx = target.getX() - getX();
        double dy = target.getEyeY() - 0.3 - getEyeY();
        double dz = target.getZ() - getZ();
        setYRot((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F);
        setXRot((float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG));
        level().addFreshEntity(new ChainBolt(level(), this, null, false, 1.8F));
        level().playSound(null, blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 1.0F, 0.6F);
    }

    /** Free: he tears the chains of up to two servants near him; they crumble and their souls go home. */
    public int tearServants(ServerLevel level) {
        List<Servant> servants = level.getEntitiesOfClass(Servant.class, getBoundingBox().inflate(16)).stream()
                .sorted(java.util.Comparator.comparingDouble(s -> s.distanceToSqr(this))).limit(2).toList();
        for (Servant servant : servants) {
            if (servant.getOwner() instanceof Player master) {
                master.displayClientMessage(Component.translatable("arcane_artifacts.imprisoned_king.tears"), true);
            }
            servant.crumble(true);
        }
        return servants.size();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Souls.spawnWisp(level, this, SoulType.GREAT);
        Souls.spawnWisp(level, this, SoulType.GREAT);
        drop(level, new ItemStack(ModItems.SCYTHE_OF_THE_REAPER.get()));
        drop(level, new ItemStack(ModItems.KINGS_CROWN.get()));
        drop(level, Relics.random(random));
        for (BlockPos lock : locks) {
            if (level.getBlockState(lock).is(ModBlocks.CHAIN_LOCK.get())) {
                level.setBlock(lock, ModBlocks.CHAIN_PILLAR.get().defaultBlockState(), 3);
            }
        }
        level.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 2, getZ(), 120, 1.0, 1.5, 1.0, 0.1);
    }

    private void drop(ServerLevel level, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, getX(), getY() + 1, getZ(), stack);
        item.setUnlimitedLifetime();
        level.addFreshEntity(item);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
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
    protected boolean canRide(net.minecraft.world.entity.Entity vehicle) {
        return false;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_AMBIENT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        rising = tag.getInt("Rising");
        freed = tag.getBoolean("Freed");
        setNoGravity(freed);
        locks.clear();
        ListTag list = tag.getList("Locks", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < list.size(); i++) {
            int[] xyz = list.getIntArray(i);
            if (xyz.length == 3) {
                locks.add(new BlockPos(xyz[0], xyz[1], xyz[2]));
            }
        }
        if (hasCustomName()) {
            bossBar.setName(getDisplayName());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Rising", rising);
        tag.putBoolean("Freed", freed);
        ListTag list = new ListTag();
        for (BlockPos lock : locks) {
            list.add(NbtUtils.writeBlockPos(lock));
        }
        tag.put("Locks", list);
    }

}
