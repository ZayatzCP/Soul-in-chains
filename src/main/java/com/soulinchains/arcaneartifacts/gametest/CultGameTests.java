package com.soulinchains.arcaneartifacts.gametest;

import com.mojang.authlib.GameProfile;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.cult.AshWitness;
import com.soulinchains.arcaneartifacts.cult.Cult;
import com.soulinchains.arcaneartifacts.cult.CultRole;
import com.soulinchains.arcaneartifacts.cult.RitualAnchorBlockEntity;
import com.soulinchains.arcaneartifacts.cult.RitualSealBlockEntity;
import com.soulinchains.arcaneartifacts.cult.RitualSummons;
import com.soulinchains.arcaneartifacts.cult.TornAcolyte;
import com.soulinchains.arcaneartifacts.registry.ModBlocks;
import com.soulinchains.arcaneartifacts.registry.ModEntities;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The Cultist (1.32): the Pairing, the Rite of the Ash Witness, the solo cap and the honest loot bonus. Two real players. */
@GameTestHolder(ArcaneArtifacts.MODID)
@PrefixGameTestTemplate(false)
public final class CultGameTests {
    private static final String ARENA = "arena";

    private CultGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name, double x, double z) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        Vec3 pos = helper.absoluteVec(new Vec3(x, 2, z));
        player.setPos(pos.x, pos.y, pos.z);
        return player;
    }

    /** A seal in the middle of the arena and four anchors around it. */
    private static RitualSealBlockEntity circle(GameTestHelper helper, List<RitualAnchorBlockEntity> anchors) {
        helper.setBlock(new BlockPos(4, 2, 4), ModBlocks.RITUAL_SEAL.get());
        for (BlockPos p : new BlockPos[]{new BlockPos(1, 2, 4), new BlockPos(7, 2, 4), new BlockPos(4, 2, 1), new BlockPos(4, 2, 7)}) {
            helper.setBlock(p, ModBlocks.RITUAL_ANCHOR.get());
            anchors.add((RitualAnchorBlockEntity) helper.getBlockEntity(p));
        }
        return (RitualSealBlockEntity) helper.getBlockEntity(new BlockPos(4, 2, 4));
    }

    @GameTest(template = ARENA)
    public static void pairingSwearsTwoRoles(GameTestHelper helper) {
        List<RitualAnchorBlockEntity> anchors = new ArrayList<>();
        RitualSealBlockEntity seal = circle(helper, anchors);
        FakePlayer a = player(helper, "confessor_p", 4.5, 3.0);
        FakePlayer b = player(helper, "warden_p", 2.0, 4.5);
        seal.onKnife(a);
        helper.assertTrue(seal.phase() == RitualSealBlockEntity.Phase.PAIRING_WAIT, "The knife on the seal must begin the Pairing");
        seal.onShieldAtAnchor(a, anchors.get(0));
        helper.assertTrue(seal.phase() == RitualSealBlockEntity.Phase.PAIRING_WAIT, "One cannot pair with oneself");
        seal.onShieldAtAnchor(b, anchors.get(0));
        helper.assertTrue(seal.phase() == RitualSealBlockEntity.Phase.PAIRING, "The shield at an anchor must confirm it");
        for (int i = 0; i < RitualSealBlockEntity.PAIRING_TICKS; i++) {
            seal.tick();
        }
        helper.assertTrue(Cult.role(a) == CultRole.CONFESSOR && Cult.role(b) == CultRole.WARDEN, "Knife: Confessor; shield: Warden");
        helper.assertTrue(Cult.partners(a, b), "They must be each other's partners");
        a.setShiftKeyDown(true);
        seal.onKnife(a);
        helper.assertTrue(!Cult.get(a).paired(), "Sneak + knife on the seal breaks the pair");
        helper.assertTrue(Cult.reconcile(b, a) && !Cult.get(b).paired(), "The partner's side is cleared when they meet the broken pair");
        helper.succeed();
    }

    /** Runs the rite to its end: strikes every window, the Warden mends and seals; returns the seal. */
    private static RitualSealBlockEntity runRite(GameTestHelper helper, Player confessor, Player lighter, Player warden, boolean diligent) {
        List<RitualAnchorBlockEntity> anchors = new ArrayList<>();
        RitualSealBlockEntity seal = circle(helper, anchors);
        for (int i = 0; i < 4; i++) {
            seal.offer(new ItemStack(ModItems.RITUAL_ASH.get()));
        }
        seal.offer(new ItemStack(ModItems.BONE_SHARD.get()));
        seal.offer(new ItemStack(ModItems.BONE_SHARD.get()));
        seal.offer(new ItemStack(Items.RED_CANDLE));
        helper.assertTrue(seal.reagentsReady(), "Four ash, two shards and a red candle make the rite ready");
        seal.onShieldAtAnchor(lighter, anchors.get(0));
        seal.onShieldAtAnchor(lighter, anchors.get(1));
        seal.onKnife(confessor);
        helper.assertTrue(seal.phase() == RitualSealBlockEntity.Phase.CHANNEL, "The knife must start the rite");
        for (int i = 0; i < RitualSealBlockEntity.ASH_TICKS + 5 && seal.phase() == RitualSealBlockEntity.Phase.CHANNEL; i++) {
            seal.tick();
            if (!diligent || seal.phase() != RitualSealBlockEntity.Phase.CHANNEL) {
                continue;
            }
            if (seal.finalWindow()) {
                seal.onShieldAtAnchor(warden, anchors.get(0));
                seal.onKnife(confessor);
            } else if (seal.windowOpen()) {
                seal.onKnife(confessor);
            }
        }
        for (TornAcolyte acolyte : helper.getLevel().getEntitiesOfClass(TornAcolyte.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(24))) {
            acolyte.discard();
        }
        return seal;
    }

    private static List<AshWitness> witnesses(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(AshWitness.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void diligentPairRaisesTwoWitnesses(GameTestHelper helper) {
        FakePlayer confessor = player(helper, "rite_confessor", 4.5, 3.0);
        FakePlayer warden = player(helper, "rite_warden", 1.5, 4.5);
        Cult.pair(confessor, warden);
        RitualSealBlockEntity seal = runRite(helper, confessor, warden, warden, true);
        helper.assertTrue(seal.phase() == RitualSealBlockEntity.Phase.IDLE, "The rite must end");
        helper.assertTrue(seal.quality() >= 80F, "Every window struck and the seal confirmed: flawless or better, got " + seal.quality());
        List<AshWitness> risen = witnesses(helper);
        helper.assertTrue(risen.size() >= 1 && risen.stream().allMatch(RitualSummons::summoned), "Ash Witnesses must rise, marked as summoned");
        helper.assertTrue(Cult.get(confessor).steady() == 1 && Cult.get(warden).steady() == 1, "A steady rite counts for both");
        risen.forEach(AshWitness::discard);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void soloRiteIsCapped(GameTestHelper helper) {
        FakePlayer confessor = player(helper, "solo_confessor", 4.5, 3.0);
        FakePlayer absent = player(helper, "absent_warden", 30, 30);
        Cult.pair(confessor, absent);
        RitualSealBlockEntity seal = runRite(helper, confessor, confessor, confessor, true);
        helper.assertTrue(seal.quality() <= 40.001F, "Without the Warden the rite stops at 40%, got " + seal.quality());
        witnesses(helper).forEach(AshWitness::discard);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void neglectedRiteIsPoor(GameTestHelper helper) {
        FakePlayer confessor = player(helper, "lazy_confessor", 4.5, 3.0);
        FakePlayer warden = player(helper, "lazy_warden", 1.5, 4.5);
        Cult.pair(confessor, warden);
        RitualSealBlockEntity seal = runRite(helper, confessor, warden, warden, false);
        helper.assertTrue(seal.quality() < 40F, "No strikes, no close: a broken rite, got " + seal.quality());
        witnesses(helper).forEach(AshWitness::discard);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ritualLootBonusIsHonest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AshWitness witness = helper.spawnWithNoFreeWill(ModEntities.ASH_WITNESS.get(), new BlockPos(4, 2, 4));
        RitualSummons.mark(witness, 1.5F);
        List<ItemEntity> drops = new ArrayList<>();
        drops.add(new ItemEntity(level, witness.getX(), witness.getY(), witness.getZ(), new ItemStack(ModItems.RITUAL_ASH.get(), 4)));
        drops.add(new ItemEntity(level, witness.getX(), witness.getY(), witness.getZ(), new ItemStack(Items.ROTTEN_FLESH, 4)));
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(witness, level.damageSources().generic(), drops, false));
        int ash = drops.stream().filter(d -> d.getItem().is(ModItems.RITUAL_ASH.get())).mapToInt(d -> d.getItem().getCount()).sum();
        int flesh = drops.stream().filter(d -> d.getItem().is(Items.ROTTEN_FLESH)).mapToInt(d -> d.getItem().getCount()).sum();
        helper.assertTrue(ash == 6 && flesh == 4, "×1.5 only on whitelisted ash: 6 ash, 4 flesh, got " + ash + " / " + flesh);
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(witness, level.damageSources().generic(), drops, false));
        helper.assertTrue(drops.stream().filter(d -> d.getItem().is(ModItems.RITUAL_ASH.get())).mapToInt(d -> d.getItem().getCount()).sum() == 6,
                "The bonus is paid once");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(6, 2, 6));
        List<ItemEntity> plain = new ArrayList<>();
        plain.add(new ItemEntity(level, zombie.getX(), zombie.getY(), zombie.getZ(), new ItemStack(ModItems.RITUAL_ASH.get(), 4)));
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(zombie, level.damageSources().generic(), plain, false));
        helper.assertTrue(plain.size() == 1 && plain.get(0).getItem().getCount() == 4, "A creature of the world gets no bonus");
        helper.assertTrue(witness.markAround() >= 1 && zombie.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "The witness marks hostiles around");
        helper.succeed();
    }

    // ------------------------------------------------------------------ 1.33

    /** Lays the reagents, lights two anchors with the warden's shield and starts the rite with the confessor's knife. */
    private static RitualSealBlockEntity startRite(GameTestHelper helper, Player confessor, Player warden, List<RitualAnchorBlockEntity> anchors) {
        RitualSealBlockEntity seal = circle(helper, anchors);
        for (int i = 0; i < 4; i++) {
            seal.offer(new ItemStack(ModItems.RITUAL_ASH.get()));
        }
        seal.offer(new ItemStack(ModItems.BONE_SHARD.get()));
        seal.offer(new ItemStack(ModItems.BONE_SHARD.get()));
        seal.offer(new ItemStack(Items.RED_CANDLE));
        seal.onShieldAtAnchor(warden, anchors.get(0));
        seal.onShieldAtAnchor(warden, anchors.get(1));
        seal.onKnife(confessor);
        return seal;
    }

    /** Lets a rite run out and clears what it brought. */
    private static void drain(GameTestHelper helper, RitualSealBlockEntity seal) {
        for (int i = 0; i < 700 && seal.phase() != RitualSealBlockEntity.Phase.IDLE; i++) {
            seal.tick();
        }
        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(24);
        helper.getLevel().getEntitiesOfClass(TornAcolyte.class, box).forEach(TornAcolyte::discard);
        helper.getLevel().getEntitiesOfClass(AshWitness.class, box).forEach(AshWitness::discard);
    }

    private static ItemStack knife(com.soulinchains.arcaneartifacts.cult.KnifeInlay inlay) {
        ItemStack knife = new ItemStack(ModItems.RITUAL_KNIFE.get());
        knife.set(com.soulinchains.arcaneartifacts.registry.ModComponents.KNIFE_INLAY, inlay.ordinal());
        return knife;
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void hammerMendsAnchorsAndSoundsTheCall(GameTestHelper helper) {
        FakePlayer confessor = player(helper, "hammer_confessor", 4.5, 3.0);
        FakePlayer warden = player(helper, "hammer_warden", 1.5, 4.5);
        Cult.pair(confessor, warden);
        List<RitualAnchorBlockEntity> anchors = new ArrayList<>();
        RitualSealBlockEntity seal = startRite(helper, confessor, warden, anchors);
        helper.assertTrue(seal.phase() == RitualSealBlockEntity.Phase.CHANNEL, "The rite must run");
        RitualAnchorBlockEntity anchor = anchors.get(0);
        anchor.damage(60);
        helper.assertTrue(seal.onHammerAtAnchor(warden, anchor) && anchor.stability() == 80, "The hammer mends +40, got " + anchor.stability());
        drain(helper, seal);
        helper.assertTrue(!seal.onHammerAtAnchor(warden, anchor), "Outside a rite the hammer does not mend");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        Zombie summoned = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(5, 2, 5));
        RitualSummons.mark(summoned, 1F);
        int called = com.soulinchains.arcaneartifacts.cult.CultTools.call(warden);
        helper.assertTrue(called == 1 && zombie.getTarget() == warden && summoned.getTarget() == null,
                "The Call turns ordinary hostiles on the Warden and leaves the summoned alone, called " + called);
        helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultTools.call(warden) == 0, "The Call has a cooldown");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void wardenInterceptsAndWards(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer confessor = player(helper, "cover_confessor", 4.5, 3.0);
        FakePlayer warden = player(helper, "cover_warden", 4.5, 6.0);
        FakePlayer stranger = player(helper, "cover_stranger", 2.0, 2.0);
        Cult.pair(confessor, warden);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(6, 2, 3));
        var mobBlow = level.damageSources().mobAttack(zombie);
        helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultTools.intercept(confessor, warden, mobBlow, 10F) == 10F,
                "An unarmed Warden does not cover");
        warden.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(ModItems.OATH_SHIELD.get()));
        float kept = com.soulinchains.arcaneartifacts.cult.CultTools.intercept(confessor, warden, mobBlow, 10F);
        helper.assertTrue(Math.abs(kept - 7F) < 1e-3, "A covering Warden takes 30% of a creature's blow, the Confessor keeps " + kept);
        float pvp = com.soulinchains.arcaneartifacts.cult.CultTools.intercept(confessor, warden, level.damageSources().playerAttack(stranger), 10F);
        helper.assertTrue(Math.abs(pvp - 8.5F) < 1e-3, "Of a player's blow only 15%, the Confessor keeps " + pvp);
        helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultTools.ward(warden, confessor)
                && confessor.hasEffect(com.soulinchains.arcaneartifacts.registry.ModEffects.WARDED), "The Seal of Shelter wards the Confessor");
        helper.assertTrue(!com.soulinchains.arcaneartifacts.cult.CultTools.ward(warden, confessor), "The Seal of Shelter has a cooldown");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void censerAndIronInlayShapeTheRite(GameTestHelper helper) {
        FakePlayer confessor = player(helper, "iron_confessor", 4.5, 3.0);
        FakePlayer warden = player(helper, "iron_warden", 1.5, 4.5);
        Cult.pair(confessor, warden);
        confessor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, knife(com.soulinchains.arcaneartifacts.cult.KnifeInlay.IRON));
        helper.setBlock(new BlockPos(5, 2, 5), ModBlocks.RITUAL_CENSER.get());
        List<RitualAnchorBlockEntity> anchors = new ArrayList<>();
        RitualSealBlockEntity seal = startRite(helper, confessor, warden, anchors);
        helper.assertTrue(seal.censerNear(), "The censer stands by the seal");
        helper.assertTrue(seal.duration() == Math.round(RitualSealBlockEntity.ASH_TICKS * 0.8F), "The iron inlay makes the rite a fifth shorter");
        int open = 0;
        boolean seen = false;
        for (int i = 0; i < 200; i++) {
            seal.tick();
            if (seal.windowOpen()) {
                seen = true;
                open++;
            } else if (seen) {
                break;
            }
        }
        helper.assertTrue(open >= RitualSealBlockEntity.WINDOW_TICKS + RitualSealBlockEntity.CENSER_WINDOW_BONUS,
                "The censer widens the window, open " + open + " ticks");
        drain(helper, seal);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void boneInlaySoftensBlowsOnAnchors(GameTestHelper helper) {
        FakePlayer confessor = player(helper, "bone_confessor", 4.5, 3.0);
        FakePlayer warden = player(helper, "bone_warden", 1.5, 4.5);
        Cult.pair(confessor, warden);
        confessor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, knife(com.soulinchains.arcaneartifacts.cult.KnifeInlay.BONE));
        List<RitualAnchorBlockEntity> anchors = new ArrayList<>();
        RitualSealBlockEntity seal = startRite(helper, confessor, warden, anchors);
        helper.assertTrue(seal.anchorDamage(TornAcolyte.ANCHOR_DAMAGE * 4) == TornAcolyte.ANCHOR_DAMAGE * 3, "Bone takes a quarter off each blow");
        drain(helper, seal);
        helper.succeed();
    }

    // ------------------------------------------------------------------ 1.34

    private static void wear(Player player, String slot, net.minecraft.world.item.Item item) {
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(inv -> inv.setEquippedCurio(slot, 0, new ItemStack(item)));
    }

    @GameTest(template = ARENA)
    public static void cultGearBendsTheRite(GameTestHelper helper) {
        FakePlayer confessor = player(helper, "gear_confessor", 4.5, 3.0);
        FakePlayer warden = player(helper, "gear_warden", 4.5, 6.0);
        helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultGear.strikeBonus(confessor, false) == 5F
                && com.soulinchains.arcaneartifacts.cult.CultGear.missPenalty(confessor) == 3F, "Bare hands: +5 / -3");
        wear(confessor, "hands", ModItems.INSCRIBER_HANDS.get());
        wear(confessor, "head", ModItems.FACE_OF_DOGMA.get());
        wear(confessor, "body", ModItems.COMMUNION_MANTLE.get());
        wear(warden, "hands", ModItems.SEAL_GAUNTLETS.get());
        wear(warden, "body", ModItems.OUTER_CIRCLE_CUIRASS.get());
        wear(warden, "head", ModItems.SEAL_KEEPER_HELM.get());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultGear.strikeBonus(confessor, false) == 6F, "Hands of the Inscriber: +6");
            helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultGear.missPenalty(confessor) == 1F, "Hands of the Inscriber: a miss costs 1");
            helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultGear.windowTicks(confessor) == RitualSealBlockEntity.WINDOW_TICKS + 6, "Face of Dogma: wider window");
            helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultGear.hurtPenalty(confessor) == 1F, "Mantle of Communion: a blow costs half");
            helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultGear.repairAmount(warden) == 37, "Gauntlets: shield mends 1.5x");
            helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultGear.hammerRepair(warden) == 55, "Gauntlets: hammer +15");
            helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultGear.interceptShare(warden, 0.3F) == 0.4F
                    && com.soulinchains.arcaneartifacts.cult.CultGear.interceptShare(warden, 0.15F) == 0.15F, "Cuirass: 40% of creatures' blows, PvP untouched");
            helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultGear.anchorDamage(20, warden, warden.position().add(2, 0, 0)) == 12,
                    "Helm: anchors near the Warden take 40% less");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void keeperOfTheCircleTakesTheBlow(GameTestHelper helper) {
        FakePlayer confessor = player(helper, "keeper_confessor", 4.5, 3.0);
        FakePlayer warden = player(helper, "keeper_warden", 4.5, 6.0);
        Cult.pair(confessor, warden);
        wear(warden, "head", ModItems.SEAL_KEEPER_HELM.get());
        wear(warden, "body", ModItems.OUTER_CIRCLE_CUIRASS.get());
        wear(warden, "belt", ModItems.ANCHOR_BELT.get());
        wear(warden, "hands", ModItems.SEAL_GAUNTLETS.get());
        wear(warden, "feet", ModItems.SENTRY_BOOTS.get());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultGear.wardenSet(warden), "The Keeper of the Circle set is complete");
            helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultGearEvents.useAnchorCharge(warden) == com.soulinchains.arcaneartifacts.cult.CultGear.BELT_CHARGE_REPAIR
                    && com.soulinchains.arcaneartifacts.cult.CultGearEvents.charges(warden) == 3, "The belt spends a charge for +35");
            confessor.setHealth(1F);
            helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultGearEvents.takeTheBlow(confessor, warden), "The Warden takes the killing blow");
            helper.assertTrue(confessor.getHealth() == 2F && warden.getHealth() == 10F, "The Confessor keeps a heart; the Warden pays 5 hearts");
            helper.assertTrue(!com.soulinchains.arcaneartifacts.cult.CultGearEvents.takeTheBlow(confessor, warden), "Once a minute");
            helper.succeed();
        });
    }
}
