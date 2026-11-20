package com.soulinchains.arcaneartifacts.necro.mob;

import com.soulinchains.arcaneartifacts.necro.ChainBolt;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Chain Ghost: a Restless of the Soul Dungeons that throws chains. Every few seconds it hooks its target from afar and
 * drags it close - into traps, off ledges, to the Jailer.
 */
public class ChainGhost extends Restless {
    public static final int THROW_TICKS = 80;
    private int throwCooldown = 40;

    public ChainGhost(EntityType<? extends ChainGhost> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Restless.createAttributes()
                .add(Attributes.MAX_HEALTH, 22)
                .add(Attributes.ATTACK_DAMAGE, 3);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        LivingEntity target = getTarget();
        if (--throwCooldown <= 0 && target != null && target.isAlive() && distanceTo(target) > 4 && distanceTo(target) < 14 && hasLineOfSight(target)) {
            throwChain(target);
            throwCooldown = THROW_TICKS;
        }
    }

    /** Aims at the target and throws a chain that reels it in. */
    public void throwChain(LivingEntity target) {
        double dx = target.getX() - getX();
        double dy = target.getEyeY() - 0.3 - getEyeY();
        double dz = target.getZ() - getZ();
        setYRot((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F);
        setXRot((float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG));
        level().addFreshEntity(new ChainBolt(level(), this, null, false, 1.6F));
        level().playSound(null, blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 1.0F, 0.7F);
    }
}
