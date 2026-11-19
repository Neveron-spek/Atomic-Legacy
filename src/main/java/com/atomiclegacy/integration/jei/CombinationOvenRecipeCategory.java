package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.CombinationOvenRecipe;
import com.atomiclegacy.items.ScrapsItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** [parity T4: JEI] CombinationHandler (NEIUniversalHandler): CombinationRecipes.getRecipes(): input -> {item, fluid}. */
public class CombinationOvenRecipeCategory extends NeiUniversalCategory<RecipeHolder<CombinationOvenRecipe>> {
    public CombinationOvenRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.COMBINATION_OVEN, blockName(HbmBlocks.FURNACE_COMBINATION.get()), stack(HbmBlocks.FURNACE_COMBINATION.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<CombinationOvenRecipe> holder) {
        CombinationOvenRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(slot(recipe.input(), 1));
        if (recipe.hasItemOutput()) out.add(slot(recipe.output()));
        if (recipe.hasFluid()) out.add(UniversalJeiRecipe.Slot.fluid(recipe.fluid().fluid(), recipe.fluid().amount()));
        return recipe(in, out, stack(HbmBlocks.FURNACE_COMBINATION.get()));
    }
}
