package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.AmmoPressRecipe;
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

/** [parity T4: JEI] AmmoPressHandler (NEIUniversalHandler): AmmoPressRecipes.getRecipes(): the non-null cells of the 3x3 input -> output. */
public class AmmoPressRecipeCategory extends NeiUniversalCategory<RecipeHolder<AmmoPressRecipe>> {
    public AmmoPressRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.AMMO_PRESS, blockName(HbmBlocks.MACHINE_AMMO_PRESS.get()), stack(HbmBlocks.MACHINE_AMMO_PRESS.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<AmmoPressRecipe> holder) {
        AmmoPressRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        for (AmmoPressRecipe.SlotIngredient input : recipe.input()) {
            if (!input.isEmpty()) in.add(slot(input.ingredient().orElseThrow(), input.count()));
        }
        out.add(slot(recipe.result()));
        return recipe(in, out, stack(HbmBlocks.MACHINE_AMMO_PRESS.get()));
    }
}
