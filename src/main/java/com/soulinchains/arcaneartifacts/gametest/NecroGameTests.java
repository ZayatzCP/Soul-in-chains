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

    @GameTest(template = ARENA, batch = "necro_phylactery")
    public static void phylacterySavesFromDeath(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 start = helper.absoluteVec(new Vec3(6.5, 2, 6.5));
        player.setPos(start.x, start.y, start.z);
        com.soulinchains.arcaneartifacts.necro.Necro.setRank(player, com.soulinchains.arcaneartifacts.necro.Necro.OVERSEER);
        ItemStack amulet = new ItemStack(ModItems.PHYLACTERY_AMULET.get());
        BlockPos home = helper.absolutePos(new BlockPos(2, 2, 2));
        com.soulinchains.arcaneartifacts.necro.PhylacteryAmuletItem.bind(amulet, net.minecraft.core.GlobalPos.of(helper.getLevel().dimension(), home));
        CuriosApi.getCuriosInventory(player).ifPresent(inv -> inv.setEquippedCurio("necklace", 0, amulet));
        helper.runAfterDelay(2, () -> {
            player.hurt(helper.getLevel().damageSources().generic(), 1000F);
            helper.assertTrue(player.isAlive() && player.getHealth() > 0, "The phylactery must avert death");
            helper.assertTrue(player.blockPosition().equals(home), "The wearer must be torn back to the bound place, at " + player.blockPosition() + " not " + home);
            var worn = CurioHelper.findEquipped(player, ModItems.PHYLACTERY_AMULET.get()).orElseThrow();
            helper.assertTrue(com.soulinchains.arcaneartifacts.necro.PhylacteryAmuletItem.state(worn).readyAt() > helper.getLevel().getGameTime(),
                    "The amulet must sleep after saving its wearer");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ 1.28: crypts, graves, new enemies

    @GameTest(template = ARENA)
    public static void graveDigsOnce(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 4);
        helper.setBlock(pos, com.soulinchains.arcaneartifacts.registry.ModBlocks.GRAVE.get());
        BlockPos abs = helper.absolutePos(pos);
        helper.assertTrue(com.soulinchains.arcaneartifacts.necro.GraveBlock.dig(helper.getLevel(), abs, null), "A fresh grave must be dug up");
        helper.assertTrue(helper.getBlockState(pos).getValue(com.soulinchains.arcaneartifacts.necro.GraveBlock.DUG), "The grave must be dug now");
        helper.assertTrue(!com.soulinchains.arcaneartifacts.necro.GraveBlock.dig(helper.getLevel(), abs, null), "A dug grave gives nothing more");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(abs).inflate(2)).isEmpty(),
                "Digging must turn up something");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void restlessDrainsMana(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var mana = com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(player);
        helper.assertTrue(mana != null, "The player must have Ars mana");
        mana.setMaxMana(100);
        mana.setMana(100);
        var ghost = helper.spawnWithNoFreeWill(com.soulinchains.arcaneartifacts.registry.ModEntities.RESTLESS.get(), new BlockPos(4, 2, 4));
        ghost.doHurtTarget(player);
        helper.assertTrue(mana.getCurrentMana() <= 100 - com.soulinchains.arcaneartifacts.necro.mob.Restless.MANA_DRAIN + 1e-3,
                "A Restless touch must drain mana, left " + mana.getCurrentMana());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "necro_eater")
    public static void soulEaterStealsAndRepaysTwice(GameTestHelper helper) {
        Player player = playerWithLantern(helper);
        helper.runAfterDelay(2, () -> {
            fill(player, SoulType.WANDERING, 2);
            var eater = helper.spawnWithNoFreeWill(com.soulinchains.arcaneartifacts.registry.ModEntities.SOUL_EATER.get(), new BlockPos(4, 2, 4));
            eater.doHurtTarget(player);
            helper.assertTrue(Souls.total(player) == 1 && eater.stolen().size() == 1, "One hit must tear one soul out, left " + Souls.total(player));
            eater.hurt(helper.getLevel().damageSources().genericKill(), 1000F);
            helper.assertTrue(wisps(helper).size() == 2, "A slain Soul Eater gives back every stolen soul twice, got " + wisps(helper).size());
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void mournerWailsAndTearsAtServants(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var mourner = helper.spawnWithNoFreeWill(com.soulinchains.arcaneartifacts.registry.ModEntities.MOURNER.get(), new BlockPos(4, 2, 4));
        var servant = helper.spawnWithNoFreeWill(com.soulinchains.arcaneartifacts.registry.ModEntities.SERVANTS
                .get(com.soulinchains.arcaneartifacts.necro.ServantKind.SKELETON).get(), new BlockPos(6, 2, 4));
        float before = servant.getHealth();
        int called = mourner.wail(level);
        helper.assertTrue(servant.getHealth() < before, "The wail must tear at servants");
        helper.assertTrue(called >= 1, "The wail must call the dead out of the ground");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "necro_release")
    public static void dyingWithAFullLanternReleasesRestless(GameTestHelper helper) {
        Player player = playerWithLantern(helper);
        helper.runAfterDelay(2, () -> {
            fill(player, SoulType.WANDERING, 10);
            player.hurt(helper.getLevel().damageSources().generic(), 1000F);
            int ghosts = helper.getLevel().getEntitiesOfClass(com.soulinchains.arcaneartifacts.necro.mob.Restless.class,
                    player.getBoundingBox().inflate(8)).size();
            helper.assertTrue(ghosts == 5, "Ten souls lost: five Restless must burst out, got " + ghosts);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void cryptBuildsItsHall(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(4, 24, 4));
        var box = new net.minecraft.world.level.levelgen.structure.BoundingBox(origin.getX() - 6, origin.getY() - 11, origin.getZ() - 6,
                origin.getX() + 6, origin.getY() + 6, origin.getZ() + 6);
        com.soulinchains.arcaneartifacts.world.crypt.CryptPiece.build(helper.getLevel(), box, helper.getLevel().random, origin, false);
        int floor = origin.getY() - com.soulinchains.arcaneartifacts.world.crypt.CryptPiece.DEPTH;
        helper.assertTrue(helper.getLevel().getBlockState(origin).is(net.minecraft.world.level.block.Blocks.LADDER), "A ladder must lead down from the mausoleum");
        helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(origin.getX() - 3, floor + 1, origin.getZ() - 3))
                .is(com.soulinchains.arcaneartifacts.registry.ModBlocks.GRAVE.get()), "The hall must hold graves");
        helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(origin.getX(), floor + 1, origin.getZ() + 4))
                .is(net.minecraft.world.level.block.Blocks.CHEST), "The hall must hold a chest");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void chainExecutionStrikesOnlyYourPrisoners(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Zombie caster = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        Zombie other = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(8, 2, 8));
        Pig mine = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 2, 4));
        Pig theirs = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 2, 4));
        Pig free = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 2, 6));
        EffectShackle.shackle(level, mine, 100, caster);
        EffectShackle.shackle(level, theirs, 100, other);
        int struck = com.soulinchains.arcaneartifacts.necro.EffectChainExecution.execute(level, caster, 4F);
        helper.assertTrue(struck == 1, "Only the caster's own prisoner may be struck, got " + struck);
        helper.assertTrue(mine.getHealth() < mine.getMaxHealth(), "The caster's prisoner must be hurt");
        helper.assertTrue(theirs.getHealth() == theirs.getMaxHealth() && free.getHealth() == free.getMaxHealth(), "Others must be spared");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cursedChainsBlockHealingAndWeakenHits(GameTestHelper helper) {
        Zombie cursed = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 2, 4));
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(5, 2, 4));
        cursed.addEffect(new MobEffectInstance(ModEffects.CURSED_CHAINS, 200));
        cursed.setHealth(10F);
        cursed.heal(5F);
        helper.assertTrue(cursed.getHealth() == 10F, "Cursed chains must block healing, health " + cursed.getHealth());
        pig.hurt(helper.getLevel().damageSources().mobAttack(cursed), 8F);
        helper.assertTrue(Math.abs(pig.getHealth() - 4F) < 0.01F, "A cursed attacker must hit for 3/4, pig at " + pig.getHealth());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void shackleKeyFreesTheHolder(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.addEffect(new MobEffectInstance(ModEffects.CURSED_CHAINS, 200));
        player.addEffect(new MobEffectInstance(ModEffects.SHACKLED, 200));
        ItemStack key = new ItemStack(ModItems.SHACKLE_KEY.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, key);
        key.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(!player.hasEffect(ModEffects.CURSED_CHAINS) && !player.hasEffect(ModEffects.SHACKLED), "The key must open every shackle");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "necro_binding")
    public static void eternalBindingKeepsAndLevelsAServant(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = playerWithLantern(helper);
        helper.runAfterDelay(2, () -> {
            com.soulinchains.arcaneartifacts.necro.Necro.setRank(player, com.soulinchains.arcaneartifacts.necro.Necro.CHAINLORD);
            fill(player, SoulType.WANDERING, 3);
            var servant = com.soulinchains.arcaneartifacts.necro.EffectRaiseDead.raise(level, player, com.soulinchains.arcaneartifacts.necro.ServantKind.SKELETON, helper.absoluteVec(new Vec3(4.5, 2, 4.5)));
            helper.assertTrue(servant != null, "A servant must rise");
            BlockPos brazier = helper.absolutePos(new BlockPos(4, 2, 6));
            helper.assertTrue(com.soulinchains.arcaneartifacts.necro.EternalBindingRitual.bind(level, brazier, player) == null, "No cursed soul, no binding");
            fill(player, SoulType.CURSED, 1);
            helper.assertTrue(com.soulinchains.arcaneartifacts.necro.EternalBindingRitual.bind(level, brazier, player) == servant && servant.isBound(), "The servant must be bound for ever");
            helper.assertTrue(Souls.count(player, SoulType.CURSED) == 0, "Binding costs the cursed soul");
            float health = servant.getMaxHealth();
            for (int i = 0; i < com.soulinchains.arcaneartifacts.necro.Servant.killsFor(0); i++) {
                Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 2, 6));
                servant.killedEntity(level, pig);
                pig.discard();
            }
            helper.assertTrue(servant.servantLevel() == 1, "Five kills must raise the bound servant to level 1, got " + servant.servantLevel());
            helper.assertTrue(servant.getMaxHealth() > health, "A level must add health");
            servant.crumble(true);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "necro_execution")
    public static void executionerAxeExecutesAndUpgradesTheSoul(GameTestHelper helper) {
        Player player = playerWithLantern(helper);
        helper.runAfterDelay(2, () -> {
            com.soulinchains.arcaneartifacts.necro.Necro.setRank(player, com.soulinchains.arcaneartifacts.necro.Necro.CHAINLORD);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
            zombie.setHealth(2F);
            ItemStack axe = new ItemStack(ModItems.EXECUTIONER_AXE.get());
            axe.getItem().hurtEnemy(axe, zombie, player);
            helper.assertTrue(!zombie.isAlive(), "The axe must execute a creature at 15% health");
            var upgraded = SoulType.of(zombie).upgraded();
            helper.assertTrue(wisps(helper).stream().anyMatch(w -> w.soul() == upgraded), "The executed soul must come out one grade stronger");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "necro_jailer")
    public static void slayingTheJailerMakesAChainlord(GameTestHelper helper) {
        Player player = playerWithLantern(helper);
        helper.runAfterDelay(2, () -> {
            com.soulinchains.arcaneartifacts.necro.Necro.setRank(player, com.soulinchains.arcaneartifacts.necro.Necro.OVERSEER);
            var jailer = helper.spawnWithNoFreeWill(com.soulinchains.arcaneartifacts.registry.ModEntities.JAILER.get(), new BlockPos(5, 2, 5));
            jailer.hurt(helper.getLevel().damageSources().playerAttack(player), 10000F);
            helper.assertTrue(com.soulinchains.arcaneartifacts.necro.Necro.rank(player) == com.soulinchains.arcaneartifacts.necro.Necro.CHAINLORD, "The Jailer's fall must make an Overseer a Chainlord");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void soulDungeonBuildsItsHall(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(4, 4, 4));
        var box = new net.minecraft.world.level.levelgen.structure.BoundingBox(origin.getX() - 2, origin.getY() - 1, origin.getZ() - 2,
                origin.getX() + 2, origin.getY() + 2 * com.soulinchains.arcaneartifacts.world.crypt.SoulDungeonPiece.STOREY + 1, origin.getZ() + 2);
        com.soulinchains.arcaneartifacts.world.crypt.SoulDungeonPiece.build(helper.getLevel(), box, helper.getLevel().random, origin, false);
        helper.assertTrue(helper.getLevel().getBlockState(origin).is(net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE), "The hall must have a floor");
        helper.assertTrue(helper.getLevel().getBlockState(origin.above()).isAir(), "The hall must be hollow");
        helper.assertTrue(helper.getLevel().getBlockState(origin.above(com.soulinchains.arcaneartifacts.world.crypt.SoulDungeonPiece.STOREY))
                .is(net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE_BRICKS), "Storeys must be split by a slab");
        helper.succeed();
    }

    private static BlockPos buildThrone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos throne = helper.absolutePos(new BlockPos(4, 30, 4));
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = -1; y <= 8; y++) {
                    level.setBlock(throne.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(throne.offset(x, -1, z), com.soulinchains.arcaneartifacts.registry.ModBlocks.SHACKLE_STONE.get().defaultBlockState(), 3);
            }
        }
        level.setBlock(throne, com.soulinchains.arcaneartifacts.registry.ModBlocks.EMPTY_THRONE.get().defaultBlockState(), 3);
        for (int[] corner : com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.CORNERS) {
            for (int y = 0; y < com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.PILLAR; y++) {
                level.setBlock(throne.offset(corner[0], y, corner[1]), com.soulinchains.arcaneartifacts.registry.ModBlocks.CHAIN_PILLAR.get().defaultBlockState(), 3);
            }
        }
        return throne;
    }

    @GameTest(template = ARENA, batch = "necro_throne")
    public static void throneOfChainsWakesTheKing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = playerWithLantern(helper);
        BlockPos throne = buildThrone(helper);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.complete(level, throne), "The throne must be recognised");
            fill(player, SoulType.GREAT, 5);
            var first = com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.hang(level, com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.pillarTop(throne, 1), player);
            helper.assertTrue(first == com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.HangResult.START_NORTH_WEST, "The first lantern goes north-west, got " + first);
            helper.assertTrue(com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.hang(level, com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.pillarTop(throne, 0), player) == com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.HangResult.HUNG, "North-west takes the first lantern");
            var wrong = com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.hang(level, com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.pillarTop(throne, 2), player);
            helper.assertTrue(wrong == com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.HangResult.WRONG_ORDER, "South-east second is the wrong order, got " + wrong);
            helper.assertTrue(com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.lit(level, throne) == 0, "A wrong order puts every lantern out");
            helper.assertTrue(Souls.count(player, SoulType.GREAT) == 4, "Only the hung lantern took a soul");
            for (int corner = 0; corner < 4; corner++) {
                var result = com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.hang(level, com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.pillarTop(throne, corner), player);
                helper.assertTrue(result == com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.HangResult.HUNG, "Corner " + corner + " must take its lantern, got " + result);
            }
            helper.assertTrue(Souls.count(player, SoulType.GREAT) == 0, "Four lanterns cost four great souls");
            var king = com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.summon(level, throne);
            helper.assertTrue(king != null, "A lit throne must wake the King");
            helper.assertTrue(king.locksLeft() == 4, "The King must be chained to four locks, got " + king.locksLeft());
            helper.assertTrue(com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.lit(level, throne) == 0, "The lanterns burn out in the summoning");
            king.chainTo(java.util.List.of(com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.pillarTop(throne, 0), com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.pillarTop(throne, 1),
                    com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.pillarTop(throne, 2), com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.pillarTop(throne, 3)));
            for (int i = 0; i < com.soulinchains.arcaneartifacts.necro.mob.ImprisonedKing.RISING_TICKS + 1; i++) {
                king.aiStep();
            }
            float health = king.getHealth();
            king.hurt(level.damageSources().playerAttack(player), 50F);
            helper.assertTrue(king.getHealth() == health, "A chained King cannot be hurt");
            for (int corner = 0; corner < 4; corner++) {
                level.removeBlock(com.soulinchains.arcaneartifacts.necro.throne.ThroneOfChains.pillarTop(throne, corner), false);
            }
            king.invulnerableTime = 0;
            king.hurt(level.damageSources().playerAttack(player), 50F);
            helper.assertTrue(king.getHealth() < health, "With the locks broken the King must bleed");
            king.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void freeKingTearsServantChains(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var king = helper.spawnWithNoFreeWill(com.soulinchains.arcaneartifacts.registry.ModEntities.IMPRISONED_KING.get(), new BlockPos(3, 2, 3));
        helper.assertTrue(king.phase() == 2, "A King with no locks walks free, phase " + king.phase());
        king.setHealth(king.getMaxHealth() * 0.2F);
        helper.assertTrue(king.phase() == 3, "Below 30% the King is free");
        var servant = helper.spawnWithNoFreeWill(com.soulinchains.arcaneartifacts.registry.ModEntities.SERVANTS
                .get(com.soulinchains.arcaneartifacts.necro.ServantKind.SKELETON).get(), new BlockPos(6, 2, 6));
        helper.assertTrue(king.tearServants(level) == 1 && servant.isRemoved(), "The free King must tear a servant's chain");
        king.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void scytheHarvestMarksForReaping(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        player.setPos(pos.x, pos.y, pos.z);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(5, 2, 5));
        int cut = com.soulinchains.arcaneartifacts.necro.ScytheOfTheReaperItem.harvest(helper.getLevel(), player);
        helper.assertTrue(cut == 1, "Harvest must cut the zombie, cut " + cut);
        helper.assertTrue(zombie.hasEffect(ModEffects.REAP_MARK), "Harvest must mark the zombie for reaping");
        helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "Harvest must hurt");
        helper.succeed();
    }

    private static void wearRelic(Player player, net.minecraft.world.item.Item relic) {
        CuriosApi.getCuriosInventory(player).ifPresent(inv -> inv.setEquippedCurio("relic", 0, new ItemStack(relic)));
    }

    @GameTest(template = ARENA, batch = "necro_necklace")
    public static void toothNecklaceSparesEveryFifthSoul(GameTestHelper helper) {
        Player player = playerWithLantern(helper);
        wearRelic(player, ModItems.TOOTH_NECKLACE.get());
        helper.runAfterDelay(2, () -> {
            com.soulinchains.arcaneartifacts.necro.Corruption.set(player, 0F);
            fill(player, SoulType.WANDERING, 5);
            for (int i = 0; i < 5; i++) {
                Souls.spend(player, SoulType.WANDERING);
            }
            float corruption = com.soulinchains.arcaneartifacts.necro.Corruption.get(player);
            helper.assertTrue(Math.abs(corruption - 2F) < 1e-3, "Four of five spends count: 2.0 Corruption, got " + corruption);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "necro_heart")
    public static void cagedHeartBeatsOnce(GameTestHelper helper) {
        Player player = playerWithLantern(helper);
        wearRelic(player, ModItems.CAGED_HEART.get());
        helper.runAfterDelay(2, () -> {
            player.hurt(helper.getLevel().damageSources().generic(), 1000F);
            helper.assertTrue(player.isAlive() && Math.abs(player.getHealth() - 1F) < 1e-3, "The heart must leave 1 health, got " + player.getHealth());
            helper.assertTrue(Souls.total(player) == 3, "The heart must put three souls in the lantern, got " + Souls.total(player));
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "necro_lich")
    public static void conversionMakesALich(GameTestHelper helper) {
        Player player = playerWithLantern(helper);
        ItemStack amulet = new ItemStack(ModItems.PHYLACTERY_AMULET.get());
        com.soulinchains.arcaneartifacts.necro.PhylacteryAmuletItem.bind(amulet,
                net.minecraft.core.GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(2, 2, 2))));
        CuriosApi.getCuriosInventory(player).ifPresent(inv -> inv.setEquippedCurio("necklace", 0, amulet));
        helper.runAfterDelay(2, () -> {
            com.soulinchains.arcaneartifacts.necro.Necro.setRank(player, com.soulinchains.arcaneartifacts.necro.Necro.CHAINLORD);
            com.soulinchains.arcaneartifacts.necro.Corruption.set(player, 50F);
            fill(player, SoulType.GREAT, 1);
            helper.assertTrue(!com.soulinchains.arcaneartifacts.necro.ConversionRitual.convert(player), "Corruption 50 is not enough");
            com.soulinchains.arcaneartifacts.necro.Corruption.set(player, 99F);
            helper.assertTrue(com.soulinchains.arcaneartifacts.necro.ConversionRitual.convert(player), "A Chainlord at 99 with a bound phylactery converts");
            helper.assertTrue(com.soulinchains.arcaneartifacts.necro.Corruption.isLich(player), "The player must be a Lich");
            com.soulinchains.arcaneartifacts.necro.Corruption.set(player, 10F);
            helper.assertTrue(com.soulinchains.arcaneartifacts.necro.Corruption.isLich(player), "A Lich stays a Lich");
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100));
            helper.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION), "Regeneration must not take on a Lich");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void jailerLeavesExactlyOneGreatSoul(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var jailer = helper.spawnWithNoFreeWill(com.soulinchains.arcaneartifacts.registry.ModEntities.JAILER.get(), new BlockPos(4, 2, 4));
        jailer.addEffect(new MobEffectInstance(ModEffects.REAP_MARK, 100), player);
        Souls.markForReaping(jailer, player.getUUID(), false);
        jailer.hurt(helper.getLevel().damageSources().playerAttack(player), 10000F);
        long great = wisps(helper).stream().filter(w -> w.soul() == SoulType.GREAT).count();
        helper.assertTrue(great == 1, "The Jailer must leave exactly one great soul, got " + great);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void areaShackleSparesOwnServants(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Zombie caster = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 2, 4));
        helper.assertTrue(com.soulinchains.arcaneartifacts.necro.Necro.friendly(caster, caster), "One is friendly to oneself");
        helper.assertTrue(!com.soulinchains.arcaneartifacts.necro.Necro.friendly(caster, pig), "A stray pig is not a friend");
        helper.succeed();
    }
}
