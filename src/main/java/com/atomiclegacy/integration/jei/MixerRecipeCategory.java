package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.MixerRecipe;
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

/** [parity T4: JEI] MixerHandler (NEIUniversalHandler): MixerRecipes.getRecipes(): {fluid1, fluid2, solid} -> fluid. */
public class MixerRecipeCategory extends NeiUniversalCategory<RecipeHolder<MixerRecipe>> {
    public MixerRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.MIXER, blockName(HbmBlocks.MACHINE_MIXER.get()), stack(HbmBlocks.MACHINE_MIXER.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<MixerRecipe> holder) {
        MixerRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        recipe.input1().ifPresent(fluid -> in.add(UniversalJeiRecipe.Slot.fluid(fluid.fluid(), fluid.amount(), fluid.pressure())));
        recipe.input2().ifPresent(fluid -> in.add(UniversalJeiRecipe.Slot.fluid(fluid.fluid(), fluid.amount(), fluid.pressure())));
        recipe.solidInput().ifPresent(solid -> in.add(slot(solid.ingredient(), solid.count())));
        out.add(UniversalJeiRecipe.Slot.fluid(recipe.output().fluid(), recipe.output().amount()));
        return recipe(in, out, stack(HbmBlocks.MACHINE_MIXER.get()));
    }
}
