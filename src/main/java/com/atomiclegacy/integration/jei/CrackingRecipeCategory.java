package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.CrackingRecipe;
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

/** [parity T4: JEI] CrackingHandler (NEIUniversalHandler): CrackingRecipes.getCrackingRecipesForNEI(): {100 mB, 200 mB steam} -> {out1, out2, 2 mB spent steam}. */
public class CrackingRecipeCategory extends NeiUniversalCategory<RecipeHolder<CrackingRecipe>> {
    public CrackingRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.CRACKING, Component.literal("Cracking"), stack(HbmBlocks.MACHINE_CATALYTIC_CRACKER.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<CrackingRecipe> holder) {
        CrackingRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(UniversalJeiRecipe.Slot.fluid(recipe.input().fluid(), 100));
        in.add(UniversalJeiRecipe.Slot.fluid(HbmFluids.byName("steam").orElse(HbmFluids.none()), 200));
        out.add(UniversalJeiRecipe.Slot.fluid(recipe.output1().fluid(), recipe.output1().amount()));
        if (!recipe.output2().fluid().isNone()) out.add(UniversalJeiRecipe.Slot.fluid(recipe.output2().fluid(), recipe.output2().amount()));
        out.add(UniversalJeiRecipe.Slot.fluid(HbmFluids.byName("spentsteam").orElse(HbmFluids.none()), 2));
        return recipe(in, out, stack(HbmBlocks.MACHINE_CATALYTIC_CRACKER.get()));
    }
}
