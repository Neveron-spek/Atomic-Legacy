package com.atomiclegacy.integration.jei;

import com.atomiclegacy.inventory.recipes.SupercomputerRecipe;
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

/** [parity T4: JEI] SuperComputerRecipeHandler (NEIGenericRecipeHandler over SuperComputerRecipes). */
public class SupercomputerRecipeCategory extends NeiGenericCategory<RecipeHolder<SupercomputerRecipe>> {
    public SupercomputerRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.SUPERCOMPUTER, NeiUniversalCategory.blockName(HbmBlocks.MACHINE_SUPERCOMPUTER.get()), List.of(new ItemStack(HbmBlocks.MACHINE_SUPERCOMPUTER.get())), Extras.POWER, false);
    }

    @Override
    protected Layout layout(RecipeHolder<SupercomputerRecipe> holder) {
        SupercomputerRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        for (SupercomputerRecipe.CountedIngredient input : recipe.ingredients()) in.add(NeiUniversalCategory.slot(input.ingredient(), input.count()));
        for (SupercomputerRecipe.FluidStack fluid : recipe.inputFluids()) in.add(UniversalJeiRecipe.Slot.fluid(fluid.type(), fluid.amount(), fluid.pressure()));
        out.add(UniversalJeiRecipe.Slot.weighted(recipe.outputs().stream().map(output -> output.stack().copy()).toList(),
                recipe.outputs().stream().map(SupercomputerRecipe.ChanceOutput::weight).toList()));
        for (SupercomputerRecipe.FluidStack fluid : recipe.outputFluids()) out.add(UniversalJeiRecipe.Slot.fluid(fluid.type(), fluid.amount(), fluid.pressure()));
        return new Layout(in, out, List.of(), recipe.duration(), recipe.power());
    }
}
