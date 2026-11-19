package com.atomiclegacy.integration.jei;

import com.atomiclegacy.inventory.recipes.PrecisionAssemblerRecipe;
import com.atomiclegacy.items.BlueprintItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/** [parity T4: JEI] PrecAssRecipeHandler (NEIGenericRecipeHandler over PrecAssRecipes). */
public class PrecisionAssemblerRecipeCategory extends NeiGenericCategory<RecipeHolder<PrecisionAssemblerRecipe>> {
    public PrecisionAssemblerRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.PRECISION_ASSEMBLER, NeiUniversalCategory.blockName(HbmBlocks.MACHINE_PRECASS.get()), List.of(new ItemStack(HbmBlocks.MACHINE_PRECASS.get())), Extras.POWER, false);
    }

    @Override
    protected Layout layout(RecipeHolder<PrecisionAssemblerRecipe> holder) {
        PrecisionAssemblerRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        for (PrecisionAssemblerRecipe.CountedIngredient input : recipe.ingredients()) in.add(NeiUniversalCategory.slot(input.ingredient(), input.count()));
        for (PrecisionAssemblerRecipe.FluidStack fluid : recipe.inputFluids()) in.add(UniversalJeiRecipe.Slot.fluid(fluid.type(), fluid.amount(), fluid.pressure()));
        if (recipe.outputMode() == PrecisionAssemblerRecipe.OutputMode.WEIGHTED) {
            out.add(UniversalJeiRecipe.Slot.weighted(recipe.outputs().stream().map(output -> output.stack().copy()).toList(),
                    recipe.outputs().stream().map(PrecisionAssemblerRecipe.ChanceOutput::weight).toList()));
        } else {
            for (int i = 0; i < recipe.outputs().size(); i++) {
                out.add(UniversalJeiRecipe.Slot.chance(recipe.outputs().get(i).stack(), recipe.outputWeight(i) / 100F));
            }
        }
        for (PrecisionAssemblerRecipe.FluidStack fluid : recipe.outputFluids()) out.add(UniversalJeiRecipe.Slot.fluid(fluid.type(), fluid.amount(), fluid.pressure()));
        return new Layout(in, out, BlueprintItem.activeRecipePools(recipe.blueprintPools()), recipe.duration(), recipe.power());
    }
}
