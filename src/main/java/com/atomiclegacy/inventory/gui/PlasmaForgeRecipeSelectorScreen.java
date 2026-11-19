package com.atomiclegacy.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.atomiclegacy.inventory.container.FusionMachineMenu;
import com.atomiclegacy.inventory.recipes.LegacyRecipeNames;
import com.atomiclegacy.inventory.recipes.PlasmaForgeRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** [parity K1] GUIScreenRecipeSelector for PlasmaForgeRecipes. */
public class PlasmaForgeRecipeSelectorScreen extends LegacyRecipeSelectorScreen<PlasmaForgeRecipe> {
    private final FusionMachineMenu menu;

    public PlasmaForgeRecipeSelectorScreen(FusionMachineScreen previousScreen, FusionMachineMenu menu) {
        super(Component.translatable("container.atomiclegacy.fusion_plasma_forge.recipe_selector"), previousScreen,
                menu.plasmaForgeRecipes(), menu.selectedPlasmaForgeRecipeId().orElse(null));
        this.menu = menu;
    }

    @Override
    protected ItemStack icon(RecipeHolder<PlasmaForgeRecipe> holder) {
        return holder.value().displayIcon();
    }

    @Override
    protected Component localizedName(RecipeHolder<PlasmaForgeRecipe> holder) {
        return localizedNameOf(holder);
    }

    @Override
    protected List<Component> describe(RecipeHolder<PlasmaForgeRecipe> holder) {
        return describeRecipe(holder);
    }

    @Override
    protected void sendSelection(@Nullable ResourceLocation selection) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, selection == null ? 0 : this.menu.buttonIdForPlasmaForgeRecipe(selection));
        }
    }

    public static Component localizedNameOf(RecipeHolder<PlasmaForgeRecipe> holder) {
        return LegacyRecipeNames.localizedName(holder.value().group(), holder.value().displayIcon());
    }

    /** [parity K1] PlasmaForgeRecipe#print. */
    static List<Component> describeRecipe(RecipeHolder<PlasmaForgeRecipe> holder) {
        PlasmaForgeRecipe recipe = holder.value();
        List<Component> lines = new ArrayList<>();
        LegacyRecipeTooltip.header(lines, localizedNameOf(holder),
                LegacyRecipeNames.internalName(LegacyRecipeNames.PLASMA_FORGE, holder, recipe.group()));
        LegacyRecipeTooltip.autoSwitch(lines, recipe.autoSwitchGroup().orElse(null));
        LegacyRecipeTooltip.duration(lines, recipe.duration());
        LegacyRecipeTooltip.power(lines, recipe.power());
        lines.add(Component.translatable("gui.recipe.plasmaIn").append(": " + K3GuiText.shortNumber(recipe.ignitionTemp()) + "TU/t")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        LegacyRecipeTooltip.inputHeader(lines);
        for (PlasmaForgeRecipe.CountedIngredient ingredient : recipe.inputItems()) {
            LegacyRecipeTooltip.inputItem(lines, ingredient.ingredient(), ingredient.count());
        }
        for (PlasmaForgeRecipe.PlasmaFluidStack fluid : recipe.inputFluids()) {
            LegacyRecipeTooltip.fluid(lines, fluid.type(), fluid.amount(), fluid.pressure());
        }
        LegacyRecipeTooltip.outputHeader(lines);
        if (!recipe.result().isEmpty()) {
            LegacyRecipeTooltip.chanceOutput(lines, recipe.result(), 1F);
        }
        return lines;
    }

    /** AStack#extractForCyclingDisplay(20) (ghost items over empty input slots). */
    static ItemStack displayIngredient(PlasmaForgeRecipe.CountedIngredient ingredient) {
        return LegacyRecipeTooltip.cycling(ingredient.ingredient(), ingredient.count());
    }
}
