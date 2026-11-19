package com.atomiclegacy.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.atomiclegacy.inventory.container.ChemicalPlantMenu;
import com.atomiclegacy.inventory.recipes.ChemicalPlantRecipe;
import com.atomiclegacy.inventory.recipes.LegacyRecipeNames;
import com.atomiclegacy.items.FluidIconItem;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** [parity K1] GUIScreenRecipeSelector for ChemicalPlantRecipes. */
public class ChemicalPlantRecipeSelectorScreen extends LegacyRecipeSelectorScreen<ChemicalPlantRecipe> {
    private final ChemicalPlantMenu menu;

    public ChemicalPlantRecipeSelectorScreen(ChemicalPlantScreen previousScreen, ChemicalPlantMenu menu) {
        super(Component.translatable("container.atomiclegacy.chemical_plant.recipe_selector"), previousScreen,
                menu.chemicalPlantRecipes(), menu.selectedRecipeId().orElse(null));
        this.menu = menu;
    }

    @Override
    protected ItemStack icon(RecipeHolder<ChemicalPlantRecipe> holder) {
        return holder.value().displayIcon();
    }

    @Override
    protected Component localizedName(RecipeHolder<ChemicalPlantRecipe> holder) {
        return localizedNameOf(holder);
    }

    @Override
    protected List<Component> describe(RecipeHolder<ChemicalPlantRecipe> holder) {
        return describeRecipe(holder);
    }

    @Override
    protected void sendSelection(@Nullable ResourceLocation selection) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, selection == null ? 0 : this.menu.buttonIdForRecipe(selection));
        }
    }

    /** GenericRecipe#getLocalizedName (setupNamed recipes use their internal name as translation key). */
    public static Component localizedNameOf(RecipeHolder<ChemicalPlantRecipe> holder) {
        return LegacyRecipeNames.localizedName(holder.value().group(), holder.value().displayIcon());
    }

    /** [parity K1] GenericRecipe#print. */
    public static List<Component> describeRecipe(RecipeHolder<ChemicalPlantRecipe> holder) {
        ChemicalPlantRecipe recipe = holder.value();
        List<Component> lines = new ArrayList<>();
        LegacyRecipeTooltip.header(lines, localizedNameOf(holder), recipe.group());
        LegacyRecipeTooltip.duration(lines, recipe.duration());
        LegacyRecipeTooltip.power(lines, recipe.power());
        LegacyRecipeTooltip.inputHeader(lines);
        for (ChemicalPlantRecipe.CountedIngredient ingredient : recipe.inputItems()) {
            LegacyRecipeTooltip.inputItem(lines, ingredient.ingredient(), ingredient.count());
        }
        for (ChemicalPlantRecipe.ChemicalFluidStack fluid : recipe.inputFluids()) {
            LegacyRecipeTooltip.fluid(lines, fluid.type(), fluid.amount(), fluid.pressure());
        }
        LegacyRecipeTooltip.outputHeader(lines);
        for (ChemicalPlantRecipe.ItemOutput output : recipe.outputs()) {
            if (output.multi()) {
                LegacyRecipeTooltip.multiOutput(lines,
                        output.pool().stream().map(ChemicalPlantRecipe.ItemOutput.Entry::stack).toList(),
                        output.pool().stream().map(ChemicalPlantRecipe.ItemOutput.Entry::weight).toList(),
                        output.pool().stream().map(ChemicalPlantRecipe.ItemOutput.Entry::chance).toList());
            } else {
                ChemicalPlantRecipe.ItemOutput.Entry entry = output.pool().getFirst();
                LegacyRecipeTooltip.chanceOutput(lines, entry.stack(), entry.chance());
            }
        }
        for (ChemicalPlantRecipe.ChemicalFluidStack fluid : recipe.outputFluids()) {
            LegacyRecipeTooltip.fluid(lines, fluid.type(), fluid.amount(), fluid.pressure());
        }
        return lines;
    }

    /** AStack#extractForCyclingDisplay(20) (ghost items over empty input slots). */
    static ItemStack displayIngredient(ChemicalPlantRecipe.CountedIngredient ingredient) {
        return LegacyRecipeTooltip.cycling(ingredient.ingredient(), ingredient.count());
    }

    public static ItemStack displayFluid(ChemicalPlantRecipe.ChemicalFluidStack fluid) {
        return FluidIconItem.forFluid(fluid.type(), fluid.amount(), fluid.pressure());
    }
}
