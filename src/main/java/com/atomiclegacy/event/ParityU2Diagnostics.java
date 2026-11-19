package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.RadioTorchBlock;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.rtty.HbmRttySystem;
import com.atomiclegacy.rtty.RttyFunctionException;
import com.atomiclegacy.rtty.RttyInfo;
import com.atomiclegacy.rtty.RttyInteractive;
import com.atomiclegacy.rtty.RttyProxyLayouts;
import com.atomiclegacy.rtty.RttyRecipeNames;
import com.atomiclegacy.rtty.RttyValueProvider;
import com.atomiclegacy.tileentity.AssemblyMachineBlockEntity;
import com.atomiclegacy.tileentity.BatterySocketBlockEntity;
import com.atomiclegacy.tileentity.ChemicalPlantBlockEntity;
import com.atomiclegacy.tileentity.DfcEmitterBlockEntity;
import com.atomiclegacy.tileentity.MachineDummyBlockEntity;
import com.atomiclegacy.tileentity.ParticleAcceleratorBlockEntity;
import com.atomiclegacy.tileentity.PileControlBlockEntity;
import com.atomiclegacy.tileentity.RadioTorchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * [parity U2] Redstone-over-Radio of the 1.7.10 machines (IRORValueProvider / IRORInteractive): getFunctionInfo tables
 * generated from the 936c60c sources, value providers vs. pure interactive components, reading a value with
 * radio_torch_reader, running functions through radio_torch_controller and directly, and the TileEntityProxyCombo
 * dummy cells (makeExtra) of every multiblock. Pad: x = 33000, y = 200. Not covered: GUI drawing of the restricted bar.
 */
final class ParityU2Diagnostics {
    private static final int BASE_X = 33_000;
    private static final int BASE_Y = 200;
    private static final int BASE_Z = 33_000;
    private static final int SPACING = 40;
    private static final Map<String, String[]> INFO = new LinkedHashMap<>();
    /** Implement IRORInteractive only (no IRORValueProvider). */
    private static final Set<String> NOT_PROVIDERS = new HashSet<>(List.of("cargo_elevator", "dfc_emitter", "pile_control",
            "pneumatic_storage_exporter", "turret_chekhov", "turret_friendly", "turret_jeremy", "turret_tauon", "turret_richard",
            "turret_howard", "turret_howard_damaged", "turret_maxwell", "turret_fritz", "turret_sentry", "turret_sentry_damaged",
            "turret_arty", "turret_himars"));
    /** PUREXRecipes.java internal names. */
    private static final List<String> PUREX_NAMES = List.of(
            "purex.uzh", "purex.flashgold", "purex.flashlead", "purex.pilepu239", "purex.pilergp", "purex.pilethorium",
            "purex.pilewaste", "purex.zirnoxnu", "purex.zirnoxmeu", "purex.zirnoxthmeu", "purex.zirnoxmox", "purex.zirnoxmep",
            "purex.zirnoxheu233", "purex.zirnoxheu235", "purex.zirnoxles", "purex.zirnoxzfbmox", "purex.platemox", "purex.platepu238be",
            "purex.platepu239", "purex.platera226be", "purex.platesa326", "purex.plateu233", "purex.plateu235", "purex.pwrmeu",
            "purex.pwrheu233", "purex.pwrheu235", "purex.pwrmen", "purex.pwrhen237", "purex.pwrmox", "purex.pwrmep",
            "purex.pwrhep239", "purex.pwrhep241", "purex.pwrmea", "purex.pwrhea242", "purex.pwrhes326", "purex.pwrhes327",
            "purex.pwrbfbam", "purex.pwrbfpu241", "purex.thoriumsalt", "purex.watzschrab", "purex.watzhes", "purex.watzmes",
            "purex.watzles", "purex.watzhen", "purex.watzmeu", "purex.watzmep", "purex.watzlead", "purex.watzboron",
            "purex.watzdu", "purex.watznaqadah", "purex.watznaqadria", "purex.icf", "purex.vitliquid", "purex.vitgaseous",
            "purex.vitsolid", "purex.schraranium", "purex.schrabzirnox", "purex.schrabpwr", "purex.schrabmen"
    );
    private static final List<int[]> FORCED = new ArrayList<>();

