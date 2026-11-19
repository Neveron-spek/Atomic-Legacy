package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.FractionTowerRecipe;
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

/** [parity T4: JEI] FractioningHandler (NEIUniversalHandler): FractionRecipes.getFractionRecipesForNEI(): 100 mB -> {out1, out2}. */
public class FractionTowerRecipeCategory extends NeiUniversalCategory<RecipeHolder<FractionTowerRecipe>> {
    public FractionTowerRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.FRACTION_TOWER, blockName(HbmBlocks.MACHINE_FRACTION_TOWER.get()), stack(HbmBlocks.MACHINE_FRACTION_TOWER.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<FractionTowerRecipe> holder) {
        FractionTowerRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(UniversalJeiRecipe.Slot.fluid(recipe.input().fluid(), 100));
        out.add(UniversalJeiRecipe.Slot.fluid(recipe.output1().fluid(), recipe.output1().amount()));
        out.add(UniversalJeiRecipe.Slot.fluid(recipe.output2().fluid(), recipe.output2().amount()));
        return recipe(in, out, stack(HbmBlocks.MACHINE_FRACTION_TOWER.get()));
    }
}
