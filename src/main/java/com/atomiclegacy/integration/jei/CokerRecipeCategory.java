package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.CokerRecipe;
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

/** [parity T4: JEI] CokingHandler (NEIUniversalHandler): CokerRecipes.getRecipes(): fluid -> {solid, byproduct}. */
public class CokerRecipeCategory extends NeiUniversalCategory<RecipeHolder<CokerRecipe>> {
    public CokerRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.COKER, blockName(HbmBlocks.MACHINE_COKER.get()), stack(HbmBlocks.MACHINE_COKER.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<CokerRecipe> holder) {
        CokerRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(UniversalJeiRecipe.Slot.fluid(recipe.input().fluid(), recipe.input().amount()));
        if (!recipe.output().isEmpty()) out.add(slot(recipe.output()));
        if (recipe.hasByproduct()) out.add(UniversalJeiRecipe.Slot.fluid(recipe.byproduct().fluid(), recipe.byproduct().amount()));
        return recipe(in, out, stack(HbmBlocks.MACHINE_COKER.get()));
    }
}
