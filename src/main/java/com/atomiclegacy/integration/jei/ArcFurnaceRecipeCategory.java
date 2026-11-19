package com.atomiclegacy.integration.jei;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.ScrapsItem;
import com.atomiclegacy.inventory.recipes.ArcFurnaceRecipe;
import com.atomiclegacy.registry.HbmBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;
import java.util.List;

public final class ArcFurnaceRecipeCategory implements IRecipeCategory<RecipeHolder<ArcFurnaceRecipe>> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/nei/gui_nei.png");

    private final boolean liquid;
    private final IDrawable background;
    private final IDrawable icon;

    public ArcFurnaceRecipeCategory(IGuiHelper helper, boolean liquid) {
        this.liquid = liquid;
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_ARC_FURNACE.get()));
    }

    @Override
    public RecipeType<RecipeHolder<ArcFurnaceRecipe>> getRecipeType() {
        return liquid ? HbmJeiPlugin.ARC_FURNACE_LIQUID : HbmJeiPlugin.ARC_FURNACE_SOLID;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(HbmBlocks.MACHINE_ARC_FURNACE.get().getDescriptionId()) /* [parity S3: JEI] NEI title */;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ArcFurnaceRecipe> holder, IFocusGroup focuses) {
        ArcFurnaceRecipe recipe = holder.value();
        List<ItemStack> inputs = Arrays.stream(recipe.input().getItems())
                .map(ItemStack::copy)
                .peek(stack -> stack.setCount(recipe.count()))
                .toList();
        builder.addInputSlot(48, 24).addItemStacks(inputs);
        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31)
                .addItemStack(new ItemStack(HbmBlocks.MACHINE_ARC_FURNACE.get()));

        if (liquid) {
            for (int index = 0; index < Math.min(3, recipe.liquidStacks().size()); index++) {
                builder.addOutputSlot(102 + index * 18, 24)
                        .addItemStack(ScrapsItem.create(recipe.liquidStacks().get(index), true));
            }
        } else {
            builder.addOutputSlot(102, 24).addItemStack(recipe.solidOutput().copy());
        }
    }

    @Override // [parity S3: JEI] NEIUniversalHandler slot and operation frames
    public void draw(RecipeHolder<ArcFurnaceRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView view, GuiGraphics graphics, double mouseX, double mouseY) {
        UniversalJeiCategory.drawFrames(graphics, 1, liquid ? Math.min(3, recipe.value().liquidStacks().size()) : 1);
    }
}
