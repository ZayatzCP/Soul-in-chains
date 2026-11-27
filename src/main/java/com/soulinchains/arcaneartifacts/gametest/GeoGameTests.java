package com.soulinchains.arcaneartifacts.gametest;

import com.mojang.authlib.GameProfile;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.geo.HoardVisuals;
import com.soulinchains.arcaneartifacts.geo.StoneGlyphs;
import com.soulinchains.arcaneartifacts.geo.StoneHoard;
import com.soulinchains.arcaneartifacts.geo.StonePhenomena;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** The stone glyphs (1.43): the hoard, tearing the ground up, the cage, the lances, the volley, the wings and the meteor. */
@GameTestHolder(ArcaneArtifacts.MODID)
@PrefixGameTestTemplate(false)
public final class GeoGameTests {
    private static final String ARENA = "arena";

    private GeoGameTests() {
    }

    private static FakePlayer mage(GameTestHelper helper, String name) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
        StoneHoard.clear(player);
        return player;
    }

    private static List<BlockState> stones(int n) {
        return new ArrayList<>(Collections.nCopies(n, Blocks.STONE.defaultBlockState()));
    }

    @GameTest(template = ARENA)
    public static void theHoardHoldsFortyEight(GameTestHelper helper) {
        FakePlayer player = mage(helper, "hoarder");
        for (int i = 0; i < 60; i++) {
            StoneHoard.add(player, Blocks.COBBLESTONE.defaultBlockState());
        }
        helper.assertTrue(StoneHoard.count(player) == StoneHoard.MAX, "No more than 48 blocks, got " + StoneHoard.count(player));
        helper.assertTrue(StoneHoard.take(player, 10).size() == 10 && StoneHoard.count(player) == 38, "Taking spends");
        StoneHoard.grantDominion(player, 100);
        helper.assertTrue(StoneHoard.take(player, 48).size() == 48 && StoneHoard.count(player) == 38, "Under Dominion nothing is spent");
        helper.assertTrue(StoneHoard.takeReal(player, 8).size() == 8 && StoneHoard.count(player) == 30, "Real blocks are spent even under Dominion");
        StoneHoard.grantDominion(player, 0);
        helper.assertTrue(!StoneHoard.dominion(player), "Dominion ends");
        StoneHoard.clear(player);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void earthenSurgeTearsOnlyTheGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = mage(helper, "surger");
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.OAK_PLANKS);
        helper.setBlock(new BlockPos(3, 1, 4), Blocks.CHEST);
        helper.setBlock(new BlockPos(5, 1, 4), Blocks.OBSIDIAN);
        int torn = StoneGlyphs.EarthenSurge.surge(level, player, helper.absolutePos(new BlockPos(4, 1, 4)), 2, 12);
        helper.assertTrue(torn == 12 && StoneHoard.count(player) == 12, "Twelve blocks torn into the hoard, got " + torn);
        helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(4, 1, 4));
        helper.assertBlockPresent(Blocks.CHEST, new BlockPos(3, 1, 4));
        helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(5, 1, 4));
        helper.assertTrue(StoneHoard.of(player).stream().allMatch(s -> s.is(Blocks.STONE)), "Only stone went into the hoard");
        StoneHoard.clear(player);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void aCageHoldsAndComesBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = mage(helper, "jailer");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        StonePhenomena.Cage cage = new StonePhenomena.Cage(level, player, zombie, 10);
        int need = StonePhenomena.Cage.plan(zombie).size();
        helper.assertTrue(need == 9, "A cage for a zombie: eight posts and a roof, got " + need);
        int used = cage.raise(stones(need));
        helper.assertTrue(used == need && cage.placed().stream().allMatch(p -> level.getBlockState(p).is(Blocks.STONE)), "The cage stands, " + used);
        for (int i = 0; i < 12 && cage.tick(); i++) {
            // the cage stands its time
        }
        helper.assertTrue(cage.placed().isEmpty() && StoneHoard.count(player) == need, "The cage fell and its blocks came back, " + StoneHoard.count(player));
        StoneHoard.clear(player);
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void lancesWoundAndHold(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = mage(helper, "lancer");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        float before = zombie.getHealth();
        StonePhenomena.Lances lances = new StonePhenomena.Lances(level, player, zombie.position(), stones(6), 8F, 0, 40);
        for (int i = 0; i < 60 && lances.tick(); i++) {
            // rise, hold, sink
        }
        helper.assertTrue(zombie.getHealth() <= before - 6F, "The lances wounded the zombie: " + before + " -> " + zombie.getHealth());
        helper.assertTrue(zombie.hasEffect(com.soulinchains.arcaneartifacts.registry.ModEffects.SHACKLED), "And hold it");
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void theVolleyFliesBlockByBlock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = mage(helper, "thrower");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(6, 2, 6));
        zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
        zombie.setHealth(200);
        StonePhenomena.Volley volley = new StonePhenomena.Volley(level, player, stones(10), List.of(zombie), zombie.position(), 2F);
        for (int i = 0; i < 200 && volley.tick(); i++) {
            // block after block
        }
        helper.assertTrue(volley.hits == 10, "Every block struck, " + volley.hits);
        helper.assertTrue(zombie.getHealth() <= 200 - 15F, "Ten blocks, two damage each, no pause: " + zombie.getHealth());
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void theMeteorFallsAndBursts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = mage(helper, "cataclyst");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
        zombie.setHealth(100);
        StonePhenomena.Meteor meteor = new StonePhenomena.Meteor(level, player, zombie.position(), stones(24), 20F, 4);
        for (int i = 0; i < 120 && meteor.tick(); i++) {
            // it falls
        }
        helper.assertTrue(zombie.getHealth() <= 100 - 15F, "Full damage at the centre: " + zombie.getHealth());
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(4, 2, 4));
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theCraterIsScorched(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = mage(helper, "scorcher");
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
                helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
            }
        }
        helper.setBlock(new BlockPos(6, 1, 4), Blocks.OAK_PLANKS);
        Vec3 center = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
        int changed = StonePhenomena.scorch(level, player, center, 3.5);
        helper.assertTrue(changed > 15, "Much of the ground is scorched, " + changed);
        helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(6, 1, 4));
        helper.assertTrue(!helper.getBlockState(new BlockPos(4, 1, 4)).is(Blocks.GRASS_BLOCK), "The middle is blasted");
        helper.assertTrue(!helper.getBlockState(new BlockPos(1, 1, 4)).is(Blocks.GRASS_BLOCK), "The ring is burnt");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void wingsNeedTwelveAndGiveFlight(GameTestHelper helper) {
        FakePlayer player = mage(helper, "flier");
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        HoardVisuals.grantWings(player, 100);
        HoardVisuals.wingFlight(player);
        helper.assertTrue(!player.getAbilities().mayfly, "No flight without twelve blocks");
        for (int i = 0; i < 12; i++) {
            StoneHoard.add(player, Blocks.STONE.defaultBlockState());
        }
        HoardVisuals.wingFlight(player);
        helper.assertTrue(player.getAbilities().mayfly, "Twelve blocks become wings");
        HoardVisuals.Feather tip = HoardVisuals.feather(player, HoardVisuals.FEATHERS - 1, 1);
        HoardVisuals.Feather root = HoardVisuals.feather(player, 0, -1);
        helper.assertTrue(tip.pos().distanceTo(player.position()) > 10 && root.pos().distanceTo(player.position()) < 3,
                "Great wings: the tip far out, the root at the shoulder");
        helper.assertTrue(player.getAbilities().getFlyingSpeed() > 0.08F, "Wing flight is fast");
        StoneHoard.takeReal(player, 1);
        HoardVisuals.wingFlight(player);
        helper.assertTrue(!player.getAbilities().mayfly, "Below twelve the wings fold");
        StoneHoard.clear(player);
        HoardVisuals.clear(player.getUUID());
        helper.succeed();
    }
}
