package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.foundry.CrucibleUtil;
import com.atomiclegacy.foundry.FoundryMaterial;
import com.atomiclegacy.foundry.FoundryMaterialStack;
import com.atomiclegacy.foundry.FoundryShape;
import com.atomiclegacy.inventory.recipes.ArcWelderRecipe;
import com.atomiclegacy.inventory.recipes.CrucibleRecipe;
import com.atomiclegacy.inventory.recipes.ElectrolyzerFluidRecipe;
import com.atomiclegacy.inventory.recipes.ElectrolyzerMetalRecipe;
import com.atomiclegacy.inventory.recipes.MixerRecipe;
import com.atomiclegacy.inventory.recipes.RockMillRecipe;
import com.atomiclegacy.items.FoundryMoldItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.tileentity.ArcWelderBlockEntity;
import com.atomiclegacy.tileentity.AshpitBlockEntity;
import com.atomiclegacy.tileentity.CrucibleBlockEntity;
import com.atomiclegacy.tileentity.FoundryCastingBlockEntity;
import com.atomiclegacy.tileentity.HeaterBlockEntity;
import com.atomiclegacy.tileentity.MixerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ServerLevelData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * [parity Q1] Dedicated-server checks of the metallurgy chain (crucible, foundry, firebox/oven, ashpit,
 * arc welder, mixer, electrolyser, rock mill, molds, material table) against HbmMods 1.7.10 @ 936c60c.
 * The recipe tables below were produced by work/tmp-q1/gen_expected.py from the recipe JSON after it was
 * compared line by line with ArcWelderRecipes, MixerRecipes, ElectrolyserFluid/MetalRecipes, RockMillRecipes
 * and CrucibleRecipes. Machines are placed on pads at x = 20000 and ticked directly.
 * Not covered: GUI drawing, sounds, particles, strand caster, ore slopper, autosaw, sawmill, thresher runs.
 */
final class ParityQ1Diagnostics {

    private ParityQ1Diagnostics() {
    }

    private static final int BASE_X = 20_000;
    private static final int BASE_Z = 20_000;
    private static final int SPACING = 32;
    private static int site;
    private static final List<int[]> FORCED = new ArrayList<>();

