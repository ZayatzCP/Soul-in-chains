package com.soulinchains.arcaneartifacts.catto;

import com.soulinchains.arcaneartifacts.catto.CattoBrain.Step;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * What Catto does between errands: it notices things lying around and decides what it needs (a better tool or weapon,
 * headwear or armor for an empty slot, food or torches for the second paw, materials for the queue, fish) and what it
 * does not. A curious Catto takes everything; unneeded things go to the main storage. With a fishing rod in the tool
 * slot and water nearby, Catto fishes while its owner is around.
 */
public final class CattoLoot {
    private CattoLoot() {
    }

    public enum Want { NONE, EQUIP, OFFHAND, BAG }

    private static final String MEMO_FISH = "fish_cooldown";

    /** The next chore, or null when there is nothing to do. */
    @Nullable
    static Step chore(Catto cat, CattoBrain brain) {
        int radius = cat.nature().pickupRadius() + (cat.moduleActive(CattoModule.MAGNET_WHISKER) ? 4 : 0);
        ItemEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (ItemEntity item : cat.level().getEntitiesOfClass(ItemEntity.class, cat.getBoundingBox().inflate(radius, 3, radius),
                e -> e.isAlive() && !e.hasPickUpDelay())) {
            if (brain.blacklisted(item.blockPosition()) || want(cat, item.getItem()) == Want.NONE) {
                continue;
            }
            double d = item.distanceToSqr(cat);
            if (d < bestDist) {
                bestDist = d;
                best = item;
            }
        }
        if (best != null) {
            Step step = new Step(Step.Kind.PICKUP, best.blockPosition());
            step.entity = best;
            step.reach = 1.3;
            step.accuracy = 0;
            step.workTicks = 2;
            return step;
        }
        // Junk goes to the main storage.
        Predicate<ItemStack> junk = s -> !needed(cat, s);
        if (!cat.storages.isEmpty() && CattoWorld.bagCount(cat, junk) > 0) {
            BlockPos storage = CattoWorld.storageFor(cat, junk);
            if (storage != null) {
                Step step = new Step(Step.Kind.PUT, storage);
                step.filter = junk;
                step.reach = 2.5;
                step.workTicks = 8;
                return step;
            }
        }
        return fishing(cat, brain);
    }

    /** Should Catto pick this up, and what for? */
    public static Want want(Catto cat, ItemStack stack) {
        if (stack.isEmpty()) {
            return Want.NONE;
        }
        if (better(cat, stack)) {
            return Want.EQUIP;
        }
        ItemStack offhand = cat.offhand();
        boolean food = stack.has(DataComponents.FOOD);
        if (offhand.isEmpty() && food || !offhand.isEmpty() && ItemStack.isSameItemSameComponents(offhand, stack)
                && offhand.getCount() < offhand.getMaxStackSize()) {
            return Want.OFFHAND;
        }
        if (CattoWorld.bagFull(cat) && !CattoWorld.bagFits(cat, stack)) {
            return Want.NONE;
        }
        if (needed(cat, stack)) {
            return Want.BAG;
        }
        if (cat.nature().takesEverything() || !cat.storages.isEmpty() && cat.nature() != CattoNature.LAZY) {
            return Want.BAG;
        }
        return Want.NONE;
    }

    /** Things Catto keeps: fish, a crafting table, torches, materials and targets of the queue. */
    public static boolean needed(Catto cat, ItemStack stack) {
        if (stack.is(ItemTags.FISHES) || stack.is(Items.CRAFTING_TABLE) || stack.is(Items.TORCH)) {
            return true;
        }
        for (CattoTask task : cat.queue) {
            if (!task.item.isEmpty() && ItemStack.isSameItem(task.item, stack)) {
                return true;
            }
        }
        return false;
    }

