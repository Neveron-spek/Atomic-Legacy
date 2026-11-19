package com.atomiclegacy.registry;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.recipes.AssemblyMachineRecipe;
import com.atomiclegacy.inventory.recipes.ArcWelderRecipe;
import com.atomiclegacy.inventory.recipes.ArcFurnaceRecipe;
import com.atomiclegacy.inventory.recipes.AmmoPressRecipe;
import com.atomiclegacy.inventory.recipes.BlastFurnaceFuelRecipe;
import com.atomiclegacy.inventory.recipes.BlastFurnaceRecipe;
import com.atomiclegacy.inventory.recipes.BdclRecipe;
import com.atomiclegacy.inventory.recipes.BreederReactorRecipe;
import com.atomiclegacy.inventory.recipes.CargoShellRecipe;
import com.atomiclegacy.inventory.recipes.CentrifugeRecipe;
import com.atomiclegacy.inventory.recipes.ChemicalPlantRecipe;
import com.atomiclegacy.inventory.recipes.CokerRecipe;
import com.atomiclegacy.inventory.recipes.CombinationOvenRecipe;
import com.atomiclegacy.inventory.recipes.CompressorRecipe;
import com.atomiclegacy.inventory.recipes.CustomMachineControllerRecipe; // [parity X1: custom machines]
import com.atomiclegacy.inventory.recipes.ConveyorExpressRecipe;
import com.atomiclegacy.inventory.recipes.CrackingRecipe;
import com.atomiclegacy.inventory.recipes.CrystallizerRecipe;
import com.atomiclegacy.inventory.recipes.CrucibleRecipe;
import com.atomiclegacy.inventory.recipes.CyclotronRecipe;
import com.atomiclegacy.inventory.recipes.ElectrolyzerFluidRecipe;
import com.atomiclegacy.inventory.recipes.ElectrolyzerMetalRecipe;
import com.atomiclegacy.inventory.recipes.ExposureChamberRecipe;
import com.atomiclegacy.inventory.recipes.FractionTowerRecipe;
import com.atomiclegacy.inventory.recipes.FluidDuctRetypeRecipe;
import com.atomiclegacy.inventory.recipes.FuelPoolRecipe;
import com.atomiclegacy.inventory.recipes.FusionBreederFluidRecipe;
import com.atomiclegacy.inventory.recipes.FusionBreederItemRecipe;
import com.atomiclegacy.inventory.recipes.FusionRecipe;
import com.atomiclegacy.inventory.recipes.GasCentrifugeRecipe;
import com.atomiclegacy.inventory.recipes.LiquefactionRecipe;
import com.atomiclegacy.inventory.recipes.LubricantCanisterRecipe;
import com.atomiclegacy.inventory.recipes.MixerRecipe;
import com.atomiclegacy.inventory.recipes.MkuCraftingRecipe;
import com.atomiclegacy.inventory.recipes.ParticleAcceleratorRecipe;
import com.atomiclegacy.inventory.recipes.PlasmaForgeRecipe;
import com.atomiclegacy.inventory.recipes.PressRecipe;
import com.atomiclegacy.inventory.recipes.PrecisionAssemblerRecipe;
import com.atomiclegacy.inventory.recipes.PyroOvenRecipe;
import com.atomiclegacy.inventory.recipes.PurexRecipe;
import com.atomiclegacy.inventory.recipes.RefineryRecipe;
import com.atomiclegacy.inventory.recipes.ReformingRecipe;
import com.atomiclegacy.inventory.recipes.HydrotreatingRecipe;
import com.atomiclegacy.inventory.recipes.RbmkOutgasserRecipe;
import com.atomiclegacy.inventory.recipes.RbmkFuelDisassemblyRecipe;
import com.atomiclegacy.inventory.recipes.RotaryFurnaceRecipe;
import com.atomiclegacy.inventory.recipes.RockMillRecipe;
import com.atomiclegacy.inventory.recipes.ShredderRecipe;
import com.atomiclegacy.inventory.recipes.SilexRecipe;
import com.atomiclegacy.inventory.recipes.ScrapsSplitRecipe;
import com.atomiclegacy.inventory.recipes.SolderingStationRecipe;
import com.atomiclegacy.inventory.recipes.SolidificationRecipe;
import com.atomiclegacy.inventory.recipes.StorageCrateUpgradeRecipe;
import com.atomiclegacy.inventory.recipes.SupercomputerRecipe;
import com.atomiclegacy.inventory.recipes.SpaceAssemblerRecipe;
import com.atomiclegacy.inventory.recipes.VacuumDistillRecipe;
import com.atomiclegacy.inventory.recipes.UniversalGrenadeRecipe;
import com.atomiclegacy.inventory.recipes.ingredient.BatteryIngredient;
import com.atomiclegacy.inventory.recipes.ingredient.FoundryMaterialIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class HbmRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, AtomicLegacy.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, AtomicLegacy.MOD_ID);
    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, AtomicLegacy.MOD_ID);

    public static final DeferredHolder<IngredientType<?>, IngredientType<FoundryMaterialIngredient>> FOUNDRY_MATERIAL_INGREDIENT =
            INGREDIENT_TYPES.register("foundry_material", () -> new IngredientType<>(FoundryMaterialIngredient.CODEC));
    public static final DeferredHolder<IngredientType<?>, IngredientType<BatteryIngredient>> BATTERY_INGREDIENT =
            INGREDIENT_TYPES.register("battery", () -> new IngredientType<>(BatteryIngredient.CODEC));
    public static final DeferredHolder<IngredientType<?>, IngredientType<com.atomiclegacy.inventory.recipes.ingredient.LegacyFluidContainerIngredient>> LEGACY_FLUID_CONTAINER_INGREDIENT =
            INGREDIENT_TYPES.register("legacy_fluid_container", () -> new IngredientType<>(com.atomiclegacy.inventory.recipes.ingredient.LegacyFluidContainerIngredient.CODEC));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ShredderRecipe>> SHREDDER =
            RECIPE_TYPES.register("shredder", () -> RecipeType.simple(AtomicLegacy.id("shredder")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<AssemblyMachineRecipe>> ASSEMBLY_MACHINE =
            RECIPE_TYPES.register("assembly_machine", () -> RecipeType.simple(AtomicLegacy.id("assembly_machine")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<PrecisionAssemblerRecipe>> PRECISION_ASSEMBLER =
            RECIPE_TYPES.register("precision_assembler", () -> RecipeType.simple(AtomicLegacy.id("precision_assembler")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ChemicalPlantRecipe>> CHEMICAL_PLANT =
            RECIPE_TYPES.register("chemical_plant", () -> RecipeType.simple(AtomicLegacy.id("chemical_plant")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<RefineryRecipe>> REFINERY =
            RECIPE_TYPES.register("refinery", () -> RecipeType.simple(AtomicLegacy.id("refinery")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<VacuumDistillRecipe>> VACUUM_DISTILL =
            RECIPE_TYPES.register("vacuum_distill", () -> RecipeType.simple(AtomicLegacy.id("vacuum_distill")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CokerRecipe>> COKER =
            RECIPE_TYPES.register("coking", () -> RecipeType.simple(AtomicLegacy.id("coking")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CombinationOvenRecipe>> COMBINATION_OVEN =
            RECIPE_TYPES.register("combination_oven", () -> RecipeType.simple(AtomicLegacy.id("combination_oven")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<RotaryFurnaceRecipe>> ROTARY_FURNACE =
            RECIPE_TYPES.register("rotary_furnace", () -> RecipeType.simple(AtomicLegacy.id("rotary_furnace")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<SolidificationRecipe>> SOLIDIFICATION =
            RECIPE_TYPES.register("solidification", () -> RecipeType.simple(AtomicLegacy.id("solidification")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<LiquefactionRecipe>> LIQUEFACTION =
            RECIPE_TYPES.register("liquefaction", () -> RecipeType.simple(AtomicLegacy.id("liquefaction")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<FractionTowerRecipe>> FRACTION_TOWER =
            RECIPE_TYPES.register("fraction_tower", () -> RecipeType.simple(AtomicLegacy.id("fraction_tower")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrackingRecipe>> CRACKING =
            RECIPE_TYPES.register("cracking", () -> RecipeType.simple(AtomicLegacy.id("cracking")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ReformingRecipe>> REFORMING =
            RECIPE_TYPES.register("reforming", () -> RecipeType.simple(AtomicLegacy.id("reforming")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<HydrotreatingRecipe>> HYDROTREATING =
            RECIPE_TYPES.register("hydrotreating", () -> RecipeType.simple(AtomicLegacy.id("hydrotreating")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<SolderingStationRecipe>> SOLDERING_STATION =
            RECIPE_TYPES.register("soldering_station", () -> RecipeType.simple(AtomicLegacy.id("soldering_station")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ArcWelderRecipe>> ARC_WELDER =
            RECIPE_TYPES.register("arc_welder", () -> RecipeType.simple(AtomicLegacy.id("arc_welder")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<RockMillRecipe>> ROCK_MILL =
            RECIPE_TYPES.register("rock_mill", () -> RecipeType.simple(AtomicLegacy.id("rock_mill")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<SupercomputerRecipe>> SUPERCOMPUTER =
            RECIPE_TYPES.register("supercomputer", () -> RecipeType.simple(AtomicLegacy.id("supercomputer")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<SpaceAssemblerRecipe>> SPACE_ASSEMBLER =
            RECIPE_TYPES.register("space_assembler", () -> RecipeType.simple(AtomicLegacy.id("space_assembler")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ArcFurnaceRecipe>> ARC_FURNACE =
            RECIPE_TYPES.register("arc_furnace", () -> RecipeType.simple(AtomicLegacy.id("arc_furnace")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CompressorRecipe>> COMPRESSOR =
            RECIPE_TYPES.register("compressor", () -> RecipeType.simple(AtomicLegacy.id("compressor")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<MixerRecipe>> MIXER =
            RECIPE_TYPES.register("mixer", () -> RecipeType.simple(AtomicLegacy.id("mixer")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<PressRecipe>> PRESS =
            RECIPE_TYPES.register("press", () -> RecipeType.simple(AtomicLegacy.id("press")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<PyroOvenRecipe>> PYRO_OVEN =
            RECIPE_TYPES.register("pyrolysis", () -> RecipeType.simple(AtomicLegacy.id("pyrolysis")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<AmmoPressRecipe>> AMMO_PRESS =
            RECIPE_TYPES.register("ammo_press", () -> RecipeType.simple(AtomicLegacy.id("ammo_press")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CentrifugeRecipe>> CENTRIFUGE =
            RECIPE_TYPES.register("centrifuge", () -> RecipeType.simple(AtomicLegacy.id("centrifuge")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<GasCentrifugeRecipe>> GAS_CENTRIFUGE =
            RECIPE_TYPES.register("gas_centrifuge", () -> RecipeType.simple(AtomicLegacy.id("gas_centrifuge")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrystallizerRecipe>> CRYSTALLIZER =
            RECIPE_TYPES.register("crystallizer", () -> RecipeType.simple(AtomicLegacy.id("crystallizer")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CyclotronRecipe>> CYCLOTRON =
            RECIPE_TYPES.register("cyclotron", () -> RecipeType.simple(AtomicLegacy.id("cyclotron")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ExposureChamberRecipe>> EXPOSURE_CHAMBER =
            RECIPE_TYPES.register("exposure_chamber", () -> RecipeType.simple(AtomicLegacy.id("exposure_chamber")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ParticleAcceleratorRecipe>> PARTICLE_ACCELERATOR =
            RECIPE_TYPES.register("particle_accelerator", () -> RecipeType.simple(AtomicLegacy.id("particle_accelerator")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<SilexRecipe>> SILEX =
            RECIPE_TYPES.register("silex", () -> RecipeType.simple(AtomicLegacy.id("silex")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ElectrolyzerFluidRecipe>> ELECTROLYZER_FLUID =
            RECIPE_TYPES.register("electrolyzer_fluid", () -> RecipeType.simple(AtomicLegacy.id("electrolyzer_fluid")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ElectrolyzerMetalRecipe>> ELECTROLYZER_METAL =
            RECIPE_TYPES.register("electrolyzer_metal", () -> RecipeType.simple(AtomicLegacy.id("electrolyzer_metal")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<BreederReactorRecipe>> BREEDER_REACTOR =
            RECIPE_TYPES.register("breeder_reactor", () -> RecipeType.simple(AtomicLegacy.id("breeder_reactor")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<FuelPoolRecipe>> FUEL_POOL =
            RECIPE_TYPES.register("fuel_pool", () -> RecipeType.simple(AtomicLegacy.id("fuel_pool")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<PurexRecipe>> PUREX =
            RECIPE_TYPES.register("purex", () -> RecipeType.simple(AtomicLegacy.id("purex")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<FusionRecipe>> FUSION =
            RECIPE_TYPES.register("fusion", () -> RecipeType.simple(AtomicLegacy.id("fusion")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<PlasmaForgeRecipe>> PLASMA_FORGE =
            RECIPE_TYPES.register("plasma_forge", () -> RecipeType.simple(AtomicLegacy.id("plasma_forge")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<FusionBreederFluidRecipe>> FUSION_BREEDER_FLUID =
            RECIPE_TYPES.register("fusion_breeder_fluid", () -> RecipeType.simple(AtomicLegacy.id("fusion_breeder_fluid")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<FusionBreederItemRecipe>> FUSION_BREEDER_ITEM =
            RECIPE_TYPES.register("fusion_breeder_item", () -> RecipeType.simple(AtomicLegacy.id("fusion_breeder_item")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<RbmkOutgasserRecipe>> RBMK_OUTGASSER =
            RECIPE_TYPES.register("rbmk_outgasser", () -> RecipeType.simple(AtomicLegacy.id("rbmk_outgasser")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrucibleRecipe>> CRUCIBLE =
            RECIPE_TYPES.register("crucible", () -> RecipeType.simple(AtomicLegacy.id("crucible")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<BlastFurnaceRecipe>> BLAST_FURNACE =
            RECIPE_TYPES.register("blast_furnace", () -> RecipeType.simple(AtomicLegacy.id("blast_furnace")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<BlastFurnaceFuelRecipe>> BLAST_FURNACE_FUEL =
            RECIPE_TYPES.register("blast_furnace_fuel", () -> RecipeType.simple(AtomicLegacy.id("blast_furnace_fuel")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShredderRecipe>> SHREDDER_SERIALIZER =
            RECIPE_SERIALIZERS.register("shredder", ShredderRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AssemblyMachineRecipe>> ASSEMBLY_MACHINE_SERIALIZER =
            RECIPE_SERIALIZERS.register("assembly_machine", AssemblyMachineRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PrecisionAssemblerRecipe>> PRECISION_ASSEMBLER_SERIALIZER =
            RECIPE_SERIALIZERS.register("precision_assembler", PrecisionAssemblerRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChemicalPlantRecipe>> CHEMICAL_PLANT_SERIALIZER =
            RECIPE_SERIALIZERS.register("chemical_plant", ChemicalPlantRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RefineryRecipe>> REFINERY_SERIALIZER =
            RECIPE_SERIALIZERS.register("refinery", RefineryRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<VacuumDistillRecipe>> VACUUM_DISTILL_SERIALIZER =
            RECIPE_SERIALIZERS.register("vacuum_distill", VacuumDistillRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CokerRecipe>> COKER_SERIALIZER =
            RECIPE_SERIALIZERS.register("coking", CokerRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CombinationOvenRecipe>> COMBINATION_OVEN_SERIALIZER =
            RECIPE_SERIALIZERS.register("combination_oven", CombinationOvenRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RotaryFurnaceRecipe>> ROTARY_FURNACE_SERIALIZER =
            RECIPE_SERIALIZERS.register("rotary_furnace", RotaryFurnaceRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SolidificationRecipe>> SOLIDIFICATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("solidification", SolidificationRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LiquefactionRecipe>> LIQUEFACTION_SERIALIZER =
            RECIPE_SERIALIZERS.register("liquefaction", LiquefactionRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FractionTowerRecipe>> FRACTION_TOWER_SERIALIZER =
            RECIPE_SERIALIZERS.register("fraction_tower", FractionTowerRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrackingRecipe>> CRACKING_SERIALIZER =
            RECIPE_SERIALIZERS.register("cracking", CrackingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BdclRecipe>> BDCL_SERIALIZER =
            RECIPE_SERIALIZERS.register("bdcl", BdclRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ReformingRecipe>> REFORMING_SERIALIZER =
            RECIPE_SERIALIZERS.register("reforming", ReformingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HydrotreatingRecipe>> HYDROTREATING_SERIALIZER =
            RECIPE_SERIALIZERS.register("hydrotreating", HydrotreatingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SolderingStationRecipe>> SOLDERING_STATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("soldering_station", SolderingStationRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ArcWelderRecipe>> ARC_WELDER_SERIALIZER =
            RECIPE_SERIALIZERS.register("arc_welder", ArcWelderRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RockMillRecipe>> ROCK_MILL_SERIALIZER =
            RECIPE_SERIALIZERS.register("rock_mill", RockMillRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SupercomputerRecipe>> SUPERCOMPUTER_SERIALIZER =
            RECIPE_SERIALIZERS.register("supercomputer", SupercomputerRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SpaceAssemblerRecipe>> SPACE_ASSEMBLER_SERIALIZER =
            RECIPE_SERIALIZERS.register("space_assembler", SpaceAssemblerRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ArcFurnaceRecipe>> ARC_FURNACE_SERIALIZER =
            RECIPE_SERIALIZERS.register("arc_furnace", ArcFurnaceRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CompressorRecipe>> COMPRESSOR_SERIALIZER =
            RECIPE_SERIALIZERS.register("compressor", CompressorRecipe.Serializer::new);

    // [parity X1: custom machines] core recipes of the hbmCustomMachines.json configurations (CraftingManager.addRecipeAuto)
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CustomMachineControllerRecipe>> CUSTOM_MACHINE_CONTROLLER_SERIALIZER =
            RECIPE_SERIALIZERS.register("custom_machine_controller", () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(CustomMachineControllerRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MixerRecipe>> MIXER_SERIALIZER =
            RECIPE_SERIALIZERS.register("mixer", MixerRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PressRecipe>> PRESS_SERIALIZER =
            RECIPE_SERIALIZERS.register("press", PressRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PyroOvenRecipe>> PYRO_OVEN_SERIALIZER =
            RECIPE_SERIALIZERS.register("pyrolysis", PyroOvenRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AmmoPressRecipe>> AMMO_PRESS_SERIALIZER =
            RECIPE_SERIALIZERS.register("ammo_press", AmmoPressRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CentrifugeRecipe>> CENTRIFUGE_SERIALIZER =
            RECIPE_SERIALIZERS.register("centrifuge", CentrifugeRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GasCentrifugeRecipe>> GAS_CENTRIFUGE_SERIALIZER =
            RECIPE_SERIALIZERS.register("gas_centrifuge", GasCentrifugeRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrystallizerRecipe>> CRYSTALLIZER_SERIALIZER =
            RECIPE_SERIALIZERS.register("crystallizer", CrystallizerRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CyclotronRecipe>> CYCLOTRON_SERIALIZER =
            RECIPE_SERIALIZERS.register("cyclotron", CyclotronRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ExposureChamberRecipe>> EXPOSURE_CHAMBER_SERIALIZER =
            RECIPE_SERIALIZERS.register("exposure_chamber", ExposureChamberRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ParticleAcceleratorRecipe>> PARTICLE_ACCELERATOR_SERIALIZER =
            RECIPE_SERIALIZERS.register("particle_accelerator", ParticleAcceleratorRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SilexRecipe>> SILEX_SERIALIZER =
            RECIPE_SERIALIZERS.register("silex", SilexRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ElectrolyzerFluidRecipe>> ELECTROLYZER_FLUID_SERIALIZER =
            RECIPE_SERIALIZERS.register("electrolyzer_fluid", ElectrolyzerFluidRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ElectrolyzerMetalRecipe>> ELECTROLYZER_METAL_SERIALIZER =
            RECIPE_SERIALIZERS.register("electrolyzer_metal", ElectrolyzerMetalRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BreederReactorRecipe>> BREEDER_REACTOR_SERIALIZER =
            RECIPE_SERIALIZERS.register("breeder_reactor", BreederReactorRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FuelPoolRecipe>> FUEL_POOL_SERIALIZER =
            RECIPE_SERIALIZERS.register("fuel_pool", FuelPoolRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PurexRecipe>> PUREX_SERIALIZER =
            RECIPE_SERIALIZERS.register("purex", PurexRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FusionRecipe>> FUSION_SERIALIZER =
            RECIPE_SERIALIZERS.register("fusion", FusionRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PlasmaForgeRecipe>> PLASMA_FORGE_SERIALIZER =
            RECIPE_SERIALIZERS.register("plasma_forge", PlasmaForgeRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FusionBreederFluidRecipe>> FUSION_BREEDER_FLUID_SERIALIZER =
            RECIPE_SERIALIZERS.register("fusion_breeder_fluid", FusionBreederFluidRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FusionBreederItemRecipe>> FUSION_BREEDER_ITEM_SERIALIZER =
            RECIPE_SERIALIZERS.register("fusion_breeder_item", FusionBreederItemRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RbmkOutgasserRecipe>> RBMK_OUTGASSER_SERIALIZER =
            RECIPE_SERIALIZERS.register("rbmk_outgasser", RbmkOutgasserRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrucibleRecipe>> CRUCIBLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("crucible", CrucibleRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlastFurnaceRecipe>> BLAST_FURNACE_SERIALIZER =
            RECIPE_SERIALIZERS.register("blast_furnace", BlastFurnaceRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlastFurnaceFuelRecipe>> BLAST_FURNACE_FUEL_SERIALIZER =
            RECIPE_SERIALIZERS.register("blast_furnace_fuel", BlastFurnaceFuelRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FluidDuctRetypeRecipe>> FLUID_DUCT_RETYPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("fluid_duct_retype", FluidDuctRetypeRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LubricantCanisterRecipe>> LUBRICANT_CANISTER_SERIALIZER =
            RECIPE_SERIALIZERS.register("lubricant_canister", LubricantCanisterRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ConveyorExpressRecipe>> CONVEYOR_EXPRESS_SERIALIZER =
            RECIPE_SERIALIZERS.register("conveyor_express", ConveyorExpressRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CargoShellRecipe>> CARGO_SHELL_SERIALIZER =
            RECIPE_SERIALIZERS.register("cargo_shell", CargoShellRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<UniversalGrenadeRecipe>> UNIVERSAL_GRENADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("universal_grenade", UniversalGrenadeRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MkuCraftingRecipe>> MKU_SERIALIZER =
            RECIPE_SERIALIZERS.register("mku", MkuCraftingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RbmkFuelDisassemblyRecipe>> RBMK_FUEL_DISASSEMBLY_SERIALIZER =
            RECIPE_SERIALIZERS.register("rbmk_fuel_disassembly", RbmkFuelDisassemblyRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ScrapsSplitRecipe>> SCRAPS_SPLIT_SERIALIZER =
            RECIPE_SERIALIZERS.register("scraps_split", ScrapsSplitRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageCrateUpgradeRecipe>> CRATE_DESH_UPGRADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("crate_desh_upgrade", StorageCrateUpgradeRecipe.DeshSerializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageCrateUpgradeRecipe>> CRATE_TUNGSTEN_UPGRADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("crate_tungsten_upgrade", StorageCrateUpgradeRecipe.TungstenSerializer::new);

    // [parity S1: carts] crate cart crafting with func_92100_c NBT copy
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<com.atomiclegacy.inventory.recipes.CrateCartRecipe>> CRATE_CART_SERIALIZER =
            RECIPE_SERIALIZERS.register("crate_cart", com.atomiclegacy.inventory.recipes.CrateCartRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageCrateUpgradeRecipe>> SAFE_SERIALIZER =
            RECIPE_SERIALIZERS.register("safe", StorageCrateUpgradeRecipe.SafeSerializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageCrateUpgradeRecipe>> MASS_STORAGE_SERIALIZER =
            RECIPE_SERIALIZERS.register("mass_storage", StorageCrateUpgradeRecipe.MassStorageSerializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageCrateUpgradeRecipe>> MASS_STORAGE_DESH_SERIALIZER =
            RECIPE_SERIALIZERS.register("mass_storage_desh", StorageCrateUpgradeRecipe.MassStorageDeshSerializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageCrateUpgradeRecipe>> MASS_STORAGE_RESISTANT_SERIALIZER =
            RECIPE_SERIALIZERS.register("mass_storage_resistant", StorageCrateUpgradeRecipe.MassStorageResistantSerializer::new);

    // [parity V3: hot items] furnace recipes whose result is ItemHot.heatUp(...)
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<com.atomiclegacy.inventory.recipes.HotSmeltingRecipe>> HOT_SMELTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("hot_smelting", () -> new net.minecraft.world.item.crafting.SimpleCookingSerializer<>(
                    com.atomiclegacy.inventory.recipes.HotSmeltingRecipe::new, 200));

    private HbmRecipeTypes() {
    }

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
        INGREDIENT_TYPES.register(eventBus);
    }
}
