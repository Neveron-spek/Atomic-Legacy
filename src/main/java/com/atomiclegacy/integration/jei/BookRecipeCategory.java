package com.atomiclegacy.integration.jei;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.recipes.LegacyCraftBookRecipes;
import com.atomiclegacy.registry.HbmItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** [parity S3: JEI] BookRecipeHandler ("Black Book"): GUIBook texture, inputs 25+(i%2)*36 / 6+(i/2)*36, result 119,24. */
public final class BookRecipeCategory implements IRecipeCategory<LegacyCraftBookRecipes.MagicDisplay> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/processing/gui_book.png");
    private final IDrawable background;
    private final IDrawable icon;

    public BookRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmItems.BOOK_OF.get()));
    }

    @Override public RecipeType<LegacyCraftBookRecipes.MagicDisplay> getRecipeType() { return ParityS3Jei.BOOK; }
    @Override public Component getTitle() { return Component.literal("Black Book"); }
    @Override public IDrawable getBackground() { return this.background; }
    @Override public IDrawable getIcon() { return this.icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, LegacyCraftBookRecipes.MagicDisplay recipe, IFocusGroup focuses) {
        for (int i = 0; i < Math.min(recipe.inputs().size(), 4); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 25 + (i % 2) * 36, 6 + (i / 2) * 36).addItemStacks(recipe.inputs().get(i));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 119, 24).addItemStack(recipe.output());
    }
}
