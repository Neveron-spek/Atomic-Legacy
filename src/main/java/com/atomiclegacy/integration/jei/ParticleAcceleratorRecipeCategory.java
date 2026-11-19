package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.ParticleAcceleratorRecipe;
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

/** [parity T4: JEI] ParticleAcceleratorHandler (NEIUniversalHandler): ParticleAcceleratorRecipes.getRecipes(): {input1, input2} -> {output1, output2}; drawExtras momentum. */
public class ParticleAcceleratorRecipeCategory extends NeiUniversalCategory<RecipeHolder<ParticleAcceleratorRecipe>> {
    public ParticleAcceleratorRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.PARTICLE_ACCELERATOR, blockName(HbmBlocks.PA_DETECTOR.get()), stack(HbmBlocks.PA_DETECTOR.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<ParticleAcceleratorRecipe> holder) {
        ParticleAcceleratorRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(slot(recipe.input1(), 1));
        in.add(slot(recipe.input2(), 1));
        if (!recipe.output1().isEmpty()) out.add(slot(recipe.output1()));
        if (!recipe.output2().isEmpty()) out.add(slot(recipe.output2()));
        return recipe(in, out, stack(HbmBlocks.PA_DETECTOR.get()));
    }

    @Override
    protected void drawExtras(RecipeHolder<ParticleAcceleratorRecipe> holder, GuiGraphics graphics, Font font) {
        left(graphics, font, "Momentum: " + String.format(Locale.US, "%,d", holder.value().momentum()), 8, 52, 0x404040);
    }
}
