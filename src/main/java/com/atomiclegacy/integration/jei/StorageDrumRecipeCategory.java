package com.atomiclegacy.integration.jei;

import com.atomiclegacy.registry.HbmBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** NeoForge JEI counterpart of the 1.7.10 RBMKWasteDecayHandler ([parity S3: JEI] NEIUniversalHandler layout and title). */
public final class StorageDrumRecipeCategory implements IRecipeCategory<StorageDrumJeiRecipe> {
    private static final ResourceLocation TEXTURE = UniversalJeiCategory.TEXTURE;

    private final IDrawable background;
    private final IDrawable icon;

    public StorageDrumRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_STORAGE_DRUM.get()));
    }

    @Override
    public RecipeType<StorageDrumJeiRecipe> getRecipeType() {
        return HbmJeiPlugin.STORAGE_DRUM;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Nuclear Waste Decay");
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, StorageDrumJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(48, 24).addItemStack(recipe.input().copy());
        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31).addItemStack(new ItemStack(HbmBlocks.MACHINE_STORAGE_DRUM.get()));
        builder.addOutputSlot(102, 24).addItemStack(recipe.output().copy());
    }

    @Override
    public void draw(StorageDrumJeiRecipe recipe, IRecipeSlotsView view, GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.blit(TEXTURE, 47, 23, 5, 87, 18, 18);
        graphics.blit(TEXTURE, 101, 23, 5, 87, 18, 18);
        graphics.blit(TEXTURE, 74, 14, 59, 87, 18, 36);
    }
}
