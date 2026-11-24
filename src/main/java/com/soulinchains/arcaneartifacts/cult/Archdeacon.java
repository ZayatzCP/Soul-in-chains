package com.soulinchains.arcaneartifacts.cult;

import com.soulinchains.arcaneartifacts.registry.ModEffects;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Archdeacon (mini-boss of the Archive of Unkept Promises) tests a pair:
 * <ul>
 *     <li>every 12 s, the <b>trial of cover</b>: a seal falls on the Confessor after 1.5 s - 10 damage, cut to a fifth if
 *     their Warden stands within 6 blocks or they are Warded; a blow that lands in full heals the Archdeacon by 10;</li>
 *     <li>every 20 s, the <b>trial of unison</b>: two points flare on either side of his hall; two players must stand on them
 *     at the same moment within 3 s, or he raises a shield (70% less damage) for 8 s. Passed, he takes 20 damage;</li>
 *     <li>below half health he calls two Renegade Sealers, once.</li>
 * </ul>
 * Everyone who fought him (and their partner nearby) is marked: rank III, Voices of Dogma, needs it.
 */
public class Archdeacon extends Monster {
    public static final float COVER_DAMAGE = 10F;
    public static final float COVER_FACTOR = 0.2F;
    public static final double COVER_RANGE = 6;
    public static final int COVER_EVERY = 240;
    public static final int UNISON_EVERY = 400;
    public static final int UNISON_WINDOW = 60;
    public static final int SHIELD_TICKS = 160;
    public static final float SHIELD_FACTOR = 0.3F;
    public static final double POINT_OFFSET = 5;
    public static final double POINT_RADIUS = 1.5;
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.arcane_artifacts.archdeacon"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private final Set<UUID> fighters = new HashSet<>();
    @Nullable
    private BlockPos home;
    @Nullable
    private UUID coverTarget;
    private int coverAt = -1;
    @Nullable
    private Vec3[] points;
    private int unisonUntil = -1;
    private int shieldUntil = -1;
    private boolean called;
    private int clock;

    public Archdeacon(EntityType<? extends Archdeacon> type, Level level) {
        super(type, level);
        xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 220)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void setHome(BlockPos pos) {
        home = pos.immutable();
    }

    public boolean shielded() {
        return tickCount < shieldUntil;
    }

    @Nullable
    public Vec3[] unisonPoints() {
        return tickCount <= unisonUntil ? points : null;
    }

    // ------------------------------------------------------------------ trials

    /** How much of the trial of cover lands on {@code confessor}: a fifth with the Warden near or a Warding on them. */
    public static float coverFactor(Player confessor, @Nullable Player warden) {
        boolean covered = confessor.hasEffect(ModEffects.WARDED)
                || warden != null && warden != confessor && warden.isAlive() && warden.distanceTo(confessor) <= COVER_RANGE;
        return covered ? COVER_FACTOR : 1F;
    }

