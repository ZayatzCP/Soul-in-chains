package com.soulinchains.arcaneartifacts.necro;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.glyph.AreaEffect;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Phantom Step (Overseer): for 2 seconds (+1 per Extend Time) you are a phantom - nothing can hurt you, creatures lose
 * sight of you, but you cannot attack. If a wall one block thick stands right in front of you, you step through it.
 */
public class EffectPhantomStep extends AreaEffect {
    public static final EffectPhantomStep INSTANCE = new EffectPhantomStep();
    public static final int BASE_TICKS = 40;

    private EffectPhantomStep() {
        super(ArcaneArtifacts.id("glyph_phantom_step"), "Phantom Step");
    }

    @Override
    protected void resolveAt(ServerLevel level, Vec3 point, @Nullable Entity target, LivingEntity shooter, SpellStats stats, SpellContext context) {
        if (shooter instanceof Player player && !Necro.requireRank(player, Necro.OVERSEER)) {
            return;
        }
        int ticks = Math.max(10, BASE_TICKS + (int) Math.round(stats.getDurationMultiplier() * 20));
        phase(level, shooter, ticks);
    }

    /** Becomes a phantom and slips through a thin wall ahead, if there is one. Returns true when it went through a wall. */
    public static boolean phase(ServerLevel level, LivingEntity who, int ticks) {
        level.sendParticles(ParticleTypes.SOUL, who.getX(), who.getY() + 1, who.getZ(), 20, 0.3, 0.6, 0.3, 0.03);
        who.addEffect(new MobEffectInstance(ModEffects.PHANTOM, ticks, 0, false, true, true));
        who.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(who.blockPosition()).inflate(24), m -> m.getTarget() == who)) {
            mob.setTarget(null);
        }
        boolean through = stepThrough(level, who);
        level.playSound(null, who.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 1.5F);
        return through;
    }

    /** A wall exactly one block thick in the facing direction, with room behind it: step through. */
    public static boolean stepThrough(Level level, LivingEntity who) {
        Direction facing = who.getDirection();
        BlockPos feet = who.blockPosition();
        BlockPos wall = feet.relative(facing);
        BlockPos behind = wall.relative(facing);
        if (!solid(level, wall) && !solid(level, wall.above())) {
            return false;
        }
        if (solid(level, behind) || solid(level, behind.above())) {
            return false;
        }
        who.teleportTo(behind.getX() + 0.5, behind.getY(), behind.getZ() + 0.5);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL, behind.getX() + 0.5, behind.getY() + 1, behind.getZ() + 0.5, 20, 0.3, 0.6, 0.3, 0.03);
        }
        return true;
    }

    private static boolean solid(Level level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    @Override
    public int getDefaultManaCost() {
        return 40;
    }

    @Override
    public SpellTier defaultTier() {
        return SpellTier.TWO;
    }

    @NotNull
    @Override
    public Set<AbstractAugment> getCompatibleAugments() {
        return augmentSetOf(AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }

    @Override
    public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
        super.addAugmentDescriptions(map);
        map.put(AugmentExtendTime.INSTANCE, "One second longer as a phantom.");
        map.put(AugmentDurationDown.INSTANCE, "A shorter phantom moment.");
    }

    @Override
    public String getBookDescription() {
        return "Overseer rank. For 2 seconds nothing can hurt you and creatures lose sight of you, but you cannot attack. Steps through a wall one block thick in front of you.";
    }

    @Override
    protected @NotNull Set<SpellSchool> getSchools() {
        return setOf(SpellSchools.NECROMANCY);
    }
}
