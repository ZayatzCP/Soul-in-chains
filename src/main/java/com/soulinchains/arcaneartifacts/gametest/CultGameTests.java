package com.soulinchains.arcaneartifacts.gametest;

import com.mojang.authlib.GameProfile;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.cult.AshWitness;
import com.soulinchains.arcaneartifacts.cult.Cult;
import com.soulinchains.arcaneartifacts.cult.CultRole;
import com.soulinchains.arcaneartifacts.cult.RitualAnchorBlockEntity;
import com.soulinchains.arcaneartifacts.cult.RitualSealBlockEntity;
import com.soulinchains.arcaneartifacts.cult.RiteScheme;
import com.soulinchains.arcaneartifacts.cult.RenegadeSealer;
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
        helper.getLevel().getEntitiesOfClass(com.soulinchains.arcaneartifacts.cult.RitualServant.class, box).forEach(e -> e.discard());
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
            helper.assertTrue(com.soulinchains.arcaneartifacts.cult.CultGear.repairAmount(warden) == 38, "Gauntlets: shield mends 1.5x (37.5 rounds to 38)");
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

    // ------------------------------------------------------------------ 1.35

    /** Lays what {@code scheme} asks, lights its anchors with the Warden and starts it; then lets it run, the Confessor
     *  striking only the closing window, the Warden confirming first when {@code synced}. */
    private static RitualSealBlockEntity runScheme(GameTestHelper helper, Player confessor, Player warden, RiteScheme scheme, boolean synced) {
        List<RitualAnchorBlockEntity> anchors = new ArrayList<>();
        RitualSealBlockEntity seal = circle(helper, anchors);
        seal.setScheme(scheme);
        for (int i = 0; i < scheme.ash; i++) seal.offer(new ItemStack(ModItems.RITUAL_ASH.get()));
        for (int i = 0; i < scheme.shards; i++) seal.offer(new ItemStack(ModItems.BONE_SHARD.get()));
        for (int i = 0; i < scheme.candles; i++) seal.offer(new ItemStack(Items.RED_CANDLE));
        for (int i = 0; i < scheme.dust; i++) seal.offer(new ItemStack(ModItems.VOW_DUST.get()));
        helper.assertTrue(seal.reagentsReady(), scheme + ": reagents laid");
        for (int i = 0; i < scheme.anchors; i++) {
            seal.onShieldAtAnchor(warden, anchors.get(i));
        }
        seal.onKnife(confessor);
        helper.assertTrue(seal.phase() == RitualSealBlockEntity.Phase.CHANNEL, scheme + " must start");
        for (int i = 0; i < scheme.ticks + 5 && seal.phase() == RitualSealBlockEntity.Phase.CHANNEL; i++) {
            seal.tick();
            if (seal.phase() == RitualSealBlockEntity.Phase.CHANNEL && seal.finalWindow()) {
                if (synced) {
                    seal.onShieldAtAnchor(warden, anchors.get(0));
                }
                seal.onKnife(confessor);
            }
        }
        return seal;
    }

    private static <T extends net.minecraft.world.entity.Entity> List<T> around(GameTestHelper helper, Class<T> kind) {
        return helper.getLevel().getEntitiesOfClass(kind, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(24));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void schemesNeedTheirRank(GameTestHelper helper) {
        FakePlayer confessor = player(helper, "rank_confessor", 4.5, 3.0);
        FakePlayer warden = player(helper, "rank_warden", 1.5, 4.5);
        Cult.pair(confessor, warden);
        List<RitualAnchorBlockEntity> anchors = new ArrayList<>();
        RitualSealBlockEntity seal = circle(helper, anchors);
        helper.assertTrue(seal.cycleScheme(1) == RiteScheme.ASH_WITNESS, "An Initiate's Book knows only the Ash Witness");
        helper.assertTrue(seal.cycleScheme(2) == RiteScheme.THREE_VOICES, "A Servant of the Circle reaches the Three Voices");
        seal.setScheme(RiteScheme.THREE_VOICES);
        RiteScheme s = RiteScheme.THREE_VOICES;
        for (int i = 0; i < s.ash; i++) seal.offer(new ItemStack(ModItems.RITUAL_ASH.get()));
        for (int i = 0; i < s.shards; i++) seal.offer(new ItemStack(ModItems.BONE_SHARD.get()));
        for (int i = 0; i < s.candles; i++) seal.offer(new ItemStack(Items.RED_CANDLE));
        seal.offer(new ItemStack(ModItems.VOW_DUST.get()));
        for (int i = 0; i < s.anchors; i++) seal.onShieldAtAnchor(warden, anchors.get(i));
        seal.onKnife(confessor);
        helper.assertTrue(seal.phase() == RitualSealBlockEntity.Phase.IDLE, "Rank I cannot start a rank II rite");
        Cult.set(confessor, Cult.get(confessor).withRank(2));
        seal.onKnife(confessor);
        helper.assertTrue(seal.phase() == RitualSealBlockEntity.Phase.CHANNEL && seal.rite() == RiteScheme.THREE_VOICES, "Rank II starts it");
        drain(helper, seal);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void riteSchemesRaiseTheirServants(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer confessor = player(helper, "raise_confessor", 4.5, 3.0);
        FakePlayer warden = player(helper, "raise_warden", 1.5, 4.5);
        BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
        int voices = RitualSummons.summon(level, at, RiteScheme.THREE_VOICES, com.soulinchains.arcaneartifacts.cult.RitualQuality.PERFECT,
                confessor, warden, com.soulinchains.arcaneartifacts.cult.KnifeInlay.NONE, RiteScheme.ASH_WITNESS);
        List<com.soulinchains.arcaneartifacts.cult.RedAcolyte> red = around(helper, com.soulinchains.arcaneartifacts.cult.RedAcolyte.class);
        helper.assertTrue(voices == 4 && red.size() == 4, "A perfect Three Voices raises four Red Acolytes, got " + voices);
        helper.assertTrue(red.stream().allMatch(r -> r.grade() == com.soulinchains.arcaneartifacts.cult.RitualServant.Grade.EMPOWERED
                && RitualSummons.summoned(r)), "Empowered and marked as summoned");
        red.forEach(e -> e.discard());
        RitualSummons.summon(level, at, RiteScheme.RED_VEIL, com.soulinchains.arcaneartifacts.cult.RitualQuality.PERFECT,
                confessor, warden, com.soulinchains.arcaneartifacts.cult.KnifeInlay.NONE, RiteScheme.ASH_WITNESS);
        RitualSummons.summon(level, at, RiteScheme.RED_VEIL, com.soulinchains.arcaneartifacts.cult.RitualQuality.WEAK,
                confessor, warden, com.soulinchains.arcaneartifacts.cult.KnifeInlay.NONE, RiteScheme.ASH_WITNESS);
        List<Integer> lives = around(helper, com.soulinchains.arcaneartifacts.cult.BoneGatekeeper.class).stream()
                .map(com.soulinchains.arcaneartifacts.cult.RitualServant::lifeLeft).sorted().toList();
        helper.assertTrue(lives.equals(List.of(1200, 2100)), "The Gatekeeper lasts 60 s when weak, 105 s when perfect, got " + lives);
        around(helper, com.soulinchains.arcaneartifacts.cult.RitualServant.class).forEach(e -> e.discard());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void psalmistSweetensTheLoot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AshWitness witness = helper.spawnWithNoFreeWill(ModEntities.ASH_WITNESS.get(), new BlockPos(4, 2, 4));
        helper.spawnWithNoFreeWill(ModEntities.PSALMIST.get(), new BlockPos(6, 2, 4));
        RitualSummons.mark(witness, 1.5F);
        List<ItemEntity> drops = new ArrayList<>();
        drops.add(new ItemEntity(level, witness.getX(), witness.getY(), witness.getZ(), new ItemStack(ModItems.RITUAL_ASH.get(), 4)));
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(witness, level.damageSources().generic(), drops, false));
        int ash = drops.stream().filter(d -> d.getItem().is(ModItems.RITUAL_ASH.get())).mapToInt(d -> d.getItem().getCount()).sum();
        helper.assertTrue(ash == 7, "×1.5 and the Psalmist's +0.25: 4 ash become 7, got " + ash);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void secondComingWantsOneBreath(GameTestHelper helper) {
        FakePlayer confessor = player(helper, "second_confessor", 4.5, 3.0);
        FakePlayer warden = player(helper, "second_warden", 1.5, 4.5);
        Cult.pair(confessor, warden);
        Cult.set(confessor, Cult.get(confessor).withRank(3).withLastRite(RiteScheme.ASH_WITNESS.name()));
        float synced = runScheme(helper, confessor, warden, RiteScheme.SECOND_COMING, true).quality();
        drain(helper, (RitualSealBlockEntity) helper.getBlockEntity(new BlockPos(4, 2, 4)));
        float apart = runScheme(helper, confessor, warden, RiteScheme.SECOND_COMING, false).quality();
        drain(helper, (RitualSealBlockEntity) helper.getBlockEntity(new BlockPos(4, 2, 4)));
        helper.assertTrue(synced - apart >= 15F, "Closing without the Warden's seal costs 15%: " + synced + " vs " + apart);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void perfectRiteMakesVoicesOfDogma(GameTestHelper helper) {
        com.soulinchains.arcaneartifacts.cult.CultData data = com.soulinchains.arcaneartifacts.cult.CultData.EMPTY.withRank(2).withSteady(3).withArchdeacon();
        com.soulinchains.arcaneartifacts.cult.CultData flawless = RitualSealBlockEntity.advance(data, RiteScheme.THREE_VOICES, 90,
                com.soulinchains.arcaneartifacts.cult.RitualQuality.FLAWLESS, false);
        helper.assertTrue(flawless.rank() == 2 && flawless.best() == 90 && flawless.lastRite().equals("THREE_VOICES"), "Flawless is not enough");
        com.soulinchains.arcaneartifacts.cult.CultData perfect = RitualSealBlockEntity.advance(flawless, RiteScheme.SECOND_COMING, 97,
                com.soulinchains.arcaneartifacts.cult.RitualQuality.PERFECT, false);
        helper.assertTrue(perfect.rank() == 3 && perfect.best() == 97, "A perfect rite at rank II gives rank III");
        helper.assertTrue(perfect.lastRite().equals("THREE_VOICES"), "The Second Coming is never the rite it repeats");
        com.soulinchains.arcaneartifacts.cult.CultData initiate = RitualSealBlockEntity.advance(com.soulinchains.arcaneartifacts.cult.CultData.EMPTY,
                RiteScheme.ASH_WITNESS, 98, com.soulinchains.arcaneartifacts.cult.RitualQuality.PERFECT, false);
        helper.assertTrue(initiate.rank() < 3, "Rank III needs rank II first");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void gatekeeperDrawsTheBlows(GameTestHelper helper) {
        FakePlayer confessor = player(helper, "gate_confessor", 4.5, 3.0);
        com.soulinchains.arcaneartifacts.cult.BoneGatekeeper gate = helper.spawnWithNoFreeWill(ModEntities.BONE_GATEKEEPER.get(), new BlockPos(4, 2, 5));
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(6, 2, 3));
        zombie.setTarget(confessor);
        helper.assertTrue(gate.guard(confessor) == 1 && zombie.getTarget() == gate, "The Gatekeeper pulls the zombie off the Confessor");
        gate.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------ 1.36

    @GameTest(template = ARENA)
    public static void inscribeAndRetribution(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer confessor = player(helper, "glyph_confessor", 4.5, 3.0);
        FakePlayer warden = player(helper, "glyph_warden", 1.5, 4.5);
        Cult.pair(confessor, warden);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(7, 2, 7));
        com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.inscribe(zombie, confessor);
        helper.assertTrue(com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.inscribedBy(zombie, confessor.getUUID())
                && !com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.inscribedBy(zombie, warden.getUUID()), "Inscribed for the caster");
        com.soulinchains.arcaneartifacts.cult.RedAcolyte red = helper.spawnWithNoFreeWill(ModEntities.RED_ACOLYTE.get(), new BlockPos(2, 2, 2));
        red.setup(com.soulinchains.arcaneartifacts.cult.RitualServant.Grade.COMMON, confessor, warden);
        RitualSummons.mark(red, 1.0F);
        Zombie stranger = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 7));
        int obeyed = com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.command(level, warden, zombie, 1, 1);
        helper.assertTrue(obeyed == 1 && red.getTarget() == zombie, "The pair's acolyte turns on the target, got " + obeyed);
        helper.assertTrue(red.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST)
                && red.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST).getAmplifier() == 1, "Strength II against the Inscribed with one Amplify");
        helper.assertTrue(Math.abs(RitualSummons.loot(red) - 1.1F) < 0.001F, "Abundance: +0.1 loot");
        com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.command(level, confessor, stranger, 0, 1);
        helper.assertTrue(red.getTarget() == stranger && Math.abs(RitualSummons.loot(red) - 1.1F) < 0.001F, "Abundance from Retribution is paid once");
        helper.assertTrue(com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.command(level, confessor, red, 0, 0) == 0, "Never against your own summons");
        red.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void wardingSealCoversOnlyThePair(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer confessor = player(helper, "seal_confessor", 4.5, 3.0);
        FakePlayer warden = player(helper, "seal_warden", 1.5, 4.5);
        Cult.pair(confessor, warden);
        com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.clearZones();
        com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.clearCooldowns(confessor);
        com.soulinchains.arcaneartifacts.cult.RedAcolyte red = helper.spawnWithNoFreeWill(ModEntities.RED_ACOLYTE.get(), new BlockPos(4, 2, 5));
        red.setup(com.soulinchains.arcaneartifacts.cult.RitualServant.Grade.COMMON, null, warden);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(5, 2, 4));
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        helper.assertTrue(com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.lay(level, at, confessor, 0, 0), "The seal is laid");
        helper.assertTrue(!com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.lay(level, at, confessor, 0, 0), "Once in 8 s");
        helper.assertTrue(red.hasEffect(com.soulinchains.arcaneartifacts.registry.ModEffects.WARDED), "The partner's summon inside is Warded");
        helper.assertTrue(!zombie.hasEffect(com.soulinchains.arcaneartifacts.registry.ModEffects.WARDED), "A stranger is not");
        com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.clearZones();
        red.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void witnessCallAnswersThePair(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer confessor = player(helper, "call_confessor", 4.5, 3.0);
        FakePlayer warden = player(helper, "call_warden", 1.5, 4.5);
        Cult.pair(confessor, warden);
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 2, 6.5));
        var one = com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.call(level, at, confessor, warden, 1, 0, 2, 0);
        helper.assertTrue(one.size() == 1 && one.get(0).grade() == com.soulinchains.arcaneartifacts.cult.RitualServant.Grade.COMMON
                && one.get(0).lifeLeft() == com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.CALL_LIFE, "Amplify: one common witness for 30 s");
        helper.assertTrue(Math.abs(RitualSummons.loot(one.get(0)) - 1.5F) < 0.001F, "Two Abundance: x1.5 loot");
        var census = com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.call(level, at, confessor, warden, 1, 2, 0, 1);
        helper.assertTrue(census.size() == 3 && census.stream().allMatch(w -> w.grade() == com.soulinchains.arcaneartifacts.cult.RitualServant.Grade.FAINT),
                "Census: three faint witnesses instead of one strong");
        long alive = around(helper, AshWitness.class).stream().filter(w -> w.isAlive() && !w.isRemoved()).count();
        helper.assertTrue(alive == com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.CALL_CAP, "No more than three called witnesses, got " + alive);
        around(helper, AshWitness.class).forEach(e -> e.discard());
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void secondVoiceAndHoldTheCircle(GameTestHelper helper) {
        FakePlayer confessor = player(helper, "voice_confessor", 4.5, 3.0);
        FakePlayer warden = player(helper, "voice_warden", 1.5, 4.5);
        Cult.pair(confessor, warden);
        List<RitualAnchorBlockEntity> anchors = new ArrayList<>();
        RitualSealBlockEntity seal = startRite(helper, confessor, warden, anchors);
        helper.assertTrue(seal.phase() == RitualSealBlockEntity.Phase.CHANNEL, "The rite runs");
        float before = seal.quality();
        helper.assertTrue(!com.soulinchains.arcaneartifacts.cult.glyph.EffectSecondVoice.voice(confessor, 0), "One voice is not a chord");
        helper.assertTrue(com.soulinchains.arcaneartifacts.cult.glyph.EffectSecondVoice.voice(warden, 0), "The Warden answers in time");
        helper.assertTrue(Math.abs(seal.quality() - before - RitualSealBlockEntity.VOICE_QUALITY) < 0.001F, "+8% quality, got " + (seal.quality() - before));
        helper.assertTrue(!seal.secondVoice(confessor), "Once per rite");
        anchors.get(0).damage(40);
        com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.clearCooldowns(confessor);
        helper.assertTrue(com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.hold(confessor, 2) > 0, "Hold the Circle mends the anchors from inside");
        helper.assertTrue(com.soulinchains.arcaneartifacts.cult.glyph.CultGlyphs.hold(confessor, 2) == 0, "Once in 2 s");
        drain(helper, seal);
        helper.succeed();
    }

    // ------------------------------------------------------------------ 1.37

    private static com.soulinchains.arcaneartifacts.cult.archive.ArchiveLockBlockEntity lock(GameTestHelper helper,
            com.soulinchains.arcaneartifacts.cult.archive.ArchiveLockBlockEntity.Mode mode, BlockPos first, BlockPos second) {
        helper.setBlock(new BlockPos(4, 5, 8), ModBlocks.ARCHIVE_LOCK.get());
        helper.setBlock(new BlockPos(4, 2, 8), net.minecraft.world.level.block.Blocks.IRON_BARS);
        helper.setBlock(new BlockPos(4, 3, 8), net.minecraft.world.level.block.Blocks.IRON_BARS);
        var lock = (com.soulinchains.arcaneartifacts.cult.archive.ArchiveLockBlockEntity) helper.getBlockEntity(new BlockPos(4, 5, 8));
        lock.configure(mode, helper.absolutePos(first), helper.absolutePos(second), helper.absolutePos(new BlockPos(4, 2, 8)), helper.absolutePos(new BlockPos(4, 3, 8)));
        return lock;
    }

    @GameTest(template = ARENA)
    public static void vowPlatesWantTwoAtOnce(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 2, 2);
        BlockPos b = new BlockPos(7, 2, 2);
        var lock = lock(helper, com.soulinchains.arcaneartifacts.cult.archive.ArchiveLockBlockEntity.Mode.PLATES, a, b);
        helper.assertTrue(!lock.press(helper.absolutePos(a), 100) && !lock.press(helper.absolutePos(b), 130), "1.5 s apart is not together");
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 8)).is(net.minecraft.world.level.block.Blocks.IRON_BARS), "The bars stay");
        helper.assertTrue(lock.press(helper.absolutePos(a), 140) && lock.opened(), "Within a second: the door opens");
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 8)).isAir() && helper.getBlockState(new BlockPos(4, 3, 8)).isAir(), "The bars are gone");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void holdingSealWantsAPartner(GameTestHelper helper) {
        BlockPos seal = new BlockPos(1, 2, 1);
        BlockPos candle = new BlockPos(7, 2, 7);
        var lock = lock(helper, com.soulinchains.arcaneartifacts.cult.archive.ArchiveLockBlockEntity.Mode.SEAL, seal, candle);
        FakePlayer holder = player(helper, "archive_holder", 1.5, 2.5);
        FakePlayer lighter = player(helper, "archive_lighter", 7.5, 6.5);
        helper.assertTrue(!lock.lightCandle(lighter), "No one holds the seal yet");
        helper.assertTrue(lock.beginHold(holder) && lock.holding(), "The seal is held");
        helper.assertTrue(!lock.lightCandle(holder), "The holder cannot light the candle too");
        RenegadeSealer renegade = helper.spawnWithNoFreeWill(ModEntities.RENEGADE_SEALER.get(), new BlockPos(2, 2, 1));
        helper.assertTrue(!lock.lightCandle(lighter), "A renegade at the seal breaks the hold");
        renegade.discard();
        helper.assertTrue(lock.lightCandle(lighter) && helper.getBlockState(new BlockPos(4, 2, 8)).isAir(), "Held by one, lit by the other: open");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void archiveFoesDoTheirWork(GameTestHelper helper) {
        FakePlayer warden = player(helper, "foe_warden", 4.5, 2.0);
        helper.setBlock(new BlockPos(1, 2, 7), ModBlocks.RITUAL_ANCHOR.get());
        RitualAnchorBlockEntity anchor = (RitualAnchorBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 7));
        anchor.activate(warden, 60);
        RenegadeSealer renegade = helper.spawnWithNoFreeWill(ModEntities.RENEGADE_SEALER.get(), new BlockPos(6, 2, 6));
        helper.assertTrue(renegade.nearestAnchor() == anchor, "The renegade finds any lit anchor nearby");
        com.soulinchains.arcaneartifacts.cult.FacelessSentry sentry = helper.spawnWithNoFreeWill(ModEntities.FACELESS_SENTRY.get(), new BlockPos(4, 2, 3));
        helper.assertTrue(sentry.shove(warden), "The sentry shuts its passage");
        sentry.provoke();
        helper.assertTrue(!sentry.shove(warden), "Provoked by the Warden, it shoves no one");
        renegade.discard();
        sentry.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void archdeaconTestsThePair(GameTestHelper helper) {
        FakePlayer confessor = player(helper, "arch_confessor", 2.5, 2.5);
        FakePlayer warden = player(helper, "arch_warden", 2.5, 5.5);
        FakePlayer away = player(helper, "arch_away", 30, 30);
        helper.assertTrue(com.soulinchains.arcaneartifacts.cult.Archdeacon.coverFactor(confessor, warden) == com.soulinchains.arcaneartifacts.cult.Archdeacon.COVER_FACTOR,
                "The Warden near: a fifth of the blow");
        helper.assertTrue(com.soulinchains.arcaneartifacts.cult.Archdeacon.coverFactor(confessor, away) == 1F, "Alone: the full blow");
        com.soulinchains.arcaneartifacts.cult.Archdeacon arch = helper.spawnWithNoFreeWill(ModEntities.ARCHDEACON.get(), new BlockPos(4, 2, 4));
        arch.setHome(helper.absolutePos(new BlockPos(4, 2, 4)));
        arch.beginUnison();
        Vec3[] points = arch.unisonPoints();
        helper.assertTrue(points != null, "Two points flare");
        confessor.setPos(points[0].x, points[0].y, points[0].z);
        helper.assertTrue(!arch.checkUnison(List.of(confessor)), "One player is not a unison");
        warden.setPos(points[1].x, points[1].y, points[1].z);
        float before = arch.getHealth();
        helper.assertTrue(arch.checkUnison(List.of(confessor, warden)) && arch.unisonPoints() == null, "Both on their points: passed");
        helper.assertTrue(arch.getHealth() <= before - 19F, "Passing costs him 20, got " + (before - arch.getHealth()));
        arch.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void voicesOfDogmaNeedTheArchdeacon(GameTestHelper helper) {
        com.soulinchains.arcaneartifacts.cult.CultData servant = com.soulinchains.arcaneartifacts.cult.CultData.EMPTY.withRank(2).withSteady(3);
        helper.assertTrue(RitualSealBlockEntity.advance(servant, RiteScheme.THREE_VOICES, 98, com.soulinchains.arcaneartifacts.cult.RitualQuality.PERFECT, false).rank() == 2,
                "A perfect rite alone is not enough now");
        FakePlayer player = player(helper, "dogma_player", 4.5, 4.5);
        Cult.set(player, servant.withBest(96));
        helper.assertTrue(Cult.reward(player) && Cult.get(player).rank() == 3 && Cult.get(player).archdeacon(), "The Archdeacon's fall after a perfect rite: rank III");
        FakePlayer early = player(helper, "dogma_early", 4.5, 4.5);
        Cult.set(early, com.soulinchains.arcaneartifacts.cult.CultData.EMPTY.withRank(3));
        helper.assertTrue(Cult.migrate(early) && Cult.get(early).rank() == 2, "Rank III without the Archdeacon goes back to II");
        helper.assertTrue(!Cult.migrate(player), "A rightful Voice keeps the rank");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cultStructuresBuild(GameTestHelper helper) {
        BlockPos house = helper.absolutePos(new BlockPos(4, 1, 4));
        com.soulinchains.arcaneartifacts.world.cult.ConfessionHousePiece.build(helper.getLevel(),
                new net.minecraft.world.level.levelgen.structure.BoundingBox(house.getX() - 1, house.getY() - 1, house.getZ() - 1,
                        house.getX() + 1, house.getY() + 2, house.getZ() + 1), helper.getLevel().random, house);
        helper.assertTrue(helper.getLevel().getBlockState(house.above()).is(ModBlocks.RITUAL_SEAL.get()), "The house keeps a training seal");
        helper.getLevel().setBlock(house.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        BlockPos archive = helper.absolutePos(new BlockPos(4, 1, -8));
        com.soulinchains.arcaneartifacts.world.cult.ArchivePiece.build(helper.getLevel(),
                new net.minecraft.world.level.levelgen.structure.BoundingBox(archive.getX() - 1, archive.getY(), archive.getZ() + 11,
                        archive.getX() + 1, archive.getY() + 5, archive.getZ() + 12), helper.getLevel().random, archive, archive.getY() + 20, false);
        var lock = helper.getLevel().getBlockEntity(archive.offset(com.soulinchains.arcaneartifacts.world.cult.ArchivePiece.LOCK_ONE));
        helper.assertTrue(lock instanceof com.soulinchains.arcaneartifacts.cult.archive.ArchiveLockBlockEntity l
                && l.owns(archive.offset(com.soulinchains.arcaneartifacts.world.cult.ArchivePiece.PLATE_A)), "The first door's lock knows its plates");
        helper.assertTrue(helper.getLevel().getBlockState(archive.offset(0, 2, 12)).is(net.minecraft.world.level.block.Blocks.IRON_BARS), "The first door is barred");
        helper.succeed();
    }
}
