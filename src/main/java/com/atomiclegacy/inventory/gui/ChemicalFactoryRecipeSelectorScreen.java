package com.atomiclegacy.inventory.gui;

import java.util.List;

import javax.annotation.Nullable;

import com.atomiclegacy.inventory.container.ChemicalFactoryMenu;
import com.atomiclegacy.inventory.recipes.ChemicalPlantRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** [parity K1] GUIScreenRecipeSelector for one chemical factory module (ChemicalPlantRecipes). */
public class ChemicalFactoryRecipeSelectorScreen extends LegacyRecipeSelectorScreen<ChemicalPlantRecipe> {
    private final ChemicalFactoryMenu menu;
    private final int module;

    public ChemicalFactoryRecipeSelectorScreen(ChemicalFactoryScreen previousScreen, ChemicalFactoryMenu menu, int module) {
        super(Component.translatable("container.atomiclegacy.chemical_factory.recipe_selector"), previousScreen,
                menu.chemicalPlantRecipes(module), menu.selectedRecipeId(module).orElse(null));
        this.menu = menu;
        this.module = module;
    }

    @Override
    protected ItemStack icon(RecipeHolder<ChemicalPlantRecipe> holder) {
        return holder.value().displayIcon();
    }

    @Override
    protected Component localizedName(RecipeHolder<ChemicalPlantRecipe> holder) {
        return ChemicalPlantRecipeSelectorScreen.localizedNameOf(holder);
    }

    @Override
    protected List<Component> describe(RecipeHolder<ChemicalPlantRecipe> holder) {
        return ChemicalPlantRecipeSelectorScreen.describeRecipe(holder);
    }

    @Override
    protected void sendSelection(@Nullable ResourceLocation selection) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            int buttonId = selection == null ? this.menu.buttonIdForClear(this.module) : this.menu.buttonIdForRecipe(this.module, selection);
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }
}
