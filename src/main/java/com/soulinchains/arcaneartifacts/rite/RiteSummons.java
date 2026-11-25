package com.soulinchains.arcaneartifacts.rite;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Creatures a rite calls up. They fight for the participants for a while: they never turn on a player or on each other,
 * look for hostile creatures nearby, may follow one participant (Klyatva) and are gone when their time runs out. What they
 * drop when killed is multiplied by the rite's loot multiplier, once. A late close (no Klyatva) makes them hostile instead.
 */
@EventBusSubscriber(modid = ArcaneArtifacts.MODID)
public final class RiteSummons {
    public static final String TAG = ArcaneArtifacts.MODID + ":rite_summoned";

    private RiteSummons() {
    }

    public static boolean summoned(Entity entity) {
        return entity.getPersistentData().contains(TAG);
    }

    public static boolean allied(Entity entity) {
        return summoned(entity) && !entity.getPersistentData().getCompound(TAG).getBoolean("hostile");
    }

    /** Marks a creature as called by a rite: its life, loot multiplier, the participant it follows and whether it is hostile. */
    public static void mark(Entity entity, long until, float loot, @Nullable UUID follows, boolean hostile) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("until", until);
        tag.putFloat("loot", loot);
        tag.putBoolean("hostile", hostile);
        if (follows != null) {
            tag.putUUID("follows", follows);
        }
        entity.getPersistentData().put(TAG, tag);
    }

    /** Calls up {@code count} creatures of a kind around {@code at}. */
    public static List<Mob> raise(ServerLevel level, BlockPos at, net.minecraft.world.entity.EntityType<?> type, int count, int lifeTicks,
                                  float loot, boolean empowered, float eliteChance, @Nullable Player follows, boolean hostile, List<Player> owners) {
        List<Mob> risen = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Entity entity = type.create(level);
            if (!(entity instanceof Mob mob)) {
                if (entity != null) {
                    entity.discard();
                }
                continue;
            }
            double angle = Math.PI * 2 * i / Math.max(1, count);
            mob.moveTo(at.getX() + 0.5 + Math.cos(angle) * 2, at.getY(), at.getZ() + 0.5 + Math.sin(angle) * 2, level.random.nextFloat() * 360, 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
            mob.setPersistenceRequired();
            if (!hostile) {
                if (mob instanceof TamableAnimal tame && !owners.isEmpty()) {
                    tame.tame(owners.get(i % owners.size()));
                }
                if (mob instanceof IronGolem golem) {
                    golem.setPlayerCreated(true);
                }
            }
            if (empowered) {
                var health = mob.getAttribute(Attributes.MAX_HEALTH);
                if (health != null) {
                    health.addOrReplacePermanentModifier(new AttributeModifier(ArcaneArtifacts.id("rite_venec"), 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, lifeTicks, 0, false, true));
                mob.setHealth(mob.getMaxHealth());
            }
            if (level.random.nextFloat() < eliteChance) {
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, lifeTicks, 1, false, true));
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, lifeTicks, 1, false, true));
                mob.setCustomName(Component.translatable("arcane_artifacts.rite.elite", mob.getType().getDescription()));
            }
            mark(mob, level.getGameTime() + lifeTicks, loot, follows == null ? null : follows.getUUID(), hostile);
            level.addFreshEntity(mob);
            level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1, mob.getZ(), 30, 0.4, 0.8, 0.4, 0.03);
            risen.add(mob);
        }
        return risen;
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity.level() instanceof ServerLevel level) || !summoned(entity) || entity.tickCount % 20 != 0) {
            return;
        }
        CompoundTag tag = entity.getPersistentData().getCompound(TAG);
        if (level.getGameTime() >= tag.getLong("until")) {
            level.sendParticles(ParticleTypes.ASH, entity.getX(), entity.getY() + 1, entity.getZ(), 20, 0.3, 0.6, 0.3, 0.01);
            entity.discard();
            return;
        }
        if (!(entity instanceof Mob mob) || tag.getBoolean("hostile")) {
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || target instanceof Player || summoned(target)) {
            LivingEntity next = level.getNearestEntity(LivingEntity.class, net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat().range(16)
                            .selector(e -> e instanceof Enemy && !summoned(e)),
                    mob, mob.getX(), mob.getY(), mob.getZ(), mob.getBoundingBox().inflate(16));
            mob.setTarget(next);
        }
        if (tag.hasUUID("follows")) {
            Player leader = level.getPlayerByUUID(tag.getUUID("follows"));
            if (leader != null && mob.getTarget() == null && mob.distanceTo(leader) > 8) {
                mob.getNavigation().moveTo(leader, 1.1);
            }
        }
    }

    /** An allied summon never takes a player or another summon as its target. */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity self = event.getEntity();
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null || !allied(self)) {
            return;
        }
        if (target instanceof Player || allied(target)) {
            event.setCanceled(true);
        }
    }

    /** The loot multiplier, paid once. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (!summoned(dead) || !(dead.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag tag = dead.getPersistentData().getCompound(TAG);
        if (tag.getBoolean("paid")) {
            return;
        }
        tag.putBoolean("paid", true);
        float extra = tag.getFloat("loot") - 1F;
        if (extra <= 0) {
            return;
        }
        List<ItemEntity> bonus = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            float more = stack.getCount() * extra;
            int whole = (int) more + (level.random.nextFloat() < more - (int) more ? 1 : 0);
            if (whole > 0) {
                bonus.add(new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(), stack.copyWithCount(whole)));
            }
        }
        event.getDrops().addAll(bonus);
    }

    /** Turns a caged creature into another kind (Transmutation); returns the new creature or null. */
    @Nullable
    public static Entity transmute(ServerLevel level, BlockPos at, net.minecraft.nbt.CompoundTag saved, ResourceLocation to) {
        var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(to).orElse(null);
        if (type == null) {
            return null;
        }
        Entity entity = type.create(level);
        if (entity == null) {
            return null;
        }
        entity.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360, 0);
        if (entity instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), net.minecraft.world.entity.MobSpawnType.CONVERSION, null);
            mob.setPersistenceRequired();
            if (saved.contains("CustomName")) {
                mob.setCustomName(Component.Serializer.fromJson(saved.getString("CustomName"), level.registryAccess()));
            }
        }
        level.addFreshEntity(entity);
        level.sendParticles(ParticleTypes.WITCH, entity.getX(), entity.getY() + 1, entity.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        return entity;
    }

    /** Hostile creatures around a site that the rite stirred up (for clearing after it ends). */
    public static List<Echo> echoesAround(ServerLevel level, BlockPos heart, int radius) {
        return level.getEntitiesOfClass(Echo.class, new AABB(heart).inflate(radius));
    }
}