    private static final List<String> EXPECTED_ARC = List.of(
            "arc_welder/fuel_tank_large|atomiclegacy:fuel_tank_large|200|5000|-|8,12,16",
            "arc_welder/fuel_tank_medium|atomiclegacy:fuel_tank_medium|100|2000|-|4,8,12",
            "arc_welder/fuel_tank_small|atomiclegacy:fuel_tank_small|60|1000|-|6,4,4",
            "arc_welder/missile_anti_ballistic|atomiclegacy:missile_anti_ballistic|100|5000|-|3,1,4",
            "arc_welder/missile_burst|atomiclegacy:missile_burst|300|25000|-|1,2,4",
            "arc_welder/missile_buster|atomiclegacy:missile_buster|100|5000|-|1,1,1",
            "arc_welder/missile_buster_strong|atomiclegacy:missile_buster_strong|200|10000|-|1,1,1",
            "arc_welder/missile_cluster|atomiclegacy:missile_cluster|100|5000|-|1,1,1",
            "arc_welder/missile_cluster_strong|atomiclegacy:missile_cluster_strong|200|10000|-|1,1,1",
            "arc_welder/missile_decoy|atomiclegacy:missile_decoy|60|2500|-|1,1,1",
            "arc_welder/missile_drill|atomiclegacy:missile_drill|300|25000|-|1,2,4",
            "arc_welder/missile_emp_strong|atomiclegacy:missile_emp_strong|200|10000|-|3,1,1",
            "arc_welder/missile_generic|atomiclegacy:missile_generic|100|5000|-|1,1,1",
            "arc_welder/missile_incendiary|atomiclegacy:missile_incendiary|100|5000|-|1,1,1",
            "arc_welder/missile_incendiary_strong|atomiclegacy:missile_incendiary_strong|200|10000|-|1,1,1",
            "arc_welder/missile_inferno|atomiclegacy:missile_inferno|300|25000|-|1,2,4",
            "arc_welder/missile_nuclear|atomiclegacy:missile_nuclear|600|50000|-|1,1,3",
            "arc_welder/missile_nuclear_cluster|atomiclegacy:missile_nuclear_cluster|600|50000|-|1,1,3",
            "arc_welder/missile_rain|atomiclegacy:missile_rain|300|25000|-|1,2,4",
            "arc_welder/missile_strong|atomiclegacy:missile_strong|200|10000|-|1,1,1",
            "arc_welder/missile_volcano|atomiclegacy:missile_volcano|600|50000|-|1,1,3",
            "arc_welder/motor|atomiclegacy:motor*2|100|400|-|2,2",
            "arc_welder/neutron_reflector|atomiclegacy:neutron_reflector*2|400|50000|-|2,1",
            "arc_welder/part_generic_lde_aluminium|atomiclegacy:part_generic_lde|200|5000|-|4,4,1",
            "arc_welder/part_generic_lde_titanium|atomiclegacy:part_generic_lde|200|10000|-|2,4,1",
            "arc_welder/plate_welded_aluminium|atomiclegacy:plate_welded_aluminium|300|10000|-|2",
            "arc_welder/plate_welded_cdalloy|atomiclegacy:plate_welded_cdalloy|1200|1000000|oxygen:1000|2",
            "arc_welder/plate_welded_combine_steel|atomiclegacy:plate_welded_combine_steel|1200|10000000|reformgas:1000|2",
            "arc_welder/plate_welded_copper|atomiclegacy:plate_welded_copper|200|1000|-|2",
            "arc_welder/plate_welded_iron|atomiclegacy:plate_welded_iron|100|100|-|2",
            "arc_welder/plate_welded_osmiridium|atomiclegacy:plate_welded_osmiridium|6000|50000000|reformgas:16000|2",
            "arc_welder/plate_welded_steel|atomiclegacy:plate_welded_steel|100|500|-|2",
            "arc_welder/plate_welded_tcalloy|atomiclegacy:plate_welded_tcalloy|1200|1000000|oxygen:1000|2",
            "arc_welder/plate_welded_titanium|atomiclegacy:plate_welded_titanium|600|50000|-|2",
            "arc_welder/plate_welded_tungsten|atomiclegacy:plate_welded_tungsten|1200|250000|oxygen:1000|2",
            "arc_welder/plate_welded_zirconium|atomiclegacy:plate_welded_zirconium|600|10000|-|2",
            "arc_welder/thruster_large|atomiclegacy:thruster_large|200|5000|-|10,1,12",
            "arc_welder/thruster_medium|atomiclegacy:thruster_medium|100|2000|-|8,1,8",
            "arc_welder/thruster_small|atomiclegacy:thruster_small|60|1000|-|4,4,4",
            "arc_welder/wire_dense_copper|atomiclegacy:wire_dense_copper|100|10000|-|8",
            "arc_welder/wire_dense_gold|atomiclegacy:wire_dense_gold|100|10000|-|8",
            "arc_welder/wire_dense_red_copper|atomiclegacy:wire_dense_red_copper|100|10000|-|8"
    );
    private static final List<String> EXPECTED_MIXER = List.of(
            "mixer/alumina_cryolite|alumina:300|40|sodium_aluminate:150|-|1",
            "mixer/alumina_fluorite|alumina:200|40|sodium_aluminate:150|-|3",
            "mixer/bauxite_solution|bauxite_solution:300|80|lye:50|-|1",
            "mixer/biofuel_fishoil|biofuel:250|20|fishoil:500|woodoil:500|-",
            "mixer/biofuel_sunfloweroil|biofuel:200|20|sunfloweroil:500|woodoil:500|-",
            "mixer/bitumen|bitumen:50|20|-|-|1",
            "mixer/chlorocalcite_mix|chlorocalcite_mix:1000|50|chlorocalcite_solution:500|sulfuric_acid:500|1",
            "mixer/chlorocalcite_solution|chlorocalcite_solution:500|50|water:250|nitric_acid:250|1",
            "mixer/coalgas_leaded|coalgas_leaded:12000|40|coalgas:10000|-|1",
            "mixer/colloid|colloid:500|20|water:500|-|1",
            "mixer/coolant|coolant:2000|50|water:1800|-|1",
            "mixer/cryogel|cryogel:2000|50|coolant:1800|-|1",
            "mixer/diesel_crack_reform|diesel_crack_reform:1000|50|diesel_crack:900|reformate:100|-",
            "mixer/diesel_reform|diesel_reform:1000|50|diesel:900|reformate:100|-",
            "mixer/egg|egg:1000|50|radiosolvent:500|-|1",
            "mixer/enderjuice|enderjuice:100|100|xpjuice:500|-|1",
            "mixer/fishoil|fishoil:100|50|-|-|1",
            "mixer/fracksol_sulfur|fracksol:1000|20|water:1000|petroleum:100|1",
            "mixer/fracksol_sulfuric|fracksol:1000|20|sulfuric_acid:900|petroleum:100|-",
            "mixer/fullerene|fullerene:250|50|radiosolvent:500|-|1",
            "mixer/gasoline_leaded|gasoline_leaded:12000|40|gasoline:10000|-|1",
            "mixer/iongel|iongel:1000|50|water:1000|hydrogen:200|1",
            "mixer/kerosene_reform|kerosene_reform:1000|50|kerosene:900|reformate:100|-",
            "mixer/lubricant_fishoil|lubricant:1000|20|fishoil:800|ethanol:200|-",
            "mixer/lubricant_heatingoil|lubricant:1000|20|heatingoil:500|unsaturateds:500|-",
            "mixer/lubricant_sunfloweroil|lubricant:1000|20|sunfloweroil:800|ethanol:200|-",
            "mixer/lye|lye:100|100|water:100|-|1",
            "mixer/mustardgas|mustardgas:1000|20|reformgas:750|chlorine:250|1",
            "mixer/nitan|nitan:1000|50|kerosene:600|mercury:200|1",
            "mixer/nitric_acid|nitric_acid:1000|50|sulfuric_acid:500|-|1",
            "mixer/nitroglycerin_fishoil|nitroglycerin:1000|20|fishoil:500|nitric_acid:500|-",
            "mixer/nitroglycerin_petroleum|nitroglycerin:1000|20|petroleum:1000|nitric_acid:1000|-",
            "mixer/oxyhydrogen_air|oxyhydrogen:1000|50|hydrogen:500|air:2000|-",
            "mixer/oxyhydrogen_oxygen|oxyhydrogen:1000|50|hydrogen:500|oxygen:500|-",
            "mixer/perfluoromethyl|perfluoromethyl:1000|20|petroleum:1000|unsaturateds:500|1",
            "mixer/petroil|petroil:1000|30|reclaimed:800|lubricant:200|-",
            "mixer/petroil_leaded|petroil_leaded:12000|40|petroil:10000|-|1",
            "mixer/pheromone_m|pheromone_m:2000|10|pheromone:1500|blood:500|1",
            "mixer/phosgene|phosgene:1000|20|unsaturateds:500|chlorine:500|-",
            "mixer/radiosolvent|radiosolvent:1000|50|reformgas:750|chlorine:250|-",
            "mixer/salient|salient:1000|20|seedslurry:500|blood:500|-",
            "mixer/schrabidic|schrabidic:16000|100|sas3:8000|peroxide:6000|1",
            "mixer/solvent_naphtha|solvent:1000|50|naphtha:500|aromatics:500|-",
            "mixer/solvent_naphtha_coker|solvent:1000|50|naphtha_coker:500|aromatics:500|-",
            "mixer/solvent_naphtha_crack|solvent:1000|50|naphtha_crack:500|aromatics:500|-",
            "mixer/solvent_naphtha_ds|solvent:1000|50|naphtha_ds:500|aromatics:500|-",
            "mixer/sulfuric_acid|sulfuric_acid:500|50|peroxide:800|-|1",
            "mixer/sunfloweroil|sunfloweroil:100|50|-|-|1",
            "mixer/syngas|syngas:1000|50|coaloil:500|steam:500|-",
            "mixer/thorium_salt|thorium_salt:1000|30|chlorine:1000|-|1"
    );
    private static final List<String> EXPECTED_EFLUID = List.of(
            "electrolyzer_fluid/alumina|alumina:200|carbondioxide:100|none:0|40|atomiclegacy:powder_aluminium*7,atomiclegacy:fluorite*2",
            "electrolyzer_fluid/calcium_chloride|calcium_chloride:250|chlorine:125|calcium_solution:125|20|",
            "electrolyzer_fluid/heavywater|heavywater:2000|deuterium:200|oxygen:200|10|",
            "electrolyzer_fluid/potassium_chloride|potassium_chloride:250|chlorine:125|none:0|20|atomiclegacy:dust",
            "electrolyzer_fluid/redmud|redmud:450|mercury:150|lye:50|20|atomiclegacy:powder_titanium*3,atomiclegacy:powder_iron*3,atomiclegacy:powder_aluminium*2",
            "electrolyzer_fluid/slop|slop:1000|mercury:250|none:0|20|atomiclegacy:niter*2,atomiclegacy:powder_limestone*2,atomiclegacy:sulfur",
            "electrolyzer_fluid/vitriol|vitriol:1000|sulfuric_acid:500|chlorine:500|20|atomiclegacy:powder_iron,atomiclegacy:nugget_mercury",
            "electrolyzer_fluid/water|water:2000|hydrogen:200|oxygen:200|10|"
    );
    private static final List<String> EXPECTED_EMETAL = List.of(
            "electrolyzer_metal/bedrock_actinide_crumbs|uranium:64|thorium:64|20|",
            "electrolyzer_metal/bedrock_actinide_primary_first|uranium:256|thorium:128|20|atomiclegacy:bedrock_ore_new",
            "electrolyzer_metal/bedrock_actinide_primary_second|uranium:128|thorium:256|20|atomiclegacy:bedrock_ore_new",
            "electrolyzer_metal/bedrock_crystal_crumbs|redstone:144|-|20|atomiclegacy:bedrock_ore_fragment_cinnabar*8",
            "electrolyzer_metal/bedrock_crystal_primary_first|redstone:576|-|20|atomiclegacy:bedrock_ore_fragment_cinnabar*16,atomiclegacy:bedrock_ore_new",
            "electrolyzer_metal/bedrock_crystal_primary_second|redstone:288|-|20|atomiclegacy:bedrock_ore_fragment_cinnabar*32,atomiclegacy:bedrock_ore_new",
            "electrolyzer_metal/bedrock_heavy_crumbs|tungsten:144|lead:144|20|",
            "electrolyzer_metal/bedrock_heavy_primary_first|tungsten:576|lead:288|20|atomiclegacy:bedrock_ore_new",
            "electrolyzer_metal/bedrock_heavy_primary_second|tungsten:288|lead:576|20|atomiclegacy:bedrock_ore_new",
            "electrolyzer_metal/bedrock_light_crumbs|iron:144|copper:144|20|",
            "electrolyzer_metal/bedrock_light_primary_first|iron:576|copper:288|20|atomiclegacy:bedrock_ore_new",
            "electrolyzer_metal/bedrock_light_primary_second|iron:288|copper:576|20|atomiclegacy:bedrock_ore_new",
            "electrolyzer_metal/bedrock_nonmetal_crumbs|slag:144|-|20|atomiclegacy:bedrock_ore_fragment_coal*18,atomiclegacy:bedrock_ore_fragment_sulfur*18",
            "electrolyzer_metal/bedrock_nonmetal_primary_first|slag:144|-|20|atomiclegacy:bedrock_ore_fragment_coal*64,atomiclegacy:bedrock_ore_fragment_sulfur*36,atomiclegacy:bedrock_ore_new",
            "electrolyzer_metal/bedrock_nonmetal_primary_second|slag:144|-|20|atomiclegacy:bedrock_ore_fragment_coal*36,atomiclegacy:bedrock_ore_fragment_sulfur*64,atomiclegacy:bedrock_ore_new",
            "electrolyzer_metal/bedrock_rare_crumbs|cobalt:80|-|20|atomiclegacy:bedrock_ore_fragment_rareearth*10",
            "electrolyzer_metal/bedrock_rare_primary_first|cobalt:320|-|20|atomiclegacy:bedrock_ore_fragment_rareearth*20,atomiclegacy:bedrock_ore_new",
            "electrolyzer_metal/bedrock_rare_primary_second|cobalt:160|-|20|atomiclegacy:bedrock_ore_fragment_rareearth*40,atomiclegacy:bedrock_ore_new",
            "electrolyzer_metal/crystal_aluminium|aluminium:144|iron:144|600|atomiclegacy:chunk_ore_cryolite*4,atomiclegacy:powder_lithium_tiny*3",
            "electrolyzer_metal/crystal_beryllium|beryllium:432|lead:32|600|atomiclegacy:powder_lithium_tiny*3,atomiclegacy:powder_quartz*2",
            "electrolyzer_metal/crystal_cobalt|cobalt:216|iron:288|600|atomiclegacy:powder_copper*4,atomiclegacy:powder_lithium_tiny*3",
            "electrolyzer_metal/crystal_copper|copper:432|lead:32|600|atomiclegacy:powder_lithium_tiny*3,atomiclegacy:sulfur*2",
            "electrolyzer_metal/crystal_gold|gold:432|lead:144|600|atomiclegacy:powder_lithium_tiny*3,atomiclegacy:nugget_mercury*2",
            "electrolyzer_metal/crystal_iron|iron:432|titanium:144|600|atomiclegacy:powder_lithium_tiny*3",
            "electrolyzer_metal/crystal_lead|lead:432|gold:144|600|atomiclegacy:powder_lithium_tiny*3",
            "electrolyzer_metal/crystal_lithium|lithium:432|boron:144|600|atomiclegacy:powder_quartz*2,atomiclegacy:fluorite*2",
            "electrolyzer_metal/crystal_plutonium|plutonium:432|polonium:144|600|atomiclegacy:powder_lithium_tiny*3",
            "electrolyzer_metal/crystal_rare|zirconium:48|boron:16|600|atomiclegacy:powder_desh_mix*3",
            "electrolyzer_metal/crystal_schrabidium|schrabidium:432|plutonium:144|600|atomiclegacy:powder_lithium_tiny*3",
            "electrolyzer_metal/crystal_schraranium|schrabidium:40|uranium:16|600|atomiclegacy:nugget_neptunium*2",
            "electrolyzer_metal/crystal_starmetal|dura_steel:288|cobalt:288|600|atomiclegacy:powder_astatine*3,atomiclegacy:nugget_mercury*8",
            "electrolyzer_metal/crystal_thorium|thorium:432|uranium:144|600|atomiclegacy:powder_lithium_tiny*3",
            "electrolyzer_metal/crystal_titanium|titanium:432|iron:144|600|atomiclegacy:powder_lithium_tiny*3",
            "electrolyzer_metal/crystal_trixite|plutonium:216|cobalt:288|600|atomiclegacy:powder_niobium*4,atomiclegacy:powder_nitan_mix*2",
            "electrolyzer_metal/crystal_tungsten|tungsten:432|iron:144|600|atomiclegacy:powder_lithium_tiny*3",
            "electrolyzer_metal/crystal_uranium|uranium:432|radium:32|600|atomiclegacy:powder_lithium_tiny*3"
    );
    private static final List<String> EXPECTED_ROCK = List.of(
            "rock_mill/basalt|1|water:250|colloid:250|200|25|minecraft:gravel@50,atomiclegacy:powder_ash_misc@25,atomiclegacy:powder_quartz@15,atomiclegacy:gravel_obsidian@10",
            "rock_mill/bauxite|1|water:250|colloid:250|200|25|minecraft:gravel@25,minecraft:clay_ball@25,atomiclegacy:stone_resource_hematite@25,atomiclegacy:ore_titanium@25",
            "rock_mill/clay|2|colloid:2500|-|200|25|minecraft:clay_ball*4@1",
            "rock_mill/cobblestone|1|water:250|colloid:250|100|25|minecraft:gravel@95,atomiclegacy:powder_quartz@5",
            "rock_mill/colloid|-|colloid:1000|-|100|25|minecraft:sand@1",
            "rock_mill/gneiss|1|water:250|colloid:250|200|25|minecraft:gravel@50,minecraft:sand@10,atomiclegacy:powder_lithium@25,atomiclegacy:powder_niobium@5,atomiclegacy:powder_uranium@5,atomiclegacy:powder_gold@5",
            "rock_mill/gravel|1|water:250|colloid:250|100|25|minecraft:sand@75,minecraft:flint@20,atomiclegacy:powder_boron@5",
            "rock_mill/hematite|1|water:250|colloid:250|200|25|minecraft:gravel@65,atomiclegacy:powder_iron@25,atomiclegacy:powder_titanium@10",
            "rock_mill/netherrack|1|water:250|lava:100|100|25|minecraft:gravel@50,minecraft:soul_sand@25,minecraft:glowstone_dust@15,atomiclegacy:powder_quartz@10",
            "rock_mill/sand|1|water:250|colloid:250|100|25|atomiclegacy:dust@90,atomiclegacy:powder_calcium@5,atomiclegacy:fluorite@5",
            "rock_mill/soul_sand|1|water:250|lava:100|100|25|minecraft:sand@50,atomiclegacy:powder_fire@25,atomiclegacy:powder_uranium@15,minecraft:blaze_powder@5,minecraft:nether_wart@5",
            "rock_mill/swamp|1|-|-|100|25|atomiclegacy:briquette_peat*4@150,minecraft:dirt@49,atomiclegacy:bacteria_anthrax_inert@1" // [parity d2] rock.swamp
    );
    private static final List<String> EXPECTED_CRUCIBLE = List.of(
            "crucible/arsenic_bronze|crucible.abronze|9|atomiclegacy:ingot_arsenic_bronze|copper:64,arsenic:8,flux:24|arsenic_bronze:72,slag:24",
            "crucible/bismuth_bronze|crucible.bbronze|9|atomiclegacy:ingot_bismuth_bronze|copper:64,bismuth:8,flux:24|bismuth_bronze:72,slag:24",
            "crucible/bscco|crucible.bscco|3|atomiclegacy:ingot_bscco|bismuth:16,strontium:16,calcium:16,copper:24|bscco:72",
            "crucible/cdalloy|crucible.cdalloy|9|atomiclegacy:ingot_cdalloy|steel:64,cadmium:8|cdalloy:72",
            "crucible/combine_steel|crucible.cmb|3|atomiclegacy:ingot_combine_steel|magnetized_tungsten:48,mud:24|combine_steel:72",
            "crucible/ferrouranium|crucible.ferro|3|atomiclegacy:ingot_ferrouranium|steel:16,u238:8|ferrouranium:24",
            "crucible/hematite|crucible.hematite|6|atomiclegacy:stone_resource_hematite|hematite:144,flux:16|iron:72,slag:24",
            "crucible/high_speed_steel|crucible.hss|9|atomiclegacy:ingot_dura_steel|steel:40,tungsten:24,cobalt:8|dura_steel:72",
            "crucible/magnetized_tungsten|crucible.magtung|3|atomiclegacy:ingot_magnetized_tungsten|tungsten:72,schrabidium:8|magnetized_tungsten:72",
            "crucible/malachite|crucible.malachite|6|atomiclegacy:stone_resource_malachite|malachite:144,flux:16|copper:72,slag:24",
            "crucible/redcopper|crucible.redcopper|2|atomiclegacy:ingot_red_copper|copper:8,redstone:8|red_copper:16",
            "crucible/steel|crucible.steel|20|atomiclegacy:ingot_steel|iron:16,carbon:24,flux:8|steel:16",
            "crucible/tcalloy|crucible.tcalloy|9|atomiclegacy:ingot_tcalloy|steel:64,technetium:8|tcalloy:72"
    );

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        ServerLevelData data = (ServerLevelData) level.getLevelData();
        long savedTime = level.getGameTime();
        site = 0;
        try {
            recipeTables(server, problems, cases);
            materials(problems, cases);
            molds(problems, cases);
            crucible(level, data, problems, cases);
            casting(level, problems, cases);
            heaters(level, problems, cases);
            arcWelder(level, problems, cases);
            mixer(level, problems, cases);
        } catch (RuntimeException | ReflectiveOperationException e) {
            AtomicLegacy.LOGGER.error("Parity Q1 diagnostics crashed", e);
            problems.add("Q1 diagnostics crashed: " + e);
        } finally {
            data.setGameTime(savedTime);
            for (int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
            FORCED.clear();
        }
        return cases[0];
    }

