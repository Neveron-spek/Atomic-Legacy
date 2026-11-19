package com.atomiclegacy.integration.jei;

import com.atomiclegacy.inventory.recipes.RefineryRecipe;
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
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * [parity T4: JEI] RefineryRecipeHandler over RefineryRecipes.getRefineryRecipe(): gui_nei_refinery.png, 1000 mB input at
 * 48,24, the four fractions (fill x10) at 111,6 / 129,15 / 111,24 / 129,33, the solid at 111,42, power bar
 * drawProgressBar(3, 6, 0, 86, 16, 52, 480, 7) and progress drawProgressBar(78, 24, 16, 86, 24, 17, 48, 0).
 */
public class RefineryRecipeCategory extends NeiTemplateCategory<RecipeHolder<RefineryRecipe>> {
    private static final int[][] OUTPUTS = {{111, 6}, {129, 15}, {111, 24}, {129, 33}};
    private final IDrawableAnimated powerBar;
    private final IDrawableAnimated progressBar;

    public RefineryRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.REFINERY, Component.literal("Refinery"), new ItemStack(HbmBlocks.MACHINE_REFINERY.get()), "gui_nei_refinery");
        this.powerBar = progress(helper, 0, 86, 16, 52, 480, 7);
        this.progressBar = progress(helper, 16, 86, 24, 17, 48, 0);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RefineryRecipe> holder, IFocusGroup focuses) {
        RefineryRecipe recipe = holder.value();
        UniversalJeiCategory.fill(builder.addSlot(RecipeIngredientRole.INPUT, 48, 24),
                UniversalJeiRecipe.Slot.fluid(recipe.input().fluid(), 1000));
        for (int i = 0; i < recipe.outputs().size() && i < OUTPUTS.length; i++) {
            RefineryRecipe.FluidOutput output = recipe.outputs().get(i);
            UniversalJeiCategory.fill(builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUTS[i][0], OUTPUTS[i][1]),
                    UniversalJeiRecipe.Slot.fluid(output.fluid(), output.amount() * 10));
        }
        if (!recipe.byproduct().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 111, 42).addItemStack(recipe.byproduct().copy());
        }
    }

    @Override
    public void draw(RecipeHolder<RefineryRecipe> recipe, IRecipeSlotsView view, GuiGraphics graphics, double mouseX, double mouseY) {
        this.powerBar.draw(graphics, 3, 6);
        this.progressBar.draw(graphics, 78, 24);
    }
}
