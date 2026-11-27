package com.soulinchains.arcaneartifacts.gametest;

import com.mojang.serialization.JsonOps;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.astral.Astral;
import com.soulinchains.arcaneartifacts.astral.AstralTerrainFeature;
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
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** The Astral (1.41): the plain and its rises, the monoliths, the landing, the gate and the Ascension rite. */
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
            helper.assertTrue(astral.getBlockState(land.below()).is(ModBlocks.ASTRAL_STONE.get()), "One lands on the plain");
            helper.assertTrue(astral.getBlockState(land).isAir() && astral.getBlockState(land.above(40)).isAir(), "Nothing stands over a landing");
        }
        helper.assertTrue(astral != null || Astral.ascend(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), List.of()) == null,
                "No Astral, no gate");
        Astral.Origin origin = new Astral.Origin(Level.NETHER, new BlockPos(12, 70, -4), true);
        Astral.Origin read = Astral.Origin.CODEC.parse(JsonOps.INSTANCE, Astral.Origin.CODEC.encodeStart(JsonOps.INSTANCE, origin).getOrThrow()).getOrThrow();
        helper.assertTrue(read.equals(origin), "The way home is saved, got " + read);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void thePlainRisesGently(GameTestHelper helper) {
        long seed = 20251120L;
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        int flat = 0;
        int samples = 0;
        for (int x = -2000; x <= 2000; x += 7) {
            for (int z = -2000; z <= 2000; z += 53) {
                int g = AstralTerrainFeature.ground(seed, x, z);
                low = Math.min(low, g);
                high = Math.max(high, g);
                if (g == Astral.FLOOR) {
                    flat++;
                }
                samples++;
                int next = AstralTerrainFeature.ground(seed, x + 1, z);
                int side = AstralTerrainFeature.ground(seed, x, z + 1);
                helper.assertTrue(Math.abs(next - g) <= 1 && Math.abs(side - g) <= 1, "Rises are gentle: at most a block per block");
            }
        }
        helper.assertTrue(low == Astral.FLOOR && high >= Astral.FLOOR + 8 && high <= Astral.FLOOR + AstralTerrainFeature.HILLS,
                "The plain rises from " + low + " to " + high);
        helper.assertTrue(flat > samples / 5 && flat < samples * 4 / 5, "Both flats and rises, " + flat + " flat of " + samples);
        helper.assertTrue(AstralTerrainFeature.ground(seed, 123, 456) == AstralTerrainFeature.ground(seed, 123, 456)
                && AstralTerrainFeature.ground(seed, 123, 456) == AstralTerrainFeature.ground(seed, 123, 456), "The same seed, the same land");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void monolithsComeInManyHeights(GameTestHelper helper) {
        int top = 512;
        int plans = 0;
        int straight = 0;
        Map<AstralTerrainFeature.Kind, Integer> kinds = new EnumMap<>(AstralTerrainFeature.Kind.class);
        for (int cx = -10; cx <= 10; cx++) {
            for (int cz = -10; cz <= 10; cz++) {
                AstralTerrainFeature.Plan plan = AstralTerrainFeature.plan(1234L, cx, cz, top);
                if (plan == null) {
                    continue;
                }
                plans++;
                kinds.merge(plan.kind(), 1, Integer::sum);
                if (plan.tiltX() == 0 && plan.tiltZ() == 0) {
                    straight++;
                }
                AstralTerrainFeature.Plan again = AstralTerrainFeature.plan(1234L, cx, cz, top);
                helper.assertTrue(again != null && again.equals(plan), "A cell always holds the same monolith");
                helper.assertTrue(Math.floorDiv(plan.x(), AstralTerrainFeature.CELL) == cx && Math.floorDiv(plan.z(), AstralTerrainFeature.CELL) == cz,
                        "A monolith stands in its own cell");
                // it never reaches out of the area the chunks around it look at
                for (int y = 1; y < plan.top(); y += 16) {
                    helper.assertTrue(Math.abs(plan.centerX(y) - plan.x()) + plan.half(y) <= 70 && Math.abs(plan.centerZ(y) - plan.z()) + plan.half(y) <= 70,
                            "A leaning monolith stays near its cell");
                }
                switch (plan.kind()) {
                    case WHOLE -> helper.assertTrue(plan.top() == top, "A whole monolith reaches the top of the world");
                    case SUNK -> helper.assertTrue(plan.top() < Astral.FLOOR + 50, "A sunk monolith barely clears the ground");
                    default -> helper.assertTrue(plan.top() > Astral.FLOOR + 100 && plan.top() <= top, "A tall one stops high up");
                }
            }
        }
        helper.assertTrue(plans > 200 && plans < 340, "About six cells in ten hold a monolith, got " + plans + " of 441");
        helper.assertTrue(kinds.size() == 3, "Whole, tall and sunk monoliths all occur: " + kinds);
        helper.assertTrue(straight > plans / 5 && straight < plans * 3 / 5, "Some straight, most leaning a little: " + straight + " of " + plans);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void aMonolithIsSolidAndPlain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(4, 1, 4));
        int top = Astral.FLOOR + 30;
        // a square monolith of half-width 3 leaning a block east every 10 blocks, built chunk by chunk over the test
        AstralTerrainFeature.Plan plan = new AstralTerrainFeature.Plan(centre.getX(), centre.getZ(), 3, 0, true, 1000, 0.1, 0, top,
                AstralTerrainFeature.Kind.TALL);
        int minX = (centre.getX() - 8) & ~15;
        int minZ = (centre.getZ() - 8) & ~15;
        for (int x = minX - 16; x <= minX + 32; x += 16) {
            for (int z = minZ - 16; z <= minZ + 32; z += 16) {
                AstralTerrainFeature.build(level, plan, x, z);
            }
        }
        int y = Astral.FLOOR + 10;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                helper.assertTrue(level.getBlockState(new BlockPos(centre.getX() + 1 + dx, y, centre.getZ() + dz)).is(ModBlocks.ASTRAL_MONOLITH.get()),
                        "One solid stone all through, shifted a block by its lean");
            }
        }
        helper.assertTrue(level.getBlockState(new BlockPos(centre.getX() - 3, y, centre.getZ())).isAir(), "The lean leaves the west side open");
        helper.assertTrue(level.getBlockState(new BlockPos(centre.getX() + 3 + 3, top - 1, centre.getZ())).is(ModBlocks.ASTRAL_MONOLITH.get())
                && level.getBlockState(new BlockPos(centre.getX() + 3, top, centre.getZ())).isAir(), "It leans further up and stops at its top");
        helper.assertTrue(level.getBlockState(new BlockPos(centre.getX(), y, centre.getZ())).getDestroySpeed(level, centre) < 0, "It cannot be broken");
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int yy = 1; yy < top; yy++) {
            for (int dx = -12; dx <= 12; dx++) {
                for (int dz = -6; dz <= 6; dz++) {
                    pos.set(centre.getX() + dx, yy, centre.getZ() + dz);
                    if (level.getBlockState(pos).is(ModBlocks.ASTRAL_MONOLITH.get())) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
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
    public static void snowLiesInDrifts(GameTestHelper helper) {
        long seed = 20251120L;
        int bare = 0;
        int deep = 0;
        int samples = 0;
        for (int x = -1500; x <= 1500; x += 11) {
            for (int z = -1500; z <= 1500; z += 37) {
                int layers = AstralTerrainFeature.snow(seed, x, z);
                helper.assertTrue(layers >= 0 && layers <= 7, "Up to seven layers of snow");
                bare += layers == 0 ? 1 : 0;
                deep += layers >= 4 ? 1 : 0;
                samples++;
            }
        }
        helper.assertTrue(bare > samples / 5 && bare < samples * 4 / 5 && deep > 0, "Drifts and bare crust between them: "
                + bare + " bare, " + deep + " deep of " + samples);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theSnowHidesForgottenThings(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(ArcaneArtifacts.MODID, "blocks/astral_snow"));
        var table = level.getServer().reloadableRegistries().getLootTable(key);
        helper.assertTrue(table != net.minecraft.world.level.storage.loot.LootTable.EMPTY, "The snow's loot table loads");
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(at))
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SHOVEL))
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, ModBlocks.ASTRAL_SNOW.get().defaultBlockState())
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
        int found = 0;
        boolean book = false;
        for (int i = 0; i < 4000; i++) {
            for (net.minecraft.world.item.ItemStack stack : table.getRandomItems(params)) {
                found++;
                helper.assertTrue(stack.has(net.minecraft.core.component.DataComponents.ITEM_NAME)
                        && stack.has(net.minecraft.core.component.DataComponents.LORE), "Every forgotten thing has its own name and lore");
                if (stack.is(net.minecraft.world.item.Items.WRITTEN_BOOK)) {
                    var content = stack.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
                    book |= content != null && !content.pages().isEmpty();
                }
            }
        }
        helper.assertTrue(found > 150 && found < 360, "About one dig in sixteen finds something, got " + found + " of 4000");
        helper.assertTrue(book, "Books in an unknown tongue are among them");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aWatcherIsGoneWhenLookedAt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        net.neoforged.neoforge.common.util.FakePlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "astral_traveller"));
        BlockPos feet = helper.absolutePos(new BlockPos(2, 2, 2));
        player.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0F, 0F);
        com.soulinchains.arcaneartifacts.astral.Watcher watcher = com.soulinchains.arcaneartifacts.registry.ModEntities.WATCHER.get().create(level);
        helper.assertTrue(watcher != null, "A Watcher can be made");
        watcher.moveTo(feet.getX() + 0.5, feet.getY() + 20, feet.getZ() + 60.5, 0F, 0F);
        double dy = watcher.getY() + watcher.getBbHeight() * 0.6 - player.getEyeY();
        float up = (float) -Math.toDegrees(Math.atan2(dy, 60));
        player.setXRot(up);
        player.setYRot(0F);
        helper.assertTrue(watcher.seenBy(player), "Looking straight at it, south and up");
        player.setYRot(90F);
        helper.assertTrue(!watcher.seenBy(player), "Looking west, it is not seen");
        player.setYRot(8F);
        helper.assertTrue(!watcher.seenBy(player), "From the corner of the eye it stays");
        helper.assertTrue(watcher.getBbHeight() > 6, "It is tall");
        watcher.discard();
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
