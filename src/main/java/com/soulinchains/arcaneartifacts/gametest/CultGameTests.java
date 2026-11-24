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
}