    // ---------------------------------------------------------------- recipe tables

    private static String id(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return stack.getCount() == 1 ? id : id + "*" + stack.getCount();
    }

    private static String fluid(HbmFluidDefinition fluid, int amount) {
        return fluid.name() + ":" + amount;
    }

    private static void recipeTables(MinecraftServer server, List<String> problems, int[] cases) {
        var rm = server.getRecipeManager();
        List<String> arc = new ArrayList<>();
        for (RecipeHolder<ArcWelderRecipe> h : rm.getAllRecipesFor(HbmRecipeTypes.ARC_WELDER.get())) {
            ArcWelderRecipe r = h.value();
            arc.add(h.id().getPath() + "|" + id(r.result()) + "|" + r.duration() + "|" + r.consumption() + "|"
                    + r.fluid().map(f -> fluid(f.fluid(), f.amount())).orElse("-") + "|"
                    + r.inputs().stream().map(i -> String.valueOf(i.count())).collect(Collectors.joining(",")));
        }
        compare(EXPECTED_ARC, arc, problems, cases, "arc welder recipes");

        List<String> mixer = new ArrayList<>();
        for (RecipeHolder<MixerRecipe> h : rm.getAllRecipesFor(HbmRecipeTypes.MIXER.get())) {
            MixerRecipe r = h.value();
            mixer.add(h.id().getPath() + "|" + fluid(r.output().fluid(), r.output().amount()) + "|" + r.duration() + "|"
                    + r.input1().map(f -> fluid(f.fluid(), f.amount())).orElse("-") + "|"
                    + r.input2().map(f -> fluid(f.fluid(), f.amount())).orElse("-") + "|"
                    + r.solidInput().map(s -> String.valueOf(s.count())).orElse("-"));
        }
        compare(EXPECTED_MIXER, mixer, problems, cases, "mixer recipes");

        List<String> efluid = new ArrayList<>();
        for (RecipeHolder<ElectrolyzerFluidRecipe> h : rm.getAllRecipesFor(HbmRecipeTypes.ELECTROLYZER_FLUID.get())) {
            ElectrolyzerFluidRecipe r = h.value();
            efluid.add(h.id().getPath() + "|" + fluid(r.input().fluid(), r.input().amount()) + "|"
                    + fluid(r.output1().fluid(), r.output1().amount()) + "|" + fluid(r.output2().fluid(), r.output2().amount()) + "|"
                    + r.duration() + "|" + r.byproducts().stream().map(ParityQ1Diagnostics::id).collect(Collectors.joining(",")));
        }
        compare(EXPECTED_EFLUID, efluid, problems, cases, "electrolyser fluid recipes");

        List<String> emetal = new ArrayList<>();
        for (RecipeHolder<ElectrolyzerMetalRecipe> h : rm.getAllRecipesFor(HbmRecipeTypes.ELECTROLYZER_METAL.get())) {
            ElectrolyzerMetalRecipe r = h.value();
            emetal.add(h.id().getPath() + "|" + material(r.output1()) + "|" + material(r.output2()) + "|" + r.duration() + "|"
                    + r.byproducts().stream().map(ParityQ1Diagnostics::id).collect(Collectors.joining(",")));
        }
        compare(EXPECTED_EMETAL, emetal, problems, cases, "electrolyser metal recipes");

        List<String> rock = new ArrayList<>();
        for (RecipeHolder<RockMillRecipe> h : rm.getAllRecipesFor(HbmRecipeTypes.ROCK_MILL.get())) {
            RockMillRecipe r = h.value();
            rock.add(h.id().getPath() + "|" + (r.input().isNone() ? "-" : String.valueOf(r.input().count())) + "|"
                    + r.inputFluid().map(f -> fluid(f.type(), f.amount())).orElse("-") + "|"
                    + r.outputFluid().map(f -> fluid(f.type(), f.amount())).orElse("-") + "|"
                    + r.duration() + "|" + r.power() + "|"
                    + r.outputs().stream().map(o -> id(o.stack()) + "@" + o.weight()).collect(Collectors.joining(",")));
        }
        compare(EXPECTED_ROCK, rock, problems, cases, "rock mill recipes");

        List<String> crucible = new ArrayList<>();
        List<RecipeHolder<CrucibleRecipe>> crucibleHolders = com.atomiclegacy.inventory.recipes.CrucibleRecipeOrder.sorted(
                rm.getAllRecipesFor(HbmRecipeTypes.CRUCIBLE.get()));
        for (RecipeHolder<CrucibleRecipe> h : crucibleHolders) {
            CrucibleRecipe r = h.value();
            crucible.add(h.id().getPath() + "|" + r.group() + "|" + r.frequency() + "|" + id(r.icon()) + "|"
                    + r.input().stream().map(m -> m.material().name() + ":" + m.amount()).collect(Collectors.joining(",")) + "|"
                    + r.output().stream().map(m -> m.material().name() + ":" + m.amount()).collect(Collectors.joining(",")));
        }
        compare(EXPECTED_CRUCIBLE, crucible, problems, cases, "crucible recipes");
        List<String> order = crucibleHolders.stream().map(h -> h.value().group()).toList();
        check(order.equals(com.atomiclegacy.inventory.recipes.CrucibleRecipeOrder.ORIGINAL_ORDER), problems, cases,
                "crucible recipe order " + order);
    }

