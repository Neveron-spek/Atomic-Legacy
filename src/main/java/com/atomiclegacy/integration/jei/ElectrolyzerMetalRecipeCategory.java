package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.ElectrolyzerMetalRecipe;
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

/** [parity T4: JEI] ElectrolyserMetalHandler (NEIUniversalHandler): ElectrolyserMetalRecipes.getRecipes(): {item, 100 mB nitric acid} -> liquid scraps + byproducts. */
public class ElectrolyzerMetalRecipeCategory extends NeiUniversalCategory<RecipeHolder<ElectrolyzerMetalRecipe>> {
    public ElectrolyzerMetalRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.ELECTROLYZER_METAL, blockName(HbmBlocks.MACHINE_ELECTROLYSER.get()), stack(HbmBlocks.MACHINE_ELECTROLYSER.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<ElectrolyzerMetalRecipe> holder) {
        ElectrolyzerMetalRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(slot(recipe.ingredient(), 1));
        in.add(UniversalJeiRecipe.Slot.fluid(HbmFluids.byName("nitric_acid").orElse(HbmFluids.none()), 100));
        recipe.output1Stack().ifPresent(stack -> out.add(slot(ScrapsItem.create(stack, true))));
        recipe.output2Stack().ifPresent(stack -> out.add(slot(ScrapsItem.create(stack, true))));
        for (ItemStack byproduct : recipe.byproducts()) out.add(slot(byproduct));
        return recipe(in, out, stack(HbmBlocks.MACHINE_ELECTROLYSER.get()));
    }
}
