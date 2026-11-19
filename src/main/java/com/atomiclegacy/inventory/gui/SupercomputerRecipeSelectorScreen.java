package com.atomiclegacy.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.atomiclegacy.inventory.container.SupercomputerMenu;
import com.atomiclegacy.inventory.recipes.LegacyRecipeNames;
import com.atomiclegacy.inventory.recipes.SupercomputerRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** [parity K1] GUIScreenRecipeSelector for SuperComputerRecipes. */
public final class SupercomputerRecipeSelectorScreen extends LegacyRecipeSelectorScreen<SupercomputerRecipe> {
    private final SupercomputerMenu menu;

    public SupercomputerRecipeSelectorScreen(SupercomputerScreen previousScreen, SupercomputerMenu menu) {
        super(Component.translatable("container.atomiclegacy.supercomputer.recipe_selector"), previousScreen,
                menu.recipes(), menu.selectedRecipeId().orElse(null));
        this.menu = menu;
    }

    @Override
    protected ItemStack icon(RecipeHolder<SupercomputerRecipe> holder) {
        return holder.value().displayIcon();
    }

    @Override
    protected Component localizedName(RecipeHolder<SupercomputerRecipe> holder) {
        return localizedNameOf(holder);
    }

    @Override
    protected List<Component> describe(RecipeHolder<SupercomputerRecipe> holder) {
        return describeRecipe(holder);
    }

    @Override
    protected void sendSelection(@Nullable ResourceLocation selection) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, selection == null ? 0 : this.menu.buttonIdForRecipe(selection));
        }
    }

    public static Component localizedNameOf(RecipeHolder<SupercomputerRecipe> holder) {
        return LegacyRecipeNames.localizedName(holder.value().group(), holder.value().displayIcon());
    }

    /** [parity K1] GenericRecipe#print. */
    public static List<Component> describeRecipe(RecipeHolder<SupercomputerRecipe> holder) {
        SupercomputerRecipe recipe = holder.value();
        List<Component> lines = new ArrayList<>();
        LegacyRecipeTooltip.header(lines, localizedNameOf(holder),
                LegacyRecipeNames.internalName(LegacyRecipeNames.SUPERCOMPUTER, holder, recipe.group()));
        LegacyRecipeTooltip.duration(lines, recipe.duration());
        LegacyRecipeTooltip.power(lines, recipe.power());
        LegacyRecipeTooltip.inputHeader(lines);
        for (SupercomputerRecipe.CountedIngredient ingredient : recipe.ingredients()) {
            LegacyRecipeTooltip.inputItem(lines, ingredient.ingredient(), ingredient.count());
        }
        for (SupercomputerRecipe.FluidStack fluid : recipe.inputFluids()) {
            LegacyRecipeTooltip.fluid(lines, fluid.type(), fluid.amount(), fluid.pressure());
        }
        LegacyRecipeTooltip.outputHeader(lines);
        if (recipe.outputs().size() == 1) {
            LegacyRecipeTooltip.chanceOutput(lines, recipe.outputs().getFirst().stack(), 1F);
        } else if (!recipe.outputs().isEmpty()) {
            LegacyRecipeTooltip.multiOutput(lines, recipe.outputs().stream().map(SupercomputerRecipe.ChanceOutput::stack).toList(),
                    recipe.outputs().stream().map(SupercomputerRecipe.ChanceOutput::weight).toList(), null);
        }
        for (SupercomputerRecipe.FluidStack fluid : recipe.outputFluids()) {
            LegacyRecipeTooltip.fluid(lines, fluid.type(), fluid.amount(), fluid.pressure());
        }
        return lines;
    }
}
