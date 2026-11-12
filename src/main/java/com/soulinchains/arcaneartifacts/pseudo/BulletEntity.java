package com.soulinchains.arcaneartifacts.pseudo;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A magic bullet of the Pseudomage's firearms: a fast, straight, orange round that releases the gun's
 * glyph where it hits. Not saved with the world.
 */
public class BulletEntity extends ThrowableItemProjectile {
    private static final DustParticleOptions TRAIL = new DustParticleOptions(new Vector3f(1.0f, 0.55f, 0.1f), 0.7f);
    private static final int LIFETIME = 60;

    private @Nullable SpellResolver resolver;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public BulletEntity(Level level, LivingEntity shooter, SpellResolver resolver) {
        super(ModEntities.BULLET.get(), shooter, level);
        this.resolver = resolver;
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BULLET.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(TRAIL, getX(), getY(), getZ(), 0, 0, 0);
        } else if (tickCount > LIFETIME) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != getOwner();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        resolve(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        resolve(result);
    }

    private void resolve(HitResult result) {
        if (!level().isClientSide) {
            if (resolver != null) {
                resolver.onResolveEffect(level(), result);
            }
            discard();
        }
    }
}
