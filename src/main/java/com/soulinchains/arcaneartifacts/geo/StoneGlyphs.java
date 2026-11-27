package com.soulinchains.arcaneartifacts.geo;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.effect.MagicHandler;
import com.soulinchains.arcaneartifacts.phenomena.Phenomena;
import com.soulinchains.arcaneartifacts.util.Allies;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The stone glyphs (1.43). The Druid tears the ground up into a Stone Hoard and works it near at hand (Earthen Surge, Stone
 * Cage, Stone Lances); the Mage of Mysteries throws it far (Myriad Volley, Stone Wings, Cataclysm, Stone Dominion). Only a
 * player has a hoard; cast by anything else, these glyphs do nothing.
 */
public final class StoneGlyphs {
    private StoneGlyphs() {
    }

    /** A stone glyph: resolves once per hit, on a creature or on a block, for a player caster. */
    abstract static class StoneEffect extends AbstractEffect {
        protected StoneEffect(String id, String name) {
            super(ArcaneArtifacts.id(id), name);
        }

        @Override
        public void onResolve(HitResult hit, Level level, @NotNull LivingEntity shooter, SpellStats stats, SpellContext context, SpellResolver resolver) {
            if (!(level instanceof ServerLevel server) || !(shooter instanceof Player player)) {
                return;
            }
            LivingEntity target = hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity l ? l : null;
            BlockPos block = hit instanceof BlockHitResult b ? b.getBlockPos() : null;
            Vec3 point = target != null ? target.position() : block != null ? Vec3.atBottomCenterOf(block.above()) : hit.getLocation();
            resolve(server, player, target, block, point, stats, context);
        }

        protected abstract void resolve(ServerLevel level, Player player, @Nullable LivingEntity target, @Nullable BlockPos block, Vec3 point,
                                        SpellStats stats, SpellContext context);

        protected static int amp(SpellStats stats) {
            return (int) Math.round(stats.getAmpMultiplier());
        }

        protected static boolean empty(Player player, int need) {
            if (!StoneHoard.has(player, need)) {
                StoneHoard.report(player, "arcane_artifacts.stone.too_little", need);
                return true;
            }
            return false;
        }
    }

    // ------------------------------------------------------------------ Druid

    /** Earthen Surge: rips the ground around the point into the hoard. */
    public static final class EarthenSurge extends StoneEffect {
        public static final EarthenSurge INSTANCE = new EarthenSurge();
        public static final int BASE = 12;

        private EarthenSurge() {
            super("glyph_earthen_surge", "Earthen Surge");
        }

        @Override
        protected void resolve(ServerLevel level, Player player, @Nullable LivingEntity target, @Nullable BlockPos block, Vec3 point, SpellStats stats, SpellContext context) {
            BlockPos center = block != null ? block : BlockPos.containing(point).below();
            int radius = 2 + Math.max(0, (int) Math.round(stats.getAoeMultiplier()));
            int count = Math.max(1, Math.min(StoneHoard.space(player), BASE + 6 * amp(stats)));
            List<BlockPos> where = new ArrayList<>();
            List<BlockState> what = new ArrayList<>();
            int torn = surge(level, player, center, radius, count, where, what);
            if (torn > 0) {
                level.playSound(null, center, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.5F, 0.6F);
                level.playSound(null, center, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.5F, 0.5F);
                Phenomena.add(new StonePhenomena.Uplift(level, player, where, what));
            }
            StoneHoard.report(player, "arcane_artifacts.stone.surge", torn);
        }

        /** Tears up to {@code count} blocks within {@code radius} of the centre, nearest first. Returns how many. */
        public static int surge(ServerLevel level, Player player, BlockPos center, int radius, int count) {
            return surge(level, player, center, radius, count, new ArrayList<>(), new ArrayList<>());
        }

