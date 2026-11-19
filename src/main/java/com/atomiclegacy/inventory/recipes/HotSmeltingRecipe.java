package com.atomiclegacy.inventory.recipes;

import com.atomiclegacy.items.LegacyHotItem;
import com.atomiclegacy.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;

/**
 * [parity V3: hot items] SmeltingRecipes.java:167-181 {@code GameRegistry.addSmelting(item, ItemHot.heatUp(new
 * ItemStack(item[, 1, meta])), xp)}: a furnace (minecraft:smelting) recipe whose result is the hot item with "heat" 100.
 * The 1.7.10 dusted-steel recipes exist once per metadata 0..9 and keep it; here one recipe copies the input's forging
 * grade, since a vanilla ingredient cannot tell grades apart.
 */
public class HotSmeltingRecipe extends SmeltingRecipe {
    public HotSmeltingRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result,
                             float experience, int cookingTime) {
        super(group, category, ingredient, result, experience, cookingTime);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        ItemStack out = super.assemble(input, registries);
        if (out.getItem() instanceof LegacyHotItem hot && hot.isDusted()) {
            LegacyHotItem.withGrade(out, LegacyHotItem.grade(input.item()));
        }
        return LegacyHotItem.heatUp(out);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return LegacyHotItem.heatUp(super.getResultItem(registries).copy());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.HOT_SMELTING_SERIALIZER.get();
    }
}
