package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A servant raised by a Shackled from souls. It is chained to its master: farther than {@code servant_tether} blocks,
 * or in another dimension, the chain breaks, the servant crumbles and its key soul goes back to the lantern. While it
 * exists it holds a share of the master's maximum mana. Commands come from the Warden's Rod: follow, guard a spot,
 * attack a target, dismiss.
 */
public class Servant extends PathfinderMob implements OwnableEntity {
    public enum Command { FOLLOW, GUARD }

    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(Servant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> COMMAND = SynchedEntityData.defineId(Servant.class, EntityDataSerializers.BYTE);

    /** Servants per master in each loaded level (for limits and the mana reserve). */
    private static final Map<UUID, Set<Servant>> BY_OWNER = new HashMap<>();

    public final ServantKind kind;
    @Nullable
    private UUID ownerId;
    @Nullable
    private BlockPos guardPos;
    private final SimpleContainer cargo = new SimpleContainer(27);
    private int abilityCooldown;

    public Servant(EntityType<? extends Servant> type, Level level, ServantKind kind) {
        super(type, level);
        this.kind = kind;
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes(ServantKind kind) {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, kind.health)
                .add(Attributes.ATTACK_DAMAGE, Math.max(1, kind.damage))
                .add(Attributes.MOVEMENT_SPEED, kind.speed)
                .add(Attributes.ARMOR, kind.armor)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ENTITY_INTERACTION_RANGE, kind == ServantKind.SKELETON ? 3.0 : 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_ID, -1);
        builder.define(COMMAND, (byte) Command.FOLLOW.ordinal());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (kind.fights()) {
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        } else {
            goalSelector.addGoal(2, new CollectLootGoal(this));
        }
        goalSelector.addGoal(4, new StayWithMasterGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        if (kind.fights()) {
            targetSelector.addGoal(1, new DefendMasterGoal(this));
            targetSelector.addGoal(2, new AssistMasterGoal(this));
            targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
            targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                    e -> command() == Command.GUARD && e instanceof Enemy));
        }
    }

    // ------------------------------------------------------------------ owner & command

    public void bind(Player master) {
        ownerId = master.getUUID();
        entityData.set(OWNER_ID, master.getId());
        register();
    }

    @Override
    @Nullable
    public UUID getOwnerUUID() {
        return ownerId;
    }

    /** The master on this side: the server finds them by UUID; the client by the synced entity id (for drawing the chain). */
    @Override
    @Nullable
    public LivingEntity getOwner() {
        if (level().isClientSide) {
            int id = entityData.get(OWNER_ID);
            return id >= 0 && level().getEntity(id) instanceof LivingEntity living ? living : null;
        }
        return ownerId == null ? null : level().getPlayerByUUID(ownerId);
    }

    public Command command() {
        return Command.values()[entityData.get(COMMAND) % Command.values().length];
    }

    public void follow() {
        entityData.set(COMMAND, (byte) Command.FOLLOW.ordinal());
        guardPos = null;
    }

    public void guard(BlockPos pos) {
        entityData.set(COMMAND, (byte) Command.GUARD.ordinal());
        guardPos = pos.immutable();
        getNavigation().stop();
    }

    @Nullable
    public BlockPos guardPos() {
        return guardPos;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other instanceof Servant servant && ownerId != null && ownerId.equals(servant.ownerId)) {
            return true;
        }
        if (ownerId != null && ownerId.equals(other.getUUID())) {
            return true;
        }
        LivingEntity owner = getOwner();
        return owner != null && owner != this && owner.isAlliedTo(other) || super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isAlliedTo(target) && super.canAttack(target);
    }

    // ------------------------------------------------------------------ registry (limits, mana reserve)

    private void register() {
        if (ownerId != null && !level().isClientSide) {
            BY_OWNER.computeIfAbsent(ownerId, k -> new HashSet<>()).add(this);
        }
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        register();
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        if (ownerId != null) {
            Set<Servant> set = BY_OWNER.get(ownerId);
            if (set != null) {
                set.remove(this);
                if (set.isEmpty()) {
                    BY_OWNER.remove(ownerId);
                }
            }
        }
    }

    public static Set<Servant> of(UUID owner) {
        Set<Servant> set = BY_OWNER.get(owner);
        if (set == null) {
            return Set.of();
        }
        set.removeIf(s -> s.isRemoved() || !s.isAlive());
        return Set.copyOf(set);
    }

    /** Share of the master's maximum mana held by their servants. */
    public static float reserveOf(UUID owner) {
        float sum = 0;
        for (Servant servant : of(owner)) {
            sum += servant.kind.reserve;
        }
        return sum;
    }

    // ------------------------------------------------------------------ life

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || tickCount % 10 != 0) {
            return;
        }
        if (ownerId == null) {
            crumble(false);
            return;
        }
        ServerPlayer master = ((ServerLevel) level()).getServer().getPlayerList().getPlayer(ownerId);
        if (master == null) {
            // The master is away: servants wait where they stand.
            if (command() == Command.FOLLOW) {
                guard(blockPosition());
            }
            return;
        }
        if (master.level() != level() || master.distanceTo(this) > Config.SERVANT_TETHER.get()) {
            crumble(true);
            return;
        }
        if (entityData.get(OWNER_ID) != master.getId()) {
            entityData.set(OWNER_ID, master.getId());
        }
        if (abilityCooldown > 0) {
            abilityCooldown -= 10;
        }
        ability(master);
    }

    /** What each kind does on its own. */
    private void ability(ServerPlayer master) {
        if (abilityCooldown > 0) {
            return;
        }
        switch (kind) {
            case ASSASSIN -> {
                LivingEntity target = getTarget();
                if (target != null && target.isAlive() && distanceTo(target) < 16 && distanceTo(target) > 3) {
                    Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5));
                    if (randomTeleport(behind.x, target.getY(), behind.z, true)) {
                        level().playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.6F, 1.4F);
                        abilityCooldown = 100;
                    }
                }
            }
            case PLAGUE_DOCTOR -> {
                boolean healed = false;
                if (master.distanceTo(this) < 8 && master.getHealth() < master.getMaxHealth()) {
                    master.heal(2.0F);
                    healed = true;
                }
                for (Servant other : of(ownerId)) {
                    if (other != this && other.distanceTo(this) < 8 && other.getHealth() < other.getMaxHealth()) {
                        other.heal(2.0F);
                        healed = true;
                    }
                }
                if (healed) {
                    ((ServerLevel) level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 2, getZ(), 6, 0.6, 0.3, 0.6, 0);
                    abilityCooldown = 60;
                }
            }
            default -> {
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            switch (kind) {
                case HOUND -> living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), this);
                case KNIGHT -> living.igniteForSeconds(3.0F);
                case PLAGUE_DOCTOR -> living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), this);
                case ROWER -> {
                    if (living.isInWater()) {
                        living.setAirSupply(Math.max(0, living.getAirSupply() - 60));
                    }
                }
                default -> {
                }
            }
        }
        return hit;
    }

    /** The chain broke (or the servant was dismissed): it crumbles to dust and its key soul goes back to the lantern. */
    public void crumble(boolean giveSoulBack) {
        if (!(level() instanceof ServerLevel level) || isRemoved()) {
            return;
        }
        level.sendParticles(ParticleTypes.SOUL, getX(), getY() + getBbHeight() / 2, getZ(), 16, 0.3, 0.5, 0.3, 0.02);
        level.sendParticles(ParticleTypes.ASH, getX(), getY() + getBbHeight() / 2, getZ(), 30, 0.3, 0.6, 0.3, 0.02);
        level.playSound(null, blockPosition(), SoundEvents.SKELETON_DEATH, SoundSource.NEUTRAL, 0.8F, 0.6F);
        if (giveSoulBack) {
            LivingEntity master = getOwner();
            if (!(master instanceof Player player) || !Souls.store(player, kind.keySoul())) {
                Souls.spawnWisp(level, this, kind.keySoul());
            }
        }
        Containers.dropContents(level, blockPosition(), cargo);
        discard();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        Containers.dropContents(level, blockPosition(), cargo);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return kind.fireImmune || super.fireImmune();
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return kind != ServantKind.ROWER && super.canDrownInFluidType(type);
    }

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, net.minecraft.world.DifficultyInstance difficulty) {
    }

    /** Gives the servant the look of its trade (never dropped). */
    public void dress() {
        switch (kind) {
            case KNIGHT -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            case ASSASSIN -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
            case SKELETON -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CHAIN));
            case PORTER -> setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHEST));
            case ROWER -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
            default -> {
            }
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    // ------------------------------------------------------------------ porter's cargo, rower's ride

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.getUUID().equals(ownerId) || player.getItemInHand(hand).getItem() instanceof WardenRodItem) {
            return super.mobInteract(player, hand);
        }
        if (kind == ServantKind.PORTER) {
            if (!level().isClientSide) {
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> ChestMenu.threeRows(id, inv, cargo), getDisplayName()));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (kind == ServantKind.ROWER && !isVehicle()) {
            if (!level().isClientSide) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    public SimpleContainer cargo() {
        return cargo;
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return kind == ServantKind.ROWER && getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        setYRot(player.getYRot());
        yRotO = getYRot();
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        if (isInWater() && player.zza > 0) {
            double climb = -Math.sin(Math.toRadians(player.getXRot())) * 0.06;
            setDeltaMovement(getDeltaMovement().add(0, climb, 0));
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        return new Vec3(player.xxa * 0.5, 0, player.zza);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return isInWater() ? 0.35F : 0.12F;
    }

    // ------------------------------------------------------------------ saving

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) {
            tag.putUUID("Master", ownerId);
        }
        tag.putByte("Command", (byte) command().ordinal());
        if (guardPos != null) {
            tag.put("GuardPos", NbtUtils.writeBlockPos(guardPos));
        }
        tag.put("Cargo", cargo.createTag(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Master")) {
            ownerId = tag.getUUID("Master");
        }
        entityData.set(COMMAND, tag.getByte("Command"));
        Optional<BlockPos> pos = NbtUtils.readBlockPos(tag, "GuardPos");
        guardPos = pos.orElse(null);
        cargo.fromTag(tag.getList("Cargo", 10), registryAccess());
        register();
    }

    @Override
    public Component getTypeName() {
        return Component.translatable("entity.arcane_artifacts." + kind.id);
    }

    // ------------------------------------------------------------------ goals

    /** Follows the master (or keeps to its guard post); teleports to the master when it falls far behind. */
    static final class StayWithMasterGoal extends Goal {
        private final Servant servant;

        StayWithMasterGoal(Servant servant) {
            this.servant = servant;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (servant.getTarget() != null || servant.isVehicle()) {
                return false;
            }
            if (servant.command() == Command.GUARD) {
                BlockPos post = servant.guardPos;
                return post != null && post.distToCenterSqr(servant.position()) > 9;
            }
            LivingEntity master = servant.getOwner();
            return master != null && master.distanceTo(servant) > 4;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && !servant.getNavigation().isDone();
        }

        @Override
        public void start() {
            tick();
        }

        @Override
        public void tick() {
            if (servant.command() == Command.GUARD && servant.guardPos != null) {
                BlockPos post = servant.guardPos;
                servant.getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 1.1);
                return;
            }
            LivingEntity master = servant.getOwner();
            if (master == null) {
                return;
            }
            double distance = master.distanceTo(servant);
            if (distance > 12 && master.onGround()) {
                servant.randomTeleport(master.getX() + servant.random.nextInt(5) - 2, master.getY(), master.getZ() + servant.random.nextInt(5) - 2, false);
                servant.getNavigation().stop();
            } else if (servant.tickCount % 10 == 0 || servant.getNavigation().isDone()) {
                servant.getNavigation().moveTo(master, 1.2);
            }
        }
    }

    /** Attacks whoever hurts the master. */
    static final class DefendMasterGoal extends TargetGoal {
        private final Servant servant;
        private int lastHurtBy;

        DefendMasterGoal(Servant servant) {
            super(servant, false);
            this.servant = servant;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity master = servant.getOwner();
            if (master == null) {
                return false;
            }
            LivingEntity attacker = master.getLastHurtByMob();
            return attacker != null && master.getLastHurtByMobTimestamp() != lastHurtBy && canAttack(attacker, TargetingConditions.DEFAULT)
                    && !servant.isAlliedTo(attacker);
        }

        @Override
        public void start() {
            LivingEntity master = servant.getOwner();
            if (master != null) {
                servant.setTarget(master.getLastHurtByMob());
                lastHurtBy = master.getLastHurtByMobTimestamp();
            }
            super.start();
        }
    }

    /** Attacks whatever the master attacks. */
    static final class AssistMasterGoal extends TargetGoal {
        private final Servant servant;
        private int lastHurt;

        AssistMasterGoal(Servant servant) {
            super(servant, false);
            this.servant = servant;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity master = servant.getOwner();
            if (master == null) {
                return false;
            }
            LivingEntity victim = master.getLastHurtMob();
            return victim != null && master.getLastHurtMobTimestamp() != lastHurt && canAttack(victim, TargetingConditions.DEFAULT)
                    && !servant.isAlliedTo(victim);
        }

        @Override
        public void start() {
            LivingEntity master = servant.getOwner();
            if (master != null) {
                servant.setTarget(master.getLastHurtMob());
                lastHurt = master.getLastHurtMobTimestamp();
            }
            super.start();
        }
    }

    /** Bone Porter: walks to loose items nearby and packs them into its 27 slots. */
    static final class CollectLootGoal extends Goal {
        private final Servant servant;
        @Nullable
        private ItemEntity item;

        CollectLootGoal(Servant servant) {
            this.servant = servant;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (servant.tickCount % 10 != 0) {
                return false;
            }
            item = servant.level().getEntitiesOfClass(ItemEntity.class, servant.getBoundingBox().inflate(6, 2, 6),
                            e -> e.isAlive() && !e.hasPickUpDelay() && servant.cargo.canAddItem(e.getItem())).stream()
                    .min((a, b) -> Double.compare(a.distanceToSqr(servant), b.distanceToSqr(servant))).orElse(null);
            return item != null;
        }

        @Override
        public boolean canContinueToUse() {
            return item != null && item.isAlive();
        }

        @Override
        public void tick() {
            if (item == null) {
                return;
            }
            if (servant.distanceTo(item) < 1.5) {
                ItemStack rest = servant.cargo.addItem(item.getItem().copy());
                if (rest.isEmpty()) {
                    item.discard();
                } else {
                    item.setItem(rest);
                }
                servant.level().playSound(null, servant.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.4F, 1.2F);
                item = null;
            } else if (servant.getNavigation().isDone() || servant.tickCount % 10 == 0) {
                servant.getNavigation().moveTo(item, 1.1);
            }
        }
    }
}