    /** A better tool or weapon, or headwear or armor for an empty slot. */
    static boolean better(Catto cat, ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof CattoGear.GearItem gear) {
            return cat.equipment.getStackInSlot(gear.slot == CattoGear.Slot.HEAD ? Catto.SLOT_HEAD : Catto.SLOT_ARMOR).isEmpty();
        }
        if (item instanceof ArmorItem armor) {
            if (armor.getType() == ArmorItem.Type.HELMET) {
                return cat.equipment.getStackInSlot(Catto.SLOT_HEAD).isEmpty();
            }
            if (armor.getType() == ArmorItem.Type.CHESTPLATE) {
                return cat.equipment.getStackInSlot(Catto.SLOT_ARMOR).isEmpty();
            }
            return false;
        }
        if (item instanceof SwordItem sword) {
            ItemStack current = cat.weapon();
            return current.isEmpty() || current.getItem() instanceof SwordItem old
                    && sword.getTier().getAttackDamageBonus() > old.getTier().getAttackDamageBonus();
        }
        if (item instanceof DiggerItem digger) {
            ItemStack current = cat.tool();
            if (current.isEmpty()) {
                return stack.is(ItemTags.PICKAXES);
            }
            return current.getItem() instanceof DiggerItem old && old.getClass() == digger.getClass()
                    && digger.getTier().getSpeed() > old.getTier().getSpeed();
        }
        return false;
    }

    static boolean perform(Catto cat, CattoBrain brain, Step step) {
        switch (step.kind) {
            case PICKUP -> {
                if (!(step.entity instanceof ItemEntity item) || !item.isAlive()) {
                    return false;
                }
                ItemStack stack = item.getItem();
                Want want = want(cat, stack);
                switch (want) {
                    case EQUIP -> {
                        int slot = slotFor(cat, stack);
                        ItemStack old = cat.equipment.getStackInSlot(slot);
                        cat.equipment.setStackInSlot(slot, stack.split(1));
                        if (!old.isEmpty()) {
                            ItemStack rest = CattoWorld.bagInsert(cat, old);
                            if (!rest.isEmpty()) {
                                cat.spawnAtLocation(rest);
                            }
                        }
                        if (stack.isEmpty()) {
                            item.discard();
                        } else {
                            item.setItem(stack);
                        }
                        cat.say(Component.translatable("arcane_artifacts.catto.loot.equip." + cat.nature().key(),
                                cat.equipment.getStackInSlot(slot).getHoverName()), 60);
                        cat.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.8F, 1.2F);
                        return true;
                    }
                    case OFFHAND -> {
                        ItemStack offhand = cat.offhand();
                        int room = offhand.isEmpty() ? stack.getMaxStackSize() : offhand.getMaxStackSize() - offhand.getCount();
                        ItemStack taken = stack.split(Math.min(room, stack.getCount()));
                        if (offhand.isEmpty()) {
                            cat.equipment.setStackInSlot(Catto.SLOT_OFFHAND, taken);
                        } else {
                            ItemStack merged = offhand.copy();
                            merged.grow(taken.getCount());
                            cat.equipment.setStackInSlot(Catto.SLOT_OFFHAND, merged);
                        }
                        if (stack.isEmpty()) {
                            item.discard();
                        } else {
                            item.setItem(stack);
                        }
                        cat.take(item, taken.getCount());
                        return true;
                    }
                    case BAG -> {
                        boolean useful = needed(cat, stack);
                        int picked = CattoWorld.pickUp(cat, item);
                        if (picked > 0 && cat.getRandom().nextInt(4) == 0) {
                            cat.say(Component.translatable("arcane_artifacts.catto.loot." + (useful ? "useful" : "junk") + "." + cat.nature().key(),
                                    stack.getHoverName()), 50);
                        }
                        return picked > 0;
                    }
                    default -> {
                        return false;
                    }
                }
            }
            case PUT -> {
                return CattoWorld.putInto(cat, step.pos, step.filter) > 0;
            }
            case GOTO -> {
                return fish(cat, brain, step);
            }
            default -> {
                return false;
            }
        }
    }

    private static int slotFor(Catto cat, ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof CattoGear.GearItem gear) {
            return gear.slot == CattoGear.Slot.HEAD ? Catto.SLOT_HEAD : Catto.SLOT_ARMOR;
        }
        if (item instanceof ArmorItem armor) {
            return armor.getType() == ArmorItem.Type.HELMET ? Catto.SLOT_HEAD : Catto.SLOT_ARMOR;
        }
        if (item instanceof SwordItem) {
            return Catto.SLOT_WEAPON;
        }
        return Catto.SLOT_TOOL;
    }

    // ------------------------------------------------------------------ fishing

    @Nullable
    private static Step fishing(Catto cat, CattoBrain brain) {
        if (!(cat.tool().getItem() instanceof CattoGear.RodItem) || CattoWorld.bagFull(cat)) {
            return null;
        }
        Object until = brain.memo.get(MEMO_FISH);
        if (until instanceof Long time && time > cat.level().getGameTime()) {
            return null;
        }
        ServerPlayer owner = cat.ownerPlayer();
        if (owner == null || owner.distanceToSqr(cat) > 12 * 12) {
            return null;
        }
        BlockPos centre = cat.blockPosition();
        BlockPos water = null;
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-6, -2, -6), centre.offset(6, 1, 6))) {
            if (cat.level().getFluidState(p).is(FluidTags.WATER) && cat.level().getBlockState(p.above()).isAir()) {
                water = p.immutable();
                break;
            }
        }
        if (water == null) {
            brain.memo.put(MEMO_FISH, cat.level().getGameTime() + 600);
            return null;
        }
        Step step = new Step(Step.Kind.GOTO, water);
        step.reach = 3.0;
        step.accuracy = 2;
        step.workTicks = 200 + cat.getRandom().nextInt(200);
        return step;
    }

    private static boolean fish(Catto cat, CattoBrain brain, Step step) {
        if (!(cat.level() instanceof ServerLevel level) || !(cat.tool().getItem() instanceof CattoGear.RodItem)) {
            return false;
        }
        brain.memo.put(MEMO_FISH, level.getGameTime() + 400);
        Vec3 at = Vec3.atCenterOf(step.pos);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, at)
                .withParameter(LootContextParams.TOOL, cat.tool())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, cat)
                .withLuck(cat.tier(CattoSkill.GATHERER) - 1)
                .create(LootContextParamSets.FISHING);
        var table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
        for (ItemStack catch_ : table.getRandomItems(params)) {
            ItemStack rest = CattoWorld.bagInsert(cat, catch_);
            if (!rest.isEmpty()) {
                cat.spawnAtLocation(rest);
            }
            cat.say(Component.translatable("arcane_artifacts.catto.loot.fish." + cat.nature().key(), catch_.getHoverName()), 60);
        }
        ItemStack rod = cat.tool().copy();
        rod.hurtAndBreak(1, cat, EquipmentSlot.MAINHAND);
        cat.equipment.setStackInSlot(Catto.SLOT_TOOL, rod);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH, at.x, at.y + 0.5, at.z, 12, 0.3, 0.1, 0.3, 0.1);
        level.playSound(null, step.pos, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.6F, 1.0F);
        cat.addXp(CattoSkill.GATHERER, 2);
        return true;
    }

    @SuppressWarnings("unused")
    private static AABB box(BlockPos pos, int r) {
        return new AABB(pos).inflate(r);
    }
}
