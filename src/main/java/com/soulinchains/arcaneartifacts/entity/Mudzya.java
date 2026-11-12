package com.soulinchains.arcaneartifacts.entity;

import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.block.MudzyaHiveBlockEntity;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Mudzya: a small purple flying creature of the beaches, about the size of a bee. Every couple of
 * minutes it fills up with Source (its belly starts to glow). A full Mudzya either carries the Source to
 * a nearby Mudzya Hive, or lets a player collect it in a glass bottle. Tempted and bred with
 * sourceberries.
 */
public class Mudzya extends Animal implements FlyingAnimal {
    private static final EntityDataAccessor<Boolean> CHARGED = SynchedEntityData.defineId(Mudzya.class, EntityDataSerializers.BOOLEAN);

    private int chargeTimer;
    private @Nullable BlockPos hivePos;
    /** The hive this Mudzya lives at; it never strays more than {@link #HOME_RANGE} blocks from it. */
    private @Nullable BlockPos home;
    public static final int HOME_RANGE = 8;

    public Mudzya(EntityType<? extends Mudzya> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 16.0F);
        this.chargeTimer = nextChargeDelay(this.random);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    /** Sand or grass under open, bright sky: the beaches. */
    public static boolean checkSpawnRules(EntityType<Mudzya> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(BlockTags.SAND) || below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.SANDSTONE) || below.is(Blocks.GRAVEL))
                && level.getRawBrightness(pos, 0) > 8;
    }

    private static int nextChargeDelay(RandomSource random) {
        int base = Math.max(5, Config.MUDZYA_CHARGE_SECONDS.get()) * 20;
        return base + random.nextInt(base / 2 + 1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.15, stack -> stack.is(ModTags.MUDZYA_FOOD), false));
        this.goalSelector.addGoal(4, new DeliverSourceGoal(this));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    public boolean isCharged() {
        return this.entityData.get(CHARGED);
    }

    public void setCharged(boolean charged) {
        this.entityData.set(CHARGED, charged);
        if (!charged) {
            this.chargeTimer = nextChargeDelay(this.random);
        }
    }

    /** Settles at a hive: from now on it lives around it. */
    public void setHome(BlockPos pos) {
        this.home = pos.immutable();
        restrictTo(this.home, HOME_RANGE);
    }

    public @Nullable BlockPos getHome() {
        return home;
    }

    /** A homeless Mudzya adopts the nearest hive within 16 blocks (wild or built by a player). */
    private void findHome() {
        BlockPos center = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-16, -8, -16), center.offset(16, 8, 16))) {
            if (level().getBlockState(pos).is(com.soulinchains.arcaneartifacts.registry.ModBlocks.MUDZYA_HIVE.get())) {
                double distance = pos.distSqr(center);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = pos.immutable();
                }
            }
        }
        if (best != null) {
            setHome(best);
        }
    }

    /** Called by a hive that noticed this full Mudzya: it will fly there to unload. */
    public void callToHive(BlockPos hive) {
        this.hivePos = hive.immutable();
    }

    private @Nullable MudzyaHiveBlockEntity hive() {
        if (this.hivePos == null) {
            return null;
        }
        if (this.hivePos.distSqr(this.blockPosition()) > 32 * 32 || !this.level().isLoaded(this.hivePos)
                || !(this.level().getBlockEntity(this.hivePos) instanceof MudzyaHiveBlockEntity hive)) {
            this.hivePos = null;
            return null;
        }
        return hive;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            if (isCharged() && this.random.nextInt(6) == 0) {
                this.level().addParticle(ParticleTypes.WITCH, getRandomX(0.4), getY() + 0.1, getRandomZ(0.4), 0, -0.02, 0);
            }
            return;
        }
        if (!isCharged() && !isBaby() && --this.chargeTimer <= 0) {
            setCharged(true);
        }
        if (this.tickCount % 100 == 0) {
            if (this.home != null && !level().getBlockState(this.home).is(com.soulinchains.arcaneartifacts.registry.ModBlocks.MUDZYA_HIVE.get())) {
                this.home = null;
                clearRestriction();
            }
            if (this.home == null) {
                findHome();
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.GLASS_BOTTLE) && isCharged() && !isBaby()) {
            if (!this.level().isClientSide) {
                setCharged(false);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.BOTTLE_OF_SOURCE.get())));
                playSound(SoundEvents.BOTTLE_FILL, 1.0F, 1.2F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModTags.MUDZYA_FOOD);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.MUDZYA.get().create(level);
    }

    @Override
    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.BEE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEE_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Charged", isCharged());
        tag.putInt("ChargeTimer", this.chargeTimer);
        if (this.home != null) {
            tag.putInt("HomeX", this.home.getX());
            tag.putInt("HomeY", this.home.getY());
            tag.putInt("HomeZ", this.home.getZ());
        }
        if (this.hivePos != null) {
            tag.putInt("HiveX", this.hivePos.getX());
            tag.putInt("HiveY", this.hivePos.getY());
            tag.putInt("HiveZ", this.hivePos.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(CHARGED, tag.getBoolean("Charged"));
        if (tag.contains("ChargeTimer")) {
            this.chargeTimer = tag.getInt("ChargeTimer");
        }
        this.hivePos = tag.contains("HiveX") ? new BlockPos(tag.getInt("HiveX"), tag.getInt("HiveY"), tag.getInt("HiveZ")) : null;
        if (tag.contains("HomeX")) {
            setHome(new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ")));
        }
    }

    /** A full Mudzya that was called by a hive flies to it and unloads its Source. */
    private static final class DeliverSourceGoal extends Goal {
        private final Mudzya mudzya;
        private int ticks;

        DeliverSourceGoal(Mudzya mudzya) {
            this.mudzya = mudzya;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!mudzya.isCharged() || mudzya.isBaby()) {
                return false;
            }
            MudzyaHiveBlockEntity hive = mudzya.hive();
            return hive != null && hive.hasRoom();
        }

        @Override
        public boolean canContinueToUse() {
            return ticks < 600 && canUse();
        }

        @Override
        public void start() {
            ticks = 0;
        }

        @Override
        public void stop() {
            mudzya.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ticks++;
            MudzyaHiveBlockEntity hive = mudzya.hive();
            if (hive == null) {
                return;
            }
            Vec3 target = Vec3.atCenterOf(hive.getBlockPos()).add(0, 0.9, 0);
            if (mudzya.position().distanceToSqr(target) < 2.2 * 2.2) {
                hive.addSource(Config.MUDZYA_SOURCE_PER_DELIVERY.get());
                mudzya.setCharged(false);
                mudzya.playSound(SoundEvents.BEEHIVE_ENTER, 0.6F, 1.4F);
                if (mudzya.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.WITCH, target.x, target.y, target.z, 8, 0.3, 0.2, 0.3, 0.0);
                }
            } else if (ticks % 20 == 1 || mudzya.getNavigation().isDone()) {
                mudzya.getNavigation().moveTo(target.x, target.y, target.z, 1.2);
            }
        }
    }
}
