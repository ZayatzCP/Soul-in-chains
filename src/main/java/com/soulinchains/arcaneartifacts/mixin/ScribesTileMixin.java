package com.soulinchains.arcaneartifacts.mixin;

import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import com.hollingsworth.arsnouveau.common.items.Glyph;
import com.soulinchains.arcaneartifacts.research.ResearchService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ars Nouveau starts glyph crafting in {@code ScribesTile#setRecipe} (from the Scribes Table GUI packet)
 * and offers no event for it. This refuses glyphs locked behind an unfinished research before any
 * experience or item is taken.
 */
@Mixin(value = ScribesTile.class, remap = false)
public abstract class ScribesTileMixin {
    @Inject(method = "setRecipe", at = @At("HEAD"), cancellable = true)
    private void arcaneArtifacts$checkResearch(RecipeHolder<GlyphRecipe> recipe, Player player, CallbackInfo ci) {
        if (recipe == null || player == null || !(recipe.value().output.getItem() instanceof Glyph glyph) || glyph.spellPart == null) {
            return;
        }
        ResourceLocation id = glyph.spellPart.getRegistryName();
        if (!ResearchService.isGlyphAvailable(player, id)) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.research.glyph_locked",
                    ResearchService.unlockingResearchName(id)).withStyle(ChatFormatting.RED), false);
            ci.cancel();
        }
    }
}
