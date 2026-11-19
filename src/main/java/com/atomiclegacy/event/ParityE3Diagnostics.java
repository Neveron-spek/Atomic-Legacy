package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.util.LegacyFurnaceFuels;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * [e3] Workbench / furnace / fuel parity against CraftingManager, crafting/* and FuelHandler of 1.7.10 (936c60c),
 * plus the tooltip/item-property and second bug-pass diagnostics of the same batch.
 * The recipe cases are the corrections found by work/parity_e3_recipes.py; the script itself is the full comparison.
 */
final class ParityE3Diagnostics {
    private ParityE3Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        try {
            fuels(problems, cases);
            recipes(server, problems, cases);
        } catch (Exception exception) {
            problems.add("E3: recipe diagnostics threw " + exception);
        }
        cases[0] += ParityE3BugDiagnostics.run(server, problems);
        cases[0] += ParityE3TooltipDiagnostics.run(server, problems);
        return cases[0];
    }

    /** FuelHandler#getBurnTime: single = 200. */
    private static void fuels(List<String> problems, int[] cases) {
        fuel("solid_fuel", 3_200, problems, cases);
        fuel("solid_fuel_presto", 8_000, problems, cases);
        fuel("solid_fuel_presto_triplet", 40_000, problems, cases);
        fuel("solid_fuel_bf", 32_000, problems, cases);
        fuel("solid_fuel_presto_bf", 80_000, problems, cases);
        fuel("solid_fuel_presto_triplet_bf", 400_000, problems, cases);
        fuel("rocket_fuel", 6_400, problems, cases);
        fuel("biomass", 400, problems, cases);
        fuel("biomass_compressed", 800, problems, cases);
        fuel("powder_coal", 1_600, problems, cases);
        fuel("scrap", 50, problems, cases);
        fuel("dust", 25, problems, cases);
        fuel("block_scrap", 400, problems, cases);
        fuel("powder_fire", 6_400, problems, cases);
        fuel("lignite", 1_200, problems, cases);
        fuel("powder_lignite", 1_200, problems, cases);
        fuel("coke_coal", 3_200, problems, cases);
        fuel("coke_lignite", 3_200, problems, cases);
        fuel("coke_petroleum", 3_200, problems, cases);
        fuel("block_coke", 32_000, problems, cases);
        fuel("coal_infernal", 4_800, problems, cases);
        fuel("coal_eternal", 3_200, problems, cases);
        fuel("crystal_coal", 6_400, problems, cases);
        fuel("powder_sawdust", 100, problems, cases);
        fuel("briquette_coal", 2_000, problems, cases);
        fuel("briquette_lignite", 1_600, problems, cases);
        fuel("briquette_wood", 400, problems, cases);
        fuel("powder_ash_wood", 100, problems, cases);
        fuel("powder_ash_coal", 200, problems, cases);
        fuel("powder_ash_misc", 100, problems, cases);
        fuel("powder_ash_fly", 200, problems, cases);
        fuel("powder_ash_soot", 100, problems, cases);
    }

    private static void fuel(String id, int expected, List<String> problems, int[] cases) {
        cases[0]++;
        Item item = item(id);
        if (item == Items.AIR) {
            problems.add("E3: fuel item " + id + " is not registered");
            return;
        }
        int actual = LegacyFurnaceFuels.burnTime(new ItemStack(item));
        if (actual != expected) {
            problems.add("E3: burn time of " + id + " is " + actual + ", FuelHandler gives " + expected);
        }
    }

    private static void recipes(MinecraftServer server, List<String> problems, int[] cases) {
        // ingredient corrections (wrong material / narrowed ore dictionary key)
        accepts(server, "custom_fall", "plate_saturnite", true, problems, cases);
        accepts(server, "custom_fall", "plate_bismuth", false, problems, cases);
        accepts(server, "custom_fall", "ingot_biorubber", true, problems, cases);
        accepts(server, "padlock_unbreakable", "plate_saturnite", true, problems, cases);
        accepts(server, "vent_chlorine_seal", "ingot_saturnite", true, problems, cases);
        accepts(server, "vent_chlorine_seal", "ingot_dineutronium", false, problems, cases);
        accepts(server, "powder_nitan_mix_standard", "powder_iodine", true, problems, cases);
        accepts(server, "powder_nitan_mix_standard", "powder_iron", false, problems, cases);
        accepts(server, "crystal_charred", "block_aluminium", true, problems, cases);
        accepts(server, "crystal_charred", "gravel_obsidian", false, problems, cases);
        accepts(server, "powder_desh_ready", "nugget_mercury", true, problems, cases);
        accepts(server, "hazmat_cloth_grey", "ingot_biorubber", true, problems, cases);
        accepts(server, "hazmat_cloth_grey", "ingot_rubber", true, problems, cases);
        accepts(server, "radio_autocal", "plate_cast_copper", true, problems, cases);
        accepts(server, "radio_autocal", "plate_copper", false, problems, cases);
        accepts(server, "battery_sc_empty", "plate_lead", true, problems, cases);
        accepts(server, "ammo_shell_explosive_gunpowder", "ingot_semtex", true, problems, cases);
        accepts(server, "ammo_shell_explosive_gunpowder", "ball_tnt", false, problems, cases);
        accepts(server, "fluid_identifier_multi", "circuit_vacuum_tube", true, problems, cases);
        accepts(server, "fluid_identifier_multi", "circuit_analog", false, problems, cases);
        accepts(server, "watz_pellet_du", "ingot_u238", true, problems, cases);
        accepts(server, "assembly_nuke", "plate_weaponsteel", true, problems, cases);
        accepts(server, "billet_mox_fuel_from_billet_mix", "billet_pu239", true, problems, cases);
        accepts(server, "cargo_elevator", "part_generic_piston_hydraulic", true, problems, cases);
        acceptsStack(server, "powder_flux_coal", new ItemStack(Items.CHARCOAL), true, problems, cases);
        acceptsStack(server, "powder_flux_coal", new ItemStack(Items.COAL), false, problems, cases);
        acceptsStack(server, "foundry_basin", new ItemStack(Items.COBBLESTONE_SLAB), true, problems, cases);
        acceptsStack(server, "foundry_basin", new ItemStack(Items.SMOOTH_STONE_SLAB), true, problems, cases);
        acceptsStack(server, "foundry_basin", new ItemStack(Items.STONE_SLAB), false, problems, cases);
        acceptsStack(server, "press_preheater", new ItemStack(Items.LAVA_BUCKET), true, problems, cases);
        acceptsStack(server, "inf_water", new ItemStack(Items.WATER_BUCKET), true, problems, cases);
        acceptsStack(server, "circuit_capacitor", new ItemStack(item("fragment_niobium")), true, problems, cases);

        // restored recipes
        for (String id : new String[] {
                "ingot_aluminium_crafting_138", "copper_ingot_crafting_139", "biomass_crafting_148", "biomass_crafting_158", "paper_crafting_205",
                "bucket_crafting_286", "torch_crafting_666", "torch_crafting_667", "name_tag_crafting_836", "lead_crafting_838",
                "ball_resin_crafting_910", "bdcl_crafting_943", "gunpowder_powder_43", "clay_ball_powder_35", "ball_dynamite_powder_29",
                "powder_iron_from_ore_byproduct_iron", "billet_polonium_from_ore_byproduct_polonium", "nugget_mercury_from_pellet_rtg_depleted",
                "smelting_scrap_plastic_to_ingot_polymer", "part_generic_piston_electric_crafting_864", "upgrade_stack_1_crafting_731"}) {
            cases[0]++;
            if (holder(server, id).isEmpty()) {
                problems.add("E3: recipe atomiclegacy:" + id + " (1.7.10 CraftingManager/crafting) is not loaded");
            }
        }

        // recipes that have no 1.7.10 counterpart
        for (String id : new String[] {"alloy_sword", "centri_stick", "oil_pipe", "piston_inserter", "pipes_steel",
                "billet_balefire_gold", "billet_flashlead", "ingot_lead_from_wire_lead"}) {
            cases[0]++;
            if (holder(server, id).isPresent()) {
                problems.add("E3: recipe atomiclegacy:" + id + " has no 1.7.10 counterpart and must not exist");
            }
        }

        // SmeltingRecipes experience
        experience(server, "smelting_ore_nether_uranium", 12.0F, problems, cases);
        experience(server, "smelting_ore_nether_plutonium", 24.0F, problems, cases);
        experience(server, "smelting_ore_nether_schrabidium", 256.0F, problems, cases);
        experience(server, "smelting_ore_nether_cobalt", 2.0F, problems, cases);
        experience(server, "smelting_ore_gneiss_iron", 5.0F, problems, cases);
        experience(server, "smelting_ore_gneiss_lithium", 10.0F, problems, cases);
        experience(server, "smelting_scrap_plastic_to_ingot_polymer", 0.1F, problems, cases);

        // sand_mix variants are the crafting result (BlockNTMSand), not the duplicate sand_<type> blocks
        for (String id : new String[] {"sand_boron", "sand_lead", "sand_uranium_crafting_685", "sand_polonium_crafting_686"}) {
            cases[0]++;
            Optional<RecipeHolder<?>> recipe = holder(server, id);
            if (recipe.isEmpty()) {
                problems.add("E3: recipe atomiclegacy:" + id + " is not loaded");
            } else {
                ItemStack result = recipe.get().value().getResultItem(server.registryAccess());
                if (result.getItem() != item("sand_mix") || result.getCount() != 8) {
                    problems.add("E3: recipe " + id + " must give 8 sand_mix, gives " + result);
                }
            }
        }
    }

    private static void accepts(MinecraftServer server, String recipe, String itemId, boolean expected, List<String> problems, int[] cases) {
        Item item = item(itemId);
        if (item == Items.AIR) {
            cases[0]++;
            problems.add("E3: item " + itemId + " (recipe " + recipe + ") is not registered");
            return;
        }
        acceptsStack(server, recipe, new ItemStack(item), expected, problems, cases);
    }

    private static void acceptsStack(MinecraftServer server, String recipeId, ItemStack stack, boolean expected, List<String> problems, int[] cases) {
        cases[0]++;
        Optional<RecipeHolder<?>> recipe = holder(server, recipeId);
        if (recipe.isEmpty()) {
            problems.add("E3: recipe atomiclegacy:" + recipeId + " is not loaded");
            return;
        }
        boolean actual = false;
        for (Ingredient ingredient : recipe.get().value().getIngredients()) {
            if (ingredient.test(stack)) {
                actual = true;
                break;
            }
        }
        if (actual != expected) {
            problems.add("E3: recipe " + recipeId + (expected ? " must accept " : " must not accept ") + BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
    }

    private static void experience(MinecraftServer server, String recipeId, float expected, List<String> problems, int[] cases) {
        cases[0]++;
        Optional<RecipeHolder<?>> recipe = holder(server, recipeId);
        if (recipe.isEmpty()) {
            problems.add("E3: smelting recipe atomiclegacy:" + recipeId + " is not loaded");
            return;
        }
        Recipe<?> value = recipe.get().value();
        if (!(value instanceof AbstractCookingRecipe cooking)) {
            problems.add("E3: recipe " + recipeId + " is not a smelting recipe");
        } else if (Math.abs(cooking.getExperience() - expected) > 1.0E-4F || cooking.getCookingTime() != 200) {
            problems.add("E3: smelting " + recipeId + " gives " + cooking.getExperience() + " xp in " + cooking.getCookingTime()
                    + " ticks, SmeltingRecipes gives " + expected + " in 200");
        }
    }

    private static Optional<RecipeHolder<?>> holder(MinecraftServer server, String id) {
        return server.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(AtomicLegacy.MOD_ID, id));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(AtomicLegacy.MOD_ID, id));
    }
}
