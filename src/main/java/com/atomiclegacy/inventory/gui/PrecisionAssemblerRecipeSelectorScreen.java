package com.atomiclegacy.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import com.atomiclegacy.inventory.container.PrecisionAssemblerMenu;
import com.atomiclegacy.inventory.recipes.LegacyRecipeNames;
import com.atomiclegacy.inventory.recipes.PrecisionAssemblerRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** [parity K1] GUIScreenRecipeSelector for PrecAssRecipes. */
public final class PrecisionAssemblerRecipeSelectorScreen extends LegacyRecipeSelectorScreen<PrecisionAssemblerRecipe> {
    private final PrecisionAssemblerMenu menu;

    public PrecisionAssemblerRecipeSelectorScreen(PrecisionAssemblerScreen previous, PrecisionAssemblerMenu menu) {
        super(Component.translatable("container.atomiclegacy.precision_assembler.recipe_selector"), previous,
                menu.recipes(), menu.selectedRecipeId().orElse(null));
        this.menu = menu;
    }

    @Override
    protected ItemStack icon(RecipeHolder<PrecisionAssemblerRecipe> holder) {
        return displayIcon(holder);
    }

    @Override
    protected Component localizedName(RecipeHolder<PrecisionAssemblerRecipe> holder) {
        return localizedNameOf(holder);
    }

    @Override
    protected List<Component> describe(RecipeHolder<PrecisionAssemblerRecipe> holder) {
        return describeRecipe(holder);
    }

    @Override
    protected void sendSelection(@Nullable ResourceLocation selection) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, selection == null ? 0 : this.menu.buttonIdForRecipe(selection));
        }
    }

    private static String internalName(RecipeHolder<PrecisionAssemblerRecipe> holder) {
        return LegacyRecipeNames.internalName(LegacyRecipeNames.PRECISION_ASSEMBLER, holder, holder.value().group());
    }

    /** GenericRecipe#getIcon: registerPair shows the first pool output, recycling recipes setIcon(broken item). */
    public static ItemStack displayIcon(RecipeHolder<PrecisionAssemblerRecipe> holder) {
        PrecisionAssemblerRecipe recipe = holder.value();
        if (internalName(holder).endsWith(".recycle") && !recipe.ingredients().isEmpty()) {
            PrecisionAssemblerRecipe.CountedIngredient first = recipe.ingredients().getFirst();
            return LegacyRecipeTooltip.firstItem(first.ingredient(), 1);
        }
        return recipe.outputs().isEmpty() ? ItemStack.EMPTY : recipe.outputs().getFirst().stack().copy();
    }

    public static Component localizedNameOf(RecipeHolder<PrecisionAssemblerRecipe> holder) {
        return LegacyRecipeNames.localizedName(internalName(holder), displayIcon(holder));
    }

    /** [parity K1] GenericRecipe#print. */
    static List<Component> describeRecipe(RecipeHolder<PrecisionAssemblerRecipe> holder) {
        PrecisionAssemblerRecipe recipe = holder.value();
        List<Component> lines = new ArrayList<>();
        LegacyRecipeTooltip.header(lines, localizedNameOf(holder), internalName(holder));
        LegacyRecipeTooltip.duration(lines, recipe.duration());
        LegacyRecipeTooltip.power(lines, recipe.power());
        LegacyRecipeTooltip.inputHeader(lines);
        for (PrecisionAssemblerRecipe.CountedIngredient ingredient : recipe.ingredients()) {
            LegacyRecipeTooltip.inputItem(lines, ingredient.ingredient(), ingredient.count());
        }
        for (PrecisionAssemblerRecipe.FluidStack fluid : recipe.inputFluids()) {
            LegacyRecipeTooltip.fluid(lines, fluid.type(), fluid.amount(), fluid.pressure());
        }
        LegacyRecipeTooltip.outputHeader(lines);
        List<PrecisionAssemblerRecipe.ChanceOutput> outputs = recipe.outputs();
        if (recipe.outputMode() == PrecisionAssemblerRecipe.OutputMode.INDEPENDENT) {
            // recycling: one ChanceOutput(stack, reclaim) per input
            for (int i = 0; i < outputs.size(); i++) {
                LegacyRecipeTooltip.chanceOutput(lines, outputs.get(i).stack(), recipe.outputWeight(i) / 100F);
            }
        } else if (outputs.size() == 1) {
            LegacyRecipeTooltip.chanceOutput(lines, outputs.getFirst().stack(), 1F);
        } else if (!outputs.isEmpty()) {
            LegacyRecipeTooltip.multiOutput(lines, outputs.stream().map(PrecisionAssemblerRecipe.ChanceOutput::stack).toList(),
                    IntStream.range(0, outputs.size()).map(recipe::outputWeight).boxed().toList(), null);
        }
        for (PrecisionAssemblerRecipe.FluidStack fluid : recipe.outputFluids()) {
            LegacyRecipeTooltip.fluid(lines, fluid.type(), fluid.amount(), fluid.pressure());
        }
        return lines;
    }
}
