package com.soulinchains.arcaneartifacts.catto;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.research.ResearchEvent;
import com.soulinchains.arcaneartifacts.research.ResearchTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Catto's behaviour as one state machine, driven every tick by a single goal so that nothing else fights over movement.
 * <pre>
 * IDLE ──queue──> PLAN ──step──> TRAVEL ──arrived──> WORK ──done──> PLAN
 *                  │  └─blocked─> WAIT ──retry/command──> PLAN
 *          danger from TRAVEL/WORK ──> RETREAT ──safe──> PLAN
 *          the bell, from anywhere ──> RETURN ──next to owner──> IDLE
 * </pre>
 * Tasks are planned one concrete step at a time by {@link CattoPlanner}, because the world changes while Catto walks.
 */
public final class CattoBrain {
    public enum State { IDLE, PLAN, TRAVEL, WORK, RETREAT, WAIT, RETURN, OFFLINE }

    /** One concrete thing to do somewhere. */
    public static final class Step {
        public enum Kind { GOTO, BREAK, PLACE, PICKUP, HARVEST, TAKE, PUT, GIVE, CRAFT }

        public final Kind kind;
        public final BlockPos pos;
        @Nullable
        public Entity entity;
        @Nullable
        public BlockState state;
        public Predicate<ItemStack> filter = s -> true;
        public int amount;
        public int workTicks = 10;
        public double reach = 2.5;
        /** Pathfinding stops this many blocks short of {@link #pos}. */
        public int accuracy = 1;
        /** For blocks found by the planner: not tried again for a while when it fails. */
        public boolean blacklistOnFail = true;

        Step(Kind kind, BlockPos pos) {
            this.kind = kind;
            this.pos = pos.immutable();
        }

        public Vec3 target() {
            return entity != null ? entity.position() : Vec3.atCenterOf(pos);
        }
    }

    /** What the planner answers. */
    public static final class Plan {
        static final Plan DONE = new Plan(null, null, false);
        static final Plan BUSY = new Plan(null, null, true);

        @Nullable
        final Step step;
        @Nullable
        final Component blocked;
        final boolean busy;

        private Plan(@Nullable Step step, @Nullable Component blocked, boolean busy) {
            this.step = step;
            this.blocked = blocked;
            this.busy = busy;
        }

        static Plan step(Step step) {
            return new Plan(step, null, false);
        }

        static Plan blocked(Component why) {
            return new Plan(null, why, false);
        }
    }

    /** The only goal Catto has besides floating: it runs the state machine. */
    public static final class Goal extends net.minecraft.world.entity.ai.goal.Goal {
        private final Catto cat;

        public Goal(Catto cat) {
            this.cat = cat;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return cat.isAlive() && !cat.isPassenger() && cat.getOwnerUUID() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            cat.brain().tick();
        }
    }

    private final Catto cat;
    State state = State.IDLE;
    @Nullable
    Step step;
    private int workLeft;
    private int workTotal;
    private int pathCooldown;
    private int stuckTicks;
    private double lastDist = Double.MAX_VALUE;
    private int retreats;
    private int safeTicks;
    private int waitTicks;
    private int idleTicks;
    private int hurtTicks;
    @Nullable
    private Component waitReason;
    private final Map<Long, Long> blacklist = new HashMap<>();
    @Nullable
    CattoScanner scanner;
    /** Planner caches tied to the current task (recipe, build layout...). */
    final Map<String, Object> memo = new HashMap<>();
    @Nullable
    private CattoTask memoTask;

    CattoBrain(Catto cat) {
        this.cat = cat;
    }

    public State state() {
        return state;
    }

    public boolean working() {
        return state != State.IDLE && state != State.OFFLINE && state != State.WAIT;
    }

    ServerLevel level() {
        return (ServerLevel) cat.level();
    }

    long now() {
        return cat.level().getGameTime();
    }

    // ------------------------------------------------------------------ commands

