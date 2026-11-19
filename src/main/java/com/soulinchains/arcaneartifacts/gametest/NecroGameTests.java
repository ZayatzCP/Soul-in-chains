package com.soulinchains.arcaneartifacts.gametest;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.necro.ChainBolt;
import com.soulinchains.arcaneartifacts.necro.EffectShackle;
import com.soulinchains.arcaneartifacts.necro.MethodChain;
import com.soulinchains.arcaneartifacts.necro.ShackleAnchor;
import com.soulinchains.arcaneartifacts.necro.SoulType;
import com.soulinchains.arcaneartifacts.necro.SoulWisp;
import com.soulinchains.arcaneartifacts.necro.Souls;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

/** The Shackled class (1.25): souls, the Soul Lantern, chains and the Chain / Shackle / Reap glyphs. */
@GameTestHolder(ArcaneArtifacts.MODID)
@PrefixGameTestTemplate(false)
public final class NecroGameTests {
    private static final String ARENA = "arena";

    private NecroGameTests() {
    }

    private static Player playerWithLantern(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        player.setPos(pos.x, pos.y, pos.z);
        CuriosApi.getCuriosInventory(player).ifPresent(inv -> inv.setEquippedCurio("belt", 0, new ItemStack(ModItems.SOUL_LANTERN.get())));
        return player;
    }