    private static String material(ElectrolyzerMetalRecipe.MaterialOutput output) {
        return output == null || output.amount() <= 0 ? "-" : output.material() + ":" + output.amount();
    }

    // ---------------------------------------------------------------- materials and molds

    private static void materials(List<String> problems, int[] cases) {
        smelt(problems, cases, new ItemStack(Items.REDSTONE), "redstone:72");
        smelt(problems, cases, new ItemStack(Items.CHARCOAL), "carbon:24");
        smelt(problems, cases, new ItemStack(Items.COAL), "carbon:36");
        smelt(problems, cases, new ItemStack(Items.RAIL), "iron:27");
        smelt(problems, cases, new ItemStack(Items.POWERED_RAIL), "gold:72,redstone:12");
        smelt(problems, cases, new ItemStack(Items.MINECART), "iron:360");
        smelt(problems, cases, new ItemStack(Items.DIAMOND), "carbon:72");
        smelt(problems, cases, new ItemStack(Items.EMERALD), "beryllium:54");
        smelt(problems, cases, item("stone_resource_limestone"), "flux:720");
        smelt(problems, cases, item("coke_coal"), "carbon:54");
        smelt(problems, cases, item("powder_ash_coal"), "carbon:16");
        smelt(problems, cases, item("casing_small"), "gunmetal:18");
        smelt(problems, cases, item("chunk_ore_cryolite"), "aluminium:72,sodium:72");
        smelt(problems, cases, item("ingot_advanced_alloy"), "");
        smelt(problems, cases, item("blades_steel"), "steel:288");
        check(FoundryMaterial.byName("advanced_alloy").isEmpty(), problems, cases, "advanced alloy is no foundry material (MAT_ALLOY commented out)");
        checkEq(FoundryMaterial.get("uranium").id(), 9200, problems, cases, "MAT_URANIUM id");
        checkEq(FoundryMaterial.get("thorium").id(), 9032, problems, cases, "MAT_THORIUM id");
        checkEq(FoundryMaterial.get("thorium").moltenColor(), 0xBF825F, problems, cases, "MAT_THORIUM molten colour");
        checkEq(FoundryMaterial.get("polonium").id(), 8410, problems, cases, "MAT_POLONIUM id");
        checkEq(FoundryMaterial.get("radium").id(), 8826, problems, cases, "MAT_RADIUM id");
        checkEq(FoundryMaterial.get("niobium").moltenColor(), 0xD576B1, problems, cases, "MAT_NIOBIUM molten colour");
        checkEq(FoundryMaterial.get("boron").moltenColor(), 0xAD72AE, problems, cases, "MAT_BORON molten colour");
        checkEq(FoundryMaterial.get("lithium").moltenColor(), 0xD6D6D6, problems, cases, "MAT_LITHIUM molten colour");
        check(FoundryMaterial.get("graphite").smeltsInto() == FoundryMaterial.get("carbon"), problems, cases, "graphite converts to carbon");
    }