    public void recall(ServerPlayer player) {
        cancelStep();
        cat.paused = true;
        cat.stay = false;
        state = State.RETURN;
        boolean sameLevel = player.level() == cat.level();
        if (!sameLevel) {
            CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.call.other_world"));
            return;
        }
        double dist = cat.distanceTo(player);
        if (dist > 24) {
            if (cat.spendSource(cat.recallCost())) {
                teleportNear(player);
                CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.call.jumped", cat.recallCost()));
            } else {
                CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.call.walking", (int) dist));
            }
        } else {
            CattoItems.tell(player, Component.translatable("arcane_artifacts.catto.call.coming"));
        }
        cat.playSound(SoundEvents.CAT_AMBIENT, 1.0F, 1.3F);
    }

    /** "Stop": drop the current step, pause the queue and sit where it is. */
    public void stop() {
        cancelStep();
        cat.paused = true;
        cat.stay = true;
        state = State.IDLE;
    }

    public void resume() {
        waitReason = null;
        if (state == State.WAIT || state == State.IDLE || state == State.RETURN) {
            state = State.PLAN;
        }
    }

    /** The first task of the queue was removed or moved away: forget everything about it. */
    public void taskRemoved() {
        cancelStep();
        resetTaskMemory();
        if (state != State.RETURN && state != State.OFFLINE) {
            state = State.PLAN;
        }
    }

    public void hurt() {
        hurtTicks = 60;
    }

    private void cancelStep() {
        if (step != null && step.kind == Step.Kind.BREAK) {
            cat.level().destroyBlockProgress(cat.getId(), step.pos, -1);
        }
        step = null;
        cat.getNavigation().stop();
    }

    private void resetTaskMemory() {
        scanner = null;
        memo.clear();
        memoTask = null;
        blacklist.clear();
        retreats = 0;
    }

    // ------------------------------------------------------------------ blacklist

    boolean blacklisted(BlockPos pos) {
        Long until = blacklist.get(pos.asLong());
        if (until == null) {
            return false;
        }
        if (until < now()) {
            blacklist.remove(pos.asLong());
            return false;
        }
        return true;
    }

    void blacklist(BlockPos pos, int ticks) {
        blacklist.put(pos.asLong(), now() + ticks);
    }

    /** Planner memory belongs to one task; a different first task starts clean. */
    Map<String, Object> memo(CattoTask task) {
        if (memoTask != task) {
            resetTaskMemory();
            memoTask = task;
        }
        return memo;
    }

    // ------------------------------------------------------------------ the tick

    void tick() {
        if (!cat.ownerOnline()) {
            if (state != State.OFFLINE) {
                cancelStep();
                state = State.OFFLINE;
            }
            sit(true);
            show(Component.translatable("arcane_artifacts.catto.status.offline"), ItemStack.EMPTY, false);
            return;
        }
        if (state == State.OFFLINE) {
            state = cat.queue.isEmpty() || cat.paused ? State.IDLE : State.PLAN;
        }
        if (hurtTicks > 0) {
            hurtTicks--;
        }
        if ((state == State.TRAVEL || state == State.WORK || state == State.PLAN) && cat.tickCount % 5 == 0 && inDanger()) {
            enterRetreat();
        }
        if (state != State.IDLE && state != State.WAIT && state != State.OFFLINE) {
            sit(false);
        }
        if (cat.moduleActive(CattoModule.MAGNET_WHISKER) && working() && cat.tickCount % 10 == 0 && !CattoWorld.bagFull(cat)) {
            CattoWorld.vacuum(cat, cat.blockPosition(), 4.0, s -> true);
        }
        switch (state) {
            case IDLE -> tickIdle();
            case PLAN -> tickPlan();
            case TRAVEL -> tickTravel();
            case WORK -> tickWork();
            case RETREAT -> tickRetreat();
            case WAIT -> tickWait();
            case RETURN -> tickReturn();
            default -> state = State.IDLE;
        }
    }

    private void tickIdle() {
        if (!cat.queue.isEmpty() && !cat.paused && !cat.stay) {
            state = State.PLAN;
            return;
        }
        Component status = cat.stay ? Component.translatable("arcane_artifacts.catto.status.staying")
                : cat.paused && !cat.queue.isEmpty() ? Component.translatable("arcane_artifacts.catto.status.paused", cat.queue.size())
                : Component.translatable("arcane_artifacts.catto.status.idle");
        show(status, ItemStack.EMPTY, false);
        if (cat.stay) {
            cat.getNavigation().stop();
            sit(true);
            return;
        }
        followOwner(6.0, 1.1);
    }

