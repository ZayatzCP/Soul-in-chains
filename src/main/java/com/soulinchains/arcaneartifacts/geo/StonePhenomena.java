package com.soulinchains.arcaneartifacts.geo;

import com.mojang.math.Transformation;
import com.soulinchains.arcaneartifacts.phenomena.Phenomena;
import com.soulinchains.arcaneartifacts.util.Allies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What the stone glyphs set going: a cage of real blocks around a creature, lances bursting from the ground, a volley of
 * blocks flying at their targets, a meteor pressed together from the hoard. Stone is not magic: it gives no recoil.
 */
public final class StonePhenomena {
    private StonePhenomena() {
    }

    /** A display stretched to {@code scale}, standing on its base (lances) or centred (everything else). */
    @Nullable
    static Display.BlockDisplay display(ServerLevel level, BlockState state, Vector3f scale, boolean standing, Vec3 at) {
        Display.BlockDisplay entity = EntityType.BLOCK_DISPLAY.create(level);
        if (entity == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        tag.put("block_state", NbtUtils.writeBlockState(state));
        Vector3f offset = new Vector3f(-scale.x / 2, standing ? 0 : -scale.y / 2, -scale.z / 2);
        tag.put("transformation", Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, new Transformation(offset, null, scale, null)).getOrThrow());
        tag.putInt("teleport_duration", 2);
        entity.load(tag);
        entity.addTag(HoardVisuals.TAG);
        entity.setPos(at.x, at.y, at.z);
        level.addFreshEntity(entity);
        return entity;
    }

    /** Stone hits a creature: the caster's thrown-block damage, no invulnerability pause between blocks. */
    static boolean strike(ServerLevel level, @Nullable Entity caster, LivingEntity target, float damage) {
        if (damage <= 0 || target == caster || caster != null && Allies.areAllied(caster, target)) {
            return false;
        }
        DamageSource source = caster != null ? level.damageSources().thrown(caster, caster) : level.damageSources().generic();
        target.invulnerableTime = 0;
        return target.hurt(source, damage);
    }

