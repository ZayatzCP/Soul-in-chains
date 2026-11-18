package com.soulinchains.arcaneartifacts.gametest;

import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.catto.Catto;
import com.soulinchains.arcaneartifacts.catto.CattoContent;
import com.soulinchains.arcaneartifacts.catto.CattoTask;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Catto doing real errands in the test arena (no owner online: test mode delivers to the bound chest). */
@GameTestHolder(ArcaneArtifacts.MODID)
@PrefixGameTestTemplate(false)
public final class CattoGameTests {
    private static final String ARENA = "arena";

    private CattoGameTests() {
    }

    private static Catto catto(GameTestHelper helper, BlockPos at) {
        Catto cat = helper.spawn(CattoContent.CATTO.get(), at);
        cat.testMode = true;
        cat.nature = com.soulinchains.arcaneartifacts.catto.CattoNature.GENTLE;
        cat.setOwnerUUID(UUID.randomUUID());
        cat.setTame(true, false);
        cat.equipment.setStackInSlot(Catto.SLOT_BAG, new ItemStack(CattoContent.TRAVELER_PACK.get()));
        return cat;
    }

    private static BlockPos chest(GameTestHelper helper, Catto cat, BlockPos at) {
        helper.setBlock(at, Blocks.CHEST);
        BlockPos abs = helper.absolutePos(at);
        cat.storages.add(abs);
        return at;
    }

    private static String why(Catto cat) {
        StringBuilder out = new StringBuilder(" [state=" + cat.brain().state() + ", status=" + cat.status().getString() + ", pos=" + cat.blockPosition());
        cat.reports.forEach(r -> out.append(" | ").append(r.key()).append(r.args()));
        return out.append("]").toString();
    }

