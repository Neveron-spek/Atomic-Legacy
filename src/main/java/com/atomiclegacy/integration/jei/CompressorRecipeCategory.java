package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.CompressorRecipe;
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

/** [parity T4: JEI] CompressorHandler (NEIUniversalHandler): CompressorRecipes.getRecipes(): pressurized fluid -> fluid; same-type entries are skipped. */
public class CompressorRecipeCategory extends NeiUniversalCategory<RecipeHolder<CompressorRecipe>> {
    public CompressorRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.COMPRESSOR, blockName(HbmBlocks.MACHINE_COMPRESSOR.get()), stack(HbmBlocks.MACHINE_COMPRESSOR.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<CompressorRecipe> holder) {
        CompressorRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(UniversalJeiRecipe.Slot.fluid(recipe.input().fluid(), recipe.input().amount(), recipe.input().pressure()));
        out.add(UniversalJeiRecipe.Slot.fluid(recipe.output().fluid(), recipe.output().amount(), recipe.output().pressure()));
        return recipe(in, out, stack(HbmBlocks.MACHINE_COMPRESSOR.get()), stack(HbmBlocks.MACHINE_COMPRESSOR_COMPACT.get()));
    }

    /** CompressorRecipes.getRecipes: "if(input.getItemDamage() == output.getItemDamage()) continue;". */
    static List<RecipeHolder<CompressorRecipe>> visible(List<RecipeHolder<CompressorRecipe>> recipes) {
        return recipes.stream().filter(holder -> holder.value().input().fluid() != holder.value().output().fluid()).toList();
    }
}
