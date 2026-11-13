package com.soulinchains.arcaneartifacts.entity;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Mycelinoid (registry id {@code gribushka}): one of the mushroom folk of the Mushroom Caves. They live in
 * colonies and collect mushrooms: throw one a mushroom and, after holding it up and turning it over, it
 * throws back the item that goes with that mushroom. When two of them meet they wave to each other, and
 * now and then a few of them dance together. What each mushroom is worth is a loot table:
 * {@code arcane_artifacts:gribushka_trades/<namespace>/<item>} (anything with such a table is accepted).
 */
public class Gribushka extends PathfinderMob {
    private static final int ADMIRE_TICKS = 70;
    /** What the body is doing, for the model: see {@link Mood}. */
    private static final EntityDataAccessor<Integer> MOOD = SynchedEntityData.defineId(Gribushka.class, EntityDataSerializers.INT);
    public static final int GREET_TICKS = 40;
    public static final int DANCE_TICKS = 120;

    /** Animations other Mycelinoids can see. */
    public enum Mood { NONE, GREET, DANCE }

    private int admireTicks;
    private int moodTicks;
    private int greetCooldown = 100;
    private int danceCooldown = 600;
    private @Nullable Gribushka partner;
    /** Client side: the tick the current mood started, for the animation. */
    private int moodStart;
    private @Nullable UUID customer;
    private @Nullable BlockPos home;

