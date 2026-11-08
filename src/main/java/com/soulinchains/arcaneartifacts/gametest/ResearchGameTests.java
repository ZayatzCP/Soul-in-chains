package com.soulinchains.arcaneartifacts.gametest;

import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import com.soulinchains.arcaneartifacts.ArcaneArtifacts;
import com.soulinchains.arcaneartifacts.research.NoteCategory;
import com.soulinchains.arcaneartifacts.research.PlayerResearch;
import com.soulinchains.arcaneartifacts.research.ResearchEvent;
import com.soulinchains.arcaneartifacts.research.ResearchManager;
import com.soulinchains.arcaneartifacts.research.ResearchService;
import com.soulinchains.arcaneartifacts.research.ResearchState;
import com.soulinchains.arcaneartifacts.research.ResearchTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Set;

/** Server-side tests of the research system (Research Table logic, triggers, glyph gating). */
@GameTestHolder(ArcaneArtifacts.MODID)
@PrefixGameTestTemplate(false)
public final class ResearchGameTests {
    private static final String ARENA = "arena";
    private static final ResourceLocation ROOT = ArcaneArtifacts.id("source_resonance");
    private static final ResourceLocation TRAJECTORIES = ArcaneArtifacts.id("trajectories");
    private static final ResourceLocation LIFE_BOND = ArcaneArtifacts.id("life_bond");
    private static final ResourceLocation PARASITISM = ArcaneArtifacts.id("glyph_parasitism");

    private ResearchGameTests() {
    }

    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(4.5, 1, 4.5));
        player.setPos(pos.x, pos.y, pos.z);
        return player;
    }

    @GameTest(template = ARENA)
    public static void researchDataLoads(GameTestHelper helper) {
        for (String id : new String[]{"source_resonance", "trajectories", "arcane_marking", "fragmentation", "life_bond",
                "flesh_adaptation", "focusing", "reflection", "singularity", "firmament", "nullification", "hoarding"}) {
            helper.assertTrue(ResearchManager.get(ArcaneArtifacts.id(id)) != null, "Research not loaded: " + id);
        }
        helper.assertTrue(ResearchManager.gatedGlyphs().contains(PARASITISM), "Parasitism is not gated by any research");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void researchFullCycle(GameTestHelper helper) {
        Player player = player(helper);
        PlayerResearch data = ResearchService.data(player);
        helper.assertTrue(ResearchService.state(data, ROOT) == ResearchState.DISCOVERED, "Root research should start discovered");
        helper.assertTrue(ResearchService.state(data, TRAJECTORIES) == ResearchState.UNKNOWN, "Dependent research should start unknown");

        // Actions before a research is started are not counted.
        ResearchService.fire(player, ResearchEvent.of(ResearchTriggers.SPELL_CAST).withGlyphs(Set.of(ResourceLocation.parse("ars_nouveau:glyph_projectile"))));
        helper.assertTrue(data.entry(ROOT) == null, "An event started a research by itself");

        // Stage I: observation (the sample is not consumed).
        ItemStack gem = new ItemStack(ItemsRegistry.SOURCE_GEM.get());
        helper.assertTrue(ResearchService.observe(player, gem) == 1, "Observing a source gem should start exactly one research");
        helper.assertTrue(gem.getCount() == 1, "Observation consumed the sample");
        helper.assertTrue(ResearchService.state(data, ROOT) == ResearchState.INVESTIGATING, "Root research not investigating");

        // Stage II: experiments, each completed once, with alternatives.
        ResearchService.fire(player, ResearchEvent.of(ResearchTriggers.SPELL_CAST).withGlyphs(Set.of(ResourceLocation.parse("ars_nouveau:glyph_touch"))));
        ResearchService.observe(player, new ItemStack(Items.LAPIS_LAZULI)); // the "compare" experiment's second alternative
        helper.assertTrue(ResearchService.state(data, ROOT) == ResearchState.INVESTIGATING, "Completed before all experiments");
        ResearchService.fire(player, ResearchEvent.of(ResearchTriggers.SPELL_EFFECT)
                .withGlyphs(Set.of(ResourceLocation.parse("ars_nouveau:glyph_heal"))).withEntity(EntityType.COW));

        // Stage III: principle and notes.
        helper.assertTrue(ResearchService.state(data, ROOT) == ResearchState.PRINCIPLE, "Principle not learned, state " + ResearchService.state(data, ROOT));
        helper.assertTrue(data.notes(NoteCategory.ARCANE) == 1, "Expected 1 arcane note, got " + data.notes(NoteCategory.ARCANE));
        helper.assertTrue(ResearchService.state(data, TRAJECTORIES) == ResearchState.DISCOVERED, "Dependent research not discovered");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void formulateNeedsNotes(GameTestHelper helper) {
        Player player = player(helper);
        PlayerResearch data = ResearchService.data(player);
        data.setState(TRAJECTORIES, ResearchState.PRINCIPLE);
        helper.assertTrue(!ResearchService.formulate(player, TRAJECTORIES), "Formulated without the required notes");
        data.addNotes(NoteCategory.ARCANE, 1);
        helper.assertTrue(ResearchService.formulate(player, TRAJECTORIES), "Could not formulate with the required notes");
        helper.assertTrue(ResearchService.state(data, TRAJECTORIES) == ResearchState.UNLOCKED, "Formulated research is not unlocked");
        helper.assertTrue(data.notes(NoteCategory.ARCANE) == 1, "Notes must not be spent");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void scribesTableRefusesLockedGlyph(GameTestHelper helper) {
        Player player = player(helper);
        helper.assertTrue(!ResearchService.isGlyphAvailable(player, PARASITISM), "Parasitism should be locked for a new player");

        RecipeHolder<?> holder = helper.getLevel().getRecipeManager().byKey(ArcaneArtifacts.id("glyph_parasitism")).orElse(null);
        helper.assertTrue(holder != null && holder.value() instanceof GlyphRecipe, "Parasitism glyph recipe missing");
        @SuppressWarnings("unchecked")
        RecipeHolder<GlyphRecipe> recipe = (RecipeHolder<GlyphRecipe>) holder;
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(pos, BlockRegistry.SCRIBES_BLOCK.get().defaultBlockState());
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof ScribesTile, "No Scribes Table tile");
        ScribesTile tile = (ScribesTile) helper.getLevel().getBlockEntity(pos);
        player.giveExperienceLevels(100);
        tile.setRecipe(recipe, player);
        helper.assertTrue(tile.recipe == null, "Scribes Table accepted a glyph whose research is unfinished");

        ResearchService.data(player).setState(LIFE_BOND, ResearchState.UNLOCKED);
        helper.assertTrue(ResearchService.isGlyphAvailable(player, PARASITISM), "Glyph still locked after its research was formulated");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void castingLockedGlyphIsRefused(GameTestHelper helper) {
        Player player = player(helper);
        com.hollingsworth.arsnouveau.api.spell.Spell spell = new com.hollingsworth.arsnouveau.api.spell.Spell(
                com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile.INSTANCE,
                com.soulinchains.arcaneartifacts.glyph.EffectParasitism.INSTANCE);
        var context = new com.hollingsworth.arsnouveau.api.spell.SpellContext(helper.getLevel(), spell, player,
                com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster.from(player));
        var event = new com.hollingsworth.arsnouveau.api.event.SpellCastEvent(spell, context);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(event.isCanceled(), "A spell with an unresearched glyph must not be cast");
        helper.succeed();
    }
}
