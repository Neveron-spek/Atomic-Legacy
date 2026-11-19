package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.ArcWelderRecipe;
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

/** [parity T4: JEI] ArcWelderHandler (NEIUniversalHandler): ArcWelderRecipes.getRecipes(): {ingredients..., fluid} -> output; drawExtras ticks and HE/t. */
public class ArcWelderRecipeCategory extends NeiUniversalCategory<RecipeHolder<ArcWelderRecipe>> {
    public ArcWelderRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.ARC_WELDER, blockName(HbmBlocks.MACHINE_ARC_WELDER.get()), stack(HbmBlocks.MACHINE_ARC_WELDER.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<ArcWelderRecipe> holder) {
        ArcWelderRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        for (ArcWelderRecipe.CountedIngredient input : recipe.inputs()) in.add(slot(input.ingredient(), input.count()));
        recipe.fluid().ifPresent(fluid -> in.add(UniversalJeiRecipe.Slot.fluid(fluid.fluid(), fluid.amount())));
        out.add(slot(recipe.result()));
        return recipe(in, out, stack(HbmBlocks.MACHINE_ARC_WELDER.get()));
    }

    @Override
    protected void drawExtras(RecipeHolder<ArcWelderRecipe> holder, GuiGraphics graphics, Font font) {
        right(graphics, font, shortNumber(holder.value().duration()) + " ticks", 164, 45, 0x404040);
        right(graphics, font, shortNumber(holder.value().consumption()) + "HE/t", 164, 57, 0x404040);
    }
}
