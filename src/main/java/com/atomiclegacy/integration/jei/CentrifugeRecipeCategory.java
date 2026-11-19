package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.CentrifugeRecipe;
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

/** [parity T4: JEI] CentrifugeRecipeHandler (NEIUniversalHandler): CentrifugeRecipes.getRecipes(): AStack -> ItemStack[]. */
public class CentrifugeRecipeCategory extends NeiUniversalCategory<RecipeHolder<CentrifugeRecipe>> {
    public CentrifugeRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.CENTRIFUGE, Component.literal("Centrifuge"), stack(HbmBlocks.MACHINE_CENTRIFUGE.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<CentrifugeRecipe> holder) {
        CentrifugeRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(slot(recipe.ingredient(), recipe.inputCount()));
        for (ItemStack result : recipe.results()) out.add(slot(result));
        return recipe(in, out, stack(HbmBlocks.MACHINE_CENTRIFUGE.get()));
    }
}
