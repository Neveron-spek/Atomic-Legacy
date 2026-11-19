package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.VacuumDistillRecipe;
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

/** [parity T4: JEI] VacuumRecipeHandler (NEIUniversalHandler): VacuumRefineryRecipes.getVacuumRecipe(): 1000 mB at 2 PU -> 4 outputs x10. */
public class VacuumDistillRecipeCategory extends NeiUniversalCategory<RecipeHolder<VacuumDistillRecipe>> {
    public VacuumDistillRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.VACUUM_DISTILL, Component.literal("Vacuum Refinery"), stack(HbmBlocks.MACHINE_VACUUM_DISTILL.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<VacuumDistillRecipe> holder) {
        VacuumDistillRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(UniversalJeiRecipe.Slot.fluid(recipe.input().fluid(), 1000, 2));
        for (VacuumDistillRecipe.FluidOutput output : recipe.outputs()) out.add(UniversalJeiRecipe.Slot.fluid(output.fluid(), output.amount() * 10));
        return recipe(in, out, stack(HbmBlocks.MACHINE_VACUUM_DISTILL.get()));
    }
}
