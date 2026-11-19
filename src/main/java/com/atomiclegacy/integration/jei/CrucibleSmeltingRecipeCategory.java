package com.atomiclegacy.integration.jei;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.registry.HbmBlocks;
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

/** [parity S3: JEI] CrucibleSmeltingHandler: gui_nei_crucible_smelting.png, input 48,24, crucible 75,42, outputs 3 per row from 102,6. */
public final class CrucibleSmeltingRecipeCategory implements IRecipeCategory<ParityS3JeiRecipes.CrucibleSmeltingDisplay> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/jei/gui_nei_crucible_smelting.png");
    private final IDrawable background;
    private final IDrawable icon;

    public CrucibleSmeltingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_CRUCIBLE.get()));
    }

    @Override public RecipeType<ParityS3JeiRecipes.CrucibleSmeltingDisplay> getRecipeType() { return ParityS3Jei.CRUCIBLE_SMELTING; }
    @Override public Component getTitle() { return Component.literal("Crucible Smelting"); }
    @Override public IDrawable getBackground() { return this.background; }
    @Override public IDrawable getIcon() { return this.icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ParityS3JeiRecipes.CrucibleSmeltingDisplay recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 48, 24).addItemStacks(recipe.inputs());
        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 42).addItemStack(new ItemStack(HbmBlocks.MACHINE_CRUCIBLE.get()));
        for (int i = 0; i < recipe.outputs().size(); i++) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 102 + (i % 3) * 18, 6 + (i / 3) * 18).addItemStack(recipe.outputs().get(i));
        }
    }
}