        /** The same, noting where each torn block was and what it was (for the flight up). */
        public static int surge(ServerLevel level, Player player, BlockPos center, int radius, int count, List<BlockPos> where, List<BlockState> what) {
            List<BlockPos> spots = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, Math.min(1, radius), radius))) {
                if (pos.distSqr(center) <= radius * radius + 1) {
                    spots.add(pos.immutable());
                }
            }
            spots.sort(Comparator.<BlockPos>comparingInt(p -> -p.getY()).thenComparingDouble(p -> p.distSqr(center)));
            int torn = 0;
            for (BlockPos pos : spots) {
                if (torn >= count) {
                    break;
                }
                BlockState state = level.getBlockState(pos);
                if (StoneHoard.tear(level, pos, player)) {
                    where.add(pos);
                    what.add(state);
                    torn++;
                }
            }
            return torn;
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
            return augmentSetOf(AugmentAmplify.INSTANCE, AugmentAOE.INSTANCE);
        }

        @Override
        public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
            super.addAugmentDescriptions(map);
            map.put(AugmentAmplify.INSTANCE, "Tears up six blocks more.");
            map.put(AugmentAOE.INSTANCE, "Reaches a block further.");
        }

        @Override
        public String getBookDescription() {
            return "Rips up to 12 blocks of earth and stone out of the ground into your Stone Hoard, where they circle you.";
        }

        @Override
        protected @NotNull Set<SpellSchool> getSchools() {
            return setOf(SpellSchools.ELEMENTAL_EARTH);
        }
    }

    /** Stone Cage: hoard blocks stand around a creature and hold it; then they come back. */
    public static final class StoneCage extends StoneEffect {
        public static final StoneCage INSTANCE = new StoneCage();
        public static final int MAX_ACTIVE = 3;
        public static final int BASE_TICKS = 120;

        private StoneCage() {
            super("glyph_stone_cage", "Stone Cage");
        }

        @Override
        protected void resolve(ServerLevel level, Player player, @Nullable LivingEntity target, @Nullable BlockPos block, Vec3 point, SpellStats stats, SpellContext context) {
            if (target == null || target == player) {
                return;
            }
            if (StonePhenomena.Cage.active(player.getUUID()) >= MAX_ACTIVE) {
                StoneHoard.report(player, "arcane_artifacts.stone.cage_limit", MAX_ACTIVE);
                return;
            }
            int need = StonePhenomena.Cage.plan(target).size();
            int ticks = Math.max(40, BASE_TICKS + (int) Math.round(stats.getDurationMultiplier() * 40));
            List<BlockState> blocks = StoneHoard.takeReal(player, Math.min(need, StoneHoard.count(player)));
            if (blocks.isEmpty()) {
                StoneHoard.report(player, "arcane_artifacts.stone.too_little", need);
                return;
            }
            StonePhenomena.Cage cage = new StonePhenomena.Cage(level, player, target, ticks);
            int used = cage.raise(blocks);
            for (int i = used; i < blocks.size(); i++) {
                StoneHoard.add(player, blocks.get(i));
            }
            if (used > 0) {
                Phenomena.add(cage);
                com.soulinchains.arcaneartifacts.necro.EffectShackle.shackle(level, target, ticks, player);
            }
            StoneHoard.report(player, "arcane_artifacts.stone.cage", used, StonePhenomena.Cage.active(player.getUUID()), MAX_ACTIVE);
        }

        @Override
        public int getDefaultManaCost() {
            return 60;
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
            map.put(AugmentExtendTime.INSTANCE, "The cage stands two seconds longer.");
            map.put(AugmentDurationDown.INSTANCE, "The cage falls sooner.");
        }

        @Override
        public String getBookDescription() {
            return "Blocks from your Stone Hoard stand around the creature and hold it for 6 seconds, then return to you. Three cages at most.";
        }

        @Override
        protected @NotNull Set<SpellSchool> getSchools() {
            return setOf(SpellSchools.ELEMENTAL_EARTH);
        }
    }

    /** Stone Lances: hoard stone bursts from the ground as lances, wounds and holds. */
    public static final class StoneLances extends StoneEffect {
        public static final StoneLances INSTANCE = new StoneLances();
        public static final int COST = 6;
        public static final float DAMAGE = 8F;

        private StoneLances() {
            super("glyph_stone_lances", "Stone Lances");
        }

        @Override
        protected void resolve(ServerLevel level, Player player, @Nullable LivingEntity target, @Nullable BlockPos block, Vec3 point, SpellStats stats, SpellContext context) {
            int aoe = Math.max(0, (int) Math.round(stats.getAoeMultiplier()));
            int cost = COST + 2 * aoe;
            if (empty(player, cost)) {
                return;
            }
            List<BlockState> blocks = StoneHoard.take(player, cost);
            float damage = MagicHandler.isHarmless(context) ? 0F : DAMAGE + 3F * amp(stats);
            int hold = Math.max(10, 40 + (int) Math.round(stats.getDurationMultiplier() * 20));
            Vec3 at = target != null ? target.position() : point;
            Phenomena.add(new StonePhenomena.Lances(level, player, at, blocks, damage, 1.5 * aoe, hold));
            StoneHoard.report(player, "arcane_artifacts.stone.lances", blocks.size());
        }

        @Override
        public int getDefaultManaCost() {
            return 70;
        }

        @Override
        public SpellTier defaultTier() {
            return SpellTier.TWO;
        }

        @NotNull
        @Override
        public Set<AbstractAugment> getCompatibleAugments() {
            return augmentSetOf(AugmentAmplify.INSTANCE, AugmentAOE.INSTANCE, AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
        }

        @Override
        public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
            super.addAugmentDescriptions(map);
            map.put(AugmentAmplify.INSTANCE, "The lances wound harder.");
            map.put(AugmentAOE.INSTANCE, "Two lances more, in a wider ring.");
            map.put(AugmentExtendTime.INSTANCE, "Holds the impaled a second longer.");
            map.put(AugmentDurationDown.INSTANCE, "Lets the impaled go sooner.");
        }

        @Override
        public String getBookDescription() {
            return "Six lances of hoard stone burst from the ground under the target: 8 damage, and they hold it for 2 seconds.";
        }

        @Override
        protected @NotNull Set<SpellSchool> getSchools() {
            return setOf(SpellSchools.ELEMENTAL_EARTH);
        }
    }

    // ------------------------------------------------------------------ Mage of Mysteries

    /** Myriad Volley: the whole hoard flies at the enemies around. */
    public static final class MyriadVolley extends StoneEffect {
        public static final MyriadVolley INSTANCE = new MyriadVolley();
        public static final float DAMAGE = 2F;
        public static final double RANGE = 16;

        private MyriadVolley() {
            super("glyph_myriad_volley", "Myriad Volley");
        }

        @Override
        protected void resolve(ServerLevel level, Player player, @Nullable LivingEntity target, @Nullable BlockPos block, Vec3 point, SpellStats stats, SpellContext context) {
            if (empty(player, 1)) {
                return;
            }
            double range = RANGE + 4 * Math.max(0, stats.getAoeMultiplier());
            List<LivingEntity> targets = new ArrayList<>();
            if (target != null && target != player) {
                targets.add(target);
            } else {
                Vec3 around = block != null || target == player ? point : player.position();
                for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(around, around).inflate(target == player ? range : 6),
                        l -> l != player && l.isAlive() && Allies.isValidHostileTarget(player, l))) {
                    targets.add(living);
                }
                targets.sort(Comparator.comparingDouble(l -> l.distanceToSqr(player)));
            }
            List<BlockState> blocks = StoneHoard.take(player, StoneHoard.MAX);
            float damage = MagicHandler.isHarmless(context) ? 0F : DAMAGE + amp(stats);
            Phenomena.add(new StonePhenomena.Volley(level, player, blocks, targets, point, damage));
            StoneHoard.report(player, "arcane_artifacts.stone.volley", blocks.size());
        }

        @Override
        public int getDefaultManaCost() {
            return 50;
        }

        @Override
        public SpellTier defaultTier() {
            return SpellTier.TWO;
        }

        @NotNull
        @Override
        public Set<AbstractAugment> getCompatibleAugments() {
            return augmentSetOf(AugmentAmplify.INSTANCE, AugmentAOE.INSTANCE);
        }

        @Override
        public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
            super.addAugmentDescriptions(map);
            map.put(AugmentAmplify.INSTANCE, "Each block wounds harder.");
            map.put(AugmentAOE.INSTANCE, "Looks for enemies four blocks further.");
        }

        @Override
        public String getBookDescription() {
            return "Your whole Stone Hoard flies at the enemies, one block after another: 2 damage each.";
        }

        @Override
        protected @NotNull Set<SpellSchool> getSchools() {
            return setOf(SpellSchools.MANIPULATION);
        }
    }

    /** Stone Wings: a dozen hoard blocks spread behind your back; you fly. */
    public static final class StoneWings extends StoneEffect {
        public static final StoneWings INSTANCE = new StoneWings();
        public static final int BASE_TICKS = 400;

        private StoneWings() {
            super("glyph_stone_wings", "Stone Wings");
        }

        @Override
        protected void resolve(ServerLevel level, Player player, @Nullable LivingEntity target, @Nullable BlockPos block, Vec3 point, SpellStats stats, SpellContext context) {
            if (StoneHoard.count(player) < HoardVisuals.WING_BLOCKS) {
                StoneHoard.report(player, "arcane_artifacts.stone.too_little", HoardVisuals.WING_BLOCKS);
                return;
            }
            int ticks = Math.max(100, BASE_TICKS + (int) Math.round(stats.getDurationMultiplier() * 200));
            HoardVisuals.grantWings(player, ticks);
            level.playSound(null, player.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.6F, 1.4F);
            StoneHoard.report(player, "arcane_artifacts.stone.wings", ticks / 20);
        }

        @Override
        public int getDefaultManaCost() {
            return 120;
        }

        @Override
        public SpellTier defaultTier() {
            return SpellTier.THREE;
        }

        @NotNull
        @Override
        public Set<AbstractAugment> getCompatibleAugments() {
            return augmentSetOf(AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
        }

        @Override
        public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
            super.addAugmentDescriptions(map);
            map.put(AugmentExtendTime.INSTANCE, "Ten seconds more of flight.");
            map.put(AugmentDurationDown.INSTANCE, "The wings fold sooner.");
        }

        @Override
        public String getBookDescription() {
            return "Twelve blocks of your Stone Hoard spread behind your back as wings: you fly for 20 seconds. Needs 12 blocks.";
        }

        @Override
        protected @NotNull Set<SpellSchool> getSchools() {
            return setOf(SpellSchools.MANIPULATION);
        }
    }

    /** Cataclysm: the hoard pressed into a meteor that falls on the point. */
    public static final class Cataclysm extends StoneEffect {
        public static final Cataclysm INSTANCE = new Cataclysm();
        public static final int COST = 24;
        public static final float DAMAGE = 28F;

        private Cataclysm() {
            super("glyph_cataclysm", "Cataclysm");
        }

        @Override
        protected void resolve(ServerLevel level, Player player, @Nullable LivingEntity target, @Nullable BlockPos block, Vec3 point, SpellStats stats, SpellContext context) {
            if (empty(player, COST)) {
                return;
            }
            List<BlockState> blocks = StoneHoard.take(player, COST);
            boolean harmless = MagicHandler.isHarmless(context);
            float damage = harmless ? 0F : Math.max(4F, DAMAGE + 6F * amp(stats));
            double radius = 6 + 1.5 * Math.max(0, stats.getAoeMultiplier());
            Phenomena.add(new StonePhenomena.Meteor(level, player, point, blocks, damage, radius, !harmless));
            StoneHoard.report(player, "arcane_artifacts.stone.cataclysm", blocks.size());
        }

        @Override
        public int getDefaultManaCost() {
            return 200;
        }

        @Override
        public SpellTier defaultTier() {
            return SpellTier.THREE;
        }

        @NotNull
        @Override
        public Set<AbstractAugment> getCompatibleAugments() {
            return augmentSetOf(AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE, AugmentAOE.INSTANCE);
        }

        @Override
        public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
            super.addAugmentDescriptions(map);
            map.put(AugmentAmplify.INSTANCE, "A heavier stone: more damage.");
            map.put(AugmentDampen.INSTANCE, "A lighter stone: less damage.");
            map.put(AugmentAOE.INSTANCE, "A wider blast.");
        }

        @Override
        public String getBookDescription() {
            return "Twenty-four blocks of your Stone Hoard become one great ball of stone that falls on the point and bursts: up to 28 damage, fire, a shockwave and a burning crater.";
        }

        @Override
        protected @NotNull Set<SpellSchool> getSchools() {
            return setOf(SpellSchools.MANIPULATION);
        }
    }

    /** Stone Dominion: for a few seconds the stone glyphs spend nothing from the hoard. */
    public static final class StoneDominion extends StoneEffect {
        public static final StoneDominion INSTANCE = new StoneDominion();
        public static final int BASE_TICKS = 80;

        private StoneDominion() {
            super("glyph_stone_dominion", "Stone Dominion");
        }

        @Override
        protected void resolve(ServerLevel level, Player player, @Nullable LivingEntity target, @Nullable BlockPos block, Vec3 point, SpellStats stats, SpellContext context) {
            int ticks = Math.max(40, BASE_TICKS + (int) Math.round(stats.getDurationMultiplier() * 40));
            StoneHoard.grantDominion(player, ticks);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1F, 0.6F);
            StoneHoard.report(player, "arcane_artifacts.stone.dominion", ticks / 20);
        }

        @Override
        public int getDefaultManaCost() {
            return 150;
        }

        @Override
        public SpellTier defaultTier() {
            return SpellTier.THREE;
        }

        @NotNull
        @Override
        public Set<AbstractAugment> getCompatibleAugments() {
            return augmentSetOf(AugmentExtendTime.INSTANCE);
        }

        @Override
        public void addAugmentDescriptions(Map<AbstractAugment, String> map) {
            super.addAugmentDescriptions(map);
            map.put(AugmentExtendTime.INSTANCE, "Two seconds more.");
        }

        @Override
        public String getBookDescription() {
            return "For 4 seconds Lances, Volley and Cataclysm take blocks from your Stone Hoard without spending them.";
        }

        @Override
        protected @NotNull Set<SpellSchool> getSchools() {
            return setOf(SpellSchools.MANIPULATION);
        }
    }
}
