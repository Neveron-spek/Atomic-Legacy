package com.atomiclegacy.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.atomiclegacy.inventory.container.FusionMachineMenu;
import com.atomiclegacy.inventory.recipes.FusionRecipe;
import com.atomiclegacy.inventory.recipes.LegacyRecipeNames;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** [parity K1] GUIScreenRecipeSelector for FusionRecipes (fusion torus). */
public final class FusionRecipeSelectorScreen extends LegacyRecipeSelectorScreen<FusionRecipe> {
    private final FusionMachineMenu menu;

    public FusionRecipeSelectorScreen(FusionMachineScreen previousScreen, FusionMachineMenu menu) {
        super(Component.translatable("container.atomiclegacy.fusion_torus.recipe_selector"), previousScreen,
                menu.fusionRecipes(), menu.selectedFusionRecipeId().orElse(null));
        this.menu = menu;
    }

    @Override
    protected ItemStack icon(RecipeHolder<FusionRecipe> holder) {
        return holder.value().displayIcon();
    }

    @Override
    protected Component localizedName(RecipeHolder<FusionRecipe> holder) {
        return localizedNameOf(holder);
    }

    @Override
    protected List<Component> describe(RecipeHolder<FusionRecipe> holder) {
        return describeRecipe(holder);
    }

    @Override
    protected void sendSelection(@Nullable ResourceLocation selection) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, selection == null ? 0 : this.menu.buttonIdForFusionRecipe(selection));
        }
    }

    public static Component localizedNameOf(RecipeHolder<FusionRecipe> holder) {
        return LegacyRecipeNames.localizedName(holder.value().group(), holder.value().displayIcon());
    }

    /** [parity K1] FusionRecipe#print. */
    static List<Component> describeRecipe(RecipeHolder<FusionRecipe> holder) {
        FusionRecipe recipe = holder.value();
        List<Component> lines = new ArrayList<>();
        lines.add(localizedNameOf(holder).copy().withStyle(ChatFormatting.YELLOW));
        LegacyRecipeTooltip.duration(lines, recipe.duration());
        LegacyRecipeTooltip.power(lines, recipe.power());
        lines.add(Component.translatable("gui.recipe.fusionIn").append(": " + K3GuiText.shortNumber(recipe.ignitionTemp()) + "KyU/t")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("gui.recipe.fusionOut").append(": " + K3GuiText.shortNumber(recipe.outputTemp()) + "TU/t")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("gui.recipe.fusionFlux").append(": " + ((int) (recipe.neutronFlux() * 10)) / 10D + " flux/t")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        LegacyRecipeTooltip.inputHeader(lines);
        for (FusionRecipe.FusionFluidStack fluid : recipe.inputFluids()) {
            LegacyRecipeTooltip.fluid(lines, fluid.fluid(), fluid.amount(), 0);
        }
        LegacyRecipeTooltip.outputHeader(lines);
        recipe.outputItem().ifPresent(output -> LegacyRecipeTooltip.chanceOutput(lines, output, 1F));
        for (FusionRecipe.FusionFluidStack fluid : recipe.outputFluids()) {
            LegacyRecipeTooltip.fluid(lines, fluid.fluid(), fluid.amount(), 0);
        }
        return lines;
    }
}
