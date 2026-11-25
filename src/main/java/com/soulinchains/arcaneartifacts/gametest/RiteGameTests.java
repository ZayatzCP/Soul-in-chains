package com.soulinchains.arcaneartifacts.gametest;

import com.mojang.authlib.GameProfile;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.rite.CageValue;
import com.soulinchains.arcaneartifacts.rite.ChalkColor;
import com.soulinchains.arcaneartifacts.rite.ChalkItem;
import com.soulinchains.arcaneartifacts.rite.ChalkMarkBlock;
import com.soulinchains.arcaneartifacts.rite.OfferingPedestalBlockEntity;
import com.soulinchains.arcaneartifacts.rite.RiteDefinition;
import com.soulinchains.arcaneartifacts.rite.RiteHeartBlockEntity;
import com.soulinchains.arcaneartifacts.rite.RiteModifiers;
import com.soulinchains.arcaneartifacts.rite.RiteQuality;
import com.soulinchains.arcaneartifacts.rite.RiteRole;
import com.soulinchains.arcaneartifacts.rite.RiteSite;
import com.soulinchains.arcaneartifacts.rite.RiteSummons;
import com.soulinchains.arcaneartifacts.rite.Rites;
import com.soulinchains.arcaneartifacts.rite.Sigil;
import com.soulinchains.arcaneartifacts.rite.SigilReading;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** The Cultist's chalk rites (1.38): sigils, strokes, the site, quality, cages, a whole rite, allies and corruption. */
@GameTestHolder(ArcaneArtifacts.MODID)
@PrefixGameTestTemplate(false)
public final class RiteGameTests {
    private static final String ARENA = "arena";
    /** The Heart, high over the arena so the site has room (relative). */
    private static final BlockPos HEART = new BlockPos(4, 12, 4);

