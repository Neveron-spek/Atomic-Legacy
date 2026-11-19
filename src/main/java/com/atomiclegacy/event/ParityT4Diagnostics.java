package com.atomiclegacy.event;

import com.atomiclegacy.inventory.recipes.CompressorRecipe;
import com.atomiclegacy.inventory.recipes.CrackingRecipe;
import com.atomiclegacy.inventory.recipes.CrystallizerRecipe;
import com.atomiclegacy.inventory.recipes.CyclotronRecipe;
import com.atomiclegacy.inventory.recipes.HydrotreatingRecipe;
import com.atomiclegacy.inventory.recipes.RefineryRecipe;
import com.atomiclegacy.inventory.recipes.ReformingRecipe;
import com.atomiclegacy.inventory.recipes.VacuumDistillRecipe;
import com.atomiclegacy.registry.HbmRecipeTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Arrays;
import java.util.List;

/**
 * [parity T4: diagnostics] Server-side checks of the recipe data the T4 JEI categories rely on when they rebuild the
 * 1.7.10 NEI RecipeSets (fixed NEI amounts, x10 scaling of per-operation fills, PU tags and skipped entries).
 */
public final class ParityT4Diagnostics {
    private ParityT4Diagnostics() {
    }

    public static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        try {
            RecipeManager rm = server.getRecipeManager();
            // RefineryRecipes.getRefineryRecipe / VacuumRefineryRecipes.getVacuumRecipe: four fractions per 100 mB, shown x10 for 1000 mB
            for (RecipeHolder<RefineryRecipe> holder : rm.getAllRecipesFor(HbmRecipeTypes.REFINERY.get())) {
                RefineryRecipe recipe = holder.value();
                check(recipe.outputs().size() == 4 && recipe.input().amount() == 100, problems, cases, "refinery fractions/100 mB " + holder.id());
            }
            for (RecipeHolder<VacuumDistillRecipe> holder : rm.getAllRecipesFor(HbmRecipeTypes.VACUUM_DISTILL.get())) {
                VacuumDistillRecipe recipe = holder.value();
                check(recipe.outputs().size() == 4 && recipe.input().amount() == 100, problems, cases, "vacuum fractions/100 mB " + holder.id());
            }
            // ReformingRecipes / HydrotreatingRecipes: 1000 mB input, outputs fill x10, hydrogen fill x10 at 1 PU
            for (RecipeHolder<ReformingRecipe> holder : rm.getAllRecipesFor(HbmRecipeTypes.REFORMING.get())) {
                check(holder.value().input().amount() == 100, problems, cases, "reforming 100 mB per operation " + holder.id());
            }
            for (RecipeHolder<HydrotreatingRecipe> holder : rm.getAllRecipesFor(HbmRecipeTypes.HYDROTREATING.get())) {
                HydrotreatingRecipe recipe = holder.value();
                check(recipe.input().amount() == 100 && recipe.hydrogen().pressure() == 1
                        && recipe.hydrogen().fluid().name().equals("hydrogen"), problems, cases, "hydrotreating hydrogen 1 PU " + holder.id());
            }
            // CrackingRecipes.getCrackingRecipesForNEI: 100 mB + 200 mB steam -> ... + 2 mB spent steam
            check(CrackingRecipe.STEAM_PER_OPERATION == 200 && CrackingRecipe.SPENT_STEAM_PER_OPERATION == 2, problems, cases, "cracking steam constants");
            for (RecipeHolder<CrackingRecipe> holder : rm.getAllRecipesFor(HbmRecipeTypes.CRACKING.get())) {
                check(holder.value().input().amount() == 100, problems, cases, "cracking 100 mB " + holder.id());
            }
            // CompressorRecipes.getRecipes skips same-type entries; at least one conversion stays visible
            List<RecipeHolder<CompressorRecipe>> compressor = rm.getAllRecipesFor(HbmRecipeTypes.COMPRESSOR.get());
            long conversions = compressor.stream().filter(h -> h.value().input().fluid() != h.value().output().fluid()).count();
            check(conversions > 0, problems, cases, "compressor conversions " + conversions + "/" + compressor.size());
            // CrystallizerRecipes.getRecipes skips scrap_plastic keys
            long plastic = rm.getAllRecipesFor(HbmRecipeTypes.CRYSTALLIZER.get()).stream()
                    .map(RecipeHolder::value).map(CrystallizerRecipe::ingredient)
                    .filter(ing -> Arrays.stream(ing.getItems()).allMatch(ParityT4Diagnostics::isScrapPlastic) && ing.getItems().length > 0)
                    .count();
            check(plastic > 0, problems, cases, "crystallizer scrap_plastic recipes present (hidden in NEI) " + plastic);
            // CyclotronRecipes.getRecipes: one entry per target item, particle is a single ComparableStack
            for (RecipeHolder<CyclotronRecipe> holder : rm.getAllRecipesFor(HbmRecipeTypes.CYCLOTRON.get())) {
                check(holder.value().particle().getItems().length == 1, problems, cases, "cyclotron particle single stack " + holder.id());
            }
        } catch (RuntimeException exception) {
            problems.add("T4: diagnostics threw " + exception);
        }
        return cases[0];
    }

    private static boolean isScrapPlastic(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().startsWith("scrap_plastic");
    }

    private static void check(boolean ok, List<String> problems, int[] cases, String what) {
        cases[0]++;
        if (!ok) {
            problems.add("T4: " + what);
        }
    }
}
