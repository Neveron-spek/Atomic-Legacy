package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.FusionBreederFluidRecipe;
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

/** [parity T4: JEI] FusionBreederHandler (NEIUniversalHandler): FluidBreederRecipes.getRecipes(): fluid -> fluid. */
public class FusionBreederFluidRecipeCategory extends NeiUniversalCategory<RecipeHolder<FusionBreederFluidRecipe>> {
    public FusionBreederFluidRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.FUSION_BREEDER_FLUID, blockName(HbmBlocks.FUSION_BREEDER.get()), stack(HbmBlocks.FUSION_BREEDER.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<FusionBreederFluidRecipe> holder) {
        FusionBreederFluidRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(UniversalJeiRecipe.Slot.fluid(recipe.input().fluid(), recipe.input().amount()));
        out.add(UniversalJeiRecipe.Slot.fluid(recipe.output().fluid(), recipe.output().amount()));
        return recipe(in, out, stack(HbmBlocks.FUSION_BREEDER.get()));
    }
}