    /** The seal falls; returns the damage dealt. */
    public float strikeCover(Player victim, @Nullable Player warden) {
        float factor = coverFactor(victim, warden);
        float damage = COVER_DAMAGE * factor;
        victim.invulnerableTime = 0;
        victim.hurt(damageSources().indirectMagic(this, this), damage);
        if (factor >= 1F) {
            heal(10F);
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new DustParticleOptions(new Vector3f(0.72F, 0.27F, 0.24F), 2.0F), victim.getX(), victim.getY() + 1, victim.getZ(),
                    30, 0.4, 0.6, 0.4, 0);
            server.playSound(null, victim.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.7F, 0.6F);
        }
        return damage;
    }

    /** Opens the trial of unison around the hall's centre. */
    public void beginUnison() {
        Vec3 centre = Vec3.atBottomCenterOf(home != null ? home : blockPosition());
        boolean alongX = random.nextBoolean();
        points = new Vec3[]{centre.add(alongX ? POINT_OFFSET : 0, 0, alongX ? 0 : POINT_OFFSET),
                centre.add(alongX ? -POINT_OFFSET : 0, 0, alongX ? 0 : -POINT_OFFSET)};
        unisonUntil = tickCount + UNISON_WINDOW;
        playSound(SoundEvents.BELL_BLOCK, 1.5F, 0.5F);
    }

    /** Whether two different players stand on the two points right now; passing ends the trial and costs him 20. */
    public boolean checkUnison(List<? extends Player> players) {
        Vec3[] open = unisonPoints();
        if (open == null) {
            return false;
        }
        Player first = null;
        Player second = null;
        for (Player p : players) {
            if (first == null && p.position().distanceTo(open[0]) <= POINT_RADIUS) {
                first = p;
            } else if (second == null && p.position().distanceTo(open[1]) <= POINT_RADIUS) {
                second = p;
            }
        }
        if (first == null || second == null || first == second) {
            return false;
        }
        unisonUntil = -1;
        points = null;
        shieldUntil = -1;
        invulnerableTime = 0;
        hurt(damageSources().magic(), 20F);
        playSound(SoundEvents.BELL_RESONATE, 1.5F, 1.2F);
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) {
            fighters.add(player.getUUID());
        }
        if (shielded() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= SHIELD_FACTOR;
        }
        return super.hurt(source, amount);
    }

    public void markFighter(Player player) {
        fighters.add(player.getUUID());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (home == null) {
            home = blockPosition();
        }
        List<Player> near = level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(24), p -> p.isAlive() && !p.isSpectator());
        if (near.isEmpty()) {
            return;
        }
        clock++;
        // trial of cover
        if (clock % COVER_EVERY == 0) {
            Player confessor = near.stream().filter(p -> Cult.role(p) == CultRole.CONFESSOR).findFirst()
                    .orElse(getTarget() instanceof Player t ? t : near.get(0));
            coverTarget = confessor.getUUID();
            coverAt = tickCount + 30;
            level.sendParticles(ParticleTypes.ENCHANT, confessor.getX(), confessor.getY() + 2.5, confessor.getZ(), 40, 0.4, 0.3, 0.4, 0.6);
            confessor.displayClientMessage(Component.translatable("arcane_artifacts.cult.archdeacon_cover"), true);
        }
        if (tickCount == coverAt && coverTarget != null) {
            Player victim = level.getPlayerByUUID(coverTarget);
            if (victim != null && victim.distanceTo(this) < 32) {
                Player warden = Cult.get(victim).partner().map(level::getPlayerByUUID).orElse(null);
                strikeCover(victim, warden);
            }
            coverTarget = null;
        }
        // trial of unison
        if (clock % UNISON_EVERY == UNISON_EVERY / 2) {
            beginUnison();
            for (Player p : near) {
                p.displayClientMessage(Component.translatable("arcane_artifacts.cult.archdeacon_unison"), true);
            }
        }
        Vec3[] open = unisonPoints();
        if (open != null) {
            for (Vec3 point : open) {
                level.sendParticles(new DustParticleOptions(new Vector3f(0.98F, 0.97F, 0.95F), 1.5F), point.x, point.y + 0.2, point.z, 6, 0.5, 0.05, 0.5, 0);
            }
            checkUnison(near);
            if (tickCount == unisonUntil) {
                shieldUntil = tickCount + SHIELD_TICKS;
                points = null;
                playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.5F, 0.6F);
            }
        }
        if (shielded() && tickCount % 5 == 0) {
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 1.2, getZ(), 6, 0.6, 0.9, 0.6, 0.1);
        }
        // the renegades
        if (!called && getHealth() < getMaxHealth() / 2) {
            called = true;
            for (int i = -1; i <= 1; i += 2) {
                RenegadeSealer sealer = ModEntities.RENEGADE_SEALER.get().create(level);
                if (sealer != null) {
                    sealer.moveTo(getX() + i * 2, getY(), getZ(), getYRot(), 0);
                    level.addFreshEntity(sealer);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, sealer.getX(), sealer.getY() + 1, sealer.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
                }
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        if (source.getEntity() instanceof Player killer) {
            fighters.add(killer.getUUID());
        }
        super.die(source);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(32))) {
            boolean fought = fighters.contains(player.getUUID())
                    || Cult.get(player).partner().map(fighters::contains).orElse(false);
            if (fought) {
                Cult.reward(player);
            }
        }
        level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.5, getZ(), 80, 0.8, 1.2, 0.8, 0.05);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
        tag.putBoolean("Called", called);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
        called = tag.getBoolean("Called");
    }
}
