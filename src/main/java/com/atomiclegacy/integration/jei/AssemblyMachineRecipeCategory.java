package com.atomiclegacy.integration.jei;

import com.atomiclegacy.inventory.recipes.AssemblyMachineRecipe;
import com.atomiclegacy.items.BlueprintItem;
import com.atomiclegacy.registry.HbmBlocks;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity T4: JEI] AssemblyMachineRecipeHandler (NEIGenericRecipeHandler with the assembler X offsets for 10+ inputs).
 * The second constructor also serves the TACZ ammo assembler integration category.
 */
public class AssemblyMachineRecipeCategory extends NeiGenericCategory<RecipeHolder<AssemblyMachineRecipe>> {
    public AssemblyMachineRecipeCategory(IGuiHelper helper) {
        this(helper, HbmJeiPlugin.ASSEMBLY_MACHINE, NeiUniversalCategory.blockName(HbmBlocks.MACHINE_ASSEMBLY_MACHINE.get()),
                new ItemStack(HbmBlocks.MACHINE_ASSEMBLY_MACHINE.get()));
    }

    public AssemblyMachineRecipeCategory(IGuiHelper helper, RecipeType<RecipeHolder<AssemblyMachineRecipe>> recipeType,
                                         Component title, ItemStack iconStack) {
        super(helper, recipeType, title, List.of(iconStack), Extras.POWER, true);
    }

    @Override
    protected Layout layout(RecipeHolder<AssemblyMachineRecipe> holder) {
        AssemblyMachineRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        for (AssemblyMachineRecipe.CountedIngredient input : recipe.ingredients()) {
            in.add(NeiUniversalCategory.slot(input.ingredient(), input.count()));
        }
        for (AssemblyMachineRecipe.AssemblyFluidStack fluid : recipe.inputFluids()) {
            in.add(UniversalJeiRecipe.Slot.fluid(fluid.type(), fluid.amount(), fluid.pressure()));
        }
        if (!recipe.outputs().isEmpty()) {
            out.add(UniversalJeiRecipe.Slot.weighted(recipe.outputs().stream().map(output -> output.stack().copy()).toList(),
                    recipe.outputs().stream().map(AssemblyMachineRecipe.ChanceOutput::weight).toList()));
        } else if (!recipe.result().isEmpty()) {
            out.add(UniversalJeiRecipe.Slot.of(recipe.result().copy()));
        }
        for (AssemblyMachineRecipe.AssemblyFluidStack fluid : recipe.outputFluids()) {
            out.add(UniversalJeiRecipe.Slot.fluid(fluid.type(), fluid.amount(), fluid.pressure()));
        }
        return new Layout(in, out, BlueprintItem.activeRecipePools(recipe.blueprintPools()), recipe.duration(), recipe.power());
    }
}
