package com.atomiclegacy.integration.jei;

import com.atomiclegacy.inventory.recipes.SpaceAssemblerRecipe;
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

/** [parity T4: JEI] SpaceAssemblerHandler (NEIGenericRecipeHandler over SpaceAssemblerRecipes, GenericRecipeNoPower). */
public class SpaceAssemblerRecipeCategory extends NeiGenericCategory<RecipeHolder<SpaceAssemblerRecipe>> {
    public SpaceAssemblerRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.SPACE_ASSEMBLER, Component.translatable(HbmItems.SATELLITE_SCIENCE_ASSEMBLER.get().getDescriptionId()), List.of(new ItemStack(HbmItems.SATELLITE_SCIENCE_ASSEMBLER.get())), Extras.NO_POWER, false);
    }

    @Override
    protected Layout layout(RecipeHolder<SpaceAssemblerRecipe> holder) {
        SpaceAssemblerRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(NeiUniversalCategory.slot(recipe.ingredient(), recipe.inputCount()));
        for (ItemStack result : recipe.results()) out.add(UniversalJeiRecipe.Slot.of(result.copy()));
        return new Layout(in, out, List.of(), recipe.duration(), 0);
    }
}
