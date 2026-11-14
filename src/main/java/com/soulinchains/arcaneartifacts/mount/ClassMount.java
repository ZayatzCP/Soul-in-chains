package com.soulinchains.arcaneartifacts.mount;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A class mount: the Spellcaster's Flying Broom, the Druid's Moss Elk, the Pseudomage's Iron Horse. It comes
 * out of its item (use the item: it appears under you and you are in the saddle), right-click rides it, and
 * sneak + right-click folds it back into the item. It goes only where its rider steers and never despawns.
 */
public abstract class ClassMount extends PathfinderMob {
    protected ClassMount(EntityType<? extends ClassMount> type, Level level) {
        super(type, level);
    }

    /** The item this mount folds back into. */
    protected abstract Item item();

    /** The item it folds into, carrying whatever the mount keeps (fuel...). */
    public ItemStack toItem() {
        return new ItemStack(item());
    }

    /** Reads back what {@link #toItem()} stored. */
    public void fromItem(ItemStack stack) {
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide) {
                fold(player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        InteractionResult special = interactWith(player, hand);
        if (special.consumesAction()) {
            return special;
        }
        if (!isVehicle()) {
            if (!level().isClientSide) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /** Something the mount does with the item in hand before being ridden (fuel...). */
    protected InteractionResult interactWith(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    /** Folds the mount back into its item and gives it to the player. */
    public void fold(Player player) {
        ejectPassengers();
        ItemStack stack = toItem();
        if (!player.addItem(stack)) {
            spawnAtLocation(stack);
        }
        discard();
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        setRot(player.getYRot(), player.getXRot() * 0.5F);
        yRotO = yBodyRot = yHeadRot = getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travel) {
        float forward = player.zza;
        if (forward <= 0F) {
            forward *= 0.25F;
        }
        return new Vec3(player.xxa * 0.5F, 0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker != null && hasPassenger(attacker)) {
            return false;   // the rider swinging at a zombie does not hit the saddle
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        boolean already = dead;
        super.die(source);
        if (!already && !level().isClientSide) {
            spawnAtLocation(toItem());   // a broken mount is still its item: nothing is lost
        }
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }
}
