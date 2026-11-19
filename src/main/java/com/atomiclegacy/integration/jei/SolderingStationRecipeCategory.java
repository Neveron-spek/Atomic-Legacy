package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.SolderingStationRecipe;
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

/** [parity T4: JEI] SolderingStationHandler (NEIUniversalHandler): SolderingRecipes.getRecipes(): {toppings, pcb, solder, fluid} -> output; drawExtras ticks and HE/t. */
public class SolderingStationRecipeCategory extends NeiUniversalCategory<RecipeHolder<SolderingStationRecipe>> {
    public SolderingStationRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.SOLDERING_STATION, blockName(HbmBlocks.MACHINE_SOLDERING_STATION.get()), stack(HbmBlocks.MACHINE_SOLDERING_STATION.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<SolderingStationRecipe> holder) {
        SolderingStationRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        for (SolderingStationRecipe.CountedIngredient input : recipe.toppings()) in.add(slot(input.ingredient(), input.count()));
        for (SolderingStationRecipe.CountedIngredient input : recipe.pcb()) in.add(slot(input.ingredient(), input.count()));
        for (SolderingStationRecipe.CountedIngredient input : recipe.solder()) in.add(slot(input.ingredient(), input.count()));
        recipe.fluid().ifPresent(fluid -> in.add(UniversalJeiRecipe.Slot.fluid(fluid.fluid(), fluid.amount())));
        out.add(slot(recipe.result()));
        return recipe(in, out, stack(HbmBlocks.MACHINE_SOLDERING_STATION.get()));
    }

    @Override
    protected void drawExtras(RecipeHolder<SolderingStationRecipe> holder, GuiGraphics graphics, Font font) {
        right(graphics, font, String.format(Locale.US, "%,d", holder.value().duration()) + " ticks", 160, 43, 0x404040);
        right(graphics, font, String.format(Locale.US, "%,d", holder.value().consumption()) + " HE/t", 160, 55, 0x404040);
    }
}
