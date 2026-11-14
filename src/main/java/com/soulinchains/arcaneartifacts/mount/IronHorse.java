package com.soulinchains.arcaneartifacts.mount;

import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Pseudomage's Iron Horse: a riveted steam horse. Fast, heavily armoured, fireproof, walks straight up
 * one-and-a-half-block steps, ignores falls under ten blocks. It burns fuel (anything a furnace burns:
 * right-click to feed it, or load its coal bunker from the inventory key while riding) while it moves; on an empty boiler it only crawls. The Pseudomage's spurs work.
 */
public class IronHorse extends ClassMount implements net.minecraft.world.entity.HasCustomInventoryScreen {
    /** Fuel ticks per furnace tick: a lump of coal (1600) runs it for 2 minutes 40 seconds of travel. */
    public static final int FUEL_PER_BURN_TICK = 2;
    public static final int MAX_FUEL = 20 * 60 * 30;
    /** The coal bunker: a furnace-style fuel slot, burnt one item at a time whenever the boiler runs dry. */
    private final net.minecraft.world.SimpleContainer bunker = new net.minecraft.world.SimpleContainer(1);
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(IronHorse.class, EntityDataSerializers.INT);

    public IronHorse(EntityType<? extends IronHorse> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.45)
                .add(Attributes.STEP_HEIGHT, 1.6)
                .add(Attributes.SAFE_FALL_DISTANCE, 10.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FUEL, 0);
    }

    public int getFuel() {
        return entityData.get(FUEL);
    }

    public void setFuel(int fuel) {
        entityData.set(FUEL, Math.max(0, Math.min(MAX_FUEL, fuel)));
    }

    @Override
    protected Item item() {
        return ModItems.IRON_HORSE.get();
    }

    public net.minecraft.world.SimpleContainer bunker() {
        return bunker;
    }

    @Override
    public ItemStack toItem() {
        ItemStack stack = super.toItem();
        if (getFuel() > 0 || !bunker.getItem(0).isEmpty()) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Fuel", getFuel());
            if (!bunker.getItem(0).isEmpty()) {
                tag.put("Bunker", bunker.getItem(0).save(level().registryAccess()));
            }
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return stack;
    }

    @Override
    public void fromItem(ItemStack stack) {
        setFuel(storedFuel(stack));
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().contains("Bunker")) {
            bunker.setItem(0, ItemStack.parseOptional(level().registryAccess(), data.copyTag().getCompound("Bunker")));
        }
    }

    /** Burns the next item from the bunker when the boiler is empty. */
    public void stoke() {
        ItemStack coal = bunker.getItem(0);
        if (getFuel() <= 0 && !coal.isEmpty() && feed(coal)) {
            ItemStack remainder = coal.getCraftingRemainingItem();
            coal.shrink(1);
            if (coal.isEmpty() && !remainder.isEmpty()) {
                bunker.setItem(0, remainder);
            }
            bunker.setChanged();
        }
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        if (!level().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, p) -> new IronHorseMenu(id, inventory, bunker, fuelData(), this), getDisplayName()));
        }
    }

    private net.minecraft.world.inventory.ContainerData fuelData() {
        return new net.minecraft.world.inventory.ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? getFuel() : MAX_FUEL;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }



    public static int storedFuel(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getInt("Fuel");
    }

    /** Feeds the boiler: true if the stack burns (one item is used). */
    public boolean feed(ItemStack stack) {
        int burn = stack.getBurnTime(RecipeType.SMELTING);
        if (burn <= 0 || getFuel() >= MAX_FUEL) {
            return false;
        }
        setFuel(getFuel() + burn * FUEL_PER_BURN_TICK);
        return true;
    }

    @Override
    protected InteractionResult interactWith(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getBurnTime(RecipeType.SMELTING) > 0) {
            if (!level().isClientSide && feed(stack)) {
                ItemStack remainder = stack.getCraftingRemainingItem();
                stack.consume(1, player);
                if (!remainder.isEmpty() && !player.addItem(remainder)) {
                    spawnAtLocation(remainder);
                }
                playSound(SoundEvents.FIRECHARGE_USE, 0.6F, 0.8F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        float speed = super.getRiddenSpeed(player);
        return getFuel() > 0 ? speed : speed * 0.25F;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        boolean moving = input.lengthSqr() > 1.0E-4;
        if (!level().isClientSide && moving) {
            stoke();
            if (getFuel() > 0 && !player.getAbilities().instabuild) {
                setFuel(getFuel() - 1);
            }
        }
        if (level().isClientSide && moving && getFuel() > 0 && tickCount % 3 == 0) {
            Vec3 chimney = position().add(Vec3.directionFromRotation(0F, getYRot()).scale(0.35)).add(0, 1.85, 0);
            level().addParticle(ParticleTypes.LARGE_SMOKE, chimney.x, chimney.y, chimney.z, 0, 0.06, 0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fuel", getFuel());
        if (!bunker.getItem(0).isEmpty()) {
            tag.put("Bunker", bunker.getItem(0).save(level().registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFuel(tag.getInt("Fuel"));
        bunker.setItem(0, tag.contains("Bunker") ? ItemStack.parseOptional(level().registryAccess(), tag.getCompound("Bunker")) : ItemStack.EMPTY);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 0.4F, 1.3F);
    }
}
