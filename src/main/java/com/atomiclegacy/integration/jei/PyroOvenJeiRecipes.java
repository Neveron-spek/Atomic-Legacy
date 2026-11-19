package com.atomiclegacy.integration.jei;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.items.BedrockOreItem;
import com.atomiclegacy.inventory.recipes.PyroOvenRecipe;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/** Mirrors PyroOvenRecipes#registerSFAuto for JEI without introducing a second fuel list. */
final class PyroOvenJeiRecipes {
    private PyroOvenJeiRecipes() {
    }

    static List<RecipeHolder<PyroOvenRecipe>> appendSolidFuelRecipes(List<RecipeHolder<PyroOvenRecipe>> registered) {
        List<RecipeHolder<PyroOvenRecipe>> recipes = new ArrayList<>(registered);
        appendBedrockOreRecipes(recipes);
        int index = 0;
        for (HbmFluidDefinition fluid : HbmFluids.definitions()) {
            long heatPerBucket = fluid.flammableHeatEnergy();
            if (fluid.isNone() || heatPerBucket <= 0L || !PyroOvenRecipe.SOLID_FUEL_FLUIDS.contains(fluid.name())) {
                continue;
            }
            boolean balefire = fluid.name().equals("balefire");
            long targetHeat = balefire ? 24_000_000L : 1_440_000L;
            long rawAmount = targetHeat * 1_000L / 2L / heatPerBucket;
            int amount = rawAmount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rawAmount;
            if (amount > 10_000) amount -= amount % 1_000;
            else if (amount > 1_000) amount -= amount % 100;
            else if (amount > 100) amount -= amount % 10;
            amount = Math.max(1, amount);

            ItemStack output = new ItemStack(balefire ? HbmItems.SOLID_FUEL_BF.get() : HbmItems.SOLID_FUEL.get());
            PyroOvenRecipe recipe = new PyroOvenRecipe(
                    "pyrolysis.solid_fuel",
                    PyroOvenRecipe.ItemInput.EMPTY,
                    new PyroOvenRecipe.FluidInput(fluid, amount),
                    output,
                    PyroOvenRecipe.FluidOutput.EMPTY,
                    60
            );
            recipes.add(new RecipeHolder<>(AtomicLegacy.id("jei/pyrolysis/solid_fuel_" + fluid.name() + "_" + index++), recipe));
        }
        return recipes;
    }

    /** Mirrors the five BedrockOreGrade roasting registrations per ore type. */
    private static void appendBedrockOreRecipes(List<RecipeHolder<PyroOvenRecipe>> recipes) {
        HbmFluidDefinition vitriol = HbmFluids.byName("vitriol").orElse(HbmFluids.none());
        int index = 0;
        for (BedrockOreItem.Type type : BedrockOreItem.Type.values()) {
            for (BedrockRoast roast : BedrockRoast.values()) {
                ItemStack input = BedrockOreItem.stackFor(HbmItems.BEDROCK_ORE_NEW, roast.input(), type);
                ItemStack output = BedrockOreItem.stackFor(HbmItems.BEDROCK_ORE_NEW, roast.output(), type);
                PyroOvenRecipe recipe = new PyroOvenRecipe(
                        "pyrolysis.bedrock_ore",
                        new PyroOvenRecipe.ItemInput(Ingredient.of(input), 1),
                        PyroOvenRecipe.FluidInput.EMPTY,
                        output,
                        new PyroOvenRecipe.FluidOutput(vitriol, 50),
                        10
                );
                recipes.add(new RecipeHolder<>(AtomicLegacy.id("jei/pyrolysis/bedrock_ore_" + type.id() + "_" + index++), recipe));
            }
        }
    }

    private enum BedrockRoast {
        BASE(BedrockOreItem.Grade.BASE, BedrockOreItem.Grade.BASE_ROASTED),
        PRIMARY(BedrockOreItem.Grade.PRIMARY, BedrockOreItem.Grade.PRIMARY_ROASTED),
        SULFURIC(BedrockOreItem.Grade.SULFURIC_BYPRODUCT, BedrockOreItem.Grade.SULFURIC_ROASTED),
        SOLVENT(BedrockOreItem.Grade.SOLVENT_BYPRODUCT, BedrockOreItem.Grade.SOLVENT_ROASTED),
        RAD(BedrockOreItem.Grade.RAD_BYPRODUCT, BedrockOreItem.Grade.RAD_ROASTED);

        private final BedrockOreItem.Grade input;
        private final BedrockOreItem.Grade output;

        BedrockRoast(BedrockOreItem.Grade input, BedrockOreItem.Grade output) {
            this.input = input;
            this.output = output;
        }

        private BedrockOreItem.Grade input() {
            return this.input;
        }

        private BedrockOreItem.Grade output() {
            return this.output;
        }
    }
}
