package com.atomiclegacy.inventory.recipes;

import com.atomiclegacy.config.CustomMachineConfigJSON;
import com.atomiclegacy.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * [parity X1: custom machines] The core recipes CustomMachineConfigJSON#readConfig registers through
 * CraftingManager.addRecipeAuto(recipeShape, recipeParts) for every configuration. Configurations are read from the
 * config folder after the datapacks, so one data recipe of this type matches all of them in config order.
 */
public final class CustomMachineControllerRecipe extends CustomRecipe {
    public CustomMachineControllerRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static ShapedRecipe find(CraftingInput input, Level level) {
        for (ShapedRecipe recipe : CustomMachineConfigJSON.coreRecipes.values()) {
            if (recipe.matches(input, level)) return recipe;
        }
        return null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input, level) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (ShapedRecipe recipe : CustomMachineConfigJSON.coreRecipes.values()) {
            if (recipe.matches(input, null)) return recipe.assemble(input, registries);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.CUSTOM_MACHINE_CONTROLLER_SERIALIZER.get();
    }
}
