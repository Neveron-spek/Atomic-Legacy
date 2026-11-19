package com.atomiclegacy.integration.jei;

import com.atomiclegacy.inventory.recipes.SilexRecipe;
import com.atomiclegacy.items.FluidIconItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.util.Wavelength;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;

/**
 * [parity T4: JEI] SILEXRecipeHandler: gui_nei_silex.png (default TemplateRecipeHandler background), input 12,24,
 * outputs at x 68 (first sep = size > 4 ? 3 : 2) and 116, chances at relx + 18, rely + 4, the integer
 * fluidProduced / fluidConsumed ratio centred on 52,43 and the wavelength centred on 33,8.
 */
public class SilexRecipeCategory extends NeiTemplateCategory<RecipeHolder<SilexRecipe>> {
    public SilexRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.SILEX, Component.literal("SILEX"), new ItemStack(HbmBlocks.MACHINE_SILEX.get()), "gui_nei_silex");
    }

    private static int[] position(int index, int size) {
        int sep = size > 4 ? 3 : 2;
        if (index < sep) {
            return new int[] {68, 24 + index * 18 - 9 * ((Math.min(size, sep) + 1) / 2)};
        }
        return new int[] {116, 24 + (index - sep) * 18 - 9 * ((Math.min(size - sep, sep) + 1) / 2)};
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<SilexRecipe> holder, IFocusGroup focuses) {
        SilexRecipe recipe = holder.value();
        if (recipe.ingredient().isPresent()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 12, 24).addItemStacks(Arrays.stream(recipe.ingredient().get().getItems()).map(ItemStack::copy).toList());
        } else if (recipe.fluidInput().isPresent()) {
            HbmFluids.byName(recipe.fluidInput().get()).ifPresent(fluid -> builder.addSlot(RecipeIngredientRole.INPUT, 12, 24).addItemStack(FluidIconItem.forFluid(fluid)));
        }
        int size = recipe.outputs().size();
        for (int i = 0; i < size; i++) {
            int[] pos = position(i, size);
            builder.addSlot(RecipeIngredientRole.OUTPUT, pos[0], pos[1]).addItemStack(recipe.outputs().get(i).stack().copy());
        }
    }

    @Override
    public void draw(RecipeHolder<SilexRecipe> holder, IRecipeSlotsView view, GuiGraphics graphics, double mouseX, double mouseY) {
        SilexRecipe recipe = holder.value();
        Font font = Minecraft.getInstance().font;
        int size = recipe.outputs().size();
        double weight = 0;
        for (int i = 0; i < size; i++) weight += recipe.outputs().get(i).weight();
        for (int i = 0; i < size; i++) {
            double chance = 100 * recipe.outputs().get(i).weight() / weight;
            int[] pos = position(i, size);
            graphics.drawString(font, ((int) (chance * 10D) / 10D) + "%", pos[0] + 18, pos[1] + 4, 0x404040, false);
        }
        double produced = recipe.fluidConsumed() == 0 ? 0 : recipe.fluidProduced() / recipe.fluidConsumed(); // int division as in 1.7.10
        String am = ((int) (produced * 10D) / 10D) + "x";
        graphics.drawString(font, am, 52 - font.width(am) / 2, 43, 0x404040, false);
        Wavelength wavelength = recipe.wavelength();
        Component text = wavelength == Wavelength.NULL
                ? Component.literal("N/A").withStyle(net.minecraft.ChatFormatting.WHITE)
                : Component.translatable(wavelength.translationKey()).withStyle(wavelength.textColor());
        graphics.drawString(font, text, 33 - font.width(text) / 2, 8, 0x404040, false);
    }
}