    public Gribushka(EntityType<? extends Gribushka> type, Level level) {
        super(type, level);
        setDropChance(EquipmentSlot.MAINHAND, 2.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    /** The loot table that says what a mushroom is worth. */
    public static ResourceKey<LootTable> tradeTable(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return ResourceKey.create(Registries.LOOT_TABLE, ArcaneArtifacts.id("gribushka_trades/" + id.getNamespace() + "/" + id.getPath()));
    }

    /** Whether Gribushkas take this item: it must have a (non-empty) trade table. */
    public static boolean isTradeItem(Level level, ItemStack stack) {
        if (stack.isEmpty() || !(level instanceof ServerLevel server)) {
            return false;
        }
        return server.getServer().reloadableRegistries().getLootTable(tradeTable(stack.getItem())) != LootTable.EMPTY;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MOOD, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(2, new SeekMushroomGoal(this));
        this.goalSelector.addGoal(2, new SocialGoal(this));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.0, stack -> !isAdmiring() && isTradeItem(level(), stack), false));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.9));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public Mood getMood() {
        Mood[] moods = Mood.values();
        return moods[Math.floorMod(this.entityData.get(MOOD), moods.length)];
    }

    public void setMood(Mood mood, int ticks) {
        this.entityData.set(MOOD, mood.ordinal());
        this.moodTicks = ticks;
    }

    /** Ticks (with partial tick) since the current mood started, on the client. */
    public float moodAge(float partialTick) {
        return this.tickCount - this.moodStart + partialTick;
    }

    /** Ticks it has been looking at the mushroom in its hand, on the client. */
    public float admireAge(float partialTick) {
        return this.admireTicks + partialTick;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (MOOD.equals(key)) {
            this.moodStart = this.tickCount;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            this.admireTicks = isAdmiring() ? this.admireTicks + 1 : 0;
        }
    }

    /** True while it is looking at a mushroom it has just been given. */
    public boolean isAdmiring() {
        return !getMainHandItem().isEmpty();
    }

    /** The colony it belongs to: it does not wander far from it. */
    public void setHome(BlockPos pos) {
        this.home = pos.immutable();
        restrictTo(this.home, 14);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.greetCooldown = Math.max(0, this.greetCooldown - 1);
        this.danceCooldown = Math.max(0, this.danceCooldown - 1);
        if (this.moodTicks > 0 && --this.moodTicks == 0) {
            setMood(Mood.NONE, 0);
        }
        if (!isAdmiring()) {
            this.admireTicks = 0;
            return;
        }
        getNavigation().stop();
        if (++this.admireTicks >= ADMIRE_TICKS) {
            finishTrade();
        }
    }

    private @Nullable ItemEntity findMushroom() {
        List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(8.0, 3.0, 8.0),
                item -> item.isAlive() && !item.hasPickUpDelay() && isTradeItem(level(), item.getItem()));
        ItemEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            double distance = distanceToSqr(item);
            if (distance < best) {
                best = distance;
                nearest = item;
            }
        }
        return nearest;
    }

    /** Takes one mushroom from the stack on the ground and starts looking at it. */
    private void take(ItemEntity item) {
        if (isAdmiring() || !item.isAlive()) {
            return;
        }
        ItemStack stack = item.getItem();
        ItemStack one = stack.split(1);
        if (stack.isEmpty()) {
            item.discard();
        } else {
            item.setItem(stack);
        }
        Entity thrower = item.getOwner();
        this.customer = thrower == null ? null : thrower.getUUID();
        setItemSlot(EquipmentSlot.MAINHAND, one);
        setMood(Mood.NONE, 0);
        this.admireTicks = 0;
        playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.4F);
        playSound(SoundEvents.VILLAGER_TRADE, 0.8F, getVoicePitch());
    }

    private void finishTrade() {
        ItemStack paid = getMainHandItem();
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.admireTicks = 0;
        if (!(level() instanceof ServerLevel server) || paid.isEmpty()) {
            return;
        }
        LootTable table = server.getServer().reloadableRegistries().getLootTable(tradeTable(paid.getItem()));
        LootParams params = new LootParams.Builder(server)
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .create(LootContextParamSets.PIGLIN_BARTER);
        Vec3 towards = position().add(getLookAngle().scale(2.0));
        if (this.customer != null) {
            Player player = server.getPlayerByUUID(this.customer);
            if (player != null && player.distanceToSqr(this) < 16 * 16) {
                towards = player.position();
            }
        }
        boolean gave = false;
        for (ItemStack stack : table.getRandomItems(params)) {
            throwItem(stack, towards);
            gave = true;
        }
        if (!gave) {
            // A trade table that rolled nothing: the mushroom is given back rather than eaten.
            throwItem(paid, towards);
        }
        playSound(gave ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_NO, 0.8F, getVoicePitch());
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.0, getZ(), 5, 0.3, 0.2, 0.3, 0.0);
        this.customer = null;
    }

    private void throwItem(ItemStack stack, Vec3 towards) {
        ItemEntity drop = new ItemEntity(level(), getX(), getEyeY() - 0.2, getZ(), stack);
        Vec3 direction = towards.subtract(position()).multiply(1, 0, 1);
        direction = direction.lengthSqr() < 1.0E-4 ? Vec3.ZERO : direction.normalize();
        drop.setDeltaMovement(direction.x * 0.3, 0.3, direction.z * 0.3);
        drop.setDefaultPickUpDelay();
        level().addFreshEntity(drop);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 1.6F + this.random.nextFloat() * 0.2F;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.home != null) {
            tag.putInt("HomeX", this.home.getX());
            tag.putInt("HomeY", this.home.getY());
            tag.putInt("HomeZ", this.home.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeX")) {
            setHome(new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ")));
        }
    }

    /** Whether it is free to greet or dance: not trading, not busy with another animation. */
    boolean isFree() {
        return !isAdmiring() && getMood() == Mood.NONE && getTarget() == null && getLastHurtByMob() == null;
    }

    /**
     * Meeting another Mycelinoid: both stop, turn to each other and wave. Now and then, instead, it starts a
     * dance and every free Mycelinoid around joins in.
     */
    private static final class SocialGoal extends Goal {
        private final Gribushka mob;

        SocialGoal(Gribushka mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (mob.getMood() != Mood.NONE) {
                return true;   // a friend started it for us
            }
            if (!mob.isFree() || mob.tickCount % 20 != 0) {
                return false;
            }
            List<Gribushka> friends = mob.level().getEntitiesOfClass(Gribushka.class, mob.getBoundingBox().inflate(7.0, 2.0, 7.0),
                    other -> other != mob && other.isFree() && mob.hasLineOfSight(other));
            if (friends.isEmpty()) {
                return false;
            }
            if (mob.danceCooldown == 0 && friends.size() >= 1 && mob.getRandom().nextInt(25) == 0) {
                mob.partner = friends.get(0);
                for (Gribushka friend : friends.subList(0, Math.min(5, friends.size()))) {
                    friend.partner = mob;
                    friend.setMood(Mood.DANCE, DANCE_TICKS);
                    friend.danceCooldown = 2400 + friend.getRandom().nextInt(1200);
                }
                mob.setMood(Mood.DANCE, DANCE_TICKS);
                mob.danceCooldown = 2400 + mob.getRandom().nextInt(1200);
                mob.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 0.6F, 1.6F);
                return true;
            }
            if (mob.greetCooldown == 0) {
                Gribushka friend = friends.stream().filter(other -> other.greetCooldown == 0).findFirst().orElse(null);
                if (friend == null) {
                    return false;
                }
                mob.partner = friend;
                friend.partner = mob;
                mob.setMood(Mood.GREET, GREET_TICKS);
                friend.setMood(Mood.GREET, GREET_TICKS);
                mob.greetCooldown = 900 + mob.getRandom().nextInt(900);
                friend.greetCooldown = 900 + friend.getRandom().nextInt(900);
                mob.playSound(SoundEvents.VILLAGER_CELEBRATE, 0.6F, mob.getVoicePitch());
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return mob.getMood() != Mood.NONE && !mob.isAdmiring();
        }

        @Override
        public void start() {
            mob.getNavigation().stop();
        }

        @Override
        public void stop() {
            mob.partner = null;
            if (mob.getMood() != Mood.NONE) {
                mob.setMood(Mood.NONE, 0);
            }
        }

        @Override
        public void tick() {
            mob.getNavigation().stop();
            if (mob.partner != null && mob.partner.isAlive()) {
                mob.getLookControl().setLookAt(mob.partner, 30.0F, 30.0F);
            }
            if (mob.getMood() == Mood.DANCE && mob.level() instanceof ServerLevel server && mob.tickCount % 10 == 0) {
                server.sendParticles(ParticleTypes.NOTE, mob.getX(), mob.getY() + 1.4, mob.getZ(), 1, 0.2, 0.1, 0.2, mob.getRandom().nextDouble());
            }
        }
    }

    /** Walks to the nearest mushroom lying on the ground and picks one up. */
    private static final class SeekMushroomGoal extends Goal {
        private final Gribushka mob;
        private @Nullable ItemEntity target;

        SeekMushroomGoal(Gribushka mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (mob.isAdmiring() || mob.tickCount % 10 != 0) {
                return false;
            }
            target = mob.findMushroom();
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && !mob.isAdmiring() && mob.distanceToSqr(target) < 14 * 14;
        }

        @Override
        public void stop() {
            target = null;
            mob.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (mob.distanceToSqr(target) < 1.7 * 1.7) {
                mob.take(target);
            } else if (mob.tickCount % 10 == 0 || mob.getNavigation().isDone()) {
                mob.getNavigation().moveTo(target, 1.15);
            }
        }
    }
}
