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

    /** The Astral, or null: the game-test server loads only the overworld, a real world loads the Astral as well. */
    private static ServerLevel astral(GameTestHelper helper) {
        return Astral.level(helper.getLevel().getServer());
    }

    @GameTest(template = ARENA)
    public static void theAstralIsAPlainUnderAHaze(GameTestHelper helper) {
        ServerLevel astral = astral(helper);
        if (astral != null) {
            helper.assertTrue(astral.getMinBuildHeight() == 0 && astral.getMaxBuildHeight() == 512, "The Astral is 512 blocks tall");
            BlockPos land = Astral.landing(astral, 5000, -3000);
            helper.assertTrue(land.getY() == Astral.FLOOR + 1, "One lands on top of the plain");
            helper.assertTrue(astral.getBlockState(land.below()).is(ModBlocks.ASTRAL_STONE.get()), "The plain is astral stone");
            helper.assertTrue(astral.getBlockState(land).isAir() && astral.getBlockState(land.above(40)).isAir(), "Nothing stands over a landing");
        }
        // without an Astral the Ascension does nothing and harms no one
        helper.assertTrue(astral != null || Astral.ascend(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), List.of()) == null,
                "No Astral, no gate");
        Astral.Origin origin = new Astral.Origin(Level.NETHER, new BlockPos(12, 70, -4), true);
        Astral.Origin read = Astral.Origin.CODEC.parse(JsonOps.INSTANCE, Astral.Origin.CODEC.encodeStart(JsonOps.INSTANCE, origin).getOrThrow()).getOrThrow();
        helper.assertTrue(read.equals(origin), "The way home is saved, got " + read);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void monolithsArePlacedBySeed(GameTestHelper helper) {
        int top = 512;
        int plans = 0;
        for (int cx = -6; cx <= 6; cx++) {
            for (int cz = -6; cz <= 6; cz++) {
                AstralMonolithFeature.Plan plan = AstralMonolithFeature.plan(1234L, cx, cz, top);
                if (plan == null) {
                    continue;
                }
                plans++;
                AstralMonolithFeature.Plan again = AstralMonolithFeature.plan(1234L, cx, cz, top);
                helper.assertTrue(again != null && again.x() == plan.x() && again.z() == plan.z() && again.half() == plan.half(),
                        "A cell always holds the same monolith");
                helper.assertTrue(plan.reach() <= 15, "A monolith stays within 15 blocks of its centre, got " + plan.reach());
                helper.assertTrue(Math.floorDiv(plan.x(), AstralMonolithFeature.CELL) == cx && Math.floorDiv(plan.z(), AstralMonolithFeature.CELL) == cz,
                        "A monolith stands in its own cell");
                helper.assertTrue(plan.half(Astral.FLOOR) > plan.half(Astral.FLOOR + 10) && plan.half(top) >= 2, "A plinth at the foot, never thinner than 2");
            }
        }
        helper.assertTrue(plans > 40 && plans < 160, "About six cells in ten hold a monolith, got " + plans + " of 169");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void aMonolithIsBuiltWithGlowingSeams(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(4, 1, 4));
        // a square monolith of half-width 3, straight, over the test; built only up to 30 blocks over the Astral floor
        int[] still = new int[1];
        AstralMonolithFeature.Plan plan = new AstralMonolithFeature.Plan(centre.getX(), centre.getZ(), 3, 0, true, 1000, still, still, 16);
        int top = Astral.FLOOR + 30;
        AstralMonolithFeature.build(level, plan, top);
        int y = Astral.FLOOR + 10;
        BlockPos middle = new BlockPos(centre.getX(), y, centre.getZ());
        helper.assertTrue(level.getBlockState(middle).is(ModBlocks.ASTRAL_MONOLITH.get()), "Solid stone inside");
        helper.assertTrue(level.getBlockState(middle.offset(0, 0, -3)).is(ModBlocks.ASTRAL_VEIN.get()), "A glowing seam down the middle of a face");
        helper.assertTrue(level.getBlockState(middle.offset(3, 0, -3)).is(ModBlocks.ASTRAL_MONOLITH.get()), "Plain stone at the corner");
        helper.assertTrue(level.getBlockState(new BlockPos(centre.getX() + 3, 80, centre.getZ() - 3)).is(ModBlocks.ASTRAL_VEIN.get()), "A glowing band");
        helper.assertTrue(level.getBlockState(middle.offset(4, 0, 0)).isAir(), "Nothing outside");
        helper.assertTrue(!level.getBlockState(new BlockPos(centre.getX() + 5, Astral.FLOOR, centre.getZ())).isAir(), "The plinth is wider");
        helper.assertTrue(level.getBlockState(new BlockPos(centre.getX(), top - 1, centre.getZ())).is(ModBlocks.ASTRAL_MONOLITH.get())
                && level.getBlockState(new BlockPos(centre.getX(), top, centre.getZ())).isAir(), "It rises to the top it is given");
        // clear it away
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int yy = Astral.FLOOR - 3; yy < top; yy++) {
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    level.setBlock(pos.set(centre.getX() + dx, yy, centre.getZ() + dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theGateCannotBeBroken(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 2, 2), ModBlocks.ASTRAL_GATE.get());
        BlockPos gate = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.assertTrue(helper.getLevel().getBlockState(gate).getDestroySpeed(helper.getLevel(), gate) < 0, "The gate cannot be broken");
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
