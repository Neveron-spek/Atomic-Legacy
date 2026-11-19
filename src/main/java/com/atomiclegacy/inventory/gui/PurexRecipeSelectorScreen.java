package com.atomiclegacy.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.atomiclegacy.inventory.container.PurexMenu;
import com.atomiclegacy.inventory.recipes.LegacyRecipeNames;
import com.atomiclegacy.inventory.recipes.PurexRecipe;
import com.atomiclegacy.items.FluidIconItem;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** [parity K1] GUIScreenRecipeSelector for PUREXRecipes. */
public final class PurexRecipeSelectorScreen extends LegacyRecipeSelectorScreen<PurexRecipe> {
    private final PurexMenu menu;

    public PurexRecipeSelectorScreen(PurexScreen previousScreen, PurexMenu menu) {
        super(Component.translatable("container.atomiclegacy.purex.recipe_selector"), previousScreen,
                menu.purexRecipes(), menu.selectedRecipeId().orElse(null));
        this.menu = menu;
    }

    @Override
    protected ItemStack icon(RecipeHolder<PurexRecipe> holder) {
        return displayIcon(holder);
    }

    @Override
    protected Component localizedName(RecipeHolder<PurexRecipe> holder) {
        return localizedNameOf(holder);
    }

    @Override
    protected List<Component> describe(RecipeHolder<PurexRecipe> holder) {
        return describeRecipe(holder);
    }

    @Override
    protected void sendSelection(@Nullable ResourceLocation selection) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, selection == null ? 0 : this.menu.buttonIdForRecipe(selection));
        }
    }

    private static String internalName(RecipeHolder<PurexRecipe> holder) {
        return LegacyRecipeNames.internalName(LegacyRecipeNames.PUREX, holder, holder.value().group());
    }

    /**
     * GenericRecipe#getIcon of PUREXRecipes: the recycling/schrabidium recipes use setIconToFirstIngredient,
     * purex.thoriumsalt setIcon(fluid_icon, THORIUM_SALT), the rest (uzh, flash*, vit*) show their first output.
     */
    public static ItemStack displayIcon(RecipeHolder<PurexRecipe> holder) {
        PurexRecipe recipe = holder.value();
        String name = internalName(holder);
        if ("purex.thoriumsalt".equals(name) && !recipe.outputFluids().isEmpty()) {
            return FluidIconItem.forFluid(recipe.outputFluids().getFirst().type());
        }
        if (LegacyRecipeNames.wrapper(name) != null && !recipe.inputItems().isEmpty()) {
            PurexRecipe.CountedIngredient first = recipe.inputItems().getFirst();
            return LegacyRecipeTooltip.firstItem(first.ingredient(), first.count());
        }
        if (!recipe.outputItems().isEmpty()) {
            return recipe.outputItems().getFirst().stack().copy();
        }
        if (!recipe.outputFluids().isEmpty()) {
            PurexRecipe.PurexFluidStack fluid = recipe.outputFluids().getFirst();
            return FluidIconItem.forFluid(fluid.type(), fluid.amount(), fluid.pressure());
        }
        return ItemStack.EMPTY;
    }

    public static Component localizedNameOf(RecipeHolder<PurexRecipe> holder) {
        return LegacyRecipeNames.localizedName(internalName(holder), displayIcon(holder));
    }

    /** [parity K1] GenericRecipe#print. */
    public static List<Component> describeRecipe(RecipeHolder<PurexRecipe> holder) {
        PurexRecipe recipe = holder.value();
        List<Component> lines = new ArrayList<>();
        LegacyRecipeTooltip.header(lines, localizedNameOf(holder), internalName(holder));
        LegacyRecipeTooltip.autoSwitch(lines, recipe.group());
        LegacyRecipeTooltip.duration(lines, recipe.duration());
        LegacyRecipeTooltip.power(lines, recipe.power());
        LegacyRecipeTooltip.inputHeader(lines);
        for (PurexRecipe.CountedIngredient input : recipe.inputItems()) {
            LegacyRecipeTooltip.inputItem(lines, input.ingredient(), input.count());
        }
        for (PurexRecipe.PurexFluidStack fluid : recipe.inputFluids()) {
            LegacyRecipeTooltip.fluid(lines, fluid.type(), fluid.amount(), fluid.pressure());
        }
        LegacyRecipeTooltip.outputHeader(lines);
        for (PurexRecipe.PurexItemOutput output : recipe.outputItems()) {
            LegacyRecipeTooltip.chanceOutput(lines, output.stack(), output.chance());
        }
        for (PurexRecipe.PurexFluidStack fluid : recipe.outputFluids()) {
            LegacyRecipeTooltip.fluid(lines, fluid.type(), fluid.amount(), fluid.pressure());
        }
        return lines;
    }

    /** AStack#extractForCyclingDisplay(20) (ghost items over empty input slots). */
    static ItemStack displayIngredient(PurexRecipe.CountedIngredient ingredient) {
        return LegacyRecipeTooltip.cycling(ingredient.ingredient(), ingredient.count());
    }
}
