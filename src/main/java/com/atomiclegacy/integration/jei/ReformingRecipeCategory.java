package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.ReformingRecipe;
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

/** [parity T4: JEI] ReformingHandler (NEIUniversalHandler): ReformingRecipes.getRecipes(): 1000 mB -> outputs x10. */
public class ReformingRecipeCategory extends NeiUniversalCategory<RecipeHolder<ReformingRecipe>> {
    public ReformingRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.REFORMING, Component.literal("Reforming"), stack(HbmBlocks.MACHINE_CATALYTIC_REFORMER.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<ReformingRecipe> holder) {
        ReformingRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(UniversalJeiRecipe.Slot.fluid(recipe.input().fluid(), 1000));
        out.add(UniversalJeiRecipe.Slot.fluid(recipe.output1().fluid(), recipe.output1().amount() * 10));
        out.add(UniversalJeiRecipe.Slot.fluid(recipe.output2().fluid(), recipe.output2().amount() * 10));
        out.add(UniversalJeiRecipe.Slot.fluid(recipe.output3().fluid(), recipe.output3().amount() * 10));
        return recipe(in, out, stack(HbmBlocks.MACHINE_CATALYTIC_REFORMER.get()));
    }
}
