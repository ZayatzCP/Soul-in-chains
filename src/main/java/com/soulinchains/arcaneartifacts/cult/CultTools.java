package com.soulinchains.arcaneartifacts.cult;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

/**
 * The Warden's covering tools: the Call (taunt), the Seal of Shelter (a short ward on the Confessor) and the Interception
 * (a share of the blows on the Confessor goes to the Warden).
 */
public final class CultTools {
    public static final int HAMMER_REPAIR = 40;
    public static final double CALL_RANGE = 8;
    public static final int CALL_COOLDOWN = 200;
    public static final int WARD_TICKS = 80;
    public static final int WARD_COOLDOWN = 360;
    public static final double WARD_RANGE = 16;
    public static final float WARD_REDUCTION = 0.6F;
    public static final double INTERCEPT_RANGE = 6;
    public static final float INTERCEPT_SHARE = 0.3F;
    private static final String CALL_READY = ArcaneArtifacts.MODID + ":call_ready";
    private static final String WARD_READY = ArcaneArtifacts.MODID + ":ward_ready";

    private CultTools() {
    }

    private static boolean ready(Player player, String key) {
        return player.getPersistentData().getLong(key) <= player.level().getGameTime();
    }

    private static void spend(Player player, String key, int ticks) {
        player.getPersistentData().putLong(key, player.level().getGameTime() + ticks);
    }

    /** The Call: every ordinary hostile creature around turns on the Warden. Returns how many answered (0 on cooldown). */
    public static int call(Player warden) {
        if (!(warden.level() instanceof ServerLevel level) || !ready(warden, CALL_READY)) {
            return 0;
        }
        int called = 0;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, warden.getBoundingBox().inflate(CALL_RANGE),
                m -> m instanceof Enemy && m.isAlive() && !m.getType().is(Tags.EntityTypes.BOSSES) && !RitualSummons.summoned(m)
                        && !m.isAlliedTo(warden))) {
            mob.setTarget(warden);
            called++;
        }
        if (called > 0) {
            spend(warden, CALL_READY, CALL_COOLDOWN);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, warden.getX(), warden.getY() + 2.2, warden.getZ(), 3, 0.3, 0.1, 0.3, 0);
            level.playSound(null, warden.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 0.6F);
        }
        return called;
    }

    /** The Seal of Shelter on the Warden's Confessor. Returns false on cooldown or with no Confessor in range. */
    public static boolean ward(Player warden, @Nullable Player confessor) {
        if (!(warden.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!ready(warden, WARD_READY)) {
            warden.displayClientMessage(Component.translatable("arcane_artifacts.cult.ward_cooldown"), true);
            return false;
        }
        if (confessor == null || confessor.distanceTo(warden) > WARD_RANGE || confessor.level() != warden.level()) {
            warden.displayClientMessage(Component.translatable("arcane_artifacts.cult.ward_no_confessor"), true);
            return false;
        }
        confessor.addEffect(new MobEffectInstance(ModEffects.WARDED, WARD_TICKS, 0, false, true, true));
        spend(warden, WARD_READY, WARD_COOLDOWN);
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI * 2 / 32;
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, confessor.getX() + Math.cos(a) * 1.4, confessor.getY() + 1 + Math.sin(a * 2) * 0.6,
                    confessor.getZ() + Math.sin(a) * 1.4, 1, 0, 0, 0, 0);
        }
        level.playSound(null, confessor.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }

    /** Whether the Warden is covering: holds the Oath Shield or the Hammer and stands near. */
    public static boolean covering(Player warden, Player confessor) {
        boolean armed = warden.getMainHandItem().getItem() instanceof OathShieldItem || warden.getOffhandItem().getItem() instanceof OathShieldItem
                || warden.getMainHandItem().getItem() instanceof SealHammerItem || warden.getOffhandItem().getItem() instanceof SealHammerItem;
        return armed && warden.isAlive() && warden.level() == confessor.level() && warden.distanceTo(confessor) <= INTERCEPT_RANGE;
    }

    /** How much of this blow on the Confessor the Warden takes over: 30% of a creature's blow, less of a player's. */
    public static float interceptShare(DamageSource source) {
        return source.getEntity() instanceof Player ? Config.CULT_INTERCEPT_PVP.get().floatValue() : INTERCEPT_SHARE;
    }

    private static boolean intercepting;

    /** Splits the blow: returns what the Confessor still takes; the Warden takes the rest from the same source. */
    public static float intercept(Player confessor, Player warden, DamageSource source, float amount) {
        if (intercepting || !covering(warden, confessor)) {
            return amount;
        }
        float taken = amount * interceptShare(source);
        if (taken <= 0) {
            return amount;
        }
        intercepting = true;
        try {
            warden.invulnerableTime = 0;
            warden.hurt(source, taken);
        } finally {
            intercepting = false;
        }
        return amount - taken;
    }

    public static boolean warded(LivingEntity entity) {
        return entity.hasEffect(ModEffects.WARDED);
    }
}