    private RiteGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name, double x, double y, double z) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        Vec3 pos = helper.absoluteVec(new Vec3(x, y, z));
        player.setPos(pos.x, pos.y, pos.z);
        return player;
    }

    private static int[][] turned(int[][] lines, int turns, boolean mirror) {
        int[][] out = new int[lines.length][2];
        for (int i = 0; i < lines.length; i++) {
            for (int j = 0; j < 2; j++) {
                int cell = lines[i][j];
                if (mirror) {
                    cell = (cell / 3) * 3 + (2 - cell % 3);
                }
                for (int t = 0; t < turns; t++) {
                    cell = (cell % 3) * 3 + (2 - cell / 3);
                }
                out[i][j] = cell;
            }
        }
        return out;
    }

    /** Draws a sigil (its marks and lines) with its north-west cell at {@code corner} (absolute, chalk layer). */
    private static void draw(ServerLevel level, BlockPos corner, Sigil sigil, ChalkColor color) {
        for (int[] line : sigil.lines) {
            for (int cell : line) {
                BlockPos p = corner.offset(cell % 3, 0, cell / 3);
                if (!(level.getBlockState(p).getBlock() instanceof ChalkMarkBlock)) {
                    level.setBlock(p, ModBlocks.CHALK_MARK.get().defaultBlockState().setValue(ChalkMarkBlock.COLOR, color), 3);
                }
            }
        }
        for (int[] line : sigil.lines) {
            ChalkMarkBlock.join(level, corner.offset(line[0] % 3, 0, line[0] / 3), corner.offset(line[1] % 3, 0, line[1] / 3));
        }
    }

    /** A 9x9 site: deepslate floor, the Heart with a pedestal, an amethyst boundary, a roof; returns the Heart (absolute). */
    private static BlockPos site(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos heart = helper.absolutePos(HEART);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                level.setBlock(heart.offset(dx, 0, dz), Blocks.DEEPSLATE_TILES.defaultBlockState(), 3);
                for (int dy = 1; dy <= 3; dy++) {
                    level.setBlock(heart.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                }
                level.setBlock(heart.offset(dx, 4, dz), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 3);
                if (Math.abs(dx) == 5 || Math.abs(dz) == 5) {
                    level.setBlock(heart.offset(dx, 1, dz), ModBlocks.CHALK_MARK.get().defaultBlockState().setValue(ChalkMarkBlock.COLOR, ChalkColor.AMETHYST), 3);
                }
            }
        }
        level.setBlock(heart, ModBlocks.RITE_HEART.get().defaultBlockState(), 3);
        level.setBlock(heart.above(), ModBlocks.OFFERING_PEDESTAL.get().defaultBlockState(), 3);
        return heart;
    }

    /** The Summoning of the Pack: Gate (cinnabar) north, Fence (white) south, Bonds (coal) west and east. */
    private static void packSigils(ServerLevel level, RiteSite.Place main, BlockPos heart) {
        draw(level, heart.offset(-1, 1, -4), Sigil.VRATA, ChalkColor.CINNABAR);
        draw(level, heart.offset(-1, 1, 2), Sigil.OGRADA, ChalkColor.WHITE);
        draw(level, heart.offset(-4, 1, -1), Sigil.UZY, ChalkColor.COAL);
        draw(level, heart.offset(2, 1, -1), Sigil.UZY, ChalkColor.COAL);
    }

    @GameTest(template = ARENA)
    public static void sigilsKeepTheirShape(GameTestHelper helper) {
        java.util.Set<Sigil> seen = new java.util.HashSet<>();
        for (Sigil sigil : Sigil.values()) {
            for (int t = 0; t < 4; t++) {
                for (boolean m : new boolean[]{false, true}) {
                    helper.assertTrue(Sigil.match(turned(sigil.lines, t, m)) == sigil, sigil + " turned " + t + (m ? " mirrored" : "") + " is still " + sigil);
                }
            }
            seen.add(Sigil.match(sigil.lines));
        }
        helper.assertTrue(seen.size() == 15, "Fifteen distinct sigils");
        helper.assertTrue(ChalkColor.BONE.serves(ChalkColor.CINNABAR) && !ChalkColor.BONE.serves(ChalkColor.AMETHYST)
                && !ChalkColor.WHITE.serves(ChalkColor.COAL), "Bone chalk stands in for any colour but amethyst");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void chalkStrokesDrawAnEye(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.DEEPSLATE_TILES);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
            }
        }
        FakePlayer player = player(helper, "chalker", 3.5, 2, 1.5);
        BlockPos nw = helper.absolutePos(new BlockPos(2, 2, 2));
        // two strokes: north to south through the middle, then west to east
        helper.assertTrue(ChalkItem.draw(level, player, null, nw.offset(1, 0, 0), ChalkColor.WHITE) == 1, "A first mark");
        helper.assertTrue(ChalkItem.draw(level, player, null, nw.offset(1, 0, 1), ChalkColor.WHITE) == 2, "The next block joins it");
        ChalkItem.draw(level, player, null, nw.offset(1, 0, 2), ChalkColor.WHITE);
        ChalkItem.endStroke(player);
        ChalkItem.draw(level, player, null, nw.offset(0, 0, 1), ChalkColor.WHITE);
        helper.assertTrue(ChalkItem.draw(level, player, null, nw.offset(1, 0, 1), ChalkColor.WHITE) == 2, "A stroke can go on from a mark");
        ChalkItem.draw(level, player, null, nw.offset(2, 0, 1), ChalkColor.WHITE);
        SigilReading eye = SigilReading.read(level, nw);
        helper.assertTrue(eye.is(Sigil.OKO), "Two crossing strokes make the Eye, got " + eye.sigil());
        helper.assertTrue(!ChalkMarkBlock.linked(level, nw.offset(0, 0, 1), Direction.NORTH), "No line where none was drawn");
        player.setShiftKeyDown(true);
        helper.assertTrue(ChalkItem.draw(level, player, null, nw.offset(2, 0, 1), ChalkColor.WHITE) == 3, "Sneaking rubs a mark out");
        helper.assertTrue(SigilReading.read(level, nw).is(Sigil.TISH), "Without one arm the Eye becomes the Hush");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void siteCallsItsRite(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos heart = site(helper);
        packSigils(level, RiteSite.Place.MAIN, heart);
        RiteSite site = RiteSite.scan(level, heart);
        helper.assertTrue(site.ok() && site.size == 9 && site.participants() == 2, "A 9x9 site for two, problems " + site.problems);
        RiteDefinition rite = Rites.calledBy(site);
        helper.assertTrue(rite != null && rite.id().getPath().equals("summon_pack"), "Gate, Fence and Bonds call the Summoning of the Pack");
        helper.assertTrue(RiteQuality.start(site, RiteModifiers.of(site)) == 55F, "Base 30 + 10 main + 3x5 sides, got " + RiteQuality.start(site, RiteModifiers.of(site)));
        // candles, a lantern and the censer
        level.setBlock(heart.offset(3, 1, 3), Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true), 3);
        level.setBlock(heart.offset(6, 1, 6), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
        level.setBlock(heart.offset(0, 3, 0), ModBlocks.RITUAL_CENSER.get().defaultBlockState(), 3);
        RiteSite lit = RiteSite.scan(level, heart);
        helper.assertTrue(RiteQuality.start(lit, RiteModifiers.of(lit)) == 55F + 4 + 3 + 8, "Candles, a lantern and the censer add, got " + RiteQuality.start(lit, RiteModifiers.of(lit)));
        // a Gate in the wrong colour does not count
        level.setBlock(heart.offset(0, 1, -4), ModBlocks.CHALK_MARK.get().defaultBlockState().setValue(ChalkMarkBlock.COLOR, ChalkColor.WHITE), 3);
        helper.assertTrue(Rites.calledBy(RiteSite.scan(level, heart)) == null, "A mixed-colour Gate calls nothing");
        level.setBlock(heart.offset(1, 4, 1), Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(RiteSite.scan(level, heart).problems.contains("roof"), "A hole in the roof is noticed");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void modifiersAddUp(GameTestHelper helper) {
        RiteModifiers two = new RiteModifiers().add(Sigil.SONM).add(Sigil.SONM);
        helper.assertTrue(two.extraCount() == 1.5F && two.quality() == -20F, "Two Hosts: +1.5 creatures for 20% quality");
        RiteModifiers clash = new RiteModifiers().add(Sigil.SONM).add(Sigil.VENEC);
        helper.assertTrue(clash.extraCount() == -1F && clash.quality() == -10F && !clash.empowered(), "Host and Crown cancel: only their prices");
        RiteModifiers others = new RiteModifiers().add(Sigil.VITOK).add(Sigil.KROV).add(Sigil.ROG).add(Sigil.ZHILA).add(Sigil.TISH);
        helper.assertTrue(others.lifeFactor() == 1.5F && others.extraChannelTicks() == 200 && others.bloodPrice() == 6F
                && others.eliteChance() == 0.15F && others.extraLoot() == 0.25F && others.fewerEchoes() == 1 && others.quality() == 10F, "Each modifier does its part");
        helper.assertTrue(RiteRole.assign(2).get(0).contains(RiteRole.READER) && RiteRole.assign(2).get(1).contains(RiteRole.TRACER)
                && RiteRole.assign(5).get(4).contains(RiteRole.GUARD), "Roles are shared out by the number of participants");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cagedCreaturesHaveWorth(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.enchant(helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 1);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, sword);
        helper.assertTrue(CageValue.of(zombie) == 4, "A zombie with a helmet and an enchanted sword: 1 + 1 + 2, got " + CageValue.of(zombie));
        Zombie baby = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(6, 2, 6));
        baby.setBaby(true);
        Chicken chicken = helper.spawnWithNoFreeWill(EntityType.CHICKEN, new BlockPos(6, 2, 6));
        baby.startRiding(chicken, true);
        helper.assertTrue(CageValue.of(baby) == 6, "A chicken jockey: (2 + 1) x 2, got " + CageValue.of(baby));
        helper.assertTrue(CageValue.single(helper.spawnWithNoFreeWill(EntityType.BLAZE, new BlockPos(4, 2, 6))) == 5, "A blaze is not of this world: 5");
        FakePlayer player = player(helper, "caging", 3, 2, 3);
        helper.assertTrue(com.soulinchains.arcaneartifacts.rite.RiteCageItem.capture(player, zombie) == null, "A zombie at full health does not go in");
        zombie.setHealth(zombie.getMaxHealth() / 3);
        ItemStack cage = com.soulinchains.arcaneartifacts.rite.RiteCageItem.capture(player, zombie);
        helper.assertTrue(cage != null && com.soulinchains.arcaneartifacts.rite.RiteCageItem.full(cage) && zombie.isRemoved(), "A worn-down zombie is caged");
        helper.assertTrue(com.soulinchains.arcaneartifacts.rite.RiteCageItem.data(cage).getInt("value") == 4, "The cage keeps its worth");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void aRiteRunsToItsEnd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos heartPos = site(helper);
        packSigils(level, RiteSite.Place.MAIN, heartPos);
        ((OfferingPedestalBlockEntity) level.getBlockEntity(heartPos.above())).setItem(new ItemStack(Items.BONE, 8));
        FakePlayer reader = player(helper, "rite_reader", HEART.getX() + 2.5, HEART.getY() + 1, HEART.getZ() + 0.5);
        FakePlayer tracer = player(helper, "rite_tracer", HEART.getX() - 2.5, HEART.getY() + 1, HEART.getZ() + 0.5);
        RiteHeartBlockEntity heart = (RiteHeartBlockEntity) level.getBlockEntity(heartPos);
        RiteSite site = RiteSite.scan(level, heartPos);
        helper.assertTrue(heart.tryStart(level, site, reader, List.of(reader)) != null || reader.isCreative(), "One player is not enough for a 9x9 site");
        String problem = heart.tryStart(level, site, reader, List.of(reader, tracer));
        helper.assertTrue(problem == null && heart.phase() == RiteHeartBlockEntity.Phase.CHANNEL, "Two players start the rite, got " + problem);
        helper.assertTrue(heart.has(reader, RiteRole.READER) && heart.has(tracer, RiteRole.TRACER), "Roles are handed out");
        helper.assertTrue(((OfferingPedestalBlockEntity) level.getBlockEntity(heartPos.above())).item().isEmpty(), "The bones are burnt");
        int traced = 0;
        for (int i = 0; i < 1200 && heart.phase() == RiteHeartBlockEntity.Phase.CHANNEL; i++) {
            heart.tick();
            RiteSite.Place fading = heart.fading();
            if (fading != null) {
                Sigil sigil = site.at(fading).sigil();
                int cell = sigil.lines[0][0];
                BlockPos mark = site.corner(fading).offset(cell % 3, 0, cell / 3);
                if (ChalkItem.draw(level, tracer, null, mark, sigil.color) == 4) {
                    traced++;
                }
            }
        }
        helper.assertTrue(traced >= 3, "The Tracer retraced fading sigils, " + traced);
        helper.assertTrue(heart.phase() == RiteHeartBlockEntity.Phase.CLOSING, "The channel ends in the closing");
        heart.use(tracer, false);
        helper.assertTrue(heart.phase() == RiteHeartBlockEntity.Phase.CLOSING, "Only the Reader closes");
        heart.use(reader, false);
        helper.assertTrue(heart.phase() == RiteHeartBlockEntity.Phase.IDLE, "The Reader closes the rite");
        AABB box = new AABB(heartPos).inflate(12);
        List<net.minecraft.world.entity.animal.Wolf> wolves = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Wolf.class, box, RiteSummons::allied);
        helper.assertTrue(wolves.size() >= 1 && wolves.stream().allMatch(w -> w.isTame()), "Tame wolves answer the rite, " + wolves.size());
        wolves.forEach(w -> w.discard());
        level.getEntitiesOfClass(com.soulinchains.arcaneartifacts.rite.Echo.class, box.inflate(8)).forEach(e -> e.discard());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void alliesNeverTurnOnPlayers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = player(helper, "ally_owner", 2, 2, 2);
        List<net.minecraft.world.entity.Mob> risen = RiteSummons.raise(level, helper.absolutePos(new BlockPos(4, 2, 4)), EntityType.IRON_GOLEM, 1, 200,
                1.5F, true, 0F, null, false, List.of(player));
        helper.assertTrue(risen.size() == 1 && risen.get(0) instanceof IronGolem, "One golem rises");
        IronGolem golem = (IronGolem) risen.get(0);
        golem.setTarget(player);
        helper.assertTrue(golem.getTarget() != player, "An allied summon does not take a player as its target");
        helper.assertTrue(golem.getMaxHealth() > 100F, "The Crown makes it stronger");
        golem.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void corruptionSpreadsAndIsCleansed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos heartPos = helper.absolutePos(new BlockPos(4, 1, 4));
        level.setBlock(heartPos, ModBlocks.RITE_HEART.get().defaultBlockState(), 3);
        RiteHeartBlockEntity heart = (RiteHeartBlockEntity) level.getBlockEntity(heartPos);
        for (int dz = -9; dz <= 9; dz++) {
            level.setBlock(heartPos.offset(7, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            level.setBlock(heartPos.offset(7, 0, dz), Blocks.AIR.defaultBlockState(), 3);
        }
        heart.setTaint(1);
        int spread = heart.spreadTaint(level, 400);
        helper.assertTrue(spread >= 1, "Taint turns grass around the site into corrupted soil, " + spread);
        int cleaned = heart.cleanse(level, 2);
        helper.assertTrue(cleaned == spread && heart.taint() == 0, "Cleansing turns it all back, " + cleaned + " of " + spread);
        BlockState after = level.getBlockState(heartPos.offset(7, -1, 0));
        helper.assertTrue(!after.is(ModBlocks.CORRUPTED_SOIL.get()), "No corrupted soil is left");
        helper.succeed();
    }
}