    private static List<SoulWisp> wisps(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(SoulWisp.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16));
    }

    @GameTest(template = ARENA)
    public static void soulTypesComeFromTags(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        var skeleton = helper.spawnWithNoFreeWill(EntityType.WITHER_SKELETON, new BlockPos(4, 2, 2));
        var villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(6, 2, 2));
        var enderman = helper.spawnWithNoFreeWill(EntityType.ENDERMAN, new BlockPos(2, 2, 6));
        var cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(6, 2, 6));
        helper.assertTrue(SoulType.of(zombie) == SoulType.WANDERING, "A zombie must leave a wandering soul, got " + SoulType.of(zombie));
        helper.assertTrue(SoulType.of(skeleton) == SoulType.CURSED, "A wither skeleton must leave a cursed soul");
        helper.assertTrue(SoulType.of(villager) == SoulType.PURE, "A villager must leave a pure soul");
        helper.assertTrue(SoulType.of(enderman) == SoulType.VOID, "An enderman must leave a void soul");
        helper.assertTrue(SoulType.of(cow) == null, "A cow has no soul worth catching");
        helper.assertTrue(SoulType.WANDERING.upgraded() == SoulType.CURSED && SoulType.VOID.upgraded() == SoulType.VOID,
                "Only weak souls go one step up");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "necro_lantern")
    public static void soulLanternHoldsTenSouls(GameTestHelper helper) {
        Player player = playerWithLantern(helper);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(CurioHelper.findEquipped(player, ModItems.SOUL_LANTERN.get()).isPresent(), "The lantern must be worn on the belt");
            for (int i = 0; i < 10; i++) {
                helper.assertTrue(Souls.store(player, SoulType.WANDERING), "Soul " + (i + 1) + " did not fit");
            }
            helper.assertTrue(!Souls.store(player, SoulType.VOID), "An 11th soul must not fit into a lantern of 10");
            helper.assertTrue(Souls.take(player, SoulType.WANDERING) && Souls.count(player, SoulType.WANDERING) == 9, "Taking a soul must leave 9");
            helper.assertTrue(!Souls.take(player, SoulType.GREAT), "There is no great soul to take");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void chainSickleKillAlwaysLeavesASoul(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.CHAIN_SICKLE.get()));
        for (int i = 0; i < 3; i++) {
            Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2 + i * 2, 2, 4));
            zombie.hurt(helper.getLevel().damageSources().playerAttack(player), 1000F);
        }
        helper.assertTrue(wisps(helper).size() == 3, "Every zombie killed with the sickle must leave a soul wisp, got " + wisps(helper).size());
        Zombie unloved = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 2, 7));
        unloved.hurt(helper.getLevel().damageSources().genericKill(), 1000F);
        helper.assertTrue(wisps(helper).size() == 3, "A mob not killed by a player must leave no soul");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void reapedSoulComesOutUpgraded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        zombie.addEffect(new MobEffectInstance(ModEffects.REAP_MARK, 100));
        // The reaper is not in the world: the soul stays as a wisp, one step up because of Amplify.
        Souls.markForReaping(zombie, java.util.UUID.randomUUID(), true);
        zombie.hurt(level.damageSources().genericKill(), 1000F);
        List<SoulWisp> wisps = wisps(helper);
        helper.assertTrue(wisps.size() == 1 && wisps.get(0).soul() == SoulType.CURSED,
                "A reaped zombie must always leave a soul, cursed with Amplify; got " + wisps.stream().map(SoulWisp::soul).toList());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void shackleHoldsACreature(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(4, 2, 4));
        helper.assertTrue(EffectShackle.shackle(level, pig, 60, null), "The pig must be shackled");
        helper.assertTrue(pig.hasEffect(ModEffects.SHACKLED), "The pig must be Shackled");
        helper.assertTrue(!level.getEntitiesOfClass(ShackleAnchor.class, pig.getBoundingBox().inflate(2)).isEmpty(), "Chains must hold the pig");
        Vec3 start = pig.position();
        pig.setDeltaMovement(1.5, 0, 0);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(pig.position().subtract(start).horizontalDistance() < 1.5, "A shackled pig must stay on the spot, moved " + pig.position().subtract(start).horizontalDistance());
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void chainFormHooksAndCarriesTheSpell(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Husk caster = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(1, 2, 4));
        caster.setYRot(-90F);
        caster.setYHeadRot(-90F);
        caster.setXRot(0F);
        // A tall target: the chain flies at the caster's eye height.
        var pig = helper.spawn(EntityType.VILLAGER, new BlockPos(8, 2, 4));
        double before = pig.distanceTo(caster);
        Spell spell = new Spell(MethodChain.INSTANCE, com.soulinchains.arcaneartifacts.necro.EffectReap.INSTANCE);
        SpellResolver resolver = new SpellResolver(new SpellContext(level, spell, caster, LivingCaster.from(caster)));
        level.addFreshEntity(new ChainBolt(level, caster, resolver, false, 1.8F));
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.hasEffect(ModEffects.REAP_MARK), "The chain must carry Reap to the villager it hooks");
            helper.assertTrue(pig.distanceTo(caster) < before - 1.0, "The chain must reel the villager in: " + before + " -> " + pig.distanceTo(caster));
        });
    }

    @GameTest(template = ARENA)
    public static void pvpHoldsPlayersShorterWithImmunity(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 2, 4));
        helper.assertTrue(com.soulinchains.arcaneartifacts.necro.Pvp.holdTicks(pig, 60) == 60, "Mobs are held for the full time");
        int first = com.soulinchains.arcaneartifacts.necro.Pvp.holdTicks(player, 60);
        helper.assertTrue(first == 30, "A player must be held half as long by default, got " + first);
        helper.assertTrue(com.soulinchains.arcaneartifacts.necro.Pvp.holdTicks(player, 60) == 0, "A player just held must be immune for a while");
        helper.assertTrue(com.soulinchains.arcaneartifacts.necro.Pvp.pull(player) < 1.0 && com.soulinchains.arcaneartifacts.necro.Pvp.pull(pig) == 1.0,
                "Chains must pull players weaker than mobs");
        helper.assertTrue(com.soulinchains.arcaneartifacts.necro.Pvp.vortex(player) < 1.0, "Vortices must throw players around less");
        helper.succeed();
    }

    // ------------------------------------------------------------------ 1.26: ranks, servants, Devour, Bond

    private static void fill(Player player, SoulType type, int n) {
        for (int i = 0; i < n; i++) {
            Souls.store(player, type);
        }
    }

    @GameTest(template = ARENA, batch = "necro_awakening")
    public static void awakeningMakesAnOverseer(GameTestHelper helper) {
        Player player = playerWithLantern(helper);
        helper.runAfterDelay(2, () -> {
            com.soulinchains.arcaneartifacts.necro.Necro.setRank(player, com.soulinchains.arcaneartifacts.necro.Necro.NOVICE);
            fill(player, SoulType.WANDERING, 9);
            helper.assertTrue(!com.soulinchains.arcaneartifacts.necro.AwakeningRitual.awaken(player), "Nine souls are not enough");
            fill(player, SoulType.VOID, 1);
            helper.assertTrue(com.soulinchains.arcaneartifacts.necro.AwakeningRitual.awaken(player), "Ten souls must awaken a Novice");
            helper.assertTrue(com.soulinchains.arcaneartifacts.necro.Necro.rank(player) == com.soulinchains.arcaneartifacts.necro.Necro.OVERSEER, "The player must be an Overseer now");
            helper.assertTrue(Souls.total(player) == 0, "The ritual must take all ten souls, left " + Souls.total(player));
            helper.assertTrue(com.soulinchains.arcaneartifacts.necro.Necro.servantLimit(player) == 3, "An Overseer keeps 3 servants");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "necro_raise")
    public static void raiseDeadSpendsSoulsAndHoldsMana(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = playerWithLantern(helper);
        helper.runAfterDelay(2, () -> {
            com.soulinchains.arcaneartifacts.necro.Necro.setRank(player, com.soulinchains.arcaneartifacts.necro.Necro.OVERSEER);
            fill(player, SoulType.WANDERING, 3);
            fill(player, SoulType.VOID, 1);
            var weak = com.soulinchains.arcaneartifacts.necro.EffectRaiseDead.choose(player, false, true);
            helper.assertTrue(weak == com.soulinchains.arcaneartifacts.necro.ServantKind.SKELETON, "Dampen must keep the void soul and pick the skeleton, got " + weak);
            var best = com.soulinchains.arcaneartifacts.necro.EffectRaiseDead.choose(player, false, false);
            helper.assertTrue(best == com.soulinchains.arcaneartifacts.necro.ServantKind.ASSASSIN, "A void soul and 2 wandering must raise the assassin, got " + best);
            var servant = com.soulinchains.arcaneartifacts.necro.EffectRaiseDead.raise(level, player, best, helper.absoluteVec(new Vec3(5.5, 2, 5.5)));
            helper.assertTrue(servant != null && player.getUUID().equals(servant.getOwnerUUID()), "The assassin must rise bound to the player");
            helper.assertTrue(Souls.count(player, SoulType.VOID) == 0 && Souls.count(player, SoulType.WANDERING) == 1, "The assassin costs a void soul and 2 wandering");
            float reserve = com.soulinchains.arcaneartifacts.necro.Servant.reserveOf(player.getUUID());
            helper.assertTrue(Math.abs(reserve - com.soulinchains.arcaneartifacts.necro.ServantKind.ASSASSIN.reserve) < 1e-4, "The assassin must hold its share of mana, got " + reserve);
            float arsReserve = com.hollingsworth.arsnouveau.api.util.ManaUtil.calcMaxMana(player).Reserve();
            helper.assertTrue(arsReserve >= reserve - 1e-4, "Ars Nouveau must see the reserved mana, got " + arsReserve);
            servant.crumble(true);
            helper.assertTrue(servant.isRemoved(), "A dismissed servant must crumble");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "necro_devour")
    public static void devourBurnsTheWeakestSoul(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = playerWithLantern(helper);
        helper.runAfterDelay(2, () -> {
            com.soulinchains.arcaneartifacts.necro.Necro.setRank(player, com.soulinchains.arcaneartifacts.necro.Necro.OVERSEER);
            fill(player, SoulType.CURSED, 1);
            fill(player, SoulType.WANDERING, 1);
            player.setHealth(10);
            var context = new SpellContext(level, new Spell(), player, LivingCaster.from(player));
            com.soulinchains.arcaneartifacts.necro.EffectDevour.INSTANCE.onResolve(new net.minecraft.world.phys.EntityHitResult(player), level, player,
                    new com.hollingsworth.arsnouveau.api.spell.SpellStats.Builder().build(), context, null);
            helper.assertTrue(player.getHealth() >= 16 - 1e-3, "Devour must heal 3 hearts, health " + player.getHealth());
            helper.assertTrue(Souls.count(player, SoulType.WANDERING) == 0 && Souls.count(player, SoulType.CURSED) == 1, "Devour must burn the weakest soul first");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void bondSplitsDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Pig a = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 2, 4));
        Pig b = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(5, 2, 4));
        com.soulinchains.arcaneartifacts.necro.EffectBond.bond(level, a, b, 100);
        a.hurt(level.damageSources().generic(), 8F);
        helper.assertTrue(Math.abs(a.getHealth() - 6F) < 0.01F && Math.abs(b.getHealth() - 6F) < 0.01F,
                "8 damage must be split 4 / 4, got " + a.getHealth() + " / " + b.getHealth());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void createCompatIsSilentWithoutCreate(GameTestHelper helper) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 2, 4));
        helper.assertTrue(!com.soulinchains.arcaneartifacts.compat.ContraptionCompat.isContraption(pig), "A pig is not a contraption");
        helper.assertTrue(com.soulinchains.arcaneartifacts.compat.ContraptionCompat.pullable(pig) == null, "Nothing to pull without Create");
        helper.assertTrue(com.soulinchains.arcaneartifacts.compat.ShipCompat.at(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 1.5, 4.5))) == null,
                "Without Sable there are no ships");
        helper.succeed();
    }

    // ------------------------------------------------------------------ 1.27: Corruption, set, Phantom Step, soils, tools

    @GameTest(template = ARENA)
    public static void corruptionGrowsInStages(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        com.soulinchains.arcaneartifacts.necro.Corruption.set(player, 0);
        for (int i = 0; i < 9; i++) {
            com.soulinchains.arcaneartifacts.necro.Corruption.spend(player, SoulType.CURSED);
        }
        helper.assertTrue(Math.abs(com.soulinchains.arcaneartifacts.necro.Corruption.get(player) - 27F) < 1e-3, "Nine cursed souls must give 27 Corruption");
        helper.assertTrue(com.soulinchains.arcaneartifacts.necro.Corruption.stage(player) == 1, "27 Corruption is Touched");
        com.soulinchains.arcaneartifacts.necro.Corruption.add(player, 500);
        helper.assertTrue(com.soulinchains.arcaneartifacts.necro.Corruption.get(player) == com.soulinchains.arcaneartifacts.necro.Corruption.MAX,
                "Without the Lich ritual Corruption stops at 99");
        helper.assertTrue(com.soulinchains.arcaneartifacts.necro.Corruption.stage(player) == 3, "99 Corruption is Unclean");
        com.soulinchains.arcaneartifacts.necro.Corruption.set(player, 0);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void daggerKillWashesCorruption(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.RITUAL_DAGGER.get()));
        com.soulinchains.arcaneartifacts.necro.Corruption.set(player, 5);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        zombie.addEffect(new MobEffectInstance(ModEffects.SHACKLED, 100));
        zombie.hurt(helper.getLevel().damageSources().playerAttack(player), 1000F);
        helper.assertTrue(Math.abs(com.soulinchains.arcaneartifacts.necro.Corruption.get(player) - 4F) < 1e-3,
                "A ritual kill must wash 1 Corruption away, got " + com.soulinchains.arcaneartifacts.necro.Corruption.get(player));
        helper.assertTrue(wisps(helper).size() == 1, "A dagger kill always leaves a soul");
        com.soulinchains.arcaneartifacts.necro.Corruption.set(player, 0);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void phantomStepsThroughAThinWall(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 start = helper.absoluteVec(new Vec3(2.5, 2, 4.5));
        player.setPos(start.x, start.y, start.z);
        player.setYRot(-90F);
        helper.setBlock(new BlockPos(3, 2, 4), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(new BlockPos(3, 3, 4), net.minecraft.world.level.block.Blocks.STONE);
        helper.assertTrue(com.soulinchains.arcaneartifacts.necro.EffectPhantomStep.stepThrough(helper.getLevel(), player), "The phantom must step through a one-block wall");
        helper.assertTrue(player.getX() > start.x + 1.5, "The player must come out on the far side, x " + player.getX() + " from " + start.x);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 2, 6));
        pig.addEffect(new MobEffectInstance(ModEffects.PHANTOM, 40));
        pig.hurt(helper.getLevel().damageSources().generic(), 5F);
        helper.assertTrue(pig.getHealth() == pig.getMaxHealth(), "A phantom cannot be hurt");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void consecratedSoilKeepsMonstersAway(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, com.soulinchains.arcaneartifacts.registry.ModBlocks.CONSECRATED_SOIL.get());
        var state = helper.getBlockState(pos);
        BlockPos abs = helper.absolutePos(pos);
        helper.assertTrue(!state.isValidSpawn(helper.getLevel(), abs, EntityType.ZOMBIE), "No monster may spawn on consecrated soil");
        helper.assertTrue(state.isValidSpawn(helper.getLevel(), abs, EntityType.PIG), "Animals may still spawn on it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "necro_vessel")
    public static void soulVesselHoldsASoul(GameTestHelper helper) {
        Player player = playerWithLantern(helper);
        helper.runAfterDelay(2, () -> {
            Souls.store(player, SoulType.VOID);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.SOUL_VESSEL.get()));
            ModItems.SOUL_VESSEL.get().use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
            helper.assertTrue(Souls.count(player, SoulType.VOID) == 0, "The soul must leave the lantern");
            boolean found = false;
            for (ItemStack stack : player.getInventory().items) {
                if (com.soulinchains.arcaneartifacts.necro.SoulVesselItem.soul(stack) == SoulType.VOID) {
                    found = true;
                }
            }
            helper.assertTrue(found, "A vessel with the void soul must be in the inventory");
            helper.succeed();
        });
    }
}
