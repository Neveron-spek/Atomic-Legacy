package com.atomiclegacy.integration.jei;

import com.atomiclegacy.inventory.recipes.RockMillRecipe;
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

/** [parity T4: JEI] RockMillRecipeHandler (NEIGenericRecipeHandler over RockMillRecipes). */
public class RockMillRecipeCategory extends NeiGenericCategory<RecipeHolder<RockMillRecipe>> {
    public RockMillRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.ROCK_MILL, NeiUniversalCategory.blockName(HbmBlocks.MACHINE_ROCKMILL.get()), List.of(new ItemStack(HbmBlocks.MACHINE_ROCKMILL.get())), Extras.POWER, false);
    }

    @Override
    protected Layout layout(RecipeHolder<RockMillRecipe> holder) {
        RockMillRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(NeiUniversalCategory.slot(recipe.input().ingredient(), recipe.input().count()));
        recipe.inputFluid().ifPresent(fluid -> in.add(UniversalJeiRecipe.Slot.fluid(fluid.type(), fluid.amount())));
        out.add(UniversalJeiRecipe.Slot.weighted(recipe.outputs().stream().map(output -> output.stack().copy()).toList(),
                recipe.outputs().stream().map(RockMillRecipe.WeightedOutput::weight).toList()));
        recipe.outputFluid().ifPresent(fluid -> out.add(UniversalJeiRecipe.Slot.fluid(fluid.type(), fluid.amount())));
        return new Layout(in, out, List.of(), recipe.duration(), recipe.power());
    }
}
