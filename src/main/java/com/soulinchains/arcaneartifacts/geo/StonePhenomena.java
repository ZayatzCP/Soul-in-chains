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
                Display.BlockDisplay display = display(level, state, new Vector3f(0.4F, 0.4F, 0.4F), false, start);
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

    // ------------------------------------------------------------------ Cataclysm

    /** The hoard pressed into one great stone falls from high above onto a point and bursts. */
    public static final class Meteor implements Phenomena.Phenomenon, Phenomena.Located {
        static final double HEIGHT = 28;
        private final ServerLevel level;
        private final UUID owner;
        private final Vec3 target;
        private final float damage;
        private final double radius;
        private final List<Display.BlockDisplay> parts = new ArrayList<>();
        private final List<Vec3> offsets = new ArrayList<>();
        private final BlockState look;
        private double y;
        private double speed;
        private int ticks;

        public Meteor(ServerLevel level, Player owner, Vec3 target, List<BlockState> blocks, float damage, double radius) {
            this.level = level;
            this.owner = owner.getUUID();
            this.target = target;
            this.damage = damage;
            this.radius = radius;
            this.y = target.y + HEIGHT;
            this.look = blocks.isEmpty() ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState() : blocks.get(0);
            float size = 1.2F + blocks.size() * 0.06F;
            int shown = Math.min(12, blocks.size());
            for (int i = 0; i < shown; i++) {
                double phi = Math.acos(1 - 2 * (i + 0.5) / shown);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                Vec3 off = new Vec3(Math.cos(theta) * Math.sin(phi), Math.cos(phi), Math.sin(theta) * Math.sin(phi)).scale(size * 0.35);
                Display.BlockDisplay part = display(level, blocks.get(i), new Vector3f(size * 0.6F, size * 0.6F, size * 0.6F), false,
                        new Vec3(target.x, y, target.z).add(off));
                if (part != null) {
                    parts.add(part);
                    offsets.add(off);
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
            return target;
        }

        @Override
        public boolean tick() {
            ticks++;
            speed = Math.min(2.2, speed + 0.08);
            y = Math.max(target.y, y - speed);
            for (int i = 0; i < parts.size(); i++) {
                Vec3 at = new Vec3(target.x, y, target.z).add(offsets.get(i));
                parts.get(i).setPos(at.x, at.y, at.z);
            }
            level.sendParticles(ParticleTypes.LARGE_SMOKE, target.x, y + 1, target.z, 3, 0.4, 0.4, 0.4, 0.01);
            if (y <= target.y || ticks > 100) {
                impact();
                return false;
            }
            return true;
        }

        private void impact() {
            cancelled();
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.x, target.y + 0.5, target.z, 1, 0, 0, 0, 0);
            crumble(level, look, target.add(0, 1, 0), 80);
            level.playSound(null, target.x, target.y, target.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5F, 0.6F);
            Entity caster = level.getEntity(owner);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(target, target).inflate(radius), LivingEntity::isAlive)) {
                double distance = living.position().distanceTo(target);
                if (distance > radius) {
                    continue;
                }
                float dealt = (float) (damage * (1 - 0.6 * distance / radius));
                if (strike(level, caster, living, dealt)) {
                    Vec3 push = living.position().subtract(target).normalize().scale(1.1);
                    living.push(push.x, 0.6, push.z);
                    living.hurtMarked = true;
                }
            }
        }

        @Override
        public void cancelled() {
            parts.forEach(Display.BlockDisplay::discard);
            parts.clear();
        }
    }
}