    private static void smelt(List<String> problems, int[] cases, ItemStack stack, String expected) {
        String actual = FoundryMaterial.smeltingMaterialsFromItem(stack).stream()
                .map(m -> m.material().name() + ":" + m.amount()).collect(Collectors.joining(","));
        check(actual.equals(expected), problems, cases, "crucible materials of " + id(stack) + ": " + actual + " expected " + expected);
    }

    private static void molds(List<String> problems, int[] cases) {
        List<String> actual = FoundryMoldItem.molds().stream()
                .map(m -> m.id() + ":" + m.size() + ":" + m.name() + ":" + m.cost() + ":" + m.titleKey())
                .toList();
        List<String> expected = List.of(
                "0:0:nugget:8:nugget", "1:0:billet:48:billet", "2:0:ingot:72:ingot", "3:0:plate:72:plate",
                "4:0:wire:72:wireFine", "19:0:plate_cast:216:plateTriple", "20:0:wire_dense:72:wireDense",
                "5:0:blade:216:blade", "6:0:blades:288:blades", "7:0:stamp:288:stamp", "8:0:shell:288:shell",
                "9:0:pipe:216:ntmpipe", "10:1:ingots:648:ingot", "11:1:plates:648:plate", "13:1:plates_cast:648:plateTriple",
                "21:1:wires_dense:648:wireDense", "12:1:block:648:block", "16:0:c9:18:c9", "17:0:c50:36:c50",
                "22:0:barrel_light:216:barrelLight", "23:0:barrel_heavy:432:barrelHeavy", "24:0:receiver_light:288:receiverLight",
                "25:0:receiver_heavy:648:receiverHeavy", "26:0:mechanism:288:gunMechanism", "27:0:stock:288:stock",
                "28:0:grip:144:grip");
        check(actual.equals(expected), problems, cases, "ItemMold ids/sizes/costs/order " + actual);
        mold(problems, cases, 16, "gunmetal", "atomiclegacy:casing_small");
        mold(problems, cases, 16, "weaponsteel", "atomiclegacy:casing_small_steel");
        mold(problems, cases, 17, "gunmetal", "atomiclegacy:casing_large");
        mold(problems, cases, 6, "steel", "atomiclegacy:blades_steel");
        mold(problems, cases, 2, "iron", "minecraft:iron_ingot");
        mold(problems, cases, 12, "stone", "minecraft:stone");
        check(!BuiltInRegistries.ITEM.containsKey(AtomicLegacy.id("mold_pipes")), problems, cases, "no invented pipes mold");
        check(!BuiltInRegistries.ITEM.containsKey(AtomicLegacy.id("blades_advanced_alloy")), problems, cases, "no invented advanced alloy blades");
        checkEq(item("mold_c9").getMaxStackSize(), 64, problems, cases, "ItemMold stack size");
        checkEq(item("blades_steel").getMaxDamage(), 400, problems, cases, "ItemBlades(400) steel");
        checkEq(item("blades_titanium").getMaxDamage(), 500, problems, cases, "ItemBlades(500) titanium");
    }

