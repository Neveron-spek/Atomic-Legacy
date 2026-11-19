package com.atomiclegacy.integration.jei;

import com.atomiclegacy.inventory.recipes.PressRecipe;
import com.atomiclegacy.items.StampItem;
import com.atomiclegacy.registry.HbmBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity T4: JEI] PressRecipeHandler: gui_nei_press.png, input 48,42 (stack size 1), stamp 48,6, result 111,24,
 * drawProgressBar(47, 24, 0, 86, 18, 18, 20, 1).
 */
public class PressRecipeCategory extends NeiTemplateCategory<RecipeHolder<PressRecipe>> {
    private final IDrawableAnimated progress;

    public PressRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.PRESS, Component.literal("Press"), new ItemStack(HbmBlocks.MACHINE_PRESS.get()), "gui_nei_press");
        this.progress = progress(helper, 0, 86, 18, 18, 20, 1);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PressRecipe> holder, IFocusGroup focuses) {
        PressRecipe recipe = holder.value();
        builder.addSlot(RecipeIngredientRole.INPUT, 48, 42).addItemStacks(NeiUniversalCategory.items(recipe.ingredient(), 1));
        builder.addSlot(RecipeIngredientRole.INPUT, 48, 6).addItemStacks(stampsFor(recipe.stamp()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 111, 24).addItemStack(recipe.result().copy());
    }

    @Override
    public void draw(RecipeHolder<PressRecipe> recipe, IRecipeSlotsView view, GuiGraphics graphics, double mouseX, double mouseY) {
        this.progress.draw(graphics, 47, 24);
    }

    /** ItemStamp.stamps.get(type). */
    private static List<ItemStack> stampsFor(StampItem.StampType type) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (StampItem.isStampOfType(stack, type)) {
                stacks.add(stack);
            }
        }
        return stacks;
    }
}
