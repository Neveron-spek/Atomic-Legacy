package com.atomiclegacy.integration.jei;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.recipes.GasCentrifugeRecipe;
import com.atomiclegacy.registry.HbmFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * [parity N1: port-only fluids hidden] JEI view of the gas centrifuge as in 1.7.10 NEI
 * (GasCentrifugeRecipes.getGasCentrifugeRecipes, 936c60c): only the four whole-cascade entries of the
 * gasCent map with real input fluids. The per-stage data recipes (leuf6/meuf6/heuf6/watz_heavy, the
 * PseudoFluidType stages) stay internal to the machine and are not shown.
 */
final class GasCentrifugeJeiRecipes {
    private GasCentrifugeJeiRecipes() {
    }

    static List<RecipeHolder<GasCentrifugeRecipe>> original() {
        return List.of(
                recipe("uf6_high_speed", "uf6", 1200, true,
                        stack("nugget_u238", 11), stack("nugget_u235", 1), stack("fluorite", 4)),
                recipe("uf6", "uf6", 1200, false,
                        stack("nugget_u238", 6), stack("nugget_uranium_fuel", 6), stack("fluorite", 4)),
                recipe("puf6", "puf6", 900, false,
                        stack("nugget_pu238", 3), stack("nugget_pu_mix", 6), stack("fluorite", 3)),
                recipe("watz", "watz", 1000, false,
                        stack("powder_iron", 1), stack("powder_lead", 1), stack("nuclear_waste_tiny", 1), stack("dust", 2))
        );
    }

    /** [parity T4: JEI] third value of the gasCent entries (GasCentrifugeRecipes.register): centrifuges in the cascade. */
    static int centrifuges(net.minecraft.resources.ResourceLocation id) {
        return switch (id.getPath()) {
            case "jei/gas_centrifuge/uf6_high_speed" -> 4;
            case "jei/gas_centrifuge/uf6" -> 2;
            case "jei/gas_centrifuge/puf6" -> 1;
            case "jei/gas_centrifuge/watz" -> 2;
            default -> 1;
        };
    }

    private static RecipeHolder<GasCentrifugeRecipe> recipe(String id, String fluid, int amount, boolean highSpeed, ItemStack... results) {
        GasCentrifugeRecipe recipe = new GasCentrifugeRecipe(
                "gas_centrifuge",
                new GasCentrifugeRecipe.FluidIngredient(HbmFluids.byName(fluid).orElse(HbmFluids.none()), amount),
                GasCentrifugeRecipe.FluidOutput.EMPTY,
                highSpeed,
                List.of(results)
        );
        return new RecipeHolder<>(AtomicLegacy.id("jei/gas_centrifuge/" + id), recipe);
    }

    private static ItemStack stack(String path, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id(path)), count);
    }
}