    private void followOwner(double near, double speed) {
        ServerPlayer owner = cat.ownerPlayer();
        if (owner == null || owner.level() != cat.level() || owner.isSpectator()) {
            sit(true);
            return;
        }
        double dist = cat.distanceTo(owner);
        if (dist > 24) {
            teleportNear(owner);
        } else if (dist > near) {
            idleTicks = 0;
            sit(false);
            if (cat.tickCount % 10 == 0 || cat.getNavigation().isDone()) {
                cat.getNavigation().moveTo(owner, speed);
            }
        } else {
            cat.getNavigation().stop();
            cat.getLookControl().setLookAt(owner, 10.0F, cat.getMaxHeadXRot());
            if (++idleTicks > 100) {
                sit(true);
            }
        }
    }

    private void tickPlan() {
        if (cat.queue.isEmpty() || cat.paused) {
            state = State.IDLE;
            return;
        }
        CattoTask task = cat.queue.get(0);
        Plan plan;
        try {
            plan = CattoPlanner.plan(cat, this, task);
        } catch (RuntimeException e) {
            ArcaneArtifacts.LOGGER.error("Catto could not plan {}", task.type, e);
            plan = Plan.blocked(Component.translatable("arcane_artifacts.catto.blocked.confused"));
        }
        if (plan.busy) {
            show(Component.translatable("arcane_artifacts.catto.status.thinking", task.describe()), task.icon(), false);
            return;
        }
        if (plan.blocked != null) {
            enterWait(plan.blocked);
            return;
        }
        if (plan.step == null) {
            finish(task);
            return;
        }
        step = plan.step;
        state = State.TRAVEL;
        pathCooldown = 0;
        stuckTicks = 0;
        lastDist = Double.MAX_VALUE;
        show(Component.translatable("arcane_artifacts.catto.status.going", task.describe()), task.icon(), false);
    }

    private void tickTravel() {
        Step s = step;
        if (s == null) {
            state = State.PLAN;
            return;
        }
        if (s.entity != null && !s.entity.isAlive()) {
            step = null;
            state = State.PLAN;
            return;
        }
        Vec3 target = s.target();
        double dist = cat.position().distanceTo(target);
        if (dist <= s.reach) {
            cat.getNavigation().stop();
            state = State.WORK;
            workTotal = Math.max(1, s.workTicks);
            workLeft = workTotal;
            return;
        }
        if (--pathCooldown <= 0 || cat.getNavigation().isDone()) {
            pathCooldown = 20;
            BlockPos goal = s.entity != null ? s.entity.blockPosition() : s.pos;
            Path path = cat.getNavigation().createPath(goal, s.accuracy);
            if (path == null || !path.canReach() && path.getDistToTarget() > s.reach + 0.5) {
                unreachable(s);
                return;
            }
            cat.getNavigation().moveTo(path, cat.moduleActive(CattoModule.CAT_BOOTS) ? 1.3 : 1.0);
        }
        if (dist >= lastDist - 0.02) {
            if (++stuckTicks > 120) {
                unreachable(s);
            }
        } else {
            stuckTicks = 0;
        }
        lastDist = Math.min(lastDist, dist);
    }

    private void unreachable(Step s) {
        if (s.blacklistOnFail) {
            blacklist(s.pos, 1200);
        }
        cat.getNavigation().stop();
        step = null;
        state = State.PLAN;
        if (s.kind == Step.Kind.GIVE) {
            enterWait(Component.translatable("arcane_artifacts.catto.blocked.owner_unreachable"));
        }
    }

