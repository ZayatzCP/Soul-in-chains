package com.soulinchains.arcaneartifacts.gametest;

import com.mojang.serialization.JsonOps;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.astral.Astral;
import com.soulinchains.arcaneartifacts.astral.AstralMonolithFeature;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.rite.RiteDefinition;
import com.soulinchains.arcaneartifacts.rite.Rites;
import com.soulinchains.arcaneartifacts.rite.Sigil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** The Astral (1.41): the dimension, its plain and monoliths, the landing, the gate and the Ascension rite. */
@GameTestHolder(ArcaneArtifacts.MODID)
@PrefixGameTestTemplate(false)
public final class AstralGameTests {
    private static final String ARENA = "arena";

    private AstralGameTests() {
    }

    private static ServerLevel astral(GameTestHelper helper) {
        ServerLevel astral = Astral.level(helper.getLevel().getServer());
        helper.assertTrue(astral != null, "The Astral is loaded");
        return astral;
    }

    @GameTest(template = ARENA)
    public static void theAstralIsAPlainUnderAHaze(GameTestHelper helper) {
        ServerLevel astral = astral(helper);
        helper.assertTrue(astral.getMinBuildHeight() == 0 && astral.getMaxBuildHeight() == 512, "The Astral is 512 blocks tall, got "
                + astral.getMinBuildHeight() + ".." + astral.getMaxBuildHeight());
        BlockPos land = Astral.landing(astral, 5000, -3000);
        helper.assertTrue(land.getY() == Astral.FLOOR + 1, "One lands on top of the plain");
        helper.assertTrue(astral.getBlockState(land.below()).is(ModBlocks.ASTRAL_STONE.get()), "The plain is astral stone");
        helper.assertTrue(astral.getBlockState(land).isAir() && astral.getBlockState(land.above(40)).isAir(), "Nothing stands over a landing");
        helper.assertTrue(astral.getBlockState(new BlockPos(land.getX(), 0, land.getZ())).is(net.minecraft.world.level.block.Blocks.BEDROCK),
                "Bedrock at the very bottom");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void monolithsRiseToTheTop(GameTestHelper helper) {
        ServerLevel astral = astral(helper);
        int top = astral.getMaxBuildHeight();
        AstralMonolithFeature.Plan found = null;
        int plans = 0;
        for (int cx = -6; cx <= 6; cx++) {
            for (int cz = -6; cz <= 6; cz++) {
                AstralMonolithFeature.Plan plan = AstralMonolithFeature.plan(astral.getSeed(), cx, cz, top);
                if (plan == null) {
                    continue;
                }
                plans++;
                AstralMonolithFeature.Plan again = AstralMonolithFeature.plan(astral.getSeed(), cx, cz, top);
                helper.assertTrue(again != null && again.x() == plan.x() && again.z() == plan.z() && again.half() == plan.half(),
                        "A cell always holds the same monolith");
                helper.assertTrue(plan.reach() <= 15, "A monolith stays within 15 blocks of its centre, got " + plan.reach());
                helper.assertTrue(Math.floorDiv(plan.x(), AstralMonolithFeature.CELL) == cx && Math.floorDiv(plan.z(), AstralMonolithFeature.CELL) == cz,
                        "A monolith stands in its own cell");
                if (found == null) {
                    found = plan;
                }
            }
        }
        helper.assertTrue(plans > 40 && plans < 160, "About six cells in ten hold a monolith, got " + plans + " of 169");
        int y = top - 1;
        BlockPos high = new BlockPos(found.x() + found.leanX(y), y, found.z() + found.leanZ(y));
        BlockState state = astral.getBlockState(high);
        helper.assertTrue(state.is(ModBlocks.ASTRAL_MONOLITH.get()) || state.is(ModBlocks.ASTRAL_VEIN.get()),
                "The monolith reaches the top of the world, got " + state + " at " + high);
        int foot = Astral.FLOOR + 10;
        BlockPos low = new BlockPos(found.x() + found.leanX(foot), foot, found.z() + found.leanZ(foot));
        helper.assertTrue(!astral.getBlockState(low).isAir(), "And stands on the plain");
        int veins = 0;
        int h = found.half(foot);
        for (int d = -h; d <= h; d++) {
            if (astral.getBlockState(low.offset(d, 0, -h)).is(ModBlocks.ASTRAL_VEIN.get())
                    || astral.getBlockState(low.offset(-h, 0, d)).is(ModBlocks.ASTRAL_VEIN.get())) {
                veins++;
            }
        }
        helper.assertTrue(veins >= 1 || found.shape() != 0, "A square monolith has a glowing seam down its face");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void theAscensionRaisesAGate(GameTestHelper helper) {
        ServerLevel astral = astral(helper);
        BlockPos gate = Astral.ascend(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), List.of());
        helper.assertTrue(gate != null && astral.getBlockState(gate).is(ModBlocks.ASTRAL_GATE.get()), "A gate rises in the Astral");
        BlockPos again = Astral.ascend(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), List.of());
        helper.assertTrue(gate.equals(again), "A second Ascension from the same site lands at the same gate");
        helper.assertTrue(astral.getBlockState(gate).getDestroySpeed(astral, gate) < 0, "The gate cannot be broken");
        astral.removeBlock(gate, false);
        // the origin a player keeps survives saving
        Astral.Origin origin = new Astral.Origin(Level.NETHER, new BlockPos(12, 70, -4), true);
        Astral.Origin read = Astral.Origin.CODEC.parse(JsonOps.INSTANCE, Astral.Origin.CODEC.encodeStart(JsonOps.INSTANCE, origin).getOrThrow()).getOrThrow();
        helper.assertTrue(read.equals(origin), "The way home is saved, got " + read);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theAscensionIsAChalkRite(GameTestHelper helper) {
        RiteDefinition rite = Rites.get(ResourceLocation.fromNamespaceAndPath(ArcaneArtifacts.MODID, "ascend"));
        helper.assertTrue(rite != null && rite.kind() == RiteDefinition.Kind.ASCEND, "The Ascension is loaded");
        helper.assertTrue(rite.main() == Sigil.VRATA && rite.south() == Sigil.OGRADA && rite.west() == Sigil.OKO && rite.east() == Sigil.OKO,
                "Gate north, Fence south, Eye west and east");
        for (RiteDefinition other : Rites.all()) {
            if (other != rite) {
                helper.assertTrue(!(other.main() == rite.main() && other.south() == rite.south() && other.west() == rite.west()
                        && other.east() == rite.east()), "No other rite has the Ascension's sigils: " + other.id());
            }
        }
        helper.succeed();
    }
}