    static {
        INFO.put("compact_launcher", new String[] {"VAL:power", "VAL:solid", "VAL:fuel", "VAL:oxidizer", "VAL:canlaunch", "FUN:launch", "FUN:launch!entity", "FUN:launch!x:z"}); // TileEntityCompactLauncher
        INFO.put("launch_pad", new String[] {"VAL:state", "VAL:power", "VAL:fuel", "VAL:oxidizer", "VAL:canlaunch", "FUN:launch", "FUN:launch!entity", "FUN:launch!x:z"}); // TileEntityLaunchPadBase
        INFO.put("launch_pad_large", new String[] {"VAL:state", "VAL:power", "VAL:fuel", "VAL:oxidizer", "VAL:canlaunch", "FUN:launch", "FUN:launch!entity", "FUN:launch!x:z"}); // TileEntityLaunchPadBase
        INFO.put("launch_table", new String[] {"VAL:power", "VAL:solid", "VAL:fuel", "VAL:oxidizer", "VAL:canlaunch", "FUN:launch", "FUN:launch!entity", "FUN:launch!x:z"}); // TileEntityLaunchTable
        INFO.put("cargo_elevator", new String[] {"VAL:extension", "FUN:setextension!amount"}); // TileEntityCargoElevator
        INFO.put("machine_chungus", new String[] {"VAL:output"}); // TileEntityChungus
        INFO.put("dfc_emitter", new String[] {"FUN:setpower!percent", "FUN:toggle", "FUN:switch!on/off"}); // TileEntityCoreEmitter
        INFO.put("dfc_stabilizer", new String[] {"VAL:durability", "VAL:durabilitypercent", "FUN:setpower!percent"}); // TileEntityCoreStabilizer
        INFO.put("machine_boiler", new String[] {"VAL:input", "VAL:output"}); // TileEntityHeatBoiler
        INFO.put("machine_industrial_boiler", new String[] {"VAL:input", "VAL:output"}); // TileEntityHeatBoilerIndustrial
        INFO.put("heater_heatex", new String[] {"VAL:hotfluid", "VAL:coldfluid", "VAL:heat"}); // TileEntityHeaterHeatex
        INFO.put("heater_oilburner", new String[] {"VAL:heat", "VAL:fuel", "VAL:burnRate", "VAL:state", "FUN:setstate!active", "FUN:setburnrate!rate"}); // TileEntityHeaterOilburner
        INFO.put("icf", new String[] {"VAL:laser", "VAL:heat", "VAL:coldfluid", "VAL:hotfluid", "VAL:stellarflux"}); // TileEntityICF
        INFO.put("launchpad_lambda", new String[] {"VAL:countdown", "VAL:power", "VAL:fuel", "VAL:oxidizer", "VAL:isauto", "VAL:canlaunch", "FUN:launch", "FUN:setauto!on/off"}); // TileEntityLaunchpadLambda
        INFO.put("launchpad_soyuz", new String[] {"VAL:state", "VAL:countdown", "VAL:power", "VAL:fuel", "VAL:oxidizer", "VAL:canlaunch", "VAL:cargomode", "FUN:launch", "FUN:launch!x:z", "FUN:setcargomode!on/off"}); // TileEntityLaunchpadSoyuz
        INFO.put("machine_assembly_factory", new String[] {"VAL:progress1", "VAL:progress2", "VAL:progress3", "VAL:progress4", "VAL:recipe1", "VAL:recipe2", "VAL:recipe3", "VAL:recipe4", "VAL:anyactive", "VAL:active1", "VAL:active2", "VAL:active3", "VAL:active4"}); // TileEntityMachineAssemblyFactory
        INFO.put("machine_assembly_machine", new String[] {"VAL:progress", "VAL:recipe", "VAL:active", "FUN:setrecipe!name"}); // TileEntityMachineAssemblyMachine
        INFO.put("machine_chemical_factory", new String[] {"VAL:progress1", "VAL:progress2", "VAL:progress3", "VAL:progress4", "VAL:recipe1", "VAL:recipe2", "VAL:recipe3", "VAL:recipe4", "VAL:anyactive", "VAL:active1", "VAL:active2", "VAL:active3", "VAL:active4"}); // TileEntityMachineChemicalFactory
        INFO.put("machine_chemical_plant", new String[] {"VAL:progress", "VAL:recipe", "VAL:active", "FUN:setrecipe!name"}); // TileEntityMachineChemicalPlant
        INFO.put("machine_combustion_engine", new String[] {"VAL:state", "VAL:throttle", "VAL:power", "VAL:fuel", "VAL:efficiency", "FUN:setstate!state", "FUN:setthrottle!throttle"}); // TileEntityMachineCombustionEngine
        INFO.put("machine_excavator", new String[] {"VAL:power", "VAL:fluid", "VAL:state", "FUN:setstate!state"}); // TileEntityMachineExcavator
        INFO.put("machine_industrial_turbine", new String[] {"VAL:output", "VAL:flywheel"}); // TileEntityMachineIndustrialTurbine
        INFO.put("machine_purex", new String[] {"VAL:progress", "VAL:recipe", "VAL:active"}); // TileEntityMachinePUREX
        INFO.put("machine_radgen", new String[] {"VAL:state", "VAL:power", "VAL:output", "VAL:output1", "VAL:output2", "VAL:output3", "VAL:output4", "VAL:output5", "VAL:output6", "VAL:output7", "VAL:output8", "VAL:output9", "VAL:output10", "VAL:output11", "VAL:output12", "VAL:progress1", "VAL:progress2", "VAL:progress3", "VAL:progress4", "VAL:progress5", "VAL:progress6", "VAL:progress7", "VAL:progress8", "VAL:progress9", "VAL:progress10", "VAL:progress11", "VAL:progress12"}); // TileEntityMachineRadGen
        INFO.put("machine_satlink", new String[] {"VAL:connected", "VAL:freq", "VAL:rx", "VAL:type", "FUN:setfreq!freq", "FUN:tx!payload", "FUN:txrx!return freq:payload"}); // TileEntityMachineSatLink
        INFO.put("machine_satlinker", new String[] {"VAL:freq", "FUN:setfreq!freq"}); // TileEntityMachineSatLinker
        INFO.put("machine_turbinegas", new String[] {"VAL:turbinepercent", "VAL:turbinespeed", "VAL:output", "VAL:state", "VAL:automode", "VAL:temp", "VAL:power", "VAL:fuel", "VAL:lubricant", "VAL:water", "VAL:steam", "FUN:setauto!auto", "FUN:setthrottle!percent", "FUN:setstate!state"}); // TileEntityMachineTurbineGas
        INFO.put("machine_turbofan", new String[] {"VAL:state", "VAL:output", "VAL:power", "VAL:fuel", "VAL:blood", "VAL:afterburner"}); // TileEntityMachineTurbofan
        INFO.put("watz", new String[] {"VAL:heat", "VAL:flux", "VAL:mud", "VAL:coolant_hot", "VAL:coolant_cold"}); // TileEntityWatz
        INFO.put("pa_detector", new String[] {"VAL:temperature", "VAL:pfmcold", "VAL:pfm"}); // TileEntityPADetector
        INFO.put("pa_dipole", new String[] {"VAL:temperature", "VAL:pfmcold", "VAL:pfm", "FUN:setthreshold!threshold"}); // TileEntityPADipole
        INFO.put("pa_quadrupole", new String[] {"VAL:temperature", "VAL:pfmcold", "VAL:pfm"}); // TileEntityPAQuadrupole
        INFO.put("pa_rfc", new String[] {"VAL:temperature", "VAL:pfmcold", "VAL:pfm"}); // TileEntityPARFC
        INFO.put("pa_source", new String[] {"VAL:status", "VAL:momentum", "VAL:defocus", "VAL:temperature", "VAL:pfmcold", "VAL:pfm", "FUN:cancel"}); // TileEntityPASource
        INFO.put("fusion_klystron", new String[] {"VAL:output", "VAL:target", "FUN:settarget!target"}); // TileEntityFusionKlystron
        INFO.put("fusion_mhdt", new String[] {"VAL:output"}); // TileEntityFusionMHDT
        INFO.put("fusion_plasma_forge", new String[] {"VAL:progress", "VAL:recipe", "VAL:active", "VAL:booster", "VAL:plasma"}); // TileEntityFusionPlasmaForge
        INFO.put("fusion_torus", new String[] {"VAL:plasma", "VAL:consumption", "VAL:progress", "VAL:recipe", "VAL:active", "VAL:temp", "FUN:setrecipe!name"}); // TileEntityFusionTorus
        INFO.put("machine_flare", new String[] {"VAL:flowstate", "VAL:ignitionstate", "VAL:output", "VAL:power", "VAL:fluid", "FUN:setflowstate!state", "FUN:setignitionstate!state"}); // TileEntityMachineGasFlare
        INFO.put("pile_control", new String[] {"FUN:setrods!percent", "FUN:extendrods!percent"}); // TileEntityPileControl
        INFO.put("pile_loader", new String[] {"VAL:meta", "VAL:depletion", "VAL:deppercent", "VAL:lifetime", "VAL:temp"}); // TileEntityPileLoader
        INFO.put("machine_battery_socket", new String[] {"VAL:fill", "VAL:maxfill", "VAL:fillpercent", "VAL:delta", "FUN:setmode!mode (0-3)", "FUN:setmode!mode:fallback (0-3)", "FUN:setredmode!mode (0-3)", "FUN:setredmode!mode:fallback (0-3)", "FUN:setpriority!priority (0-2)"}); // TileEntityBatterySocket
        INFO.put("mass_storage", new String[] {"VAL:type", "VAL:fill", "VAL:fillpercent", "FUN:toggleoutput"}); // TileEntityMassStorage
        INFO.put("fluid_counter_valve", new String[] {"VAL:value", "VAL:state", "FUN:reset", "FUN:setstate!state"}); // TileEntityFluidCounterValve
        INFO.put("fluid_duct_gauge", new String[] {"VAL:deltatick", "VAL:deltasecond"}); // FluidDuctGauge
        INFO.put("red_cable_gauge", new String[] {"VAL:deltatick", "VAL:deltasecond"}); // BlockCableGauge
        INFO.put("pneumatic_storage_exporter", new String[] {"FUN:setfilter!slot:itemid:itemmeta:amount", "FUN:setcontinuous!on/off", "FUN:request", "FUN:requestslot!slot", "FUN:checkavailability!itemid:itemmeta:returnchannel"}); // TileEntityPneumoStorageExporter
        INFO.put("pwr_controller", new String[] {"VAL:rods", "VAL:coreheat", "VAL:hullheat", "VAL:coldbuf", "VAL:hotbuf", "VAL:flux", "VAL:depletion", "FUN:setrods!percent", "FUN:jettison"}); // TileEntityPWRController
        INFO.put("machine_zirnox", new String[] {"VAL:heat", "VAL:pressure", "VAL:water", "VAL:steam", "VAL:co2", "VAL:state", "VAL:slottype", "VAL:slotdep", "VAL:slotmaxdep", "FUN:setslot!id (0 to 23)", "FUN:setstate!active (0 or 1)", "FUN:ventco2"}); // TileEntityReactorZirnox
        INFO.put("geiger", new String[] {"VAL:rad"}); // TileEntityGeiger
        INFO.put("turret_chekhov", new String[] {"FUN:setactive!active (0 or 1)", "FUN:targetplayers!enabled (0 or 1)", "FUN:targetanimals!enabled (0 or 1)", "FUN:targetmobs!enabled (0 or 1)", "FUN:targetmachines!enabled (0 or 1)", "FUN:addwhitelist!name", "FUN:removewhitelist!name"}); // TileEntityTurretBaseNT
        INFO.put("turret_friendly", new String[] {"FUN:setactive!active (0 or 1)", "FUN:targetplayers!enabled (0 or 1)", "FUN:targetanimals!enabled (0 or 1)", "FUN:targetmobs!enabled (0 or 1)", "FUN:targetmachines!enabled (0 or 1)", "FUN:addwhitelist!name", "FUN:removewhitelist!name"}); // TileEntityTurretBaseNT
        INFO.put("turret_jeremy", new String[] {"FUN:setactive!active (0 or 1)", "FUN:targetplayers!enabled (0 or 1)", "FUN:targetanimals!enabled (0 or 1)", "FUN:targetmobs!enabled (0 or 1)", "FUN:targetmachines!enabled (0 or 1)", "FUN:addwhitelist!name", "FUN:removewhitelist!name"}); // TileEntityTurretBaseNT
        INFO.put("turret_tauon", new String[] {"FUN:setactive!active (0 or 1)", "FUN:targetplayers!enabled (0 or 1)", "FUN:targetanimals!enabled (0 or 1)", "FUN:targetmobs!enabled (0 or 1)", "FUN:targetmachines!enabled (0 or 1)", "FUN:addwhitelist!name", "FUN:removewhitelist!name"}); // TileEntityTurretBaseNT
        INFO.put("turret_richard", new String[] {"FUN:setactive!active (0 or 1)", "FUN:targetplayers!enabled (0 or 1)", "FUN:targetanimals!enabled (0 or 1)", "FUN:targetmobs!enabled (0 or 1)", "FUN:targetmachines!enabled (0 or 1)", "FUN:addwhitelist!name", "FUN:removewhitelist!name"}); // TileEntityTurretBaseNT
        INFO.put("turret_howard", new String[] {"FUN:setactive!active (0 or 1)", "FUN:targetplayers!enabled (0 or 1)", "FUN:targetanimals!enabled (0 or 1)", "FUN:targetmobs!enabled (0 or 1)", "FUN:targetmachines!enabled (0 or 1)", "FUN:addwhitelist!name", "FUN:removewhitelist!name"}); // TileEntityTurretBaseNT
        INFO.put("turret_howard_damaged", new String[] {"FUN:setactive!active (0 or 1)", "FUN:targetplayers!enabled (0 or 1)", "FUN:targetanimals!enabled (0 or 1)", "FUN:targetmobs!enabled (0 or 1)", "FUN:targetmachines!enabled (0 or 1)", "FUN:addwhitelist!name", "FUN:removewhitelist!name"}); // TileEntityTurretBaseNT
        INFO.put("turret_maxwell", new String[] {"FUN:setactive!active (0 or 1)", "FUN:targetplayers!enabled (0 or 1)", "FUN:targetanimals!enabled (0 or 1)", "FUN:targetmobs!enabled (0 or 1)", "FUN:targetmachines!enabled (0 or 1)", "FUN:addwhitelist!name", "FUN:removewhitelist!name"}); // TileEntityTurretBaseNT
        INFO.put("turret_fritz", new String[] {"FUN:setactive!active (0 or 1)", "FUN:targetplayers!enabled (0 or 1)", "FUN:targetanimals!enabled (0 or 1)", "FUN:targetmobs!enabled (0 or 1)", "FUN:targetmachines!enabled (0 or 1)", "FUN:addwhitelist!name", "FUN:removewhitelist!name"}); // TileEntityTurretBaseNT
        INFO.put("turret_sentry", new String[] {"FUN:setactive!active (0 or 1)", "FUN:targetplayers!enabled (0 or 1)", "FUN:targetanimals!enabled (0 or 1)", "FUN:targetmobs!enabled (0 or 1)", "FUN:targetmachines!enabled (0 or 1)", "FUN:addwhitelist!name", "FUN:removewhitelist!name"}); // TileEntityTurretBaseNT
        INFO.put("turret_sentry_damaged", new String[] {"FUN:setactive!active (0 or 1)", "FUN:targetplayers!enabled (0 or 1)", "FUN:targetanimals!enabled (0 or 1)", "FUN:targetmobs!enabled (0 or 1)", "FUN:targetmachines!enabled (0 or 1)", "FUN:addwhitelist!name", "FUN:removewhitelist!name"}); // TileEntityTurretBaseNT
        INFO.put("turret_arty", new String[] {"FUN:setactive!active (0 or 1)", "FUN:targetplayers!enabled (0 or 1)", "FUN:targetanimals!enabled (0 or 1)", "FUN:targetmobs!enabled (0 or 1)", "FUN:targetmachines!enabled (0 or 1)", "FUN:addwhitelist!name", "FUN:removewhitelist!name", "FUN:setmode!mode", "FUN:enqueue!x:y:z"}); // TileEntityTurretArty
        INFO.put("turret_himars", new String[] {"FUN:setactive!active (0 or 1)", "FUN:setmanual!enabled (0 or 1)", "FUN:targetplayers!enabled (0 or 1)", "FUN:targetanimals!enabled (0 or 1)", "FUN:targetmobs!enabled (0 or 1)", "FUN:targetmachines!enabled (0 or 1)", "FUN:addwhitelist!name", "FUN:removewhitelist!name", "FUN:enqueue!x:y:z"}); // TileEntityTurretHIMARS
    }