    static void crumble(ServerLevel level, BlockState state, Vec3 at, int count) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, count, 0.3, 0.3, 0.3, 0.1);
    }

    // ------------------------------------------------------------------ Stone Cage

    /** Real blocks from the hoard stand around a creature and hold it; when the cage falls, its blocks go back to the hoard. */
    public static final class Cage implements Phenomena.Phenomenon, Phenomena.Located {
        private final ServerLevel level;
        private final UUID owner;
        private final LivingEntity target;
        private final java.lang.ref.WeakReference<Player> keeper;
        private final List<BlockPos> placed = new ArrayList<>();
        private final List<BlockState> states = new ArrayList<>();
        private final int life;
        private int ticks;

        public Cage(ServerLevel level, Player owner, LivingEntity target, int life) {
            this.level = level;
            this.owner = owner.getUUID();
            this.keeper = new java.lang.ref.WeakReference<>(owner);
            this.target = target;
            this.life = life;
        }

        /** The places of a cage around a creature: two rings of side posts and a roof (only air is used). */
        public static List<BlockPos> plan(LivingEntity target) {
            AABB box = target.getBoundingBox();
            int x0 = (int) Math.floor(box.minX) - 1;
            int x1 = (int) Math.floor(box.maxX - 1e-4) + 1;
            int z0 = (int) Math.floor(box.minZ) - 1;
            int z1 = (int) Math.floor(box.maxZ - 1e-4) + 1;
            int y0 = (int) Math.floor(box.minY);
            int y1 = (int) Math.floor(box.maxY - 1e-4) + 1;
            List<BlockPos> out = new ArrayList<>();
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    for (int z = z0; z <= z1; z++) {
                        boolean edgeX = x == x0 || x == x1;
                        boolean edgeZ = z == z0 || z == z1;
                        // the walls, without the corners: a cage, not a box
                        if (edgeX != edgeZ) {
                            out.add(new BlockPos(x, y, z));
                        }
                    }
                }
            }
            for (int x = x0 + 1; x < x1; x++) {
                for (int z = z0 + 1; z < z1; z++) {
                    out.add(new BlockPos(x, y1, z));
                }
            }
            return out;
        }

        /** Raises the cage from the given blocks; returns how many it used (the rest go back to the caller). */
        public int raise(List<BlockState> blocks) {
            int used = 0;
            for (BlockPos pos : plan(target)) {
                if (used >= blocks.size()) {
                    break;
                }
                if (level.getBlockState(pos).canBeReplaced()) {
                    BlockState state = blocks.get(used++);
                    level.setBlock(pos, state, 3);
                    placed.add(pos);
                    states.add(state);
                }
            }
            level.playSound(null, target.blockPosition(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.2F, 0.6F);
            return used;
        }

        public List<BlockPos> placed() {
            return placed;
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
            return target.position();
        }

        @Override
        public boolean tick() {
            ticks++;
            if (ticks >= life || placed.isEmpty()) {
                fall();
                return false;
            }
            return true;
        }

        @Override
        public void cancelled() {
            fall();
        }

        /** The cage crumbles; whatever still stands goes back to its owner's hoard (or stays, if the hoard is full or gone). */
        public void fall() {
            Player player = keeper.get();
            if (player != null && player.isRemoved()) {
                player = null;
            }
            for (int i = 0; i < placed.size(); i++) {
                BlockPos pos = placed.get(i);
                BlockState state = states.get(i);
                if (level.getBlockState(pos).is(state.getBlock()) && player != null && StoneHoard.space(player) > 0) {
                    level.removeBlock(pos, false);
                    crumble(level, state, Vec3.atCenterOf(pos), 6);
                    StoneHoard.add(player, state);
                }
            }
            placed.clear();
        }

        public static int active(UUID owner) {
            return (int) Phenomena.active().stream().filter(p -> p instanceof Cage && p.owner().equals(owner)).count();
        }
    }

    // ------------------------------------------------------------------ Stone Lances

    /** Lances of hoard stone burst from the ground under and around a creature, wound it and hold it, then sink. */
    public static final class Lances implements Phenomena.Phenomenon, Phenomena.Located {
        static final int RISE = 4;
        static final int HOLD = 30;
        private final ServerLevel level;
        private final UUID owner;
        private final Vec3 center;
        private final List<Display.BlockDisplay> lances = new ArrayList<>();
        private final List<Vec3> bases = new ArrayList<>();
        private final float damage;
        private final double radius;
        private final int holdTicks;
        private int ticks;

        public Lances(ServerLevel level, Player owner, Vec3 center, List<BlockState> blocks, float damage, double radius, int holdTicks) {
            this.level = level;
            this.owner = owner.getUUID();
            this.center = center;
            this.damage = damage;
            this.radius = radius;
            this.holdTicks = holdTicks;
            for (int i = 0; i < blocks.size(); i++) {
                double angle = Math.PI * 2 * i / blocks.size();
                double r = i == 0 ? 0 : 0.6 + radius * 0.5;
                Vec3 base = new Vec3(center.x + Math.cos(angle) * r, center.y - 2.6, center.z + Math.sin(angle) * r);
                Display.BlockDisplay lance = display(level, blocks.get(i), new Vector3f(0.35F, 2.6F, 0.35F), true, base);
                if (lance != null) {
                    lances.add(lance);
                    bases.add(base);
                }
            }
            level.playSound(null, center.x, center.y, center.z, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.PLAYERS, 1.5F, 0.6F);
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

        @Override
        public boolean tick() {
            ticks++;
            double rise = ticks <= RISE ? 2.6 * ticks / RISE : ticks <= RISE + HOLD ? 2.6 : Math.max(0, 2.6 - 0.3 * (ticks - RISE - HOLD));
            for (int i = 0; i < lances.size(); i++) {
                Vec3 base = bases.get(i);
                lances.get(i).setPos(base.x, base.y + rise, base.z);
            }
            if (ticks == RISE) {
                impale();
            }
            if (ticks > RISE + HOLD + 10) {
                cancelled();
                return false;
            }
            return true;
        }

        private void impale() {
            Entity caster = level.getEntity(owner);
            double reach = 0.9 + radius;
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(reach, 2.5, reach), LivingEntity::isAlive)) {
                if (Math.hypot(living.getX() - center.x, living.getZ() - center.z) > reach) {
                    continue;
                }
                if (strike(level, caster, living, damage) && holdTicks > 0) {
                    com.soulinchains.arcaneartifacts.necro.EffectShackle.shackle(level, living, holdTicks, caster instanceof LivingEntity l ? l : null);
                }
            }
            level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 1, center.z, 30, radius + 0.5, 1, radius + 0.5, 0.2);
        }

        @Override
        public void cancelled() {
            lances.forEach(Display.BlockDisplay::discard);
            lances.clear();
        }
    }

    // ------------------------------------------------------------------ Myriad Volley

    /** The hoard flies: one block a tick leaves its owner and homes on a target, wounding it where it strikes. */
    public static final class Volley implements Phenomena.Phenomenon, Phenomena.Located {
        static final double SPEED = 1.3;
        static final int LIFE = 80;
        private final ServerLevel level;
        private final UUID owner;
        private final List<BlockState> waiting;
        private final List<LivingEntity> targets;
        private final Vec3 fallback;
        private final float damage;
        private final java.lang.ref.WeakReference<Player> thrower;
        private final List<Shot> flying = new ArrayList<>();
        private int ticks;
        private int launched;
        public int hits;

        private record Shot(Display.BlockDisplay display, BlockState state, @Nullable LivingEntity target, Vec3[] pos, int[] age) {
        }

        public Volley(ServerLevel level, Player owner, List<BlockState> blocks, List<LivingEntity> targets, Vec3 fallback, float damage) {
            this.level = level;
            this.owner = owner.getUUID();
            this.waiting = new ArrayList<>(blocks);
            this.thrower = new java.lang.ref.WeakReference<>(owner);
            this.targets = targets;
            this.fallback = fallback;
            this.damage = damage;
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
            return fallback;
        }

        @Override
        public boolean tick() {
            ticks++;
            Player caster = thrower.get();
            if (caster != null && (caster.isRemoved() || caster.level() != level)) {
                caster = null;
            }
            if (!waiting.isEmpty() && caster != null) {
                BlockState state = waiting.remove(waiting.size() - 1);
                LivingEntity target = targets.isEmpty() ? null : targets.get(launched % targets.size());
                Vec3 start = caster.position().add((level.random.nextDouble() - 0.5) * 2, 1.4 + level.random.nextDouble(), (level.random.nextDouble() - 0.5) * 2);
                Display.BlockDisplay display = display(level, state, new Vector3f(0.7F, 0.7F, 0.7F), false, start);
                if (display != null) {
                    flying.add(new Shot(display, state, target, new Vec3[]{start}, new int[]{0}));
                    launched++;
                }
                if (launched % 4 == 1) {
                    level.playSound(null, caster.blockPosition(), SoundEvents.STONE_HIT, SoundSource.PLAYERS, 0.8F, 1.4F);
                }
            } else if (caster == null) {
                waiting.clear();
            }
            var it = flying.iterator();
            while (it.hasNext()) {
                Shot shot = it.next();
                shot.age()[0]++;
                boolean alive = shot.target() != null && shot.target().isAlive();
                Vec3 aim = alive ? shot.target().position().add(0, shot.target().getBbHeight() / 2, 0) : fallback;
                Vec3 pos = shot.pos()[0];
                Vec3 to = aim.subtract(pos);
                if (to.length() <= SPEED || shot.age()[0] > LIFE) {
                    if (alive && strike(level, caster, shot.target(), damage)) {
                        hits++;
                    }
                    crumble(level, shot.state(), aim, 8);
                    shot.display().discard();
                    it.remove();
                    continue;
                }
                pos = pos.add(to.normalize().scale(SPEED));
                shot.pos()[0] = pos;
                shot.display().setPos(pos.x, pos.y, pos.z);
                shot.display().setYRot(shot.display().getYRot() + 31);
                shot.display().setXRot((shot.display().getXRot() + 23) % 90);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, shot.state()), pos.x, pos.y, pos.z, 1, 0.1, 0.1, 0.1, 0);
            }
            return !waiting.isEmpty() || !flying.isEmpty();
        }

        @Override
        public void cancelled() {
            flying.forEach(s -> s.display().discard());
            flying.clear();
            waiting.clear();
        }
    }

    // ------------------------------------------------------------------ Earthen Surge

    /** Torn blocks are seen to fly: each leaves its hole, rises tumbling and swings in to its owner, then joins the rings. */
    public static final class Uplift implements Phenomena.Phenomenon, Phenomena.Located {
        static final int FLIGHT = 16;
        private final ServerLevel level;
        private final UUID owner;
        private final java.lang.ref.WeakReference<Player> keeper;
        private final List<Display.BlockDisplay> blocks = new ArrayList<>();
        private final List<Vec3> from = new ArrayList<>();
        private final List<Integer> delay = new ArrayList<>();
        private final Vec3 center;
        private int ticks;

        public Uplift(ServerLevel level, Player owner, List<BlockPos> torn, List<BlockState> states) {
            this.level = level;
            this.owner = owner.getUUID();
            this.keeper = new java.lang.ref.WeakReference<>(owner);
            this.center = owner.position();
            for (int i = 0; i < torn.size(); i++) {
                Vec3 at = Vec3.atCenterOf(torn.get(i));
                Display.BlockDisplay d = display(level, states.get(i), new Vector3f(0.95F, 0.95F, 0.95F), false, at);
                if (d != null) {
                    blocks.add(d);
                    from.add(at);
                    delay.add(level.random.nextInt(6));
                }
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

        @Override
        public boolean tick() {
            ticks++;
            Player player = keeper.get();
            Vec3 to = player != null && !player.isRemoved() ? player.position().add(0, 1.2, 0) : center.add(0, 1.2, 0);
            for (int i = 0; i < blocks.size(); i++) {
                double t = Math.max(0, Math.min(1, (ticks - delay.get(i)) / (double) FLIGHT));
                Vec3 a = from.get(i);
                // up out of the hole first, then a curve in
                Vec3 lifted = a.add(0, 2.5, 0);
                Vec3 p = a.scale((1 - t) * (1 - t)).add(lifted.scale(2 * t * (1 - t))).add(to.scale(t * t));
                Display.BlockDisplay d = blocks.get(i);
                d.setPos(p.x, p.y, p.z);
                d.setYRot(d.getYRot() + 19);
                d.setXRot((float) (Math.sin((ticks + i) * 0.4) * 70));
                if (ticks - delay.get(i) == 1) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, a.x, a.y, a.z, 2, 0.3, 0.2, 0.3, 0.01);
                }
            }
            if (ticks > FLIGHT + 6) {
                cancelled();
                return false;
            }
            return true;
        }

        @Override
        public void cancelled() {
            blocks.forEach(Display.BlockDisplay::discard);
            blocks.clear();
        }
    }

    // ------------------------------------------------------------------ Cataclysm

    /**
     * The hoard pressed into one great ball of stone, many blocks across, turning as it falls from high above onto the point,
     * trailing fire and smoke. It bursts in a flash: fire, a ring of shock that throws everything away, flying debris - and a
     * scorched, burning crater where the ground was earth and stone.
     */
    public static final class Meteor implements Phenomena.Phenomenon, Phenomena.Located {
        static final double HEIGHT = 45;
        static final int PARTS = 46;
        private final ServerLevel level;
        private final UUID owner;
        private final java.lang.ref.WeakReference<Player> caster;
        private final Vec3 target;
        private final float damage;
        private final double radius;
        private final boolean scorch;
        private final double ball;
        private final List<Display.BlockDisplay> parts = new ArrayList<>();
        private final List<Vec3> offsets = new ArrayList<>();
        private final List<BlockState> looks = new ArrayList<>();
        private double y;
        private double speed;
        private int ticks;

        public Meteor(ServerLevel level, Player owner, Vec3 target, List<BlockState> blocks, float damage, double radius) {
            this(level, owner, target, blocks, damage, radius, true);
        }

        public Meteor(ServerLevel level, Player owner, Vec3 target, List<BlockState> blocks, float damage, double radius, boolean scorch) {
            this.level = level;
            this.owner = owner.getUUID();
            this.caster = new java.lang.ref.WeakReference<>(owner);
            this.target = target;
            this.damage = damage;
            this.radius = radius;
            this.scorch = scorch;
            this.y = target.y + HEIGHT;
            this.ball = 2.5 + blocks.size() * 0.12;
            List<BlockState> from = blocks.isEmpty() ? List.of(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()) : blocks;
            float size = (float) (ball * 0.62);
            for (int i = 0; i < PARTS; i++) {
                // spread evenly over the ball (a Fibonacci sphere), each part a big block
                double phi = Math.acos(1 - 2 * (i + 0.5) / PARTS);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                Vec3 off = new Vec3(Math.cos(theta) * Math.sin(phi), Math.cos(phi), Math.sin(theta) * Math.sin(phi)).scale(ball * 0.78);
                BlockState look = from.get(i % from.size());
                Display.BlockDisplay part = display(level, look, new Vector3f(size, size, size), false, new Vec3(target.x, y, target.z).add(off));
                if (part != null) {
                    parts.add(part);
                    offsets.add(off);
                    looks.add(look);
                }
            }
            level.playSound(null, target.x, target.y + HEIGHT, target.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 4F, 0.4F);
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
            return target;
        }

        public double ball() {
            return ball;
        }

        @Override
        public boolean tick() {
            ticks++;
            speed = Math.min(2.4, speed + 0.09);
            y = Math.max(target.y, y - speed);
            double spin = ticks * 0.06;
            double cos = Math.cos(spin);
            double sin = Math.sin(spin);
            for (int i = 0; i < parts.size(); i++) {
                Vec3 o = offsets.get(i);
                Vec3 turned = new Vec3(o.x * cos - o.z * sin, o.y, o.x * sin + o.z * cos);
                Vec3 at = new Vec3(target.x, y + ball, target.z).add(turned);
                Display.BlockDisplay d = parts.get(i);
                d.setPos(at.x, at.y, at.z);
                d.setYRot((float) Math.toDegrees(-spin) + i * 13);
            }
            level.sendParticles(ParticleTypes.FLAME, target.x, y + ball * 2, target.z, 12, ball * 0.6, ball * 0.4, ball * 0.6, 0.02);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, target.x, y + ball * 2.2, target.z, 6, ball * 0.5, ball * 0.5, ball * 0.5, 0.02);
            if (ticks % 6 == 0) {
                level.playSound(null, target.x, y, target.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2F, 0.4F);
            }
            if (y <= target.y || ticks > 120) {
                impact();
                return false;
            }
            return true;
        }

        private void impact() {
            cancelled();
            // the flash and the fire
            level.sendParticles(ParticleTypes.FLASH, target.x, target.y + 1, target.z, 3, 0.5, 0.5, 0.5, 0);
            for (int i = 0; i < 5; i++) {
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.x + (level.random.nextDouble() - 0.5) * radius,
                        target.y + 1 + level.random.nextDouble() * 2, target.z + (level.random.nextDouble() - 0.5) * radius, 1, 0, 0, 0, 0);
            }
            level.sendParticles(ParticleTypes.FLAME, target.x, target.y + 1, target.z, 260, radius * 0.4, 1.2, radius * 0.4, 0.35);
            level.sendParticles(ParticleTypes.LAVA, target.x, target.y + 1, target.z, 60, radius * 0.4, 0.5, radius * 0.4, 0);
            level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, target.x, target.y + 1, target.z, 60, radius * 0.5, 1, radius * 0.5, 0.05);
            for (BlockState look : looks.subList(0, Math.min(6, looks.size()))) {
                crumble(level, look, target.add(0, 1.5, 0), 40);
            }
            level.playSound(null, target.x, target.y, target.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 6F, 0.45F);
            level.playSound(null, target.x, target.y, target.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3F, 0.6F);
            // the blow
            Entity source = caster.get();
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(target, target).inflate(radius), LivingEntity::isAlive)) {
                double distance = living.position().distanceTo(target);
                if (distance > radius) {
                    continue;
                }
                float dealt = (float) (damage * (1 - 0.6 * distance / radius));
                if (strike(level, source, living, dealt)) {
                    living.igniteForSeconds(4);
                    Vec3 push = living.position().subtract(target).normalize().scale(1.6);
                    living.push(push.x, 0.8, push.z);
                    living.hurtMarked = true;
                }
            }
            Phenomena.add(new Shockwave(level, owner, target, radius * 2.2));
            Phenomena.add(new Debris(level, owner, target, looks.subList(0, Math.min(18, looks.size()))));
            if (scorch && source instanceof LivingEntity living) {
                scorch(level, living, target, radius * 0.85);
            }
        }

        @Override
        public void cancelled() {
            parts.forEach(Display.BlockDisplay::discard);
            parts.clear();
        }
    }

    /**
     * The crater: within the radius the top of the ground (only earth and stone the caster may break) is blasted, the middle
     * dug a block deeper, and what is left is scorched to blackstone, netherrack and magma, much of it burning.
     */
    public static int scorch(ServerLevel level, LivingEntity caster, Vec3 center, double radius) {
        int changed = 0;
        int r = (int) Math.ceil(radius);
        BlockPos base = BlockPos.containing(center);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.hypot(dx, dz);
                if (d > radius) {
                    continue;
                }
                BlockPos top = null;
                for (int dy = 3; dy >= -4; dy--) {
                    BlockPos p = base.offset(dx, dy, dz);
                    if (!level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) {
                        top = p;
                        break;
                    }
                }
                if (top == null || !StoneHoard.tearable(level, top, caster)) {
                    continue;
                }
                if (d < radius * 0.45 && StoneHoard.tearable(level, top, caster)) {
                    level.setBlock(top, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    top = top.below();
                    if (!StoneHoard.tearable(level, top, caster)) {
                        continue;
                    }
                }
                float roll = level.random.nextFloat();
                BlockState burnt = roll < 0.15F ? net.minecraft.world.level.block.Blocks.MAGMA_BLOCK.defaultBlockState()
                        : roll < 0.55F ? net.minecraft.world.level.block.Blocks.BLACKSTONE.defaultBlockState()
                        : roll < 0.8F ? net.minecraft.world.level.block.Blocks.NETHERRACK.defaultBlockState()
                        : net.minecraft.world.level.block.Blocks.COARSE_DIRT.defaultBlockState();
                level.setBlock(top, burnt, 3);
                changed++;
                if (burnt.is(net.minecraft.world.level.block.Blocks.NETHERRACK) && level.random.nextFloat() < 0.6F && level.getBlockState(top.above()).isAir()) {
                    level.setBlock(top.above(), net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 3);
                }
            }
        }
        return changed;
    }

    /** A ring of shock spreading from a blast: dust and cloud along its edge, everything it passes thrown outwards. */
    public static final class Shockwave implements Phenomena.Phenomenon, Phenomena.Located {
        static final int LIFE = 14;
        private final ServerLevel level;
        private final UUID owner;
        private final Vec3 center;
        private final double reach;
        private final java.util.Set<Integer> pushed = new java.util.HashSet<>();
        private int ticks;

        public Shockwave(ServerLevel level, UUID owner, Vec3 center, double reach) {
            this.level = level;
            this.owner = owner;
            this.center = center;
            this.reach = reach;
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

        @Override
        public boolean tick() {
            ticks++;
            double r = reach * ticks / LIFE;
            int n = (int) (r * 6) + 8;
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * i / n;
                level.sendParticles(ParticleTypes.CLOUD, center.x + Math.cos(a) * r, center.y + 0.6, center.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0.02);
                if (i % 3 == 0) {
                    level.sendParticles(ParticleTypes.POOF, center.x + Math.cos(a) * r, center.y + 1.6, center.z + Math.sin(a) * r, 1, 0, 0.2, 0, 0.02);
                }
            }
            for (Entity e : level.getEntities((Entity) null, new AABB(center, center).inflate(r + 1, 3, r + 1), e -> e instanceof LivingEntity && !pushed.contains(e.getId()))) {
                double d = Math.hypot(e.getX() - center.x, e.getZ() - center.z);
                if (Math.abs(d - r) < 1.5 && !e.getUUID().equals(owner)) {
                    Vec3 out = new Vec3(e.getX() - center.x, 0, e.getZ() - center.z).normalize().scale(0.9);
                    e.push(out.x, 0.35, out.z);
                    e.hurtMarked = true;
                    pushed.add(e.getId());
                }
            }
            return ticks < LIFE;
        }
    }

    /** Pieces of the meteor flung out on arcs, burning, and gone where they fall. */
    public static final class Debris implements Phenomena.Phenomenon, Phenomena.Located {
        static final int LIFE = 40;
        private final ServerLevel level;
        private final UUID owner;
        private final Vec3 center;
        private final List<Display.BlockDisplay> pieces = new ArrayList<>();
        private final List<Vec3> pos = new ArrayList<>();
        private final List<Vec3> vel = new ArrayList<>();
        private int ticks;

        public Debris(ServerLevel level, UUID owner, Vec3 center, List<BlockState> looks) {
            this.level = level;
            this.owner = owner;
            this.center = center;
            for (BlockState look : looks) {
                Vec3 start = center.add(0, 1.5, 0);
                double a = level.random.nextDouble() * Math.PI * 2;
                double speed = 0.5 + level.random.nextDouble() * 0.8;
                Display.BlockDisplay d = display(level, look, new Vector3f(0.9F, 0.9F, 0.9F), false, start);
                if (d != null) {
                    pieces.add(d);
                    pos.add(start);
                    vel.add(new Vec3(Math.cos(a) * speed, 0.7 + level.random.nextDouble() * 0.7, Math.sin(a) * speed));
                }
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

        @Override
        public boolean tick() {
            ticks++;
            for (int i = pieces.size() - 1; i >= 0; i--) {
                Vec3 p = pos.get(i).add(vel.get(i));
                Vec3 v = vel.get(i).add(0, -0.08, 0).scale(0.98);
                Display.BlockDisplay d = pieces.get(i);
                if (p.y < center.y - 1 || !level.getBlockState(BlockPos.containing(p)).isAir() && ticks > 3) {
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 0.5, p.z, 3, 0.2, 0.2, 0.2, 0.01);
                    d.discard();
                    pieces.remove(i);
                    pos.remove(i);
                    vel.remove(i);
                    continue;
                }
                pos.set(i, p);
                vel.set(i, v);
                d.setPos(p.x, p.y, p.z);
                d.setYRot(d.getYRot() + 23);
                d.setXRot((d.getXRot() + 17) % 90);
                level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0);
            }
            if (ticks >= LIFE || pieces.isEmpty()) {
                cancelled();
                return false;
            }
            return true;
        }

        @Override
        public void cancelled() {
            pieces.forEach(Display.BlockDisplay::discard);
            pieces.clear();
        }
    }
}
