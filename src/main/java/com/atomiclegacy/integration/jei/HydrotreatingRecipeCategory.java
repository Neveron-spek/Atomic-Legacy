package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.HydrotreatingRecipe;
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

/** [parity T4: JEI] HydrotreatingHandler (NEIUniversalHandler): HydrotreatingRecipes.getRecipes(): {1000 mB, hydrogen x10 at 1 PU} -> outputs x10. */
public class HydrotreatingRecipeCategory extends NeiUniversalCategory<RecipeHolder<HydrotreatingRecipe>> {
    public HydrotreatingRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.HYDROTREATING, Component.literal("Hydrotreating"), stack(HbmBlocks.MACHINE_HYDROTREATER.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<HydrotreatingRecipe> holder) {
        HydrotreatingRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(UniversalJeiRecipe.Slot.fluid(recipe.input().fluid(), 1000));
        in.add(UniversalJeiRecipe.Slot.fluid(recipe.hydrogen().fluid(), recipe.hydrogen().amount() * 10, 1));
        out.add(UniversalJeiRecipe.Slot.fluid(recipe.output1().fluid(), recipe.output1().amount() * 10));
        out.add(UniversalJeiRecipe.Slot.fluid(recipe.output2().fluid(), recipe.output2().amount() * 10));
        return recipe(in, out, stack(HbmBlocks.MACHINE_HYDROTREATER.get()));
    }
}