    private ParityU2Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        Player fake = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        try {
            recipeNames(level, problems, cases);
            int index = 0;
            for (Map.Entry<String, String[]> entry : INFO.entrySet()) {
                String id = entry.getKey();
                BlockPos pos = id.equals("launchpad_soyuz") ? new BlockPos(BASE_X, BASE_Y, BASE_Z - 200)
                        : new BlockPos(BASE_X + (index % 8) * SPACING, BASE_Y, BASE_Z + (index / 8) * SPACING);
                index++;
                try {
                    machine(level, fake, id, entry.getValue(), pos, problems, cases);
                } catch (RuntimeException e) {
                    AtomicLegacy.LOGGER.error("Parity U2 diagnostics failed for " + id, e);
                    problems.add("U2: " + id + " crashed: " + e);
                }
            }
            plainDuct(level, new BlockPos(BASE_X - SPACING, BASE_Y, BASE_Z), problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity U2 diagnostics crashed", e);
            problems.add("U2 diagnostics crashed: " + e);
        } finally {
            for (int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
            FORCED.clear();
        }
        return cases[0];
    }

    /// ModuleMachineBase#getRecipeName: internal names of the port recipes ///
    private static void recipeNames(ServerLevel level, List<String> problems, int[] cases) {
        for (RecipeHolder<?> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PUREX.get())) {
            String name = RttyRecipeNames.purex(holder.id());
            check(PUREX_NAMES.contains(name), problems, cases, "PUREX recipe " + holder.id() + " maps to an original name: " + name);
        }
        for (RecipeHolder<com.atomiclegacy.inventory.recipes.AssemblyMachineRecipe> holder
                : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ASSEMBLY_MACHINE.get())) {
            check(holder.value().group().matches("[a-z]+[.][^ ]+"), problems, cases, "assembly recipe group is the internal name: " + holder.id());
        }
        for (RecipeHolder<com.atomiclegacy.inventory.recipes.ChemicalPlantRecipe> holder
                : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CHEMICAL_PLANT.get())) {
            check(holder.value().group().matches("[a-z]+[.][^ ]+"), problems, cases, "chemical recipe group is the internal name: " + holder.id());
        }
        checkEq(RttyRecipeNames.purex(null), "null", problems, cases, "unset recipe is \"null\"");
    }

    private static void machine(ServerLevel level, Player fake, String id, String[] expected, BlockPos pos, List<String> problems, int[] cases) {
        boolean soyuz = id.equals("launchpad_soyuz");
        prepare(level, pos, soyuz ? 64 : 14);
        Block block = BuiltInRegistries.BLOCK.get(AtomicLegacy.id(id));
        BlockState state = block.defaultBlockState();
        Direction facing = soyuz ? Direction.NORTH : Direction.EAST;
        for (Property<?> property : state.getProperties()) {
            if (property instanceof DirectionProperty direction && property.getName().equals("facing")
                    && direction.getPossibleValues().contains(facing)) {
                state = state.setValue(direction, facing);
            }
        }
        level.setBlock(pos, state, 3);
        block.setPlacedBy(level, pos, level.getBlockState(pos), fake, new ItemStack(block));
        BlockEntity core = level.getBlockEntity(pos);
        if (core instanceof MachineDummyBlockEntity dummy) core = dummy.core();
        if (!(core instanceof RttyInfo info)) {
            check(false, problems, cases, id + " has no ROR block entity: " + core);
            return;
        }
        check(Arrays.equals(info.rttyFunctionInfo(), expected), problems, cases, id + " getFunctionInfo: " + Arrays.toString(info.rttyFunctionInfo()));
        boolean provider = !NOT_PROVIDERS.contains(id);
        boolean interactive = Arrays.stream(expected).anyMatch(entry -> entry.startsWith(RttyInfo.PREFIX_FUNCTION));
        check((core instanceof RttyValueProvider p && p.rttyProvidesValues()) == provider, problems, cases, id + " IRORValueProvider = " + provider);
        check((core instanceof RttyInteractive r && r.rttyInteractive()) == interactive, problems, cases, id + " IRORInteractive = " + interactive);

        // proxies: the original makeExtra cells are dummies of this core; other dummies do not forward
        BlockPos corePos = core.getBlockPos();
        Direction coreFacing = RttyProxyLayouts.facing(core.getBlockState());
        boolean layout = RttyProxyLayouts.hasLayout(id);
        BlockPos[] cells = layout ? RttyProxyLayouts.proxyCells(id, corePos, coreFacing) : null;
        List<BlockPos> dummies = dummies(level, corePos, soyuz ? 60 : 12);
        BlockPos proxy = null;
        BlockPos plain = null;
        if (cells != null) {
            for (BlockPos cell : cells) {
                if (cell.equals(corePos)) continue;
                boolean isDummy = level.getBlockEntity(cell) instanceof MachineDummyBlockEntity dummy && corePos.equals(dummy.getCorePos());
                // Known structure gap (not ROR): the port ICF footprint lacks the y+2..4 wings at dir +-2 of MachineICF.fillSpace,
                // so its four side ports (+-6, 3, +-2) do not exist; recorded in parity-u2.md.
                if (id.equals("icf") && Math.abs(cell.getY() - corePos.getY()) == 3 && !isDummy) continue;
                check(isDummy, problems, cases, id + " proxy cell " + cell.subtract(corePos) + " (facing " + coreFacing + ") is a dummy of the core");
                if (isDummy && proxy == null) proxy = cell;
            }
            for (BlockPos dummy : dummies) {
                if (!Arrays.asList(cells).contains(dummy)) {
                    plain = dummy;
                    break;
                }
            }
            if (plain != null) check(!RttyProxyLayouts.isProxy(core, plain), problems, cases, id + " plain dummy " + plain.subtract(corePos) + " is no proxy");
        } else if (!dummies.isEmpty() && layout) {
            boolean forwards = RttyProxyLayouts.isProxy(core, dummies.get(0));
            if (forwards) proxy = dummies.get(0);
            else plain = dummies.get(0);
        }
        check(!layout || dummies.isEmpty() || proxy != null || plain != null, problems, cases, id + " dummies classified");

        // radio_torch_reader on a proxy cell (or the core): VAL broadcast equals provideRORValue
        String value = Arrays.stream(expected).filter(entry -> entry.startsWith(RttyInfo.PREFIX_VALUE)).findFirst().orElse(null);
        BlockPos readerOn = proxy != null ? proxy : corePos;
        Direction side = airSide(level, readerOn);
        if (side != null) {
            BlockPos torch = readerOn.relative(side);
            BlockState readerState = HbmBlocks.RADIO_TORCH_READER.get().defaultBlockState().setValue(RadioTorchBlock.FACING, side);
            boolean readerExpected = provider || (proxy != null && core instanceof RttyValueProvider);
            check(readerState.canSurvive(level, torch) == readerExpected, problems, cases,
                    id + " reader may stand on " + (proxy != null ? "a proxy" : "the core") + " = " + readerExpected);
            BlockState controllerState = HbmBlocks.RADIO_TORCH_CONTROLLER.get().defaultBlockState().setValue(RadioTorchBlock.FACING, side);
            boolean controllerExpected = interactive || (proxy != null && core instanceof RttyInteractive);
            check(controllerState.canSurvive(level, torch) == controllerExpected, problems, cases, id + " controller may stand there = " + controllerExpected);
            if (provider && value != null) {
                level.setBlock(torch, readerState, 3);
                if (level.getBlockEntity(torch) instanceof RadioTorchBlockEntity reader) {
                    String channel = "u2r" + Integer.toHexString(id.hashCode()); // CHANNEL_LIMIT 15
                    reader.setReaderEntry(0, channel, value.substring(RttyInfo.PREFIX_VALUE.length()));
                    RadioTorchBlockEntity.tick(level, torch, level.getBlockState(torch), reader);
                    publish(level);
                    String provided = ((RttyValueProvider) core).provideRttyValue(RttyInfo.PREFIX_VALUE + value.substring(RttyInfo.PREFIX_VALUE.length()).toLowerCase(Locale.US));
                    checkEq(signal(level, channel), provided, problems, cases, id + " reader broadcasts " + value);
                } else {
                    check(false, problems, cases, id + " reader block entity missing");
                }
                level.setBlock(torch, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        if (plain != null && provider) {
            Direction plainSide = airSide(level, plain);
            if (plainSide != null) {
                check(!HbmBlocks.RADIO_TORCH_READER.get().defaultBlockState().setValue(RadioTorchBlock.FACING, plainSide)
                        .canSurvive(level, plain.relative(plainSide)), problems, cases, id + " reader may not stand on a plain dummy");
            }
        }
        functions(level, id, core, readerOn, side, problems, cases);
    }

    /// runRORFunction semantics of the original machines ///
    private static void functions(ServerLevel level, String id, BlockEntity core, BlockPos target, Direction side, List<String> problems, int[] cases) {
        RttyValueProvider values = core instanceof RttyValueProvider p ? p : null;
        RttyInteractive run = core instanceof RttyInteractive r ? r : null;
        switch (id) {
            case "machine_assembly_machine", "machine_chemical_plant" -> {
                String recipe = id.equals("machine_assembly_machine") ? "ass.plateiron" : "chem.hydrogen";
                viaController(level, target, side, "setrecipe!" + recipe, "u2c" + Integer.toHexString(id.hashCode()));
                checkEq(values.provideRttyValue("VAL:recipe"), recipe, problems, cases, id + " setrecipe via controller");
                check(restricted(core), problems, cases, id + " RoR recipe sets restrictedMode");
                run.runRttyFunction("FUN:setrecipe", new String[] {"nonexistent"});
                checkEq(values.provideRttyValue("VAL:recipe"), "nonexistent", problems, cases, id + " unknown RoR recipe name is kept");
                if (core instanceof AssemblyMachineBlockEntity assembler) assembler.setSelectedRecipe(null);
                else ((ChemicalPlantBlockEntity) core).setSelectedRecipe(null);
                check(!restricted(core), problems, cases, id + " GUI selection clears restrictedMode");
                checkEq(values.provideRttyValue("VAL:recipe"), "null", problems, cases, id + " no recipe -> \"null\"");
                checkEq(values.provideRttyValue("VAL:active"), "0", problems, cases, id + " active");
            }
            case "machine_purex", "fusion_plasma_forge" ->
                    checkEq(values.provideRttyValue("VAL:recipe"), "null", problems, cases, id + " unset recipe -> \"null\"");
            case "machine_assembly_factory", "machine_chemical_factory" -> {
                checkEq(values.provideRttyValue("VAL:recipe4"), "null", problems, cases, id + " unset recipe -> \"null\"");
                checkEq(values.provideRttyValue("VAL:anyactive"), "0", problems, cases, id + " anyactive");
            }
            case "fusion_torus" -> {
                run.runRttyFunction("FUN:setrecipe", new String[] {"fus.bf"});
                checkEq(values.provideRttyValue("VAL:recipe"), "fus.bf", problems, cases, id + " setrecipe by internal name");
            }
            case "machine_battery_socket" -> {
                BatterySocketBlockEntity socket = (BatterySocketBlockEntity) core;
                run.runRttyFunction("FUN:setmode", new String[] {Integer.toString(socket.redLow()), "3"});
                checkEq(socket.redLow(), 3, problems, cases, id + " setmode with the current mode uses the fallback");
                run.runRttyFunction("FUN:setmode", new String[] {"1", "2"});
                checkEq(socket.redLow(), 1, problems, cases, id + " setmode with a new mode ignores the fallback");
                viaController(level, target, side, "setredmode!0", "u2.socket");
                checkEq(socket.redHigh(), 0, problems, cases, id + " setredmode via controller");
                run.runRttyFunction("FUN:setpriority", new String[] {"2"});
                check(socket.priority() == com.atomiclegacy.power.PowerEndpoint.ConnectionPriority.HIGH, problems, cases, id + " setpriority!2 -> HIGH");
                check(throwsFormat(() -> run.runRttyFunction("FUN:setpriority", new String[] {"3"})), problems, cases, id + " setpriority range 0..2");
                checkEq(values.provideRttyValue("VAL:fillpercent"), "0", problems, cases, id + " fillpercent without battery");
            }
            case "machine_turbinegas" -> {
                run.runRttyFunction("FUN:setthrottle", new String[] {"150"});
                checkEq(values.provideRttyValue("VAL:turbinepercent"), "100", problems, cases, id + " setthrottle clamps to 100 (slider 60)");
                run.runRttyFunction("FUN:setthrottle", new String[] {"50"});
                checkEq(values.provideRttyValue("VAL:turbinepercent"), "50", problems, cases, id + " setthrottle 50");
                run.runRttyFunction("FUN:setauto", new String[] {"1"});
                checkEq(values.provideRttyValue("VAL:automode"), "1", problems, cases, id + " setauto");
                run.runRttyFunction("FUN:setthrottle", new String[] {"x"});
                checkEq(values.provideRttyValue("VAL:turbinepercent"), "50", problems, cases, id + " NumberFormatException is ignored");
            }
            case "machine_combustion_engine" -> {
                viaController(level, target, side, "setthrottle!99", "u2.combustion");
                checkEq(values.provideRttyValue("VAL:throttle"), "30", problems, cases, id + " setthrottle clamps to 30");
                run.runRttyFunction("FUN:setstate", new String[] {"1"});
                checkEq(values.provideRttyValue("VAL:state"), "1", problems, cases, id + " setstate");
                checkEq(values.provideRttyValue("VAL:efficiency"), "0", problems, cases, id + " efficiency without piston");
            }
            case "heater_oilburner" -> {
                run.runRttyFunction("FUN:setburnrate", new String[] {"7"});
                checkEq(values.provideRttyValue("VAL:burnrate"), "7", problems, cases, id + " setburnrate (VAL:burnRate is read lower-case)");
                check(throwsFormat(() -> run.runRttyFunction("FUN:setburnrate", new String[] {"11"})), problems, cases, id + " setburnrate max 10 without overdrive");
                run.runRttyFunction("FUN:setstate", new String[] {"on"});
                checkEq(values.provideRttyValue("VAL:state"), "1", problems, cases, id + " setstate parseBool");
            }
            case "pa_dipole" -> {
                viaController(level, target, side, "setthreshold!1234", "u2.dipole");
                checkEq(((ParticleAcceleratorBlockEntity) core).threshold(), 1234, problems, cases, id + " setthreshold via controller");
            }
            case "pa_source" -> {
                run.runRttyFunction("FUN:cancel", new String[0]);
                checkEq(values.provideRttyValue("VAL:status"), "IDLE", problems, cases, id + " cancel -> IDLE");
                checkEq(values.provideRttyValue("VAL:defocus"), "0", problems, cases, id + " defocus without particle");
            }
            case "launchpad_lambda" -> {
                run.runRttyFunction("FUN:setauto", new String[] {"true"});
                checkEq(values.provideRttyValue("VAL:isauto"), "1", problems, cases, id + " setauto");
                run.runRttyFunction("FUN:launch", new String[0]);
                checkEq(values.provideRttyValue("VAL:countdown"), "0", problems, cases, id + " launch needs canLaunch");
            }
            case "launchpad_soyuz" -> {
                run.runRttyFunction("FUN:setcargomode", new String[] {"1"});
                checkEq(values.provideRttyValue("VAL:cargomode"), "1", problems, cases, id + " setcargomode");
                run.runRttyFunction("FUN:launch", new String[] {"100", "200"});
                checkEq(values.provideRttyValue("VAL:state"), "ABSENT", problems, cases, id + " state is the SoyuzStatus name");
            }
            case "launch_pad", "launch_pad_large", "compact_launcher", "launch_table" -> {
                run.runRttyFunction("FUN:launch", new String[] {"1", "2"});
                checkEq(values.provideRttyValue("VAL:canlaunch"), "0", problems, cases, id + " canlaunch without missile");
            }
            case "dfc_emitter" -> {
                DfcEmitterBlockEntity emitter = (DfcEmitterBlockEntity) core;
                boolean before = emitter.on();
                run.runRttyFunction("FUN:toggle", new String[0]);
                check(emitter.on() != before, problems, cases, id + " toggle");
                run.runRttyFunction("FUN:switch", new String[] {"off"});
                check(!emitter.on(), problems, cases, id + " switch!off");
                run.runRttyFunction("FUN:setpower", new String[] {"40"});
                checkEq(emitter.watts(), 40, problems, cases, id + " setpower");
            }
            case "dfc_stabilizer" -> checkEq(values.provideRttyValue("VAL:durability"), "0", problems, cases, id + " durability without lens");
            case "pile_control" -> {
                PileControlBlockEntity control = (PileControlBlockEntity) core;
                run.runRttyFunction("FUN:setrods", new String[] {"50"});
                check(Math.abs(control.targetLevel() - 0.5D) < 1e-9, problems, cases, id + " setrods!50");
                run.runRttyFunction("FUN:extendrods", new String[] {"-80"});
                check(control.targetLevel() == 0.0D, problems, cases, id + " extendrods clamps to 0");
            }
            case "pile_loader" -> checkEq(values.provideRttyValue("VAL:meta"), "-1", problems, cases, id + " meta without rod");
            case "machine_satlinker" -> {
                run.runRttyFunction("FUN:setfreq", new String[] {"5"});
                checkEq(values.provideRttyValue("VAL:freq"), "0", problems, cases, id + " no chip: freq 0");
                check(throwsFormat(() -> run.runRttyFunction("FUN:setfreq", new String[] {"0"})), problems, cases, id + " setfreq range 1..SAT_ID_RANGE");
            }
            case "machine_excavator" -> check(throwsFormat(() -> run.runRttyFunction("FUN:setstate", new String[] {"maybe"})),
                    problems, cases, id + " setstate uses parseBool");
            case "machine_flare" -> {
                run.runRttyFunction("FUN:setflowstate", new String[] {"true"});
                checkEq(values.provideRttyValue("VAL:flowstate"), "1", problems, cases, id + " setflowstate parseBool");
            }
            case "pneumatic_storage_exporter" -> check(throwsFormat(() -> run.runRttyFunction("FUN:setcontinuous", new String[] {"yes"})),
                    problems, cases, id + " setcontinuous uses parseBool");
            case "fluid_duct_gauge", "red_cable_gauge" -> checkEq(values.provideRttyValue("VAL:deltatick"), "0", problems, cases, id + " deltatick");
            default -> {
            }
        }
    }

    private static boolean restricted(BlockEntity core) {
        return core instanceof AssemblyMachineBlockEntity assembler ? assembler.rttyRestricted() : ((ChemicalPlantBlockEntity) core).rttyRestricted();
    }

    /** Plain fluid ducts are no ROR components (only TileEntityFluidCounterValve and TileEntityPipeGauge). */
    private static void plainDuct(ServerLevel level, BlockPos pos, List<String> problems, int[] cases) {
        prepare(level, pos, 2);
        level.setBlock(pos, HbmBlocks.FLUID_DUCT_NEO.get().defaultBlockState(), 3);
        check(!HbmBlocks.RADIO_TORCH_READER.get().defaultBlockState().setValue(RadioTorchBlock.FACING, Direction.UP).canSurvive(level, pos.above()),
                problems, cases, "reader may not stand on a plain fluid duct");
    }

    private static void viaController(ServerLevel level, BlockPos target, Direction side, String command, String channel) {
        if (side == null) return;
        BlockPos torch = target.relative(side);
        level.setBlock(torch, HbmBlocks.RADIO_TORCH_CONTROLLER.get().defaultBlockState().setValue(RadioTorchBlock.FACING, side), 3);
        if (level.getBlockEntity(torch) instanceof RadioTorchBlockEntity controller) {
            controller.setChannel(channel);
            HbmRttySystem.broadcast(level, channel, command);
            publish(level);
            RadioTorchBlockEntity.tick(level, torch, level.getBlockState(torch), controller);
        }
        level.setBlock(torch, Blocks.AIR.defaultBlockState(), 3);
    }

    private static List<BlockPos> dummies(ServerLevel level, BlockPos core, int radius) {
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(core.offset(-radius, -3, -radius), core.offset(radius, 10, radius))) {
            if (level.getBlockEntity(p) instanceof MachineDummyBlockEntity dummy && core.equals(dummy.getCorePos())) result.add(p.immutable());
        }
        return result;
    }

    private static Direction airSide(ServerLevel level, BlockPos pos) {
        for (Direction direction : new Direction[] {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
            if (level.getBlockState(pos.relative(direction)).isAir()) return direction;
        }
        return null;
    }

    private static void prepare(ServerLevel level, BlockPos pos, int radius) {
        for (int cx = (pos.getX() - radius) >> 4; cx <= (pos.getX() + radius) >> 4; cx++) {
            for (int cz = (pos.getZ() - radius) >> 4; cz <= (pos.getZ() + radius) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
                FORCED.add(new int[] {cx, cz});
            }
        }
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, -3, -radius), pos.offset(radius, 10, radius))) {
            if (!level.getBlockState(p).isAir()) {
                if (level.getBlockEntity(p) instanceof net.minecraft.world.Clearable clearable) clearable.clearContent();
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
        }
    }

    private static boolean throwsFormat(Runnable action) {
        try {
            action.run();
            return false;
        } catch (RttyFunctionException expected) {
            return true;
        }
    }

    private static void publish(ServerLevel level) {
        HbmRttySystem.onLevelTick(new LevelTickEvent.Post(() -> true, level));
    }

    private static String signal(ServerLevel level, String channel) {
        HbmRttySystem.Channel chan = HbmRttySystem.listen(level, channel);
        return chan == null ? null : chan.signal();
    }

    private static void check(boolean condition, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (!condition) problems.add("U2: " + message);
    }

    private static void checkEq(Object actual, Object expected, List<String> problems, int[] cases, String message) {
        check(Objects.equals(actual, expected), problems, cases, message + ": " + actual + " expected " + expected);
    }
}
