package com.soulinchains.arcaneartifacts.client.notebook;

import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ImbuementRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.List;
import java.util.Optional;

/**
 * Draws the recipes of the notebook: crafting table (shaped / shapeless), Imbuement Chamber,
 * Enchanting Apparatus and Scribes Table. Ingredients with several options cycle once per second;
 * hovering any slot shows the item tooltip.
 */
final class RecipeRenderer {
    private static final int SLOT = 18;

    private RecipeRenderer() {
    }

    static void render(GuiGraphics g, ArcaneNotebookScreen screen, ResourceLocation id, int x, int y, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Optional<RecipeHolder<?>> holder = minecraft.level.getRecipeManager().byKey(id);
        if (holder.isEmpty()) {
            screen.drawCentered(g, Component.literal("???"), x, y + 60, ArcaneNotebookScreen.INK_COLOR);
            return;
        }
        Object recipe = holder.get().value();
        int cx = x + ArcaneNotebookScreen.PAGE_WIDTH / 2;
        if (recipe instanceof ShapedRecipe shaped) {
            station(g, screen, "minecraft:crafting_table", x, y);
            List<Ingredient> ingredients = shaped.getIngredients();
            for (int i = 0; i < ingredients.size(); i++) {
                slot(g, screen, ingredients.get(i), x + 10 + (i % shaped.getWidth()) * SLOT, y + 40 + (i / shaped.getWidth()) * SLOT, mouseX, mouseY);
            }
            arrow(g, x + 70, y + 59);
            output(g, screen, shaped.getResultItem(minecraft.level.registryAccess()), x + 100, y + 58, mouseX, mouseY);
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            station(g, screen, "minecraft:crafting_table", x, y);
            List<Ingredient> ingredients = shapeless.getIngredients();
            for (int i = 0; i < ingredients.size(); i++) {
                slot(g, screen, ingredients.get(i), x + 10 + (i % 3) * SLOT, y + 40 + (i / 3) * SLOT, mouseX, mouseY);
            }
            arrow(g, x + 70, y + 59);
            output(g, screen, shapeless.getResultItem(minecraft.level.registryAccess()), x + 100, y + 58, mouseX, mouseY);
            screen.drawCentered(g, Component.translatable("arcane_artifacts.notebook.shapeless"), x, y + 104, ArcaneNotebookScreen.HINT_COLOR);
        } else if (recipe instanceof ImbuementRecipe imbuement) {
            station(g, screen, "ars_nouveau:imbuement_chamber", x, y);
            circle(g, screen, imbuement.input, imbuement.pedestalItems, cx, y + 72, mouseX, mouseY);
            output(g, screen, imbuement.output, cx - 9, y + 120, mouseX, mouseY);
            source(g, screen, imbuement.source, x, y + 144);
        } else if (recipe instanceof EnchantingApparatusRecipe apparatus) {
            station(g, screen, "ars_nouveau:enchanting_apparatus", x, y);
            circle(g, screen, apparatus.reagent(), apparatus.pedestalItems(), cx, y + 72, mouseX, mouseY);
            output(g, screen, apparatus.result(), cx - 9, y + 120, mouseX, mouseY);
            source(g, screen, apparatus.sourceCost(), x, y + 144);
        } else if (recipe instanceof GlyphRecipe glyph) {
            station(g, screen, "ars_nouveau:scribes_table", x, y);
            List<Ingredient> inputs = glyph.inputs;
            int perRow = 6;
            int rowStart = x + (ArcaneNotebookScreen.PAGE_WIDTH - perRow * SLOT) / 2;
            for (int i = 0; i < inputs.size(); i++) {
                slot(g, screen, inputs.get(i), rowStart + (i % perRow) * SLOT, y + 36 + (i / perRow) * SLOT, mouseX, mouseY);
            }
            arrowDown(g, cx - 4, y + 80);
            output(g, screen, glyph.output, cx - 9, y + 96, mouseX, mouseY);
            screen.drawCentered(g, Component.translatable("arcane_artifacts.notebook.experience", glyph.exp), x, y + 122, ArcaneNotebookScreen.HINT_COLOR);
        } else {
            screen.drawCentered(g, Component.literal(id.toString()), x, y + 60, ArcaneNotebookScreen.INK_COLOR);
        }
    }

