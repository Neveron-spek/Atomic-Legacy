package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.RotaryFurnaceRecipe;
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

/** [parity T4: JEI] RotaryFurnaceHandler (NEIUniversalHandler): RotaryFurnaceRecipes.getRecipes(): {ingredients..., fluid} -> liquid scraps; drawExtras ticks and steam. */
public class RotaryFurnaceRecipeCategory extends NeiUniversalCategory<RecipeHolder<RotaryFurnaceRecipe>> {
    public RotaryFurnaceRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.ROTARY_FURNACE, blockName(HbmBlocks.MACHINE_ROTARY_FURNACE.get()), stack(HbmBlocks.MACHINE_ROTARY_FURNACE.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<RotaryFurnaceRecipe> holder) {
        RotaryFurnaceRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        for (RotaryFurnaceRecipe.CountedIngredient input : recipe.inputs()) in.add(slot(input.ingredient(), input.count()));
        if (recipe.hasFluid()) in.add(UniversalJeiRecipe.Slot.fluid(recipe.fluid().fluid(), recipe.fluid().amount()));
        out.add(slot(ScrapsItem.create(recipe.output().stack(), true)));
        return recipe(in, out, stack(HbmBlocks.MACHINE_ROTARY_FURNACE.get()));
    }

    @Override
    protected void drawExtras(RecipeHolder<RotaryFurnaceRecipe> holder, GuiGraphics graphics, Font font) {
        RotaryFurnaceRecipe recipe = holder.value();
        String steam = HbmFluids.byName("steam").map(fluid -> Component.translatable(fluid.translationKey()).getString()).orElse("");
        right(graphics, font, String.format(Locale.US, "%,d", recipe.duration()) + " ticks", 160, 43, 0x404040);
        right(graphics, font, steam + ": " + String.format(Locale.US, "%,d", recipe.steam()) + " mB/t", 160, 55, 0x404040);
    }
}
