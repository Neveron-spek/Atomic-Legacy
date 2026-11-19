package com.atomiclegacy.integration.jei;

import com.atomiclegacy.inventory.recipes.CyclotronRecipe;
import com.atomiclegacy.registry.HbmBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity T4: JEI] CyclotronRecipeHandler: gui_nei_cyclotron.png, particle 21,24, target 75,24 (stack size 1),
 * result 129,24, drawProgressBar(44, 24, 100, 119, 24, 16, 48, 0). CyclotronRecipes.getRecipes() lists one entry per
 * item of the target AStack, so {@link #expand} splits multi-item ingredients the same way.
 */
public class CyclotronRecipeCategory extends NeiTemplateCategory<RecipeHolder<CyclotronRecipe>> {
    private final IDrawableAnimated progressBar;

    public CyclotronRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.CYCLOTRON, Component.literal("Cyclotron"), new ItemStack(HbmBlocks.MACHINE_CYCLOTRON.get()), "gui_nei_cyclotron");
        this.progressBar = progress(helper, 100, 119, 24, 16, 48, 0);
    }

    static List<RecipeHolder<CyclotronRecipe>> expand(List<RecipeHolder<CyclotronRecipe>> recipes) {
        List<RecipeHolder<CyclotronRecipe>> expanded = new ArrayList<>();
        for (RecipeHolder<CyclotronRecipe> holder : recipes) {
            CyclotronRecipe recipe = holder.value();
            ItemStack[] targets = recipe.input().getItems();
            if (targets.length <= 1) {
                expanded.add(holder);
                continue;
            }
            for (int i = 0; i < targets.length; i++) {
                CyclotronRecipe single = new CyclotronRecipe(recipe.group(), recipe.particle(), Ingredient.of(targets[i].copyWithCount(1)),
                        recipe.output(), recipe.antimatter());
                expanded.add(new RecipeHolder<>(holder.id().withSuffix("/" + i), single));
            }
        }
        return expanded;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CyclotronRecipe> holder, IFocusGroup focuses) {
        CyclotronRecipe recipe = holder.value();
        builder.addSlot(RecipeIngredientRole.INPUT, 21, 24).addItemStacks(NeiUniversalCategory.items(recipe.particle(), 1));
        builder.addSlot(RecipeIngredientRole.INPUT, 75, 24).addItemStacks(NeiUniversalCategory.items(recipe.input(), 1));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 129, 24).addItemStack(recipe.output().copy());
    }

    @Override
    public void draw(RecipeHolder<CyclotronRecipe> recipe, IRecipeSlotsView view, GuiGraphics graphics, double mouseX, double mouseY) {
        this.progressBar.draw(graphics, 44, 24);
    }
}
