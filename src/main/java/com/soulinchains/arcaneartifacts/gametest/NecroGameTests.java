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

    @GameTest(template = ARENA)
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
}
