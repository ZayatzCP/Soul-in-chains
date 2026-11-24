package com.soulinchains.arcaneartifacts.cult;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Torn Acolyte: a failed initiate drawn to a rite. It goes for the anchors first (every blow takes {@link #ANCHOR_DAMAGE}
 * stability), then for whoever stands in its way. Fades after two minutes.
 */
public class TornAcolyte extends Monster {
    public static final int ANCHOR_DAMAGE = 15;
    public static final int LIFE_TICKS = 2400;
    @Nullable
    private BlockPos seal;
    private int lifeLeft = LIFE_TICKS;

    public TornAcolyte(EntityType<? extends TornAcolyte> type, Level level) {
        super(type, level);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new StrikeAnchorGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Sent against the circle of this seal. */
    public void besiege(BlockPos sealPos) {
        seal = sealPos.immutable();
    }

    @Nullable
    RitualSealBlockEntity circle() {
        return seal != null && level().getBlockEntity(seal) instanceof RitualSealBlockEntity circle ? circle : null;
    }

    @Nullable
    RitualAnchorBlockEntity nearestAnchor() {
        if (seal == null || !(level().getBlockEntity(seal) instanceof RitualSealBlockEntity circle)) {
            return null;
        }
        RitualAnchorBlockEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (RitualAnchorBlockEntity anchor : circle.anchorsAround()) {
            if (!anchor.active()) {
                continue;
            }
            double d = distanceToSqr(Vec3.atCenterOf(anchor.getBlockPos()));
            if (d < bestD) {
                bestD = d;
                best = anchor;
            }
        }
        return best;
    }

    /** Whether it crumbles after {@link #LIFE_TICKS}. */
    protected boolean fades() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && fades() && --lifeLeft <= 0) {
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeLeft", lifeLeft);
        if (seal != null) {
            tag.put("Seal", NbtUtils.writeBlockPos(seal));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeLeft = tag.contains("LifeLeft") ? tag.getInt("LifeLeft") : LIFE_TICKS;
        seal = NbtUtils.readBlockPos(tag, "Seal").orElse(null);
    }

    /** Walks to the nearest lit anchor and strikes it, unless someone hurt it first. */
    static final class StrikeAnchorGoal extends Goal {
        private final TornAcolyte acolyte;
        @Nullable
        private RitualAnchorBlockEntity anchor;
        private int cooldown;

        StrikeAnchorGoal(TornAcolyte acolyte) {
            this.acolyte = acolyte;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (acolyte.getLastHurtByMob() != null && acolyte.tickCount - acolyte.getLastHurtByMobTimestamp() < 100) {
                return false;
            }
            anchor = acolyte.nearestAnchor();
            return anchor != null;
        }

        @Override
        public boolean canContinueToUse() {
            return anchor != null && anchor.active() && !anchor.isRemoved() && canUse();
        }

        @Override
        public void tick() {
            if (anchor == null) {
                return;
            }
            Vec3 at = Vec3.atCenterOf(anchor.getBlockPos());
            acolyte.getLookControl().setLookAt(at.x, at.y, at.z);
            if (acolyte.distanceToSqr(at) > 4.0) {
                if (acolyte.tickCount % 10 == 0) {
                    acolyte.getNavigation().moveTo(at.x, at.y, at.z, 1.0);
                }
            } else if (--cooldown <= 0) {
                acolyte.getNavigation().stop();
                acolyte.swing(InteractionHand.MAIN_HAND);
                RitualSealBlockEntity circle = acolyte.circle();
                anchor.damage(circle == null ? ANCHOR_DAMAGE : circle.anchorDamage(ANCHOR_DAMAGE, anchor.getBlockPos()));
                acolyte.level().playSound(null, anchor.getBlockPos(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 0.8F, 1.2F);
                cooldown = 30;
            }
        }
    }
}
