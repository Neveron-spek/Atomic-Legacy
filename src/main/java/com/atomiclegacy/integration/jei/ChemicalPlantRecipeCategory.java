package com.atomiclegacy.integration.jei;

import com.atomiclegacy.inventory.recipes.ChemicalPlantRecipe;
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

/** [parity T4: JEI] ChemicalPlantRecipeHandler (NEIGenericRecipeHandler over ChemicalPlantRecipes). */
public class ChemicalPlantRecipeCategory extends NeiGenericCategory<RecipeHolder<ChemicalPlantRecipe>> {
    public ChemicalPlantRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.CHEMICAL_PLANT, NeiUniversalCategory.blockName(HbmBlocks.MACHINE_CHEMICAL_PLANT.get()), List.of(new ItemStack(HbmBlocks.MACHINE_CHEMICAL_PLANT.get()), new ItemStack(HbmBlocks.MACHINE_CHEMICAL_FACTORY.get())), Extras.POWER, false);
    }

    @Override
    protected Layout layout(RecipeHolder<ChemicalPlantRecipe> holder) {
        ChemicalPlantRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        for (ChemicalPlantRecipe.CountedIngredient input : recipe.inputItems()) in.add(NeiUniversalCategory.slot(input.ingredient(), input.count()));
        for (ChemicalPlantRecipe.ChemicalFluidStack fluid : recipe.inputFluids()) in.add(UniversalJeiRecipe.Slot.fluid(fluid.type(), fluid.amount(), fluid.pressure()));
        for (ChemicalPlantRecipe.ItemOutput output : recipe.outputs()) {
            // IOutput#getAllPossibilities: a weighted pool cycles through its entries with the percentage as tooltip
            if (output.multi()) {
                List<ItemStack> stacks = new ArrayList<>();
                List<Integer> weights = new ArrayList<>();
                for (ChemicalPlantRecipe.ItemOutput.Entry entry : output.pool()) {
                    stacks.add(entry.stack().copy());
                    weights.add(entry.weight());
                }
                out.add(UniversalJeiRecipe.Slot.weighted(stacks, weights));
            } else {
                out.add(UniversalJeiRecipe.Slot.chance(output.pool().getFirst().stack().copy(), output.pool().getFirst().chance()));
            }
        }
        for (ChemicalPlantRecipe.ChemicalFluidStack fluid : recipe.outputFluids()) out.add(UniversalJeiRecipe.Slot.fluid(fluid.type(), fluid.amount(), fluid.pressure()));
        return new Layout(in, out, BlueprintItem.activeRecipePools(recipe.blueprintPools()), recipe.duration(), recipe.power());
    }
}
