package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.ExposureChamberRecipe;
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

/** [parity T4: JEI] ExposureChamberHandler (NEIUniversalHandler): ExposureChamberRecipes.getRecipes(): {ingredient x8, particle} -> output x8. */
public class ExposureChamberRecipeCategory extends NeiUniversalCategory<RecipeHolder<ExposureChamberRecipe>> {
    public ExposureChamberRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.EXPOSURE_CHAMBER, blockName(HbmBlocks.MACHINE_EXPOSURE_CHAMBER.get()), stack(HbmBlocks.MACHINE_EXPOSURE_CHAMBER.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<ExposureChamberRecipe> holder) {
        ExposureChamberRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(slot(recipe.ingredient(), 8));
        in.add(slot(recipe.particle(), 1));
        out.add(slot(recipe.output().copyWithCount(8)));
        return recipe(in, out, stack(HbmBlocks.MACHINE_EXPOSURE_CHAMBER.get()));
    }
}