    private void tickWork() {
        Step s = step;
        if (s == null) {
            state = State.PLAN;
            return;
        }
        Vec3 target = s.target();
        cat.getLookControl().setLookAt(target.x, target.y, target.z);
        if (--workLeft > 0) {
            if (s.kind == Step.Kind.BREAK) {
                int progress = (int) ((1.0F - workLeft / (float) workTotal) * 10.0F);
                cat.level().destroyBlockProgress(cat.getId(), s.pos, progress);
                if (cat.tickCount % 6 == 0) {
                    BlockState state = cat.level().getBlockState(s.pos);
                    var sound = state.getSoundType(cat.level(), s.pos, cat);
                    cat.level().playSound(null, s.pos, sound.getHitSound(), net.minecraft.sounds.SoundSource.BLOCKS, 0.4F, sound.getPitch() * 0.6F);
                }
            } else if (cat.tickCount % 8 == 0 && (s.kind == Step.Kind.CRAFT || s.kind == Step.Kind.PLACE)) {
                level().sendParticles(ParticleTypes.CRIT, target.x, target.y + 0.4, target.z, 2, 0.2, 0.2, 0.2, 0.0);
            }
            return;
        }
        CattoTask task = cat.queue.isEmpty() ? null : cat.queue.get(0);
        boolean ok = task != null && CattoPlanner.perform(cat, this, task, s);
        if (!ok && s.blacklistOnFail) {
            blacklist(s.pos, 600);
        }
        if (cat.moduleActive(CattoModule.FIREFLY)) {
            CattoWorld.lightUp(cat);
        }
        step = null;
        state = State.PLAN;
    }

    private void tickRetreat() {
        show(Component.translatable("arcane_artifacts.catto.status.retreating"), new ItemStack(net.minecraft.world.item.Items.SHIELD), true);
        ServerPlayer owner = cat.ownerPlayer();
        if (inDanger()) {
            safeTicks = 0;
        } else if (++safeTicks > 40) {
            state = State.PLAN;
            return;
        }
        if (owner != null && owner.level() == cat.level() && cat.distanceTo(owner) > 3) {
            if (cat.tickCount % 10 == 0) {
                cat.getNavigation().moveTo(owner, 1.33);
            }
        } else {
            Monster threat = nearestThreat();
            if (threat != null && cat.tickCount % 10 == 0) {
                Vec3 away = cat.position().subtract(threat.position()).normalize().scale(8).add(cat.position());
                cat.getNavigation().moveTo(away.x, away.y, away.z, 1.33);
            }
        }
    }

    private void tickWait() {
        sit(true);
        show(Component.translatable("arcane_artifacts.catto.status.waiting",
                waitReason == null ? Component.empty() : waitReason), cat.queue.isEmpty() ? ItemStack.EMPTY : cat.queue.get(0).icon(), true);
        if (cat.paused || cat.queue.isEmpty()) {
            state = State.IDLE;
            return;
        }
        if (++waitTicks >= 200) {
            waitTicks = 0;
            state = State.PLAN;
        }
    }

    private void tickReturn() {
        show(Component.translatable("arcane_artifacts.catto.status.returning"), new ItemStack(CattoContent.BELL.get()), false);
        ServerPlayer owner = cat.ownerPlayer();
        if (owner == null || owner.level() != cat.level()) {
            state = State.IDLE;
            return;
        }
        if (cat.distanceTo(owner) <= 3) {
            cat.getNavigation().stop();
            state = State.IDLE;
            return;
        }
        if (cat.tickCount % 10 == 0 || cat.getNavigation().isDone()) {
            cat.getNavigation().moveTo(owner, 1.33);
        }
    }

    // ------------------------------------------------------------------ transitions

