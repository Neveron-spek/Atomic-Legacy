package com.atomiclegacy.inventory.gui;

import java.util.List;

import javax.annotation.Nullable;

import com.atomiclegacy.inventory.container.AssemblyFactoryMenu;
import com.atomiclegacy.inventory.recipes.AssemblyMachineRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** [parity K1] GUIScreenRecipeSelector for one assembly factory module (AssemblyMachineRecipes). */
public class AssemblyFactoryRecipeSelectorScreen extends LegacyRecipeSelectorScreen<AssemblyMachineRecipe> {
    private final AssemblyFactoryMenu menu;
    private final int module;

    public AssemblyFactoryRecipeSelectorScreen(AssemblyFactoryScreen previousScreen, AssemblyFactoryMenu menu, int module) {
        super(Component.translatable("container.atomiclegacy.assembly_factory.recipe_selector"), previousScreen,
                menu.assemblyRecipes(module), menu.selectedRecipeId(module).orElse(null));
        this.menu = menu;
        this.module = module;
    }

    @Override
    protected ItemStack icon(RecipeHolder<AssemblyMachineRecipe> holder) {
        return AssemblyRecipeSelectorScreen.displayIcon(holder);
    }

    @Override
    protected Component localizedName(RecipeHolder<AssemblyMachineRecipe> holder) {
        return AssemblyRecipeSelectorScreen.localizedNameOf(holder);
    }

    @Override
    protected List<Component> describe(RecipeHolder<AssemblyMachineRecipe> holder) {
        return AssemblyRecipeSelectorScreen.describeRecipe(holder);
    }

    @Override
    protected void sendSelection(@Nullable ResourceLocation selection) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            int buttonId = selection == null ? this.menu.buttonIdForClear(this.module) : this.menu.buttonIdForRecipe(this.module, selection);
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }
}