    private static void mold(List<String> problems, int[] cases, int id, String material, String expected) {
        FoundryMoldItem.Mold mold = FoundryMoldItem.molds().stream().filter(m -> m.id() == id).findFirst().orElseThrow();
        String actual = mold.outputFor(FoundryMaterial.get(material)).map(ParityQ1Diagnostics::id).orElse("-");
        check(actual.equals(expected), problems, cases, "mold " + id + " + " + material + " -> " + actual + " expected " + expected);
    }

    // ---------------------------------------------------------------- machines

    @SuppressWarnings("unchecked")
    private static void crucible(ServerLevel level, ServerLevelData data, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level, 100);
        level.setBlock(pos.below(), HbmBlocks.HEATER_FIREBOX.get().defaultBlockState(), 2 | 16);
        level.setBlock(pos, HbmBlocks.MACHINE_CRUCIBLE.get().defaultBlockState(), 2 | 16);
        CrucibleBlockEntity crucible = (CrucibleBlockEntity) level.getBlockEntity(pos);
        HeaterBlockEntity firebox = (HeaterBlockEntity) level.getBlockEntity(pos.below());
        checkEq(crucible.getMaxStackSize(), 1, problems, cases, "crucible stack limit");
        setField(firebox, "heatEnergy", 100_000);
        setField(crucible, "heat", 0);
        CrucibleBlockEntity.tick(level, pos, level.getBlockState(pos), crucible);
        checkEq(crucible.heat(), 25_000, problems, cases, "crucible pulls ceil(diff * 0.25)");
        checkEq((int) field(firebox, "heatEnergy"), 75_000, problems, cases, "firebox loses the pulled heat");
        setField(firebox, "heatEnergy", 50_000);
        setField(crucible, "heat", 50_000);
        CrucibleBlockEntity.tick(level, pos, level.getBlockState(pos), crucible);
        checkEq(crucible.heat(), 50_000, problems, cases, "equal heat: no pull and no cooling");

        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2 | 16);
        setField(crucible, "heat", 100_000);
        setField(crucible, "progress", 0);
        crucible.setItem(1, new ItemStack(Items.IRON_INGOT, 1));
        CrucibleBlockEntity.tick(level, pos, level.getBlockState(pos), crucible);
        checkEq(crucible.heat(), 97_500, problems, cases, "full crucible: no cooling, smelting takes (heat - 50000) * 0.05");
        checkEq(crucible.progress(), 2_500, problems, cases, "crucible smelt progress (heat - 50000) * 0.05");
        crucible.setItem(1, ItemStack.EMPTY);

        crucible.setSelectedRecipe(AtomicLegacy.id("crucible/steel"));
        List<FoundryMaterialStack> stack = (List<FoundryMaterialStack>) field(crucible, "recipeStack");
        stack.clear();
        stack.add(new FoundryMaterialStack(FoundryMaterial.get("iron"), 16));
        stack.add(new FoundryMaterialStack(FoundryMaterial.get("carbon"), 24));
        stack.add(new FoundryMaterialStack(FoundryMaterial.get("flux"), 8));
        data.setGameTime(4_001L);
        CrucibleBlockEntity.tick(level, pos, level.getBlockState(pos), crucible);
        checkEq(amount(crucible.recipeStack(), "steel"), 0, problems, cases, "steel only every 20 ticks");
        data.setGameTime(4_020L);
        CrucibleBlockEntity.tick(level, pos, level.getBlockState(pos), crucible);
        checkEq(amount(crucible.recipeStack(), "steel"), 16, problems, cases, "crucible.steel 2 iron + 3 carbon + 1 flux nuggets -> 2 steel nuggets");
        checkEq(amount(crucible.recipeStack(), "iron"), 0, problems, cases, "crucible.steel consumes iron");
        stack.clear();
        ((List<FoundryMaterialStack>) field(crucible, "wasteStack")).clear();
        crucible.clearContent();
        cleanup(level, pos);
    }

    private static int amount(List<FoundryMaterialStack> stacks, String material) {
        return stacks.stream().filter(s -> s.material() == FoundryMaterial.get(material)).mapToInt(FoundryMaterialStack::amount).sum();
    }

    private static void casting(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level, 100);
        level.setBlock(pos, HbmBlocks.FOUNDRY_MOLD.get().defaultBlockState(), 2 | 16);
        FoundryCastingBlockEntity mold = (FoundryCastingBlockEntity) level.getBlockEntity(pos);
        mold.setItem(FoundryCastingBlockEntity.MOLD_SLOT, FoundryMoldItem.stackFor(2));
        FoundryMaterialStack iron = new FoundryMaterialStack(FoundryMaterial.get("iron"), 72);
        check(!mold.canAcceptPartialPour(level, pos, 0, 0, 0, Direction.DOWN, iron), problems, cases, "mold rejects pours from below");
        check(mold.getSlotsForFace(Direction.UP).length == 1 && !mold.canPlaceItemThroughFace(0, FoundryMoldItem.stackFor(2), Direction.UP),
                problems, cases, "mold automation: output slot only, no insertion");
        FoundryMaterialStack left = mold.pour(level, pos, 0, 0, 0, Direction.UP, iron);
        check(left == null && mold.amount() == 72, problems, cases, "mold takes one ingot of iron");
        for (int tick = 0; tick < 99; tick++) FoundryCastingBlockEntity.tick(level, pos, level.getBlockState(pos), mold);
        check(!mold.hasOutput(), problems, cases, "fresh mold cools for 100 ticks");
        FoundryCastingBlockEntity.tick(level, pos, level.getBlockState(pos), mold);
        check(id(mold.getItem(FoundryCastingBlockEntity.OUTPUT_SLOT)).equals("minecraft:iron_ingot"), problems, cases,
                "mold casts an iron ingot after 100 ticks: " + id(mold.getItem(FoundryCastingBlockEntity.OUTPUT_SLOT)));

        mold.clearContent();
        level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 2 | 16);
        CrucibleUtil.PourTarget blocked = CrucibleUtil.findPouringTarget(level, pos.getX() + 0.5D, pos.getY() + 2.25D, pos.getZ() + 0.5D, 6.0D);
        check(blocked != null && blocked.pos().equals(pos.above()) && blocked.acceptor() == null, problems, cases,
                "pour hitscan stops at the first solid block");
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2 | 16);
        CrucibleUtil.PourTarget open = CrucibleUtil.findPouringTarget(level, pos.getX() + 0.5D, pos.getY() + 2.25D, pos.getZ() + 0.5D, 6.0D);
        check(open != null && open.pos().equals(pos) && open.side() == Direction.UP && open.acceptor() != null, problems, cases,
                "pour hitscan reaches the mold from above");
        cleanup(level, pos);
    }

    private static void heaters(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level, 100);
        level.setBlock(pos.below(), HbmBlocks.MACHINE_ASHPIT.get().defaultBlockState(), 2 | 16);
        level.setBlock(pos, HbmBlocks.HEATER_FIREBOX.get().defaultBlockState(), 2 | 16);
        HeaterBlockEntity firebox = (HeaterBlockEntity) level.getBlockEntity(pos);
        BlockEntity ashpit = level.getBlockEntity(pos.below());
        firebox.setItem(0, new ItemStack(Items.COAL, 2));
        HeaterBlockEntity.tick(level, pos, level.getBlockState(pos), firebox);
        checkEq((int) field(firebox, "burnTime"), 2_000, problems, cases, "firebox coal burn time 1600 * 1.25");
        checkEq((int) field(firebox, "heatEnergy"), 200, problems, cases, "firebox heats on the ignition tick");
        check(ashpit instanceof AshpitBlockEntity, problems, cases, "ashpit below the firebox");
        if (ashpit instanceof AshpitBlockEntity) {
            checkEq((int) field(ashpit, "ashLevelCoal"), 2_000, problems, cases, "firebox feeds coal ash = base burn time");
        }
        HeaterBlockEntity.tick(level, pos, level.getBlockState(pos), firebox);
        checkEq((int) field(firebox, "burnTime"), 1_999, problems, cases, "firebox burn time decrement");
        checkEq((int) field(firebox, "heatEnergy"), 400, problems, cases, "firebox heat after two ticks");
        firebox.clearContent();
        cleanup(level, pos);

        BlockPos charcoalPos = pad(level, 100);
        level.setBlock(charcoalPos, HbmBlocks.HEATER_FIREBOX.get().defaultBlockState(), 2 | 16);
        HeaterBlockEntity charcoal = (HeaterBlockEntity) level.getBlockEntity(charcoalPos);
        charcoal.setItem(0, new ItemStack(Items.CHARCOAL));
        HeaterBlockEntity.tick(level, charcoalPos, level.getBlockState(charcoalPos), charcoal);
        checkEq((int) field(charcoal, "burnTime"), 1_600, problems, cases, "charcoal has no coal bonus (no ore dictionary name)");
        checkEq((int) field(charcoal, "burnHeat"), 100, problems, cases, "charcoal burn heat");
        cleanup(level, charcoalPos);

        BlockPos ovenPos = pad(level, 100);
        level.setBlock(ovenPos, HbmBlocks.HEATER_OVEN.get().defaultBlockState(), 2 | 16);
        HeaterBlockEntity oven = (HeaterBlockEntity) level.getBlockEntity(ovenPos);
        oven.setItem(0, new ItemStack(Items.COAL));
        HeaterBlockEntity.tick(level, ovenPos, level.getBlockState(ovenPos), oven);
        checkEq((int) field(oven, "burnTime"), 250, problems, cases, "oven coal burn time (int) (2000 * 0.125)");
        checkEq((int) field(oven, "burnHeat"), 1_000, problems, cases, "oven coal burn heat 500 * 2");
        cleanup(level, ovenPos);
    }

    private static void arcWelder(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level, 100);
        level.setBlock(pos, HbmBlocks.MACHINE_ARC_WELDER.get().defaultBlockState(), 2 | 16);
        ArcWelderBlockEntity welder = (ArcWelderBlockEntity) level.getBlockEntity(pos);
        welder.setItem(0, new ItemStack(item("plate_cast_tcalloy").getItem(), 2));
        setField(welder, "power", 1_000_000L);
        ArcWelderBlockEntity.tick(level, pos, level.getBlockState(pos), welder);
        checkEq((int) field(welder, "progress"), 0, problems, cases, "TC alloy weld waits for oxygen");
        checkEq((long) field(welder, "maxPower"), 20_000_000L, problems, cases, "recipe found without fluid: maxPower = consumption * 20");
        HbmFluidTank tank = (HbmFluidTank) field(welder, "tank");
        tank.setType(HbmFluids.byName("oxygen").orElseThrow());
        tank.setAmount(1_000);
        ArcWelderBlockEntity.tick(level, pos, level.getBlockState(pos), welder);
        checkEq((int) field(welder, "progress"), 1, problems, cases, "TC alloy weld runs with oxygen");
        checkEq((long) field(welder, "power"), 0L, problems, cases, "TC alloy weld draws 1,000,000 HE per tick");
        ArcWelderBlockEntity.tick(level, pos, level.getBlockState(pos), welder);
        checkEq((int) field(welder, "progress"), 0, problems, cases, "progress resets when power runs out");
        welder.clearContent();
        cleanup(level, pos);
    }

    @SuppressWarnings("unchecked")
    private static void mixer(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level, 100);
        level.setBlock(pos, HbmBlocks.MACHINE_MIXER.get().defaultBlockState(), 2 | 16);
        MixerBlockEntity mixer = (MixerBlockEntity) level.getBlockEntity(pos);
        setField(mixer, "configuredOutput", HbmFluids.byName("fracksol").orElseThrow());
        Method outputs = MixerBlockEntity.class.getDeclaredMethod("outputRecipes");
        outputs.setAccessible(true);
        List<String> ids = ((List<RecipeHolder<MixerRecipe>>) outputs.invoke(mixer)).stream().map(h -> h.id().getPath()).toList();
        check(ids.equals(List.of("mixer/fracksol_sulfuric", "mixer/fracksol_sulfur")), problems, cases, "mixer FRACKSOL recipe index order " + ids);
        setField(mixer, "configuredOutput", HbmFluids.byName("lubricant").orElseThrow());
        ids = ((List<RecipeHolder<MixerRecipe>>) outputs.invoke(mixer)).stream().map(h -> h.id().getPath()).toList();
        check(ids.equals(List.of("mixer/lubricant_heatingoil", "mixer/lubricant_fishoil", "mixer/lubricant_sunfloweroil")),
                problems, cases, "mixer LUBRICANT recipe index order " + ids);
        mixer.clearContent();
        cleanup(level, pos);
    }

    // ---------------------------------------------------------------- helpers

    private static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id(path)));
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        return findField(owner.getClass(), name).get(owner);
    }

    private static void setField(Object owner, String name, Object value) throws ReflectiveOperationException {
        findField(owner.getClass(), name).set(owner, value);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void check(boolean condition, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (!condition) problems.add("Q1: " + message);
    }

    private static void checkEq(long actual, long expected, List<String> problems, int[] cases, String message) {
        check(actual == expected, problems, cases, message + ": " + actual + " expected " + expected);
    }

    private static void compare(List<String> expectedIn, List<String> actualIn, List<String> problems, int[] cases, String what) {
        List<String> expected = new ArrayList<>(expectedIn);
        List<String> actual = new ArrayList<>(actualIn);
        Collections.sort(expected);
        Collections.sort(actual);
        List<String> missing = new ArrayList<>(expected);
        for (String a : actual) missing.remove(a);
        List<String> extra = new ArrayList<>(actual);
        for (String e : expected) extra.remove(e);
        cases[0] += expected.size();
        if (!missing.isEmpty() || !extra.isEmpty()) problems.add("Q1: " + what + " differ: missing " + missing + ", extra " + extra);
    }

    private static BlockPos pad(ServerLevel level, int y) {
        int index = site++;
        BlockPos pos = new BlockPos(BASE_X + index * SPACING, y, BASE_Z);
        for (int cx = (pos.getX() - 8) >> 4; cx <= (pos.getX() + 8) >> 4; cx++) {
            for (int cz = (pos.getZ() - 8) >> 4; cz <= (pos.getZ() + 8) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
                FORCED.add(new int[] {cx, cz});
            }
        }
        for (int dx = -3; dx <= 3; dx++) for (int dy = -4; dy <= 8; dy++) for (int dz = -3; dz <= 3; dz++) {
            BlockPos p = pos.offset(dx, dy, dz);
            level.setBlock(p, dy < -1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        return pos;
    }

    private static void cleanup(ServerLevel level, BlockPos pos) {
        for (BlockPos cursor : BlockPos.betweenClosed(pos.offset(-3, -4, -3), pos.offset(3, 8, 3))) {
            BlockEntity be = level.getBlockEntity(cursor);
            if (be instanceof net.minecraft.world.Container container) container.clearContent();
            if (!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }

    @SuppressWarnings("unused")
    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(AtomicLegacy.id(path));
    }

    @SuppressWarnings("unused")
    private static ResourceLocation rl(String path) {
        return AtomicLegacy.id(path);
    }
}
