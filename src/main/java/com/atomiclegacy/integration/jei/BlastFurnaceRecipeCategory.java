package com.atomiclegacy.integration.jei;

import com.atomiclegacy.inventory.recipes.BlastFurnaceRecipe;
import com.atomiclegacy.items.BlueprintItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/** [parity T4: JEI] BlastFurnaceHandler (NEIGenericRecipeHandler over BlastFurnaceRecipesNT, GenericRecipeNoPower). */
public class BlastFurnaceRecipeCategory extends NeiGenericCategory<RecipeHolder<BlastFurnaceRecipe>> {
    public BlastFurnaceRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.BLAST_FURNACE, NeiUniversalCategory.blockName(HbmBlocks.MACHINE_BLAST_FURNACE.get()), List.of(new ItemStack(HbmBlocks.MACHINE_BLAST_FURNACE.get())), Extras.NO_POWER, false);
    }

    @Override
    protected Layout layout(RecipeHolder<BlastFurnaceRecipe> holder) {
        BlastFurnaceRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(NeiUniversalCategory.slot(recipe.inputA(), recipe.inputACount()));
        in.add(NeiUniversalCategory.slot(recipe.inputB(), recipe.inputBCount()));
        out.add(UniversalJeiRecipe.Slot.of(recipe.result().copy()));
        if (!recipe.byproduct().isEmpty()) out.add(UniversalJeiRecipe.Slot.of(recipe.byproduct().copy()));
        return new Layout(in, out, List.of(), recipe.duration(), 0);
    }
}
