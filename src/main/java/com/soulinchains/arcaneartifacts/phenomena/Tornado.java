package com.soulinchains.arcaneartifacts.phenomena;

import com.hollingsworth.arsnouveau.api.ANFakePlayer;
import com.hollingsworth.arsnouveau.api.particle.ParticleEmitter;
import com.hollingsworth.arsnouveau.api.particle.timelines.TimelineEntryData;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.common.datagen.BlockTagProvider;
import com.soulinchains.arcaneartifacts.fx.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A tornado: a funnel that spins creatures and items around its axis and lifts them up; what it carries falls when it
 * dies down. The caster is never caught.
 * <p>
 * Like Ars Nouveau's Burst, but over time: when the spell goes on after the Tornado, the rest of the spell is cast on
 * every creature caught in the funnel (with Sensitive: on the ground under it), each at most once every
 * {@link #PER_TARGET_COOLDOWN} ticks and {@link #MAX_PROCS} times in all. The element of the funnel (fire, frost,
 * water) comes from the rest of the spell, or from the world: a tornado that passes over lava or fire catches fire.
 * With Sensitive it also tears loose blocks out of the ground and whirls them around, then flings them when it ends.
 */
public final class Tornado implements Phenomena.Phenomenon, Phenomena.Located {
    public enum Element { AIR, FIRE, FROST, WATER }

    public static final int PER_TARGET_COOLDOWN = 10;
    public static final int MAX_PROCS = 60;

    /** Everything the tornado needs from the spell. */
    public static final class Settings {
        public double radius = 4.0;
        public int durationTicks = 120;
        public double strength = 1.0;
        /** Rest of the spell to cast on what is caught, or null. */
        @Nullable
        public SpellResolver carrier;
        /** Cast the rest of the spell on blocks and tear blocks loose. */
        public boolean sensitive;
        /** Dampen: thrown blocks are put down gently. */
        public boolean gentle;
        /** Horizontal motion per tick (Accelerate). */
        public Vec3 drift = Vec3.ZERO;
        /** Ticks between two casts of the rest of the spell. */
        public int procInterval = 10;
        /** Hardest block that can be torn loose. */
        public float maxHardness = 3.0F;
        public Element element = Element.AIR;
        public TimelineEntryData style;
        public TimelineEntryData impact;
    }

    private final ServerLevel level;
    private final UUID owner;
    private final Settings settings;
    private final boolean elementFromSpell;
    private Element element;
    private Vec3 center;
    private int ticksLeft;
    private int age;
    private int procsLeft = MAX_PROCS;
    private int liftBudget;
    private final Map<UUID, Integer> nextProc = new HashMap<>();
    private final Set<Long> blocksDone = new HashSet<>();
    private final List<FallingBlockEntity> lifted = new ArrayList<>();
    private final List<ParticleEmitter> emitters = new ArrayList<>();
    private final double[] strandProgress = new double[STRANDS];
    private final Vec3[] strandPosition = new Vec3[STRANDS];

    public Tornado(ServerLevel level, UUID owner, Vec3 center, Settings settings) {
        this.level = level;
        this.owner = owner;
        this.center = center;
        this.settings = settings;
        this.ticksLeft = settings.durationTicks;
        this.element = settings.element;
        this.elementFromSpell = settings.element != Element.AIR;
        this.liftBudget = settings.sensitive ? 12 + (int) Math.round(settings.radius * 1.5) : 0;
        for (int i = 0; i < STRANDS; i++) {
            final int strand = i;
            strandProgress[i] = i / (double) STRANDS;
            strandPosition[i] = funnelPoint(i, strandProgress[i]);
            emitters.add(Fx.follow(settings.style, () -> strandPosition[strand]));
        }
    }

    @Override
    public ServerLevel level() {
        return level;
    }

    @Override
    public UUID owner() {
        return owner;
    }

    @Override
    public Vec3 center() {
        return center;
    }

    public Element element() {
        return element;
    }

    public List<FallingBlockEntity> lifted() {
        return lifted;
    }