    /** Station icon and name at the top of the page. */
    private static void station(GuiGraphics g, ArcaneNotebookScreen screen, String itemId, int x, int y) {
        ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId)));
        Component name = icon.getHoverName();
        int width = Math.min(ArcaneNotebookScreen.PAGE_WIDTH, 18 + screen.font().width(name));
        int start = x + (ArcaneNotebookScreen.PAGE_WIDTH - width) / 2;
        g.renderItem(icon, start, y);
        ArcaneNotebookScreen.drawFitted(g, screen.font(), name, start + 18, y + 4, width - 18, ArcaneNotebookScreen.TITLE_COLOR, false);
        g.fill(x + 10, y + 19, x + ArcaneNotebookScreen.PAGE_WIDTH - 10, y + 20, 0x40_5B2A86);
    }

    /** Central item with the pedestal items arranged around it. */
    private static void circle(GuiGraphics g, ArcaneNotebookScreen screen, Ingredient center, List<Ingredient> pedestals,
                               int cx, int cy, int mouseX, int mouseY) {
        // Fits between the station header (y+20) and the result (y+110): at most 34 px, wide enough for 12 pedestals.
        int radius = pedestals.size() > 10 ? 38 : pedestals.size() > 6 ? 32 : 27;
        for (int i = 0; i < pedestals.size(); i++) {
            double angle = 2 * Math.PI * i / pedestals.size() - Math.PI / 2;
            int sx = cx - 9 + (int) Math.round(Math.cos(angle) * radius);
            int sy = cy - 9 + (int) Math.round(Math.sin(angle) * radius);
            slot(g, screen, pedestals.get(i), sx, sy, mouseX, mouseY);
        }
        slot(g, screen, center, cx - 9, cy - 9, mouseX, mouseY);
    }

    private static void source(GuiGraphics g, ArcaneNotebookScreen screen, int amount, int x, int y) {
        screen.drawCentered(g, Component.translatable("arcane_artifacts.notebook.source", amount), x, y, ArcaneNotebookScreen.HINT_COLOR);
    }

    private static void slot(GuiGraphics g, ArcaneNotebookScreen screen, Ingredient ingredient, int sx, int sy, int mouseX, int mouseY) {
        screen.blitSlot(g, sx, sy);
        ItemStack[] options = ingredient.getItems();
        if (options.length == 0) {
            return;
        }
        ItemStack stack = options[(int) ((System.currentTimeMillis() / 1000L) % options.length)];
        item(g, screen, stack, sx, sy, mouseX, mouseY);
    }

    private static void output(GuiGraphics g, ArcaneNotebookScreen screen, ItemStack stack, int sx, int sy, int mouseX, int mouseY) {
        screen.blitSlot(g, sx, sy);
        item(g, screen, stack, sx, sy, mouseX, mouseY);
    }

    private static void item(GuiGraphics g, ArcaneNotebookScreen screen, ItemStack stack, int sx, int sy, int mouseX, int mouseY) {
        g.renderItem(stack, sx + 1, sy + 1);
        g.renderItemDecorations(screen.font(), stack, sx + 1, sy + 1);
        if (mouseX >= sx && mouseX < sx + SLOT && mouseY >= sy && mouseY < sy + SLOT) {
            screen.setHovered(stack);
        }
    }

    private static void arrow(GuiGraphics g, int x, int y) {
        g.blit(ArcaneNotebookScreen.TEXTURE, x, y, 80, ArcaneNotebookScreen.UI_V, 22, 15, ArcaneNotebookScreen.TEX_W, ArcaneNotebookScreen.TEX_H);
    }

    private static void arrowDown(GuiGraphics g, int x, int y) {
        g.blit(ArcaneNotebookScreen.TEXTURE, x, y, 104, ArcaneNotebookScreen.UI_V, 9, 12, ArcaneNotebookScreen.TEX_W, ArcaneNotebookScreen.TEX_H);
    }
}
