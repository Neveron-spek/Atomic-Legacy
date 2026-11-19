package com.atomiclegacy.integration.jei;

import com.atomiclegacy.inventory.recipes.PurexRecipe;
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

/** [parity T4: JEI] PUREXRecipeHandler (NEIGenericRecipeHandler over PUREXRecipes, PUREXRecipe#printNEIExtras). */
public class PurexRecipeCategory extends NeiGenericCategory<RecipeHolder<PurexRecipe>> {
    public PurexRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.PUREX, NeiUniversalCategory.blockName(HbmBlocks.MACHINE_PUREX.get()), List.of(new ItemStack(HbmBlocks.MACHINE_PUREX.get())), Extras.PUREX, false);
    }

    @Override
    protected Layout layout(RecipeHolder<PurexRecipe> holder) {
        PurexRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        for (PurexRecipe.CountedIngredient input : recipe.inputItems()) in.add(NeiUniversalCategory.slot(input.ingredient(), input.count()));
        for (PurexRecipe.PurexFluidStack fluid : recipe.inputFluids()) in.add(UniversalJeiRecipe.Slot.fluid(fluid.type(), fluid.amount(), fluid.pressure()));
        for (PurexRecipe.PurexItemOutput output : recipe.outputItems()) out.add(UniversalJeiRecipe.Slot.chance(output.stack(), output.chance()));
        for (PurexRecipe.PurexFluidStack fluid : recipe.outputFluids()) out.add(UniversalJeiRecipe.Slot.fluid(fluid.type(), fluid.amount(), fluid.pressure()));
        return new Layout(in, out, List.of(), recipe.duration(), recipe.power());
    }
}
