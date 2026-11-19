package com.atomiclegacy.integration.jei;

import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.inventory.recipes.CrystallizerRecipe;
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

/** [parity T4: JEI] CrystallizerRecipeHandler (NEIUniversalHandler): CrystallizerRecipes.getRecipes(): {acid, item} -> output, scrap_plastic skipped. */
public class CrystallizerRecipeCategory extends NeiUniversalCategory<RecipeHolder<CrystallizerRecipe>> {
    public CrystallizerRecipeCategory(IGuiHelper helper) {
        super(helper, HbmJeiPlugin.CRYSTALLIZER, Component.literal("Acidizer"), stack(HbmBlocks.MACHINE_CRYSTALLIZER.get()));
    }

    @Override
    protected UniversalJeiRecipe layout(RecipeHolder<CrystallizerRecipe> holder) {
        CrystallizerRecipe recipe = holder.value();
        List<UniversalJeiRecipe.Slot> in = new ArrayList<>();
        List<UniversalJeiRecipe.Slot> out = new ArrayList<>();
        in.add(UniversalJeiRecipe.Slot.fluid(recipe.acid().type(), recipe.acid().amount()));
        in.add(slot(recipe.ingredient(), recipe.inputCount()));
        out.add(slot(recipe.result()));
        return recipe(in, out, stack(HbmBlocks.MACHINE_CRYSTALLIZER.get()));
    }

    @Override
    protected void drawExtras(RecipeHolder<CrystallizerRecipe> holder, GuiGraphics graphics, Font font) {
        float productivity = holder.value().productivity();
        if (productivity > 0) {
            left(graphics, font, "Effectiveness: +" + Math.min((int) (productivity * 100), 99) + "% per level", 8, 52, 0x404040);
        }
    }

    /** CrystallizerRecipes.getRecipes: "if(stack.item == ModItems.scrap_plastic) continue;" (item keys only; the port flattened scrap_plastic metas into scrap_plastic_*). */
    static List<RecipeHolder<CrystallizerRecipe>> visible(List<RecipeHolder<CrystallizerRecipe>> recipes) {
        return recipes.stream().filter(holder -> {
            ItemStack[] items = holder.value().ingredient().getItems();
            return !(items.length > 0 && java.util.Arrays.stream(items).allMatch(stack -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().startsWith("scrap_plastic")));
        }).toList();
    }
}
