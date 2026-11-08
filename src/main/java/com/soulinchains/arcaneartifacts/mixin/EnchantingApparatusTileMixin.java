package com.soulinchains.arcaneartifacts.mixin;

import com.hollingsworth.arsnouveau.common.block.tile.EnchantingApparatusTile;
import com.hollingsworth.arsnouveau.common.crafting.recipes.IEnchantingRecipe;
import com.soulinchains.arcaneartifacts.research.ResearchService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ars Nouveau starts apparatus crafting in {@code EnchantingApparatusTile#attemptCraft} (a player using
 * the reagent on the apparatus) and offers no event for it. Recipes whose result is locked behind an
 * unfinished research are refused before any Source or item is taken. Crafts started without a player
 * (automation) are not affected.
 */
@Mixin(value = EnchantingApparatusTile.class, remap = false)
public abstract class EnchantingApparatusTileMixin {
    @Inject(method = "attemptCraft", at = @At("HEAD"), cancellable = true)
    private void arcaneArtifacts$checkResearch(ItemStack catalyst, Player player, CallbackInfoReturnable<Boolean> cir) {
        EnchantingApparatusTile self = (EnchantingApparatusTile) (Object) this;
        if (player == null || self.getLevel() == null || self.getLevel().isClientSide || catalyst.isEmpty()) {
            return;
        }
        IEnchantingRecipe recipe = self.getRecipe(catalyst, player);
        if (recipe == null) {
            return;
        }
        ItemStack result = recipe.getResultItem(self.getLevel().registryAccess());
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(result.getItem());
        if (!ResearchService.isItemAvailable(player, id)) {
            player.displayClientMessage(Component.translatable("arcane_artifacts.research.item_locked",
                    ResearchService.itemResearchName(id)).withStyle(ChatFormatting.RED), false);
            cir.setReturnValue(false);
        }
    }
}