    @Override
    public boolean tick() {
        if (ticksLeft-- <= 0) {
            finish();
            return false;
        }
        age++;
        move();
        if (!elementFromSpell && age % 10 == 0) {
            senseWorld();
        }
        double radius = settings.radius;
        AABB area = new AABB(center, center).inflate(radius, 1, radius).expandTowards(0, funnelHeight(), 0);
        boolean proc = settings.carrier != null && procsLeft > 0 && age % settings.procInterval == 0;
        for (Entity entity : level.getEntities((Entity) null, area, e -> e.isAlive() && !e.getUUID().equals(owner) && !e.isSpectator()
                && !(e instanceof Player p && p.getAbilities().instabuild))) {
            if (!spin(entity)) {
                continue;
            }
            if (entity instanceof LivingEntity living) {
                worldElement(living);
                if (proc && !settings.sensitive && procsLeft > 0 && nextProc.getOrDefault(entity.getUUID(), 0) <= age) {
                    nextProc.put(entity.getUUID(), age + PER_TARGET_COOLDOWN);
                    procsLeft--;
                    cast(new EntityHitResult(living));
                    Fx.burst(level, settings.impact, living.position().add(0, living.getBbHeight() / 2, 0));
                }
            }
        }
        if (proc && settings.sensitive) {
            for (int i = 0; i < 3 && procsLeft > 0; i++) {
                BlockPos ground = randomGround(true);
                if (ground != null) {
                    procsLeft--;
                    cast(new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false));
                }
            }
        }
        if (settings.sensitive && liftBudget > 0 && age % 4 == 0) {
            tearLoose();
        }
        lifted.removeIf(Entity::isRemoved);
        particles();
        return true;
    }

    // ------------------------------------------------------------------ motion

    /** Accelerate: the funnel crawls along the ground; it stops at walls taller than two blocks. */
    private void move() {
        if (settings.drift.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 next = center.add(settings.drift);
        BlockPos at = BlockPos.containing(next);
        for (int up = 0; up <= 2 && solid(at); up++) {
            at = at.above();
        }
        if (solid(at)) {
            return;
        }
        int down = 0;
        while (down < 4 && !solid(at.below())) {
            at = at.below();
            down++;
        }
        center = new Vec3(next.x, at.getY(), next.z);
    }

    private boolean solid(BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /** Returns false when the entity is outside the funnel. */
    private boolean spin(Entity entity) {
        Vec3 offset = entity.position().subtract(center);
        Vec3 flat = new Vec3(offset.x, 0, offset.z);
        double distance = flat.length();
        if (distance > settings.radius) {
            return false;
        }
        double strength = settings.strength;
        Vec3 tangent = distance > 0.1 ? new Vec3(-flat.z, 0, flat.x).normalize() : Vec3.ZERO;
        Vec3 inward = distance > 0.1 ? flat.normalize().scale(-0.08) : Vec3.ZERO;
        double lift = entity.getY() < center.y + settings.radius * 2.2 ? 0.13 * strength + 0.06 : -0.02;
        Vec3 motion = tangent.scale(0.45 * strength).add(inward).add(0, lift, 0).add(settings.drift);
        entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5).add(motion));
        entity.hurtMarked = true;
        entity.fallDistance = 0;
        return true;
    }

    // ------------------------------------------------------------------ the rest of the spell

    private void cast(net.minecraft.world.phys.HitResult hit) {
        SpellResolver carrier = settings.carrier;
        if (carrier == null) {
            return;
        }
        SpellResolver resolver = carrier.getNewResolver(carrier.spellContext.clone().makeChildContext());
        resolver.onResolveEffect(level, hit);
    }

    /** A random top block under the funnel (the block itself, not the air above it). */
    @Nullable
    private BlockPos randomGround(boolean once) {
        double r = settings.radius * Math.sqrt(level.random.nextDouble());
        double a = level.random.nextDouble() * Math.PI * 2;
        BlockPos column = BlockPos.containing(center.x + Math.cos(a) * r, center.y + 2, center.z + Math.sin(a) * r);
        for (int i = 0; i < 6; i++) {
            BlockPos pos = column.below(i);
            if (solid(pos)) {
                if (once && !blocksDone.add(pos.asLong())) {
                    return null;
                }
                return pos;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ elements

    /** Over lava or fire the funnel catches fire; over water it turns into a waterspout. */
    private void senseWorld() {
        BlockPos base = BlockPos.containing(center);
        int r = (int) Math.ceil(settings.radius * 0.6);
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-r, -1, -r), base.offset(r, 0, r))) {
            BlockState state = level.getBlockState(pos);
            if (state.getFluidState().is(FluidTags.LAVA) || state.is(net.minecraft.tags.BlockTags.FIRE)) {
                element = Element.FIRE;
                return;
            }
            if (state.getFluidState().is(FluidTags.WATER) && element == Element.AIR) {
                element = Element.WATER;
            }
        }
    }

    /** What the funnel's own element does to a creature it carries (an element from the spell comes with the spell). */
    private void worldElement(LivingEntity living) {
        if (elementFromSpell) {
            return;
        }
        switch (element) {
            case FIRE -> living.igniteForSeconds(3.0F);
            case WATER -> living.clearFire();
            case FROST -> living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ blocks

    private void tearLoose() {
        BlockPos pos = randomGround(false);
        if (pos == null) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty() || hardness < 0 || hardness > settings.maxHardness
                || state.is(BlockTagProvider.BREAK_BLACKLIST) || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
            return;
        }
        Entity caster = level.getEntity(owner);
        if (!(caster instanceof LivingEntity living) || !BlockUtil.destroyRespectsClaim(ANFakePlayer.getOrFakePlayer(level, living), level, pos)) {
            return;
        }
        FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
        block.setNoGravity(true);
        block.setDeltaMovement(0, 0.35, 0);
        lifted.add(block);
        liftBudget--;
    }

    /** The end: lifted blocks are flung away (or put down gently with Dampen). */
    private void releaseBlocks(boolean fling) {
        for (FallingBlockEntity block : lifted) {
            if (block.isRemoved()) {
                continue;
            }
            block.setNoGravity(false);
            if (fling && !settings.gentle) {
                Vec3 flat = block.position().subtract(center).multiply(1, 0, 1);
                Vec3 out = flat.lengthSqr() > 0.01 ? new Vec3(-flat.z, 0, flat.x).normalize().scale(0.7).add(flat.normalize().scale(0.3)) : Vec3.ZERO;
                block.setDeltaMovement(out.add(0, 0.25, 0));
                block.setHurtsEntities(2.0F, 20);
            } else {
                block.setDeltaMovement(0, -0.1, 0);
            }
            block.hurtMarked = true;
        }
        lifted.clear();
    }

    @Override
    public void cancelled() {
        releaseBlocks(false);
    }

    private void finish() {
        releaseBlocks(true);
        Fx.burst(level, settings.impact, center);
    }

    // ------------------------------------------------------------------ particles

    private static final int STRANDS = 3;
    private static final int STEPS_PER_TICK = 4;
    private static final double SWEEP_STEPS = 44.0;
    /** The funnel walls lean out at about 75 degrees from the ground. */
    private static final double WALL_SLOPE = 1.0 / Math.tan(Math.toRadians(75));

    private double funnelHeight() {
        return Math.max(4.0, settings.radius * 2.2);
    }

    private Vec3 funnelPoint(int strand, double t) {
        double h = t * funnelHeight();
        double r = 0.5 + h * WALL_SLOPE;
        double angle = strand * (Math.PI * 2 / STRANDS) + t * Math.PI * 4 + age * 0.25;
        return center.add(Math.cos(angle) * r, h, Math.sin(angle) * r);
    }

    /** Drawn with the glyph's Ars particle timeline (restylable); the element adds its own particles on top. */
    private void particles() {
        @Nullable ParticleOptions extra = switch (element) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case WATER -> ParticleTypes.SPLASH;
            default -> null;
        };
        for (int i = 0; i < STRANDS; i++) {
            for (int step = 0; step < STEPS_PER_TICK; step++) {
                strandProgress[i] += 1.0 / SWEEP_STEPS;
                if (strandProgress[i] >= 1.0) {
                    strandProgress[i] -= 1.0;
                    strandPosition[i] = funnelPoint(i, strandProgress[i]);
                    final int strand = i;
                    emitters.set(i, Fx.follow(settings.style, () -> strandPosition[strand]));
                }
                strandPosition[i] = funnelPoint(i, strandProgress[i]);
                emitters.get(i).tick(level);
            }
            if (extra != null) {
                Vec3 p = strandPosition[i];
                level.sendParticles(extra, p.x, p.y, p.z, 2, 0.15, 0.15, 0.15, 0.01);
            }
        }
        if (element == Element.FIRE && age % 3 == 0) {
            level.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.2, center.z, 1, settings.radius * 0.3, 0, settings.radius * 0.3, 0);
        }
        if (age % 10 == 0) {
            Fx.ring(level, settings.impact, center, settings.radius, Math.max(10, (int) (settings.radius * 3)));
        }
    }
}
