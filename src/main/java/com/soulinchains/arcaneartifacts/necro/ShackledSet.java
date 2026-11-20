package com.soulinchains.arcaneartifacts.necro;

import com.soulinchains.arcaneartifacts.accessory.ArcaneSets;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/**
 * The Shackled set: Soul Lantern, Shackle Gloves, Shroud, Bone Crown, Phylactery Amulet, Shackled Boots.
 * <ul>
 *     <li>Gloves: chains fly 1.5 times farther, shackles hold 1 second longer.</li>
 *     <li>Shroud: servants rise with 30% more health; +75 maximum mana.</li>
 *     <li>Crown: one more servant; undead weaker than your rank leave you alone.</li>
 *     <li>Full set, Chain Reaction: a servant that falls lashes the nearest enemies with its chain; caught souls are
 *     sometimes one step stronger.</li>
 * </ul>
 */
public final class ShackledSet {
    public static final int SHROUD_MANA = 75;
    public static final float SHROUD_HEALTH = 0.3F;
    public static final double GLOVES_RANGE = 1.5;
    public static final int GLOVES_HOLD_TICKS = 20;
    public static final float SOUL_UPGRADE_CHANCE = 0.15F;
    public static final double BOOTS_KNOCKBACK = 0.5;

    private ShackledSet() {
    }

    public static boolean wearsGloves(LivingEntity entity) {
        return ArcaneSets.has(entity, ModItems.SHACKLE_GLOVES.get());
    }

    public static boolean wearsShroud(LivingEntity entity) {
        return ArcaneSets.has(entity, ModItems.SHROUD.get());
    }

    /** The Bone Crown or the Imprisoned King's Crown, which does everything the Bone Crown does. */
    public static boolean wearsCrown(LivingEntity entity) {
        return ArcaneSets.has(entity, ModItems.BONE_CROWN.get()) || wearsKingsCrown(entity);
    }

    public static boolean wearsKingsCrown(LivingEntity entity) {
        return ArcaneSets.has(entity, ModItems.KINGS_CROWN.get());
    }

    public static boolean wearsBoots(LivingEntity entity) {
        return ArcaneSets.has(entity, ModItems.SHACKLED_BOOTS.get());
    }

    public static boolean complete(LivingEntity entity) {
        return ArcaneSets.Set.SHACKLED.complete(entity);
    }

    /** Chain Reaction: the fallen servant's chain lashes up to three enemies around it. */
    public static void chainReaction(ServerLevel level, Servant fallen, Player master) {
        float damage = 5 + 2 * Necro.rank(master);
        List<LivingEntity> foes = level.getEntitiesOfClass(LivingEntity.class, new AABB(fallen.blockPosition()).inflate(6),
                        e -> e.isAlive() && e != master && !fallen.isAlliedTo(e)
                                && (e instanceof Mob mob && (mob.getTarget() == master || mob.getTarget() instanceof Servant) || e == master.getLastHurtMob()))
                .stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(fallen))).limit(3).toList();
        for (LivingEntity foe : foes) {
            foe.hurt(level.damageSources().indirectMagic(fallen, master), damage);
            level.sendParticles(ParticleTypes.SOUL, foe.getX(), foe.getY() + foe.getBbHeight() / 2, foe.getZ(), 8, 0.2, 0.3, 0.2, 0.02);
        }
        if (!foes.isEmpty()) {
            level.playSound(null, fallen.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.2F, 0.6F);
        }
    }

    /** A soul caught by a full-set wearer is sometimes one step stronger. */
    public static SoulType caught(LivingEntity catcher, SoulType type) {
        if (complete(catcher) && catcher.getRandom().nextFloat() < SOUL_UPGRADE_CHANCE) {
            return type.upgraded();
        }
        return type;
    }
}
