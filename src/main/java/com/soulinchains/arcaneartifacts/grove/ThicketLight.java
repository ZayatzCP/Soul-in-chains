package com.soulinchains.arcaneartifacts.grove;

import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A Thicket Light: a small living glow that drifts between the dark oaks at night. Glow berries tame it (one in three);
 * then it follows its friend, lights the dark around them and stings their enemies with sparks. Shift + right-click makes
 * it stay. Any light can be caught in a glass bottle: a Light in a Jar, a lamp that can let it out again.
 */
public class ThicketLight extends TamableAnimal implements FlyingAnimal {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(ThicketLight.class, EntityDataSerializers.INT);
    /** How far it stings, how often, and how hard. */
    public static final double STING_RANGE = 10;
    public static final int STING_COOLDOWN = 40;
    public static final float STING_DAMAGE = 3F;
    /** The light a tame one carries. */
    public static final int GLOW = 10;

    private int stingCooldown;
    @Nullable
    private BlockPos litAt;

    public ThicketLight(EntityType<? extends ThicketLight> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.FLYING_SPEED, 0.7)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    /** At night, anywhere it can float. */
    public static boolean checkSpawnRules(EntityType<ThicketLight> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, net.minecraft.util.RandomSource random) {
        return reason != MobSpawnType.NATURAL || level.getSkyDarken() >= 4 && level.getBlockState(pos).isAir();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLOR, 0);
    }

    public LightColor color() {
        return LightColor.byId(entityData.get(COLOR));
    }

    public void setColor(LightColor color) {
        entityData.set(COLOR, color.ordinal());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        setColor(LightColor.random(level.getRandom()));
        return super.finalizeSpawn(level, difficulty, reason, data == null ? new AgeableMob.AgeableMobGroupData(false) : data);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new PanicGoal(this, 1.6));
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    // ------------------------------------------------------------------ friends

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.GLASS_BOTTLE) && isAlive()) {
            if (!level().isClientSide) {
                ItemStack jar = jar();
                held.consume(1, player);
                if (!player.getInventory().add(jar)) {
                    player.drop(jar, false);
                }
                level().playSound(null, blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1F, 1.4F);
                discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!isTame() && held.is(Items.GLOW_BERRIES)) {
            if (!level().isClientSide) {
                held.consume(1, player);
                if (random.nextInt(3) == 0) {
                    tame(player);
                    setOrderedToSit(false);
                    navigation.stop();
                    level().broadcastEntityEvent(this, (byte) 7);
                    level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.2F, 1.6F);
                } else {
                    level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isTame() && isOwnedBy(player) && player.isShiftKeyDown()) {
            if (!level().isClientSide) {
                setOrderedToSit(!isOrderedToSit());
                navigation.stop();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /** This light caught in a jar: the jar block's item, carrying the light (colour, friend, name). */
    public ItemStack jar() {
        ItemStack stack = new ItemStack(ModItems.LIGHT_JAR.get());
        CompoundTag light = new CompoundTag();
        saveWithoutId(light);
        light.remove("UUID");
        light.remove("Pos");
        light.remove("Motion");
        CompoundTag be = new CompoundTag();
        be.putString("id", "arcane_artifacts:light_jar");
        be.put(LightJarBlockEntity.LIGHT, light);
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(be));
        stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of(LightJarBlock.COLOR.getName(), color().getSerializedName())));
        return stack;
    }

    /** A light let out at a point, from what a jar kept (or a new one of the colour, when it kept nothing). */
    @Nullable
    public static ThicketLight release(ServerLevel level, @Nullable CompoundTag kept, LightColor color, Vec3 at) {
        ThicketLight light = ModEntities.THICKET_LIGHT.get().create(level);
        if (light == null) {
            return null;
        }
        if (kept != null && !kept.isEmpty()) {
            light.load(kept);
        } else {
            light.setColor(color);
        }
        light.moveTo(at.x, at.y, at.z, 0F, 0F);
        light.setOrderedToSit(false);
        level.addFreshEntity(light);
        return light;
    }

    // ------------------------------------------------------------------ life

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount % 3 == 0) {
                Vec3 c = position().add(0, getBbHeight() / 2, 0);
                level().addParticle(new DustParticleOptions(color().vector(), 0.6F), c.x + (random.nextDouble() - 0.5) * 0.3,
                        c.y + (random.nextDouble() - 0.5) * 0.3, c.z + (random.nextDouble() - 0.5) * 0.3, 0, 0, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        LivingEntity owner = getOwner();
        if (isTame() && owner != null && owner.level() == level && !isOrderedToSit()) {
            double d = distanceTo(owner);
            if (d > 24) {
                moveTo(owner.getX() + random.nextDouble() - 0.5, owner.getY() + 2, owner.getZ() + random.nextDouble() - 0.5);
                navigation.stop();
            } else if (d > 3.5 && (tickCount % 10 == 0 || navigation.isDone())) {
                navigation.moveTo(owner.getX(), owner.getY() + 1.8, owner.getZ(), 1.3);
            }
        }
        if (isTame() && owner != null && --stingCooldown <= 0) {
            LivingEntity target = enemyOf(owner);
            if (target != null) {
                sting(level, owner, target);
                stingCooldown = STING_COOLDOWN;
            }
        }
        if (tickCount % 5 == 0) {
            carryLight(level);
        }
    }

    /** Who the light stings: whoever its friend fights or whoever hunts its friend, near and in sight. */
    @Nullable
    public LivingEntity enemyOf(LivingEntity owner) {
        for (LivingEntity candidate : new LivingEntity[]{owner.getLastHurtByMob(), owner.getLastHurtMob()}) {
            if (candidate != null && candidate != this && candidate != owner && candidate.isAlive() && distanceTo(candidate) <= STING_RANGE
                    && !com.soulinchains.arcaneartifacts.util.Allies.areAllied(owner, candidate) && hasLineOfSight(candidate)) {
                return candidate;
            }
        }
        for (Mob mob : level().getEntitiesOfClass(Mob.class, new AABB(blockPosition()).inflate(STING_RANGE),
                m -> m instanceof Enemy && m.getTarget() == owner && m.isAlive())) {
            if (hasLineOfSight(mob)) {
                return mob;
            }
        }
        return null;
    }

    /** A spark from the light to the target: a thin crackling line and a sting. */
    public void sting(ServerLevel level, LivingEntity owner, LivingEntity target) {
        Vec3 from = position().add(0, getBbHeight() / 2, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0);
        Vec3 step = to.subtract(from);
        int n = Math.max(4, (int) (step.length() * 3));
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(step.scale(i / (double) n)).add((random.nextDouble() - 0.5) * 0.25, (random.nextDouble() - 0.5) * 0.25, (random.nextDouble() - 0.5) * 0.25);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(new DustParticleOptions(color().vector(), 1.2F), to.x, to.y, to.z, 6, 0.2, 0.2, 0.2, 0);
        level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.NEUTRAL, 1F, 1.9F);
        target.hurt(level.damageSources().indirectMagic(this, owner), STING_DAMAGE);
    }

    /** A tame light lights the air it floats in (an invisible light block that moves with it). */
    private void carryLight(ServerLevel level) {
        BlockPos here = blockPosition();
        if (litAt != null && (!isTame() || !litAt.equals(here))) {
            dropLight(level);
        }
        if (isTame() && litAt == null) {
            BlockState state = level.getBlockState(here);
            if (state.is(Blocks.AIR)) {
                level.setBlock(here, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, GLOW), 3);
                litAt = here;
            }
        }
    }

    private void dropLight(ServerLevel level) {
        if (litAt != null && level.isLoaded(litAt) && level.getBlockState(litAt).is(Blocks.LIGHT)) {
            level.setBlock(litAt, Blocks.AIR.defaultBlockState(), 3);
        }
        litAt = null;
    }

    @Nullable
    public BlockPos litAt() {
        return litAt;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level) {
            dropLight(level);
        }
        super.remove(reason);
    }

    // ------------------------------------------------------------------ a small, light, flying thing

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Color", color().getSerializedName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        for (LightColor c : LightColor.values()) {
            if (c.getSerializedName().equals(tag.getString("Color"))) {
                setColor(c);
            }
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean isFlying() {
        return !onGround();
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isTame() && !hasCustomName() && !isPersistenceRequired();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return random.nextInt(4) == 0 ? SoundEvents.AMETHYST_BLOCK_CHIME : null;
    }

    @Override
    protected float getSoundVolume() {
        return 0.25F;
    }
}
