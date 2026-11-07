package com.soulinchains.arcaneartifacts.gametest;

import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.Config;
import com.soulinchains.arcaneartifacts.accessory.DamageCategory;
import com.soulinchains.arcaneartifacts.accessory.FocusingGlovesItem;
import com.soulinchains.arcaneartifacts.accessory.RingOfReversalItem;
import com.soulinchains.arcaneartifacts.accessory.WreathData;
import com.soulinchains.arcaneartifacts.accessory.WreathOfManipulationItem;
import com.soulinchains.arcaneartifacts.damage.ParasitismManager;
import com.soulinchains.arcaneartifacts.entity.ArcaneMissileEntity;
import com.soulinchains.arcaneartifacts.glyph.ArcaneGlyphs;
import com.soulinchains.arcaneartifacts.glyph.AugmentSplitting;
import com.soulinchains.arcaneartifacts.glyph.MethodArcaneMissile;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.spell.SplittingHandler;
import com.soulinchains.arcaneartifacts.util.CurioHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.List;

/**
 * Server-side game tests. They only run when the namespace is enabled
 * ({@code -Dneoforge.enabledGameTestNamespaces=arcane_artifacts}), e.g. by {@code ./gradlew runGameTestServer}
 * in CI; in a normal game they are not even registered.
 */
@GameTestHolder(ArcaneArtifacts.MODID)
@PrefixGameTestTemplate(false)
public final class ArcaneGameTests {
    private static final String ARENA = "arena";

    private ArcaneGameTests() {
    }

    // ------------------------------------------------------------------ helpers