    private void finish(CattoTask task) {
        cat.queue.remove(task);
        cat.addBond(1);
        cat.report(false, "arcane_artifacts.catto.report.done." + task.type.key(), "#" + descriptionId(task), String.valueOf(task.done));
        if (task.type == CattoTask.Type.EXPLORE) {
            CattoPlanner.reportFindings(cat, task);
        }
        ServerPlayer owner = cat.ownerPlayer();
        if (owner != null) {
            ResearchTriggers.fire(owner, ResearchEvent.of(ArcaneArtifacts.id("catto_" + task.type.key())));
            owner.displayClientMessage(Component.translatable("arcane_artifacts.catto.says", cat.getDisplayName(),
                    Component.translatable("arcane_artifacts.catto.task_done", task.describe())), true);
        }
        level().sendParticles(ParticleTypes.HAPPY_VILLAGER, cat.getX(), cat.getY() + 0.6, cat.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
        cat.playSound(SoundEvents.CAT_PURREOW, 0.8F, 1.0F);
        resetTaskMemory();
        cat.changed();
        state = State.PLAN;
    }

    private static String descriptionId(CattoTask task) {
        if (task.type == CattoTask.Type.MINE && task.block != null) {
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(task.block).getDescriptionId();
        }
        if (task.type == CattoTask.Type.BUILD && task.blueprint != null) {
            return "arcane_artifacts.catto.blueprint_target";
        }
        if (task.item.isEmpty()) {
            return "arcane_artifacts.catto.anything";
        }
        return task.item.getDescriptionId();
    }

    void enterWait(Component why) {
        boolean newReason = waitReason == null || !waitReason.equals(why);
        cancelStep();
        state = State.WAIT;
        waitTicks = 0;
        waitReason = why;
        if (newReason) {
            cat.report(true, why);
            ServerPlayer owner = cat.ownerPlayer();
            if (owner != null) {
                owner.displayClientMessage(Component.translatable("arcane_artifacts.catto.says", cat.getDisplayName(), why), false);
            }
            cat.playSound(SoundEvents.CAT_HISS, 0.5F, 1.2F);
        }
    }

    private void enterRetreat() {
        cancelStep();
        state = State.RETREAT;
        safeTicks = 0;
        if (++retreats >= 3) {
            retreats = 0;
            enterWait(Component.translatable("arcane_artifacts.catto.blocked.too_dangerous"));
            return;
        }
        cat.report(true, "arcane_artifacts.catto.report.retreat");
        cat.playSound(SoundEvents.CAT_HISS, 0.8F, 1.0F);
    }

    // ------------------------------------------------------------------ danger

    @Nullable
    private Monster nearestThreat() {
        List<Monster> monsters = cat.level().getEntitiesOfClass(Monster.class, cat.getBoundingBox().inflate(8),
                m -> m.isAlive() && (m instanceof Creeper || !(m instanceof NeutralMob) || m.getTarget() != null)
                        && !(m.getTarget() instanceof Player p && p.isCreative())
                        // Game tests share the world with other tests' monsters: only one hunting Catto counts there.
                        && (!cat.testMode || m.getTarget() == cat));
        Monster best = null;
        double bestDist = Double.MAX_VALUE;
        for (Monster m : monsters) {
            double d = m.distanceToSqr(cat);
            if (d < bestDist) {
                bestDist = d;
                best = m;
            }
        }
        return best;
    }

    private boolean inDanger() {
        if (cat.getHealth() < cat.getMaxHealth() * 0.4F || hurtTicks > 0) {
            return true;
        }
        if ((cat.isOnFire() || cat.isInLava()) && !cat.moduleActive(CattoModule.CAT_BOOTS)) {
            return true;
        }
        return nearestThreat() != null;
    }

    // ------------------------------------------------------------------ helpers

    private void sit(boolean sit) {
        if (cat.isInSittingPose() != sit) {
            cat.setInSittingPose(sit);
        }
        if (!sit) {
            idleTicks = 0;
        }
    }

    void show(Component status, ItemStack icon, boolean alert) {
        cat.show(state, status, icon, alert);
    }

    void teleportNear(ServerPlayer owner) {
        BlockPos base = owner.blockPosition();
        for (int i = 0; i < 20; i++) {
            BlockPos pos = base.offset(cat.getRandom().nextInt(7) - 3, cat.getRandom().nextInt(3) - 1, cat.getRandom().nextInt(7) - 3);
            if (Math.abs(pos.getX() - base.getX()) < 2 && Math.abs(pos.getZ() - base.getZ()) < 2) {
                continue;
            }
            if (cat.level().getBlockState(pos.below()).isFaceSturdy(cat.level(), pos.below(), net.minecraft.core.Direction.UP)
                    && cat.level().noCollision(cat, cat.getBoundingBox().move(Vec3.atBottomCenterOf(pos).subtract(cat.position())))
                    && cat.level().getFluidState(pos).isEmpty()) {
                level().sendParticles(ParticleTypes.WITCH, cat.getX(), cat.getY() + 0.4, cat.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
                cat.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, cat.getYRot(), cat.getXRot());
                cat.getNavigation().stop();
                level().sendParticles(ParticleTypes.WITCH, cat.getX(), cat.getY() + 0.4, cat.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
                return;
            }
        }
    }
}
