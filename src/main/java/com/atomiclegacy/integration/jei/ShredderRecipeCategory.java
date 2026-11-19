package com.atomiclegacy.integration.jei;

import com.atomiclegacy.inventory.recipes.ShredderRecipe;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
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

import java.util.List;

/**
 * [parity T4: JEI] ShredderRecipeHandler: gui_nei_shredder.png, input 39,24 (stack size 1), result 129,24, blades
 * (getBlades: steel, titanium, desh) at 84,6 and 84,42, power bar drawProgressBar(3, 6, 36, 86, 16, 52, 480, 7) and
 * progress drawProgressBar(80, 23, 100, 118, 24, 16, 48, 0).
 */
public class ShredderRecipeCategory extends NeiTemplateCategory<RecipeHolder<ShredderRecipe>> {
    private final IDrawableAnimated powerBar;
    private final IDrawableAnimated progressBar;

    public ShredderRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.SHREDDER, Component.literal("Shredder"), new ItemStack(HbmBlocks.MACHINE_SHREDDER.get()), "gui_nei_shredder");
        this.powerBar = progress(helper, 36, 86, 16, 52, 480, 7);
        this.progressBar = progress(helper, 100, 118, 24, 16, 48, 0);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ShredderRecipe> holder, IFocusGroup focuses) {
        ShredderRecipe recipe = holder.value();
        builder.addSlot(RecipeIngredientRole.INPUT, 39, 24).addItemStacks(NeiUniversalCategory.items(recipe.ingredient(), 1));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 129, 24).addItemStack(recipe.result().copy());
        List<ItemStack> blades = List.of(new ItemStack(HbmItems.BLADES_STEEL.get()), new ItemStack(HbmItems.BLADES_TITANIUM.get()),
                new ItemStack(HbmItems.BLADES_DESH.get()));
        builder.addSlot(RecipeIngredientRole.CATALYST, 84, 6).addItemStacks(blades);
        builder.addSlot(RecipeIngredientRole.CATALYST, 84, 42).addItemStacks(blades);
    }

    @Override
    public void draw(RecipeHolder<ShredderRecipe> recipe, IRecipeSlotsView view, GuiGraphics graphics, double mouseX, double mouseY) {
        this.powerBar.draw(graphics, 3, 6);
        this.progressBar.draw(graphics, 80, 23);
    }
}
