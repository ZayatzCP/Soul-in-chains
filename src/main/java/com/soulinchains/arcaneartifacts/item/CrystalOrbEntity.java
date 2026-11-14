package com.soulinchains.arcaneartifacts.item;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Crystal Ball's spell, thrown like a real heavy ball: it leaves the hand going a little upwards,
 * gravity pulls it into an arc and the air slows it a little; where it lands, the spell resolves.
 */
public class CrystalOrbEntity extends ThrowableItemProjectile {
    public static final double GRAVITY = 0.05;
    public static final float SPEED = 1.35F;
    public static final float LIFT_DEGREES = 8F;
    private static final int MAX_AGE = 200;
    private @Nullable SpellResolver resolver;

    public CrystalOrbEntity(EntityType<? extends CrystalOrbEntity> type, Level level) {
        super(type, level);
    }

    public CrystalOrbEntity(Level level, LivingEntity shooter, @Nullable SpellResolver resolver) {
        super(ModEntities.CRYSTAL_ORB.get(), shooter, level);
        this.resolver = resolver;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CRYSTAL_BALL.get();
    }

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    /** Throws it where {@code shooter} looks, a little upwards, like a ball. */
    public void throwFrom(LivingEntity shooter) {
        shootFromRotation(shooter, shooter.getXRot() - LIFT_DEGREES, shooter.getYRot(), 0F, SPEED, 0.5F);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Vec3 motion = getDeltaMovement();
            for (int i = 0; i < 2; i++) {
                level().addParticle(ParticleTypes.WITCH, getX() - motion.x * i * 0.5, getY() + 0.15 - motion.y * i * 0.5,
                        getZ() - motion.z * i * 0.5, 0, 0, 0);
            }
            level().addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.15, getZ(), 0, 0, 0);
        } else if (this.tickCount > MAX_AGE) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) {
            return;
        }
        if (resolver != null) {
            resolver.onResolveEffect(level(), result);
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
        discard();
    }
}
