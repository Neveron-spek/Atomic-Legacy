package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.LiquefactionRecipe;
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

/** [parity T4: JEI] LiquefactionHandler (NEIUniversalHandler): LiquefactionRecipes.getRecipes(): item -> fluid. */
public class LiquefactionRecipeCategory extends NeiUniversalCategory<RecipeHolder<LiquefactionRecipe>> {
    public LiquefactionRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.LIQUEFACTION, blockName(HbmBlocks.MACHINE_LIQUEFACTOR.get()), stack(HbmBlocks.MACHINE_LIQUEFACTOR.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<LiquefactionRecipe> holder) {
        LiquefactionRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(slot(recipe.ingredient(), 1));
        out.add(UniversalJeiRecipe.Slot.fluid(recipe.output().fluid(), recipe.output().amount()));
        return recipe(in, out, stack(HbmBlocks.MACHINE_LIQUEFACTOR.get()));
    }
}
