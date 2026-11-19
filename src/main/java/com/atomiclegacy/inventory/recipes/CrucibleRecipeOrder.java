package com.atomiclegacy.inventory.recipes;

import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * [parity Q1] CrucibleRecipes#registerDefaults registration order. The recipe selector and
 * NEI list recipes in that order; data-pack recipes keep it through their group name.
 */
public final class CrucibleRecipeOrder {
    public static final List<String> ORIGINAL_ORDER = List.of(
            "crucible.steel", "crucible.hematite", "crucible.malachite", "crucible.redcopper", "crucible.hss",
            "crucible.ferro", "crucible.tcalloy", "crucible.cdalloy", "crucible.bbronze", "crucible.abronze",
            "crucible.cmb", "crucible.magtung", "crucible.bscco"
    );

    private CrucibleRecipeOrder() {
    }

    public static int index(CrucibleRecipe recipe) {
        int index = ORIGINAL_ORDER.indexOf(recipe.group());
        return index < 0 ? ORIGINAL_ORDER.size() : index;
    }

    public static List<RecipeHolder<CrucibleRecipe>> sorted(Collection<RecipeHolder<CrucibleRecipe>> recipes) {
        return recipes.stream()
                .sorted(Comparator.<RecipeHolder<CrucibleRecipe>>comparingInt(holder -> index(holder.value()))
                        .thenComparing(holder -> holder.id().toString()))
                .toList();
    }
}