    private static int inChest(GameTestHelper helper, BlockPos at, Item item) {
        if (!(helper.getBlockEntity(at) instanceof ChestBlockEntity chest)) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (chest.getItem(i).is(item)) {
                n += chest.getItem(i).getCount();
            }
        }
        return n;
    }

    @GameTest(template = ARENA)
    public static void cattoTaskSurvivesSaving(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        CattoTask mine = CattoTask.mine(Blocks.IRON_ORE, new BlockPos(1, 3, 3), 12);
        mine.done = 5;
        CattoTask loaded = CattoTask.load(mine.save(registries), registries);
        helper.assertTrue(loaded != null && loaded.type == CattoTask.Type.MINE && loaded.count == 12 && loaded.done == 5
                && loaded.a.equals(new BlockPos(1, 3, 3)) && mine.block.equals(loaded.block), "Mine task changed after saving");
        CattoTask fetch = CattoTask.fetch(new ItemStack(Items.IRON_INGOT), 9);
        CattoTask fetched = CattoTask.load(fetch.save(registries), registries);
        helper.assertTrue(fetched != null && fetched.item.is(Items.IRON_INGOT) && fetched.count == 9, "Fetch task changed after saving");
        CattoTask restarted = mine.restart(registries);
        helper.assertTrue(restarted.done == 0 && restarted.count == 12, "A scroll copy keeps progress");

        Catto cat = catto(helper, new BlockPos(4, 2, 4));
        cat.queue.add(mine);
        cat.xp[2] = 200;
        cat.bag.setStackInSlot(0, new ItemStack(Items.APPLE, 7));
        CompoundTag tag = cat.saveWithoutId(new CompoundTag());
        Catto copy = CattoContent.CATTO.get().create(helper.getLevel());
        helper.assertTrue(copy != null, "Catto could not be created");
        copy.load(tag);
        helper.assertTrue(copy.queue.size() == 1 && copy.xp[2] == 200 && copy.bag.getStackInSlot(0).getCount() == 7
                && copy.equipment.getStackInSlot(Catto.SLOT_BAG).is(CattoContent.TRAVELER_PACK.get()), "Catto forgot something when saved");
        helper.assertTrue(copy.capacity() == 9, "The traveler's pack should give 9 slots");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 800)
    public static void cattoMinesAndStores(GameTestHelper helper) {
        Catto cat = catto(helper, new BlockPos(1, 2, 1));
        cat.equipment.setStackInSlot(Catto.SLOT_TOOL, new ItemStack(Items.STONE_PICKAXE));
        BlockPos chest = chest(helper, cat, new BlockPos(1, 2, 7));
        helper.setBlock(new BlockPos(5, 2, 5), Blocks.COPPER_ORE);
        helper.setBlock(new BlockPos(6, 2, 5), Blocks.COPPER_ORE);
        helper.setBlock(new BlockPos(5, 2, 6), Blocks.COPPER_ORE);
        cat.queue.add(CattoTask.mine(Blocks.COPPER_ORE, helper.absolutePos(new BlockPos(5, 2, 5)), 3));
        helper.succeedWhen(() -> {
            for (BlockPos ore : new BlockPos[]{new BlockPos(5, 2, 5), new BlockPos(6, 2, 5), new BlockPos(5, 2, 6)}) {
                helper.assertTrue(!helper.getBlockState(ore).is(Blocks.COPPER_ORE), "Copper ore still at " + ore + why(cat));
            }
            helper.assertTrue(inChest(helper, chest, Items.RAW_COPPER) >= 3, "Raw copper is not in the chest yet");
            helper.assertTrue(cat.queue.isEmpty(), "The errand is not finished");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void cattoWillNotMineNextToLava(GameTestHelper helper) {
        Catto cat = catto(helper, new BlockPos(1, 2, 1));
        cat.equipment.setStackInSlot(Catto.SLOT_TOOL, new ItemStack(Items.STONE_PICKAXE));
        chest(helper, cat, new BlockPos(1, 2, 7));
        helper.setBlock(new BlockPos(5, 2, 5), Blocks.COPPER_ORE);
        helper.setBlock(new BlockPos(6, 2, 5), Blocks.LAVA);
        cat.queue.add(CattoTask.mine(Blocks.COPPER_ORE, helper.absolutePos(new BlockPos(5, 2, 5)), 1));
        helper.runAtTickTime(300, () -> {
            helper.assertBlockPresent(Blocks.COPPER_ORE, new BlockPos(5, 2, 5));
            helper.assertTrue(cat.queue.isEmpty(), "Catto should give up on the only ore, which is next to lava" + why(cat));
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void cattoGathersItems(GameTestHelper helper) {
        Catto cat = catto(helper, new BlockPos(4, 2, 4));
        BlockPos chest = chest(helper, cat, new BlockPos(1, 2, 7));
        for (BlockPos at : new BlockPos[]{new BlockPos(1, 2, 1), new BlockPos(7, 2, 2), new BlockPos(6, 2, 7)}) {
            Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(at));
            ItemEntity item = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, new ItemStack(Items.APPLE, 2));
            item.setDeltaMovement(Vec3.ZERO);
            helper.getLevel().addFreshEntity(item);
        }
        cat.queue.add(CattoTask.collect(helper.absolutePos(new BlockPos(0, 2, 0)), helper.absolutePos(new BlockPos(8, 3, 8)), ItemStack.EMPTY));
        helper.succeedWhen(() -> helper.assertTrue(inChest(helper, chest, Items.APPLE) == 6, "Not all apples are in the chest"));
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void cattoBuildsAWall(GameTestHelper helper) {
        Catto cat = catto(helper, new BlockPos(4, 2, 2));
        cat.bag.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 16));
        cat.queue.add(CattoTask.fill(helper.absolutePos(new BlockPos(2, 2, 6)), helper.absolutePos(new BlockPos(6, 2, 6)),
                new ItemStack(Items.COBBLESTONE)));
        helper.succeedWhen(() -> {
            for (int x = 2; x <= 6; x++) {
                helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(x, 2, 6));
            }
            helper.assertTrue(cat.queue.isEmpty(), "The build errand is not finished");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void cattoCraftsPlanks(GameTestHelper helper) {
        Catto cat = catto(helper, new BlockPos(4, 2, 4));
        cat.bag.setStackInSlot(1, new ItemStack(Items.CRAFTING_TABLE));
        BlockPos chest = chest(helper, cat, new BlockPos(1, 2, 7));
        if (helper.getBlockEntity(chest) instanceof ChestBlockEntity box) {
            box.setItem(0, new ItemStack(Items.OAK_LOG, 4));
        }
        cat.queue.add(CattoTask.craft(new ItemStack(Items.OAK_PLANKS), 8));
        helper.succeedWhen(() -> {
            helper.assertTrue(inChest(helper, chest, Items.OAK_PLANKS) >= 8, "The planks are not in the chest yet");
            helper.assertTrue(cat.queue.isEmpty(), "The craft errand is not finished");
        });
    }

    @GameTest(template = ARENA)
    public static void blueprintCopiesABuilding(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.OAK_PLANKS);
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.OAK_PLANKS);
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.GLASS);
        ItemStack parchment = new ItemStack(CattoContent.BLUEPRINT.get());
        var structure = com.soulinchains.arcaneartifacts.catto.CattoItems.BlueprintItem.capture(helper.getLevel(),
                helper.absolutePos(new BlockPos(2, 2, 2)), helper.absolutePos(new BlockPos(3, 3, 2)));
        helper.assertTrue(structure != null && structure.getInt("Count") == 3, "The blueprint should hold 3 blocks");
        var layout = com.soulinchains.arcaneartifacts.catto.CattoItems.BlueprintItem.layout(helper.getLevel(), structure,
                helper.absolutePos(new BlockPos(5, 2, 5)));
        helper.assertTrue(layout.size() == 3 && layout.get(layout.size() - 1).getValue().is(Blocks.GLASS)
                && layout.get(layout.size() - 1).getKey().equals(helper.absolutePos(new BlockPos(6, 3, 5))), "The copy is laid out wrong");
        helper.assertTrue(parchment.is(CattoContent.BLUEPRINT.get()), "Blueprint item missing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void wildCattoHasNoOwnerAndNoVillageHere(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.assertTrue(com.soulinchains.arcaneartifacts.catto.CattoVillageSpawner.trySpawn(level, at) == null,
                "A Catto should not move into the test arena: there is no village");
        helper.assertTrue(com.soulinchains.arcaneartifacts.catto.CattoVillageSpawner.spawnAt(level, at) != null, "No room for a village Catto");
        Catto wild = level.getEntitiesOfClass(Catto.class, new net.minecraft.world.phys.AABB(at).inflate(1)).stream().findFirst().orElse(null);
        helper.assertTrue(wild != null && wild.getOwnerUUID() == null && !wild.isTame(), "The village Catto should have no owner");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cattoGearFitsItsSlots(GameTestHelper helper) {
        Catto cat = catto(helper, new BlockPos(4, 2, 4));
        helper.assertTrue(cat.fitsSlot(Catto.SLOT_HEAD, new ItemStack(CattoContent.SOMBRERITO.get())), "The sombrero goes on the head");
        helper.assertTrue(!cat.fitsSlot(Catto.SLOT_ARMOR, new ItemStack(CattoContent.SOMBRERITO.get())), "The sombrero is not armor");
        helper.assertTrue(cat.fitsSlot(Catto.SLOT_TOOL, new ItemStack(CattoContent.HAMMER.get())), "The hammer is a tool");
        helper.assertTrue(cat.fitsSlot(Catto.SLOT_WEAPON, new ItemStack(Items.IRON_SWORD)), "A sword is a weapon");
        helper.assertTrue(cat.fitsSlot(Catto.SLOT_OFFHAND, new ItemStack(Items.COOKED_COD)) && cat.fitsSlot(Catto.SLOT_OFFHAND, new ItemStack(Items.SHIELD))
                && cat.fitsSlot(Catto.SLOT_OFFHAND, new ItemStack(Items.TORCH)), "The second paw takes food, a shield and torches");
        int before = cat.maxSource();
        cat.equipment.setStackInSlot(Catto.SLOT_HEAD, new ItemStack(CattoContent.ARCANE_HOOD.get()));
        cat.equipment.setStackInSlot(Catto.SLOT_ARMOR, new ItemStack(CattoContent.ARCANE_MANTLE.get()));
        helper.assertTrue(cat.maxSource() == before + 600, "Arcane gear should add 600 Source");
        helper.assertTrue(cat.fullSet() == com.soulinchains.arcaneartifacts.catto.CattoGear.Set.ARCANE, "Hood and mantle make the arcane set");
        helper.assertTrue(cat.armor() == 4, "Hood and mantle give 4 armor");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void cattoHammerKnocksOutNeighbours(GameTestHelper helper) {
        Catto cat = catto(helper, new BlockPos(1, 2, 1));
        cat.equipment.setStackInSlot(Catto.SLOT_TOOL, new ItemStack(CattoContent.HAMMER.get()));
        chest(helper, cat, new BlockPos(1, 2, 7));
        helper.setBlock(new BlockPos(5, 2, 5), Blocks.COPPER_ORE);
        helper.setBlock(new BlockPos(6, 2, 5), Blocks.COPPER_ORE);
        helper.setBlock(new BlockPos(5, 2, 6), Blocks.COPPER_ORE);
        cat.queue.add(CattoTask.mine(Blocks.COPPER_ORE, helper.absolutePos(new BlockPos(5, 2, 5)), 1));
        helper.succeedWhen(() -> {
            for (BlockPos ore : new BlockPos[]{new BlockPos(5, 2, 5), new BlockPos(6, 2, 5), new BlockPos(5, 2, 6)}) {
                helper.assertTrue(!helper.getBlockState(ore).is(Blocks.COPPER_ORE), "The hammer left copper ore at " + ore + why(cat));
            }
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void brawlerCattoFights(GameTestHelper helper) {
        Catto cat = catto(helper, new BlockPos(2, 2, 2));
        cat.nature = com.soulinchains.arcaneartifacts.catto.CattoNature.BRAWLER;
        cat.equipment.setStackInSlot(Catto.SLOT_WEAPON, new ItemStack(Items.IRON_SWORD));
        net.minecraft.world.entity.monster.Husk husk = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.HUSK, new BlockPos(6, 2, 6));
        husk.setTarget(cat);
        helper.succeedWhen(() -> helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "The brawler did not hit the husk" + why(cat)));
    }

    @GameTest(template = ARENA)
    public static void cattoVillagePiecesLoad(GameTestHelper helper) {
        var manager = helper.getLevel().getStructureManager();
        for (String piece : com.soulinchains.arcaneartifacts.catto.CattoVillage.PIECES) {
            var template = manager.get(ArcaneArtifacts.id("village/" + piece));
            helper.assertTrue(template.isPresent(), "Missing village piece " + piece);
            var jigsaws = template.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                    Blocks.JIGSAW);
            helper.assertTrue(!jigsaws.isEmpty(), "Village piece " + piece + " has no jigsaw to join a street");
            var cushions = template.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                    com.soulinchains.arcaneartifacts.catto.CattoVillage.CUSHION.get());
            helper.assertTrue(!cushions.isEmpty(), "Village piece " + piece + " has no Catto Cushion");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void cattoPicksUpABetterTool(GameTestHelper helper) {
        Catto cat = catto(helper, new BlockPos(2, 2, 2));
        Vec3 pos = helper.absoluteVec(new Vec3(5.5, 2, 5.5));
        ItemEntity pick = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, new ItemStack(Items.IRON_PICKAXE));
        pick.setNoPickUpDelay();
        pick.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(pick);
        helper.succeedWhen(() -> helper.assertTrue(cat.tool().is(Items.IRON_PICKAXE), "Catto did not take the pickaxe" + why(cat)
                + " want=" + com.soulinchains.arcaneartifacts.catto.CattoLoot.want(cat, new ItemStack(Items.IRON_PICKAXE))
                + " alive=" + pick.isAlive() + " delay=" + pick.hasPickUpDelay() + " at=" + pick.blockPosition()
                + " seen=" + helper.getLevel().getEntitiesOfClass(ItemEntity.class, cat.getBoundingBox().inflate(6, 3, 6)).size()));
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void gentleCattoGuardsWhenTold(GameTestHelper helper) {
        Catto cat = catto(helper, new BlockPos(2, 2, 2));
        cat.mode = com.soulinchains.arcaneartifacts.catto.CattoMode.GUARD;
        cat.guardPos = helper.absolutePos(new BlockPos(3, 2, 3));
        cat.equipment.setStackInSlot(Catto.SLOT_WEAPON, new ItemStack(Items.IRON_SWORD));
        net.minecraft.world.entity.monster.Husk husk = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.HUSK, new BlockPos(6, 2, 6));
        husk.setTarget(cat);
        helper.succeedWhen(() -> helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "The guarding Catto did not fight" + why(cat)));
    }

    @GameTest(template = ARENA)
    public static void cattoKeepsItsCoat(GameTestHelper helper) {
        Catto cat = catto(helper, new BlockPos(4, 2, 4));
        cat.setCoat(com.soulinchains.arcaneartifacts.catto.CattoCoat.CALICO);
        cat.nature = com.soulinchains.arcaneartifacts.catto.CattoNature.THRIFTY;
        Catto copy = CattoContent.CATTO.get().create(helper.getLevel());
        helper.assertTrue(copy != null, "Catto could not be created");
        copy.load(cat.saveWithoutId(new CompoundTag()));
        helper.assertTrue(copy.coat() == com.soulinchains.arcaneartifacts.catto.CattoCoat.CALICO
                && copy.nature() == com.soulinchains.arcaneartifacts.catto.CattoNature.THRIFTY, "Coat or temperament lost on saving");
        helper.assertTrue(com.soulinchains.arcaneartifacts.catto.CattoCoat.fromVanilla("minecraft:calico") == com.soulinchains.arcaneartifacts.catto.CattoCoat.CALICO,
                "A calico cat should stay calico");
        helper.succeed();
    }
}
