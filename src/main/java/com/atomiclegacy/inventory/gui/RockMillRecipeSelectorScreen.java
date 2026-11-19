package com.atomiclegacy.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.atomiclegacy.inventory.container.RockMillMenu;
import com.atomiclegacy.inventory.recipes.LegacyRecipeNames;
import com.atomiclegacy.inventory.recipes.RockMillRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** [parity K1] GUIScreenRecipeSelector for RockMillRecipes. */
public final class RockMillRecipeSelectorScreen extends LegacyRecipeSelectorScreen<RockMillRecipe> {
    private final RockMillMenu menu;

    public RockMillRecipeSelectorScreen(RockMillScreen previousScreen, RockMillMenu menu) {
        super(Component.translatable("container.atomiclegacy.rock_mill.recipe_selector"), previousScreen,
                menu.rockMillRecipes(), menu.selectedRecipeId().orElse(null));
        this.menu = menu;
    }

    @Override
    protected ItemStack icon(RecipeHolder<RockMillRecipe> holder) {
        return holder.value().displayIcon();
    }

    @Override
    protected Component localizedName(RecipeHolder<RockMillRecipe> holder) {
        return localizedNameOf(holder);
    }

    @Override
    protected List<Component> describe(RecipeHolder<RockMillRecipe> holder) {
        return describeRecipe(holder);
    }

    @Override
    protected void sendSelection(@Nullable ResourceLocation selection) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, selection == null ? 0 : this.menu.buttonIdForRecipe(selection));
        }
    }

    private static String internalName(RecipeHolder<RockMillRecipe> holder) {
        return LegacyRecipeNames.internalName(LegacyRecipeNames.ROCK_MILL, holder, holder.value().group());
    }

    public static Component localizedNameOf(RecipeHolder<RockMillRecipe> holder) {
        return LegacyRecipeNames.localizedName(internalName(holder), holder.value().displayIcon());
    }

    /** [parity K1] GenericRecipe#print. */
    public static List<Component> describeRecipe(RecipeHolder<RockMillRecipe> holder) {
        RockMillRecipe recipe = holder.value();
        List<Component> lines = new ArrayList<>();
        LegacyRecipeTooltip.header(lines, localizedNameOf(holder), internalName(holder));
        LegacyRecipeTooltip.autoSwitch(lines, recipe.autoSwitchGroup().orElse(null));
        LegacyRecipeTooltip.duration(lines, recipe.duration());
        LegacyRecipeTooltip.power(lines, recipe.power());
        LegacyRecipeTooltip.inputHeader(lines);
        if (!recipe.input().isNone()) {
            LegacyRecipeTooltip.inputItem(lines, recipe.input().ingredient(), recipe.input().count());
        }
        recipe.inputFluid().ifPresent(fluid -> LegacyRecipeTooltip.fluid(lines, fluid.type(), fluid.amount(), 0));
        LegacyRecipeTooltip.outputHeader(lines);
        if (recipe.outputs().size() == 1) {
            LegacyRecipeTooltip.chanceOutput(lines, recipe.outputs().getFirst().stack(), 1F);
        } else if (!recipe.outputs().isEmpty()) {
            LegacyRecipeTooltip.multiOutput(lines, recipe.outputs().stream().map(RockMillRecipe.WeightedOutput::stack).toList(),
                    recipe.outputs().stream().map(RockMillRecipe.WeightedOutput::weight).toList(), null);
        }
        recipe.outputFluid().ifPresent(fluid -> LegacyRecipeTooltip.fluid(lines, fluid.type(), fluid.amount(), 0));
        return lines;
    }
}
