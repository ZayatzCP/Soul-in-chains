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
import com.soulinchains.arcaneartifacts.accessory.FocusingBootsItem;
import com.soulinchains.arcaneartifacts.accessory.FocusingGlovesItem;
import com.soulinchains.arcaneartifacts.accessory.WreathOfManipulationItem;
import com.soulinchains.arcaneartifacts.glyph.EffectCounterspell;
import com.soulinchains.arcaneartifacts.glyph.EffectWeatherShift;
import com.soulinchains.arcaneartifacts.phenomena.Phenomena;
import com.soulinchains.arcaneartifacts.phenomena.Vortex;
import com.soulinchains.arcaneartifacts.registry.ModEffects;
import com.soulinchains.arcaneartifacts.damage.ParasitismManager;
import com.soulinchains.arcaneartifacts.entity.ArcaneMissileEntity;
import com.soulinchains.arcaneartifacts.glyph.ArcaneGlyphs;
import com.soulinchains.arcaneartifacts.glyph.AugmentShrapnel;
import com.soulinchains.arcaneartifacts.glyph.MethodArcaneMissile;
import com.soulinchains.arcaneartifacts.registry.ModItems;
import com.soulinchains.arcaneartifacts.spell.ShrapnelHandler;
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

    /**
     * Puts the item in the Curios slot and checks the slot itself. Curios caches
     * {@code findFirstCurio} results for the current game tick, so tests look the item up
     * through the mod's helpers only from the next tick on (see {@link #afterEquip}).
     */
    private static void equip(GameTestHelper helper, Player player, String slot, Item item) {
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        helper.assertTrue(inventory != null, "Player has no Curios inventory");
        var handler = inventory.getStacksHandler(slot).orElse(null);
        helper.assertTrue(handler != null, "Player has no Curios slot '" + slot + "'");
        helper.assertTrue(handler.getStacks().getSlots() > 0, "Curios slot '" + slot + "' has size 0");
        inventory.setEquippedCurio(slot, 0, new ItemStack(item));
        helper.assertTrue(handler.getStacks().getStackInSlot(0).is(item),
                "Slot '" + slot + "' holds " + handler.getStacks().getStackInSlot(0) + " instead of " + item);
    }

    /** Waits (up to 40 ticks) until Curios reports the item as equipped, then runs the scenario. */
    private static void afterEquip(GameTestHelper helper, Player player, Item item, Runnable body) {
        poll(helper, player, item, body, 0);
    }

    private static void poll(GameTestHelper helper, Player player, Item item, Runnable body, int attempt) {
        helper.runAfterDelay(1, () -> {
            if (CurioHelper.findEquipped(player, item).isPresent()) {
                body.run();
            } else if (attempt < 40) {
                poll(helper, player, item, body, attempt + 1);
            } else {
                String held = CuriosApi.getCuriosInventory(player)
                        .map(inv -> inv.getCurios().toString()).orElse("no inventory");
                helper.fail("Curios does not report " + item + " as equipped after 40 ticks; inventory: " + held);
            }
        });
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
                "glyph_arcane_missile", "glyph_arcane_mark", "glyph_parasitism", "glyph_shrapnel", "glyph_pursuit",
                "glyph_beam", "glyph_black_hole", "glyph_tornado", "glyph_weather_shift", "glyph_counterspell",
                "glyph_meteor_shower", "glyph_time_warp", "old_parchment", "researcher_notebook", "research_table",
                "spell_staff", "bottomless_belt", "amulet_of_opposites",
                "glyph_anvil_drop", "glyph_satiety", "rose_gold_block", "rose_gold_nugget", "rose_gold_ingot_from_block",
                "rose_gold_ingot_from_nuggets", "oath_scroll", "heart_charm", "diamond_charm", "club_charm", "spade_charm",
                "enchanted_poisonous_potato", "gilded_pufferfish", "gilded_fermented_spider_eye", "glyph_pity", "glyph_cupid",
                "glyph_harmless", "ring_of_calm", "hop_slippers", "mirror_shield", "focusing_shield")) {
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
        helper.assertTrue(MethodProjectile.INSTANCE.compatibleAugments.contains(AugmentShrapnel.INSTANCE), "Projectile does not accept Shrapnel");
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
    public static void wreathBattleRush(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = player(helper, 4.5, 1, 4.5);
        equip(helper, player, "head", ModItems.WREATH_OF_MANIPULATION.get());
        afterEquip(helper, player, ModItems.WREATH_OF_MANIPULATION.get(), () -> {
            ItemStack wreath = CurioHelper.findEquipped(player, ModItems.WREATH_OF_MANIPULATION.get()).orElseThrow();
            WreathOfManipulationItem.startRush(player, wreath);
            helper.assertTrue(player.hasEffect(ModEffects.BATTLE_RUSH), "Battle Rush not granted");
            helper.assertTrue(player.hasEffect(ModEffects.RUSH_EXHAUSTION), "Rush cooldown not started");
            helper.assertTrue(WreathOfManipulationItem.state(wreath).isActive(level.getGameTime()), "Wreath does not show the rush");

            float hp = player.getHealth();
            hurtFresh(player, level.damageSources().magic(), 6f);
            helper.assertTrue(player.getHealth() == hp, "Took damage during Battle Rush");

            SpellContext context = new SpellContext(level, new Spell(MethodProjectile.INSTANCE, EffectHarm.INSTANCE), player, LivingCaster.from(player));
            var cost = new com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent.Pre(context, 40);
            NeoForge.EVENT_BUS.post(cost);
            helper.assertTrue(cost.currentCost == 0, "Spell still costs " + cost.currentCost + " mana during Battle Rush");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void ringNegatesBigHitsAndThrowsAttacker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Husk attacker = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(5, 1, 4));
        Player player = player(helper, 4.5, 1, 4.5);
        equip(helper, player, "ring", ModItems.RING_OF_REVERSAL.get());
        afterEquip(helper, player, ModItems.RING_OF_REVERSAL.get(), () -> {
            float hp = player.getHealth();
            hurtFresh(player, level.damageSources().mobAttack(attacker), player.getMaxHealth() * 0.75f);
            helper.assertTrue(player.getHealth() == hp, "A hit above half health was not negated");
            Vec3 push = attacker.getDeltaMovement();
            helper.assertTrue(Math.hypot(push.x, push.z) > 0.5 && push.y > 0, "Attacker was not thrown back: " + push);

            hurtFresh(player, level.damageSources().mobAttack(attacker), 3f);
            helper.assertTrue(player.getHealth() < hp, "A small hit must still hurt");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void bootsHaveBuiltInSelf(GameTestHelper helper) {
        Spell effects = new Spell(EffectHarm.INSTANCE);
        Spell cast = FocusingBootsItem.withSelf(effects);
        helper.assertTrue(cast.getCastMethod() == com.hollingsworth.arsnouveau.common.spell.method.MethodSelf.INSTANCE
                && cast.unsafeList().size() == 2, "Boots must prepend Self, got " + cast.getDisplayString());
        Spell projectile = new Spell(MethodProjectile.INSTANCE, EffectHarm.INSTANCE);
        helper.assertTrue(FocusingBootsItem.withSelf(projectile).getCastMethod() == MethodProjectile.INSTANCE, "A spell with a form is kept as is");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void glovesDefaultToProjectile(GameTestHelper helper) {
        ItemStack gloves = new ItemStack(ModItems.FOCUSING_GLOVES.get());
        helper.assertTrue(FocusingGlovesItem.castableSpell(gloves).getCastMethod() == MethodProjectile.INSTANCE, "Empty gloves must default to Projectile");
        var caster = FocusingGlovesItem.storedSpell(gloves);
        helper.assertTrue(caster.isEmpty(), "New gloves must hold no spell");
        com.hollingsworth.arsnouveau.api.spell.SpellCaster spellCaster = gloves.get(com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry.SPELL_CASTER);
        gloves.set(com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry.SPELL_CASTER, spellCaster.setSpell(new Spell(EffectHarm.INSTANCE)));
        Spell effective = FocusingGlovesItem.castableSpell(gloves);
        helper.assertTrue(effective.getCastMethod() == MethodProjectile.INSTANCE && effective.unsafeList().size() == 2, "A formless spell must be cast as Projectile -> Harm, got " + effective.getDisplayString());
        gloves.set(com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry.SPELL_CASTER, spellCaster.setSpell(new Spell(MethodArcaneMissile.INSTANCE, EffectHarm.INSTANCE)));
        helper.assertTrue(FocusingGlovesItem.castableSpell(gloves).getCastMethod() == MethodArcaneMissile.INSTANCE, "A spell with its own form must keep it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void mantleReflectsSpellProjectile(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Husk caster = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(7, 1, 7));
        Player wearer = player(helper, 2.5, 1, 2.5);
        equip(helper, wearer, "back", ModItems.MANTLE_OF_REFLECTION.get());
        afterEquip(helper, wearer, ModItems.MANTLE_OF_REFLECTION.get(), () -> {
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

                net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(EntityType.ARROW, level);
                arrow.setOwner(caster);
                arrow.setPos(at.x, at.y, at.z);
                arrow.setDeltaMovement(at.subtract(caster.position()).normalize().scale(1.5));
                ProjectileImpactEvent arrowHit = new ProjectileImpactEvent(arrow, new EntityHitResult(wearer));
                NeoForge.EVENT_BUS.post(arrowHit);
                helper.assertTrue(arrowHit.isCanceled() && arrow.getOwner() == wearer, "Mantle did not reflect an arrow");
            } finally {
                Config.MANTLE_REFLECT_CHANCE.set(oldChance);
            }
            helper.succeed();
        });
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
    public static void shrapnelHitsSecondTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Husk first = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 1, 4));
        Husk second = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(7, 1, 7));
        Player caster = player(helper, 1.5, 1, 1.5);
        SpellResolver resolver = new SpellResolver(new SpellContext(level,
                new Spell(MethodArcaneMissile.INSTANCE, AugmentShrapnel.INSTANCE, EffectHarm.INSTANCE), caster, LivingCaster.from(caster)));
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
        context.tag.putBoolean(ShrapnelHandler.SPLIT_CHILD_TAG, true);
        SpellDamageEvent.Pre child = new SpellDamageEvent.Pre(level.damageSources().magic(), caster, target, 8f, context);
        NeoForge.EVENT_BUS.post(child);
        helper.assertTrue(Math.abs(child.damage - 8f * Config.SPLIT_DAMAGE_MULTIPLIER.get().floatValue()) < 0.001f, "Split damage not scaled: " + child.damage);
        SpellDamageEvent.Pre normal = new SpellDamageEvent.Pre(level.damageSources().magic(), caster, target, 8f,
                new SpellContext(level, new Spell(), caster, LivingCaster.from(caster)));
        NeoForge.EVENT_BUS.post(normal);
        helper.assertTrue(normal.damage == 8f, "Normal spell damage must not be scaled");
        helper.succeed();
    }

    // ------------------------------------------------------------------ new glyphs

    @GameTest(template = ARENA)
    public static void weatherShiftChangesWeather(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        EffectWeatherShift.apply(level, EffectWeatherShift.Weather.THUNDER, 1200);
        helper.assertTrue(level.isThundering() || level.getLevelData().isThundering(), "Thunderstorm not started");
        EffectWeatherShift.apply(level, EffectWeatherShift.Weather.RAIN, 1200);
        helper.assertTrue(level.getLevelData().isRaining() && !level.getLevelData().isThundering(), "Rain not started");
        EffectWeatherShift.apply(level, EffectWeatherShift.Weather.CLEAR, 1200);
        helper.assertTrue(!level.getLevelData().isRaining(), "Sky did not clear");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void blackHolePullsCreatures(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 start = helper.absoluteVec(new Vec3(8.5, 2.0, 4.5));
        net.minecraft.world.entity.item.ItemEntity victim = new net.minecraft.world.entity.item.ItemEntity(level, start.x, start.y, start.z,
                new ItemStack(net.minecraft.world.item.Items.STONE));
        victim.setNoGravity(true);
        level.addFreshEntity(victim);
        Vec3 center = helper.absoluteVec(new Vec3(4.5, 2.0, 4.5));
        double before = victim.position().distanceTo(center);
        Phenomena.add(new Vortex(level, java.util.UUID.randomUUID(), Vortex.Kind.BLACK_HOLE, center, 8.0, 40, 0f, 0.3,
                com.soulinchains.arcaneartifacts.fx.Fx.defaults(com.soulinchains.arcaneartifacts.fx.ModTimelines.BLACK_HOLE).main, com.soulinchains.arcaneartifacts.fx.Fx.defaults(com.soulinchains.arcaneartifacts.fx.ModTimelines.BLACK_HOLE).impact));
        helper.runAfterDelay(15, () -> {
            double after = victim.position().distanceTo(center);
            helper.assertTrue(after < before - 1.0, "Black hole did not pull: " + before + " -> " + after);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void counterspellCancelsSpells(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Husk caster = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(6, 1, 6));
        SpellResolver resolver = new SpellResolver(new SpellContext(level, new Spell(MethodProjectile.INSTANCE, EffectHarm.INSTANCE), caster, LivingCaster.from(caster)));
        EntityProjectileSpell projectile = new EntityProjectileSpell(level, resolver);
        projectile.setOwner(caster);
        Vec3 at = caster.position().add(1, 1, 0);
        projectile.setPos(at.x, at.y, at.z);
        level.addFreshEntity(projectile);
        Phenomena.add(new Vortex(level, caster.getUUID(), Vortex.Kind.TORNADO, at, 3.0, 200, 0f, 1.0,
                com.soulinchains.arcaneartifacts.fx.Fx.defaults(com.soulinchains.arcaneartifacts.fx.ModTimelines.TORNADO).main, com.soulinchains.arcaneartifacts.fx.Fx.defaults(com.soulinchains.arcaneartifacts.fx.ModTimelines.TORNADO).impact));
        int undone = EffectCounterspell.counter(level, caster.getUUID(), caster.position());
        helper.assertTrue(undone >= 2, "Counterspell undid " + undone + " spells, expected the projectile and the tornado");
        helper.assertTrue(projectile.isRemoved(), "Spell projectile survived Counterspell");
        helper.assertTrue(Phenomena.active().stream().noneMatch(p -> p.owner().equals(caster.getUUID())), "Tornado survived Counterspell");
        helper.succeed();
    }

    // ------------------------------------------------------------------ new items

    @GameTest(template = ARENA)
    public static void beltPouchesStackAnyAmount(GameTestHelper helper) {
        var contents = com.soulinchains.arcaneartifacts.belt.BeltContents.EMPTY;
        ItemStack cobble = new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64);
        int pouch = contents.firstFree();
        for (int i = 0; i < 2000; i++) {
            contents = contents.add(pouch, cobble, 64);
        }
        helper.assertTrue(contents.get(pouch).count() == 128000L, "Pouch holds " + contents.get(pouch).count());
        helper.assertTrue(contents.collects(new ItemStack(net.minecraft.world.item.Items.COBBLESTONE)), "Pouch must collect its own item");
        helper.assertTrue(!contents.collects(new ItemStack(net.minecraft.world.item.Items.DIRT)), "Pouch must not collect other items");
        contents = contents.remove(pouch, 128000L);
        helper.assertTrue(contents.get(pouch).isEmpty(), "An emptied pouch must forget its item");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void staffAmplifiesOnlyItsOwnSpells(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = player(helper, 4.5, 1, 4.5);
        ItemStack staff = new ItemStack(ModItems.SPELL_STAFF.get());
        SpellContext fromStaff = new SpellContext(level, new Spell(MethodProjectile.INSTANCE, EffectHarm.INSTANCE), player, LivingCaster.from(player), staff);
        var builder = new com.hollingsworth.arsnouveau.api.spell.SpellStats.Builder();
        ModItems.SPELL_STAFF.get().applyItemModifiers(staff, builder, EffectHarm.INSTANCE, null, level, player, fromStaff);
        helper.assertTrue(builder.build().getAmpMultiplier() == 2.0, "Staff spells must get +2 Amplify, got " + builder.build().getAmpMultiplier());
        SpellContext fromBook = new SpellContext(level, new Spell(MethodProjectile.INSTANCE, EffectHarm.INSTANCE), player, LivingCaster.from(player), new ItemStack(net.minecraft.world.item.Items.BOOK));
        var other = new com.hollingsworth.arsnouveau.api.spell.SpellStats.Builder();
        ModItems.SPELL_STAFF.get().applyItemModifiers(staff, other, EffectHarm.INSTANCE, null, level, player, fromBook);
        helper.assertTrue(other.build().getAmpMultiplier() == 0.0, "Spells from other casters must not be amplified");
        helper.assertTrue(com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.from(staff).getMaxSlots() == 5, "The staff must have 5 slots");
        helper.succeed();
    }

    // ------------------------------------------------------------------ batch 3

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void anvilDropCrushes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Husk victim = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 1, 4));
        var style = com.soulinchains.arcaneartifacts.fx.Fx.defaults(com.soulinchains.arcaneartifacts.fx.ModTimelines.ANVIL_DROP);
        Phenomena.add(new com.soulinchains.arcaneartifacts.phenomena.AnvilDrop(level, java.util.UUID.randomUUID(), victim.position(),
                60f, 0, 1f, false, style.main, style.impact));
        helper.succeedWhen(() -> helper.assertTrue(!victim.isAlive(), "The anvil has not crushed the husk yet"));
    }

    @GameTest(template = ARENA)
    public static void wellFedKeepsHungerFull(GameTestHelper helper) {
        Player player = player(helper, 4.5, 1, 4.5);
        player.getFoodData().setFoodLevel(4);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.soulinchains.arcaneartifacts.registry.ModEffects.WELL_FED, 200));
        helper.succeedWhen(() -> {
            player.tick();
            helper.assertTrue(player.getFoodData().getFoodLevel() == 20, "Hunger is " + player.getFoodData().getFoodLevel());
        });
    }

    @GameTest(template = ARENA)
    public static void swornCompanionsAreAllies(GameTestHelper helper) {
        Player first = player(helper, 3.5, 1, 3.5);
        Player second = player(helper, 5.5, 1, 5.5);
        Player stranger = player(helper, 6.5, 1, 6.5);
        var bond = new com.soulinchains.arcaneartifacts.team.TeamBond(java.util.UUID.randomUUID(), "Test company");
        ItemStack hearts = new ItemStack(ModItems.HEART_CHARM.get());
        hearts.set(com.soulinchains.arcaneartifacts.registry.ModComponents.TEAM_BOND, bond);
        ItemStack spades = new ItemStack(ModItems.SPADE_CHARM.get());
        spades.set(com.soulinchains.arcaneartifacts.registry.ModComponents.TEAM_BOND, bond);
        CuriosApi.getCuriosInventory(first).orElseThrow().setEquippedCurio("charm", 0, hearts);
        CuriosApi.getCuriosInventory(second).orElseThrow().setEquippedCurio("charm", 0, spades);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(com.soulinchains.arcaneartifacts.team.TeamHandler.sworn(first, second), "Sworn companions are not recognised");
            helper.assertTrue(com.soulinchains.arcaneartifacts.util.Allies.areAllied(first, second), "Sworn companions must be allies");
            helper.assertTrue(!com.soulinchains.arcaneartifacts.team.TeamHandler.sworn(first, stranger), "A stranger must not be sworn");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ batch 4

    @GameTest(template = ARENA)
    public static void pityWeakensSpells(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = player(helper, 4.5, 1, 4.5);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.soulinchains.arcaneartifacts.registry.ModEffects.PITY, 200));
        SpellContext context = new SpellContext(level, new Spell(MethodProjectile.INSTANCE, EffectHarm.INSTANCE), player, LivingCaster.from(player));
        var stats = new com.hollingsworth.arsnouveau.api.spell.SpellStats.Builder().build(EffectHarm.INSTANCE, null, level, player, context);
        helper.assertTrue(stats.getAmpMultiplier() == -2.0, "Pity must act like two Dampen, got " + stats.getAmpMultiplier());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void bonkStunsAndCannotBeExtended(GameTestHelper helper) {
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 1, 4));
        helper.assertTrue(com.soulinchains.arcaneartifacts.effect.Bonk.apply(husk, 160), "BONK! was not applied");
        helper.assertTrue(!com.soulinchains.arcaneartifacts.effect.Bonk.apply(husk, 160), "A running BONK! must not be extended");
        var bonk = husk.getEffect(com.soulinchains.arcaneartifacts.registry.ModEffects.BONK);
        helper.assertTrue(bonk != null && bonk.getDuration() == 160, "BONK! must last 8 seconds");
        helper.assertTrue(husk.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == 0.0, "A bonked creature must not move");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void harmlessFitsEveryEffect(GameTestHelper helper) {
        helper.assertTrue(EffectHarm.INSTANCE.compatibleAugments.contains(com.soulinchains.arcaneartifacts.glyph.AugmentHarmless.INSTANCE), "Harm must accept Harmless");
        helper.assertTrue(!com.soulinchains.arcaneartifacts.glyph.EffectCupid.INSTANCE.compatibleAugments.contains(com.soulinchains.arcaneartifacts.glyph.AugmentHarmless.INSTANCE)
                && com.soulinchains.arcaneartifacts.glyph.EffectCupid.INSTANCE.compatibleAugments.isEmpty(), "Cupid takes no augments");
        Player caster = player(helper, 4.5, 1, 4.5);
        helper.assertTrue(com.soulinchains.arcaneartifacts.effect.MagicHandler.isHarmless(new SpellContext(helper.getLevel(),
                new Spell(MethodProjectile.INSTANCE, EffectHarm.INSTANCE, com.soulinchains.arcaneartifacts.glyph.AugmentHarmless.INSTANCE),
                caster, LivingCaster.from(caster))), "Harmless spell not recognised");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void calmRingKnowsItsMonsters(GameTestHelper helper) {
        var creeper = helper.spawnWithNoFreeWill(EntityType.CREEPER, new BlockPos(3, 1, 3));
        var zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(5, 1, 5));
        helper.assertTrue(com.soulinchains.arcaneartifacts.accessory.RingOfCalmItem.category(creeper).equals("explosive"), "Creeper is not explosive");
        helper.assertTrue(com.soulinchains.arcaneartifacts.accessory.RingOfCalmItem.category(zombie).equals("undead"), "Zombie is not undead");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void enchantmentsAreRegistered(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        helper.assertTrue(registry.getHolder(com.soulinchains.arcaneartifacts.effect.Bonk.HARD_HAT).isPresent(), "Hard Hat missing");
        helper.assertTrue(registry.getHolder(com.soulinchains.arcaneartifacts.enchant.HeavyProjectile.KEY).isPresent(), "Heavy Projectile missing");
        helper.succeed();
    }
}
