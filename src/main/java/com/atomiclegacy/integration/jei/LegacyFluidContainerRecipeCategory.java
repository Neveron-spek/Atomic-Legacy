package com.atomiclegacy.integration.jei;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** JEI-only view of the 1.7.10 fixed fluid-container conversions. */
public final class LegacyFluidContainerRecipeCategory implements IRecipeCategory<LegacyFluidContainerJeiRecipe> {
    private static final ResourceLocation TEXTURE = AtomicLegacy.id("textures/gui/nei/gui_nei_fluid.png"); // [parity S3: JEI] FluidRecipeHandler texture

    private final IDrawable background;
    private final IDrawable icon;

    public LegacyFluidContainerRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmItems.TANK_STEEL.get()));
    }

    @Override
    public RecipeType<LegacyFluidContainerJeiRecipe> getRecipeType() {
        return HbmJeiPlugin.LEGACY_FLUID_CONTAINER;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Fluid Containers") /* [parity S3: JEI] NEI title */;
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
    public void setRecipe(IRecipeLayoutBuilder builder, LegacyFluidContainerJeiRecipe recipe, IFocusGroup focuses) {
        // [parity S3: JEI] FluidRecipeHandler.SmeltingSet: fluid 30,24, empty container 48,24, full container 120,24
        addFluid(builder.addInputSlot(30, 24), recipe);
        if (!recipe.inputItem().isEmpty()) {
            builder.addInputSlot(48, 24).addItemStack(recipe.inputItem().copy());
        }
        builder.addOutputSlot(120, 24).addItemStack(recipe.outputItem().copy());
    }

    private static void addFluid(IRecipeSlotBuilder slot, LegacyFluidContainerJeiRecipe recipe) {
        Fluid fluid = HbmFluids.toNeoFluid(recipe.fluid());
        if (fluid == Fluids.EMPTY || recipe.amount() <= 0) {
            return;
        }
        slot.setFluidRenderer(recipe.amount(), false, 16, 16).addFluidStack(fluid, recipe.amount());
        HbmJeiFluidTooltips.addTo(slot);
    }
}