    private static Player player(GameTestHelper helper, double x, double y, double z) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(x, y, z));
        player.setPos(pos.x, pos.y, pos.z);
        return player;
    }

    private static void equip(GameTestHelper helper, Player player, String slot, Item item) {
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        helper.assertTrue(inventory != null, "Player has no Curios inventory");
        helper.assertTrue(inventory.getStacksHandler(slot).isPresent(), "Player has no Curios slot '" + slot + "'");
        inventory.setEquippedCurio(slot, 0, new ItemStack(item));
        helper.assertTrue(CurioHelper.findEquipped(player, item).isPresent(), "Could not equip " + item + " in slot " + slot);
    }

    private static void hurtFresh(Player player, net.minecraft.world.damagesource.DamageSource source, float amount) {
        player.invulnerableTime = 0;
        player.hurt(source, amount);
    }

    // ------------------------------------------------------------------ content

    @GameTest(template = ARENA)
    public static void recipesLoad(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (String id : List.of("rose_gold_ingot",
                "wreath_of_manipulation", "focusing_boots", "focusing_gloves", "ring_of_reversal", "mantle_of_reflection",
                "glyph_arcane_missile", "glyph_arcane_mark", "glyph_parasitism", "glyph_splitting", "glyph_pursuit")) {
            helper.assertTrue(recipes.byKey(ArcaneArtifacts.id(id)).isPresent(), "Recipe failed to load: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void glyphsRegistered(GameTestHelper helper) {
        for (AbstractSpellPart part : ArcaneGlyphs.ALL) {
            helper.assertTrue(GlyphRegistry.getSpellPart(part.getRegistryName()) == part, "Glyph not in GlyphRegistry: " + part.getRegistryName());
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(part.getRegistryName()), "Glyph item not registered: " + part.getRegistryName());
        }
        helper.assertTrue(MethodProjectile.INSTANCE.compatibleAugments.contains(AugmentSplitting.INSTANCE), "Projectile does not accept Splitting");
        helper.succeed();
    }

    // ------------------------------------------------------------------ parasitism

    @GameTest(template = ARENA)
    public static void parasitismRedirectsDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DamageSources sources = level.damageSources();
        Husk host = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(6, 1, 6));
        Player caster = player(helper, 2.5, 1, 2.5);

        helper.assertTrue(ParasitismManager.tryLink(level, caster, host), "Link was not created");
        float casterHp = caster.getHealth();
        float hostHp = host.getHealth();
        hurtFresh(caster, sources.generic(), 6f);
        helper.assertTrue(caster.getHealth() == casterHp, "Caster took redirected damage: " + casterHp + " -> " + caster.getHealth());
        helper.assertTrue(host.getHealth() < hostHp, "Host did not take the redirected damage");

        helper.assertTrue(!ParasitismManager.tryLink(level, caster, host), "A second link was created while linked / on cooldown");

        hurtFresh(caster, sources.fellOutOfWorld(), 2f);
        helper.assertTrue(caster.getHealth() < casterHp, "Void damage must never be redirected");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void parasitismHasNoRecursion(GameTestHelper helper) {
        // Mutual links: the player redirects to the husk, the husk redirects back to the player.
        // Redirected damage must stop at the husk instead of bouncing back and forth.
        ServerLevel level = helper.getLevel();
        Player player = player(helper, 2.5, 1, 2.5);
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(6, 1, 6));
        helper.assertTrue(ParasitismManager.tryLink(level, player, husk), "Link player->husk was not created");
        helper.assertTrue(ParasitismManager.tryLink(level, husk, player), "Link husk->player was not created");
        float playerHp = player.getHealth();
        float huskHp = husk.getHealth();
        hurtFresh(player, level.damageSources().generic(), 4f);
        helper.assertTrue(player.getHealth() == playerHp, "Damage bounced back to the player");
        helper.assertTrue(Math.abs((huskHp - husk.getHealth()) - 4f) < 0.01f, "Husk should take exactly the redirected 4 damage, took " + (huskHp - husk.getHealth()));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void parasitismBreaksWhenHostUnavailable(GameTestHelper helper) {
        // A host that is no longer in the world (here: a player object that never joined it) breaks the link
        // and the damage reaches the caster normally.
        ServerLevel level = helper.getLevel();
        Player caster = player(helper, 2.5, 1, 2.5);
        Player ghost = player(helper, 6.5, 1, 6.5);
        helper.assertTrue(ParasitismManager.tryLink(level, caster, ghost), "Link was not created");
        float hp = caster.getHealth();
        hurtFresh(caster, level.damageSources().generic(), 3f);
        helper.assertTrue(caster.getHealth() < hp, "Damage was swallowed although the host is unavailable");
        helper.assertTrue(!ParasitismManager.hasLink(caster), "Link to an unavailable host was not broken");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void parasitismImmuneHostStillProtects(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Blaze host = helper.spawnWithNoFreeWill(EntityType.BLAZE, new BlockPos(6, 1, 6));
        Player caster = player(helper, 2.5, 1, 2.5);
        helper.assertTrue(ParasitismManager.tryLink(level, caster, host), "Link was not created");
        float casterHp = caster.getHealth();
        float hostHp = host.getHealth();
        hurtFresh(caster, level.damageSources().inFire(), 3f);
        helper.assertTrue(caster.getHealth() == casterHp, "Caster took fire damage while linked");
        helper.assertTrue(host.getHealth() < hostHp, "Fire-immune host did not receive the converted damage");
        helper.succeed();
    }

    // ------------------------------------------------------------------ accessories

    @GameTest(template = ARENA)
    public static void damageCategories(GameTestHelper helper) {
        DamageSources s = helper.getLevel().damageSources();
        helper.assertTrue(DamageCategory.classify(s.fall()) == null, "Fall damage must not adapt");
        helper.assertTrue(DamageCategory.classify(s.fellOutOfWorld()) == null, "Void damage must not adapt");
        helper.assertTrue(DamageCategory.classify(s.lava()) == DamageCategory.LAVA, "lava");
        helper.assertTrue(DamageCategory.classify(s.inFire()) == DamageCategory.FIRE, "fire");
        helper.assertTrue(DamageCategory.classify(s.drown()) == DamageCategory.DROWNING, "drowning");
        helper.assertTrue(DamageCategory.classify(s.inWall()) == DamageCategory.SUFFOCATION, "suffocation");
        helper.assertTrue(DamageCategory.classify(s.magic()) == DamageCategory.MAGIC, "magic");
        helper.assertTrue(DamageCategory.classify(s.cactus()) == DamageCategory.PHYSICAL, "physical");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void wreathAdaptsAfterFiveHits(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = player(helper, 4.5, 1, 4.5);
        equip(helper, player, "head", ModItems.WREATH_OF_MANIPULATION.get());
        int needed = Config.WREATH_HITS_TO_ADAPT.get();
        for (int i = 0; i < needed - 1; i++) {
            hurtFresh(player, level.damageSources().cactus(), 1f);
        }
        WreathData before = WreathOfManipulationItem.data(CurioHelper.findEquipped(player, ModItems.WREATH_OF_MANIPULATION.get()).orElseThrow());
        helper.assertTrue(before.hits(DamageCategory.PHYSICAL) == needed - 1, "Expected " + (needed - 1) + " counted hits, got " + before.hits(DamageCategory.PHYSICAL));
        helper.assertTrue(!before.isAdapted(DamageCategory.PHYSICAL, level.getGameTime()), "Adapted too early");

        hurtFresh(player, level.damageSources().cactus(), 1f);
        WreathData after = WreathOfManipulationItem.data(CurioHelper.findEquipped(player, ModItems.WREATH_OF_MANIPULATION.get()).orElseThrow());
        helper.assertTrue(after.isAdapted(DamageCategory.PHYSICAL, level.getGameTime()), "Not adapted after " + needed + " hits");
        helper.assertTrue(after.hits(DamageCategory.PHYSICAL) == 0, "Counter must reset when adapting");
        long remaining = after.remainingTicks(DamageCategory.PHYSICAL, level.getGameTime());
        helper.assertTrue(remaining >= Config.WREATH_MIN_ADAPT_SECONDS.get() * 20L && remaining <= Config.WREATH_MAX_ADAPT_SECONDS.get() * 20L,
                "Adaptation duration out of range: " + remaining + " ticks");
        helper.assertTrue(player.hasEffect(DamageCategory.PHYSICAL.grantedEffect()), "Resistance was not granted");

        for (int i = 0; i < needed; i++) {
            hurtFresh(player, level.damageSources().fall(), 1f);
        }
        WreathData fall = WreathOfManipulationItem.data(CurioHelper.findEquipped(player, ModItems.WREATH_OF_MANIPULATION.get()).orElseThrow());
        helper.assertTrue(fall.hits().size() == 0 && fall.adaptedUntil().size() == 1, "Fall damage changed the wreath state");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ringChargesWithRateLimit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = player(helper, 4.5, 1, 4.5);
        equip(helper, player, "ring", ModItems.RING_OF_REVERSAL.get());
        hurtFresh(player, level.damageSources().cactus(), 2f);
        hurtFresh(player, level.damageSources().cactus(), 2f);
        int charge = RingOfReversalItem.charge(CurioHelper.findEquipped(player, ModItems.RING_OF_REVERSAL.get()).orElseThrow());
        helper.assertTrue(charge == Config.RING_CHARGE_PER_HIT.get(), "Expected one rate-limited charge, got " + charge);
        hurtFresh(player, level.damageSources().cactus(), 0.5f);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void glovesPatternMatching(GameTestHelper helper) {
        List<AbstractSpellPart> projectile = FocusingGlovesItem.pattern(new ItemStack(ModItems.FOCUSING_GLOVES.get()));
        helper.assertTrue(projectile.equals(List.of(MethodProjectile.INSTANCE)), "Default pattern must be Projectile, was " + projectile);
        helper.assertTrue(FocusingGlovesItem.matches(projectile, new Spell(MethodProjectile.INSTANCE, EffectHarm.INSTANCE)), "Projectile -> Harm should match");
        helper.assertTrue(!FocusingGlovesItem.matches(projectile, new Spell(MethodArcaneMissile.INSTANCE, EffectHarm.INSTANCE)), "Missile spell must not match Projectile pattern");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void mantleReflectsSpellProjectile(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Husk caster = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(7, 1, 7));
        Player wearer = player(helper, 2.5, 1, 2.5);
        equip(helper, wearer, "back", ModItems.MANTLE_OF_REFLECTION.get());
        double oldChance = Config.MANTLE_REFLECT_CHANCE.get();
        Config.MANTLE_REFLECT_CHANCE.set(1.0);
        try {
            SpellResolver resolver = new SpellResolver(new SpellContext(level, new Spell(MethodProjectile.INSTANCE, EffectHarm.INSTANCE), caster, LivingCaster.from(caster)));
            EntityProjectileSpell projectile = new EntityProjectileSpell(level, resolver);
            Vec3 at = wearer.position().add(0, 1, 0);
            projectile.setPos(at.x, at.y, at.z);
            projectile.setDeltaMovement(at.subtract(caster.position()).normalize().scale(0.8));
            ProjectileImpactEvent event = new ProjectileImpactEvent(projectile, new EntityHitResult(wearer));
            NeoForge.EVENT_BUS.post(event);
            helper.assertTrue(event.isCanceled(), "Mantle did not reflect the spell");
            helper.assertTrue(projectile.getOwner() == wearer, "Reflected projectile must belong to the wearer");
            Vec3 toCaster = caster.position().subtract(projectile.position()).normalize();
            helper.assertTrue(projectile.getDeltaMovement().normalize().dot(toCaster) > 0.9, "Reflected projectile is not aimed at its caster");
            helper.assertTrue(wearer.getCooldowns().isOnCooldown(ModItems.MANTLE_OF_REFLECTION.get()), "Mantle cooldown not started");

            EntityProjectileSpell second = new EntityProjectileSpell(level, resolver);
            ProjectileImpactEvent again = new ProjectileImpactEvent(second, new EntityHitResult(wearer));
            NeoForge.EVENT_BUS.post(again);
            helper.assertTrue(!again.isCanceled(), "Mantle reflected during its cooldown");
        } finally {
            Config.MANTLE_REFLECT_CHANCE.set(oldChance);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ glyphs in flight

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void missileHomesOntoTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Husk target = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(7, 1, 7));
        Player caster = player(helper, 1.5, 1, 1.5);
        SpellResolver resolver = new SpellResolver(new SpellContext(level, new Spell(MethodArcaneMissile.INSTANCE, EffectHarm.INSTANCE), caster, LivingCaster.from(caster)));
        ArcaneMissileEntity missile = new ArcaneMissileEntity(level, resolver);
        missile.configure(0, false);
        Vec3 start = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
        missile.setPos(start.x, start.y, start.z);
        missile.shoot(1, 0, 0, 0.5f, 0f); // fired sideways: it has to turn toward the husk
        level.addFreshEntity(missile);
        helper.succeedWhen(() -> helper.assertTrue(target.getHealth() < target.getMaxHealth(), "Missile has not hit the husk yet"));
    }

    @GameTest(template = ARENA, timeoutTicks = 160)
    public static void splittingHitsSecondTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Husk first = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 1, 4));
        Husk second = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(7, 1, 7));
        Player caster = player(helper, 1.5, 1, 1.5);
        SpellResolver resolver = new SpellResolver(new SpellContext(level,
                new Spell(MethodArcaneMissile.INSTANCE, AugmentSplitting.INSTANCE, EffectHarm.INSTANCE), caster, LivingCaster.from(caster)));
        ArcaneMissileEntity missile = new ArcaneMissileEntity(level, resolver);
        missile.configure(0, false);
        Vec3 start = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
        missile.setPos(start.x, start.y, start.z);
        missile.shoot(1, 0, 1, 0.5f, 0f);
        level.addFreshEntity(missile);
        helper.succeedWhen(() -> {
            helper.assertTrue(first.getHealth() < first.getMaxHealth(), "First husk not hit");
            helper.assertTrue(second.getHealth() < second.getMaxHealth(), "Split projectile did not reach the second husk");
        });
    }

    @GameTest(template = ARENA)
    public static void splitChildrenDealHalfDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Husk target = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 1, 4));
        Player caster = player(helper, 1.5, 1, 1.5);
        SpellContext context = new SpellContext(level, new Spell(), caster, LivingCaster.from(caster));
        context.tag.putBoolean(SplittingHandler.SPLIT_CHILD_TAG, true);
        SpellDamageEvent.Pre child = new SpellDamageEvent.Pre(level.damageSources().magic(), caster, target, 8f, context);
        NeoForge.EVENT_BUS.post(child);
        helper.assertTrue(Math.abs(child.damage - 8f * Config.SPLIT_DAMAGE_MULTIPLIER.get().floatValue()) < 0.001f, "Split damage not scaled: " + child.damage);
        SpellDamageEvent.Pre normal = new SpellDamageEvent.Pre(level.damageSources().magic(), caster, target, 8f,
                new SpellContext(level, new Spell(), caster, LivingCaster.from(caster)));
        NeoForge.EVENT_BUS.post(normal);
        helper.assertTrue(normal.damage == 8f, "Normal spell damage must not be scaled");
        helper.succeed();
    }
}
