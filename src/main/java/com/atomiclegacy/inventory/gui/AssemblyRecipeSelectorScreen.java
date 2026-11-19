package com.atomiclegacy.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.atomiclegacy.inventory.container.AssemblyMachineMenu;
import com.atomiclegacy.inventory.recipes.AssemblyMachineAutoSwitch;
import com.atomiclegacy.inventory.recipes.AssemblyMachineRecipe;
import com.atomiclegacy.inventory.recipes.LegacyRecipeNames;
import com.atomiclegacy.items.FluidIconItem;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** [parity K1] GUIScreenRecipeSelector for AssemblyMachineRecipes. */
public class AssemblyRecipeSelectorScreen extends LegacyRecipeSelectorScreen<AssemblyMachineRecipe> {
    private final AssemblyMachineMenu menu;

    public AssemblyRecipeSelectorScreen(AssemblyMachineScreen previousScreen, AssemblyMachineMenu menu) {
        super(Component.translatable("container.atomiclegacy.assembly_machine.recipe_selector"), previousScreen,
                menu.assemblyRecipes(), menu.selectedRecipeId().orElse(null));
        this.menu = menu;
    }

    @Override
    protected ItemStack icon(RecipeHolder<AssemblyMachineRecipe> holder) {
        return displayIcon(holder);
    }

    @Override
    protected Component localizedName(RecipeHolder<AssemblyMachineRecipe> holder) {
        return localizedNameOf(holder);
    }

    @Override
    protected List<Component> describe(RecipeHolder<AssemblyMachineRecipe> holder) {
        return describeRecipe(holder);
    }

    @Override
    protected void sendSelection(@Nullable ResourceLocation selection) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, selection == null ? 0 : this.menu.buttonIdForRecipe(selection));
        }
    }

    /**
     * GenericRecipe#getIcon: first output (ChanceOutputMulti: first of the pool); "ass.unpackage*" uses
     * setIcon(ItemFluidIcon.make(type, 32_000)); recipes without item output show their first output fluid.
     */
    public static ItemStack displayIcon(RecipeHolder<AssemblyMachineRecipe> holder) {
        AssemblyMachineRecipe recipe = holder.value();
        if (recipe.group().startsWith("ass.unpackage") && !recipe.outputFluids().isEmpty()) {
            return FluidIconItem.forFluid(recipe.outputFluids().getFirst().type(), 32_000, 0);
        }
        if ("ass.nitra".equals(recipe.group()) && !recipe.ingredients().isEmpty()) {
            // setIconToFirstIngredient
            return LegacyRecipeTooltip.firstItem(recipe.ingredients().getFirst().ingredient(), recipe.ingredients().getFirst().count());
        }
        if (!recipe.outputs().isEmpty()) {
            return recipe.outputs().getFirst().stack().copy();
        }
        if (!recipe.result().isEmpty()) {
            return recipe.result().copy();
        }
        if (!recipe.outputFluids().isEmpty()) {
            AssemblyMachineRecipe.AssemblyFluidStack fluid = recipe.outputFluids().getFirst();
            return FluidIconItem.forFluid(fluid.type(), fluid.amount(), fluid.pressure());
        }
        return ItemStack.EMPTY;
    }

    public static Component localizedNameOf(RecipeHolder<AssemblyMachineRecipe> holder) {
        return LegacyRecipeNames.localizedName(internalName(holder), displayIcon(holder));
    }

    private static String internalName(RecipeHolder<AssemblyMachineRecipe> holder) {
        return LegacyRecipeNames.internalName(LegacyRecipeNames.ASSEMBLY, holder, holder.value().group());
    }

    /** [parity K1] GenericRecipe#print. */
    public static List<Component> describeRecipe(RecipeHolder<AssemblyMachineRecipe> holder) {
        AssemblyMachineRecipe recipe = holder.value();
        List<Component> lines = new ArrayList<>();
        LegacyRecipeTooltip.header(lines, localizedNameOf(holder), internalName(holder));
        LegacyRecipeTooltip.autoSwitch(lines, AssemblyMachineAutoSwitch.groupOf(recipe.group()).orElse(null));
        LegacyRecipeTooltip.duration(lines, recipe.duration());
        LegacyRecipeTooltip.power(lines, recipe.power());
        LegacyRecipeTooltip.inputHeader(lines);
        for (AssemblyMachineRecipe.CountedIngredient ingredient : recipe.ingredients()) {
            LegacyRecipeTooltip.inputItem(lines, ingredient.ingredient(), ingredient.count());
        }
        for (AssemblyMachineRecipe.AssemblyFluidStack fluid : recipe.inputFluids()) {
            LegacyRecipeTooltip.fluid(lines, fluid.type(), fluid.amount(), fluid.pressure());
        }
        LegacyRecipeTooltip.outputHeader(lines);
        if (!recipe.outputs().isEmpty()) {
            LegacyRecipeTooltip.multiOutput(lines,
                    recipe.outputs().stream().map(AssemblyMachineRecipe.ChanceOutput::stack).toList(),
                    recipe.outputs().stream().map(AssemblyMachineRecipe.ChanceOutput::weight).toList(), null);
        } else if (!recipe.result().isEmpty()) {
            LegacyRecipeTooltip.chanceOutput(lines, recipe.result(), 1F);
        }
        for (AssemblyMachineRecipe.AssemblyFluidStack fluid : recipe.outputFluids()) {
            LegacyRecipeTooltip.fluid(lines, fluid.type(), fluid.amount(), fluid.pressure());
        }
        return lines;
    }

    /** AStack#extractForCyclingDisplay(20) (ghost items over empty input slots). */
    static ItemStack displayIngredient(AssemblyMachineRecipe.CountedIngredient ingredient) {
        return LegacyRecipeTooltip.cycling(ingredient.ingredient(), ingredient.count());
    }
}
