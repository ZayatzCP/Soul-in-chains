package com.soulinchains.arcaneartifacts.entity;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.spore.Spores;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Amanitoid: the fly-agaric guardian of a Mycelinoid colony. It does not trade; like an iron golem it walks
 * the colony and fights monsters, and anyone who hurts a Mycelinoid. It carries a Mushroom Club and a
 * Mushroom Shield, raises the shield in a fight and bursts spore clouds when blows land on it.
 */
public class Amanitoid extends Gribushka {
    private static final int BLOCK_TICKS = 40;
    private int blockTicks;
    private int blockCooldown;

    public Amanitoid(EntityType<? extends Amanitoid> type, Level level) {
        super(type, level);
        setDropChance(EquipmentSlot.MAINHAND, 0.1F);
        setDropChance(EquipmentSlot.OFFHAND, 0.1F);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(2, new SocialGoal(this));
        this.goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Gribushka.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false,
                entity -> entity instanceof Enemy && !(entity instanceof Creeper)));
    }

    /** A guardian holds its club, not a mushroom to trade: it never "admires". */
    @Override
    public boolean isAdmiring() {
        return false;
    }

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance difficulty) {
        equip();
    }

    public void equip() {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.MUSHROOM_CLUB.get()));
        setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.MUSHROOM_SHIELD.get()));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                                  @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        populateDefaultEquipmentSlots(getRandom(), difficulty);
        return data;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.blockCooldown = Math.max(0, this.blockCooldown - 1);
        if (isUsingItem()) {
            if (--this.blockTicks <= 0) {
                stopUsingItem();
                this.blockCooldown = 50;
            }
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && this.blockCooldown == 0 && distanceToSqr(target) < 5 * 5 && getRandom().nextInt(15) == 0) {
            raiseShield();
        }
    }

    private void raiseShield() {
        if (getOffhandItem().is(ModItems.MUSHROOM_SHIELD.get())) {
            startUsingItem(InteractionHand.OFF_HAND);
            this.blockTicks = BLOCK_TICKS;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (isUsingItem()) {
            stopUsingItem();
            this.blockCooldown = 30;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living && getMainHandItem().getItem() instanceof com.soulinchains.arcaneartifacts.spore.MushroomClubItem) {
            com.soulinchains.arcaneartifacts.spore.MushroomClubItem.dust(living);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !isUsingItem() && this.blockCooldown == 0 && getRandom().nextInt(3) == 0) {
            raiseShield();
        }
        return hurt;
    }

    @Override
    public float getVoicePitch() {
        return 0.75F + getRandom().nextFloat() * 0.1F;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    /** When someone hurts a Mycelinoid, the guardians nearby go after them. */
    @EventBusSubscriber(modid = ArcaneArtifacts.MODID)
    public static final class Alarm {
        private Alarm() {
        }

        @SubscribeEvent
        public static void onDamage(LivingDamageEvent.Post event) {
            if (!(event.getEntity() instanceof Gribushka victim) || victim.level().isClientSide) {
                return;
            }
            if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || Spores.isMycelial(attacker)
                    || attacker instanceof Player player && (player.isCreative() || player.isSpectator())) {
                return;
            }
            for (Amanitoid guard : victim.level().getEntitiesOfClass(Amanitoid.class, victim.getBoundingBox().inflate(16.0))) {
                if (guard != victim && guard.getTarget() == null) {
                    guard.setTarget(attacker);
                }
            }
        }
    }
}
