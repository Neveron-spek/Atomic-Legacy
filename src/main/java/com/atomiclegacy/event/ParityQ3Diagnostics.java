package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.fluid.CombustibleFuelGrade;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.inventory.recipes.anvil.AnvilConstructionRecipe;
import com.atomiclegacy.inventory.recipes.anvil.AnvilIngredient;
import com.atomiclegacy.inventory.recipes.anvil.HbmAnvilRecipes;
import com.atomiclegacy.items.CombustionPistonSetItem;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.tileentity.ChargerBlockEntity;
import com.atomiclegacy.tileentity.CombustionEngineBlockEntity;
import com.atomiclegacy.tileentity.CoolingTowerBlockEntity;
import com.atomiclegacy.tileentity.IndustrialTurbineBlockEntity;
import com.atomiclegacy.tileentity.LargeTurbineBlockEntity;
import com.atomiclegacy.tileentity.LegacyMachineBlockEntity;
import com.atomiclegacy.tileentity.LeviathanTurbineBlockEntity;
import com.atomiclegacy.tileentity.PoweredSteamCondenserBlockEntity;
import com.atomiclegacy.tileentity.SolarBoilerBlockEntity;
import com.atomiclegacy.tileentity.SteamCondenserBlockEntity;
import com.atomiclegacy.tileentity.SteamEngineBlockEntity;
import com.atomiclegacy.tileentity.StirlingGeneratorBlockEntity;
import com.atomiclegacy.tileentity.TeslaCoilBlockEntity;
import com.atomiclegacy.tileentity.WoodBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * [parity Q3] Dedicated-server checks of generators and thermal machines against the pinned 1.7.10 sources
 * (TileEntityMachineCombustionEngine, MachineWoodBurner, SteamEngine, Stirling, MachineTurbofan, MachineRadGen,
 * SolarBoiler, Condenser/CondenserPowered/TowerSmall/TowerLarge, MachineLargeTurbine, TurbineBase + Chungus +
 * MachineIndustrialTurbine, Charger, Tesla, ChimneyBrick/Industrial). Expected numbers are the literals of the original
 * or are derived from the L2-verified fluid traits with the original formulas. Machines are placed on pads at
 * x = 22000, y = 200 and ticked directly. Not covered: GUI drawing, sounds, particles, entity effects.
 */
final class ParityQ3Diagnostics {

    private ParityQ3Diagnostics() {
    }

    private static final int BASE_X = 22_000;
    private static final int BASE_Z = 22_000;
    private static final int BASE_Y = 200;
    private static final int SPACING = 32;
    private static int site;
    private static final List<int[]> FORCED = new ArrayList<>();

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        site = 0;
        try {
            tables(server, problems, cases);
            combustion(level, problems, cases);
            woodBurner(level, problems, cases);
            stirling(level, problems, cases);
            steamEngine(level, problems, cases);
            solarBoiler(level, problems, cases);
            condensers(level, problems, cases);
            largeTurbine(level, problems, cases);
            leviathan(level, problems, cases);
            industrialTurbine(level, problems, cases);
            turbofan(level, problems, cases);
            radgen(level, problems, cases);
            charger(level, problems, cases);
            tesla(level, problems, cases);
        } catch (RuntimeException | ReflectiveOperationException e) {
            AtomicLegacy.LOGGER.error("Parity Q3 diagnostics crashed", e);
            problems.add("Q3 diagnostics crashed: " + e);
        } finally {
            for (int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
            FORCED.clear();
        }
        return cases[0];
    }

    // ---------------------------------------------------------------- tables and recipes

    private static void tables(MinecraftServer server, List<String> problems, int[] cases) {
        // ItemPistons.EnumPistonType
        double[][] pistons = {
                {1.00, 0.75, 0.25, 0.00, 0.00},
                {0.50, 1.00, 0.90, 0.50, 0.00},
                {0.00, 0.50, 1.00, 0.75, 0.00},
                {0.50, 0.75, 1.00, 0.90, 0.50}};
        CombustionPistonSetItem.PistonType[] types = CombustionPistonSetItem.PistonType.values();
        checkEq(types.length, 4, problems, cases, "piston types");
        for (int t = 0; t < Math.min(4, types.length); t++) {
            for (int g = 0; g < 5; g++) {
                check(types[t].efficiency(CombustibleFuelGrade.values()[g]) == pistons[t][g], problems, cases,
                        "piston " + types[t].id() + " grade " + g + " = " + types[t].efficiency(CombustibleFuelGrade.values()[g]));
            }
        }
        // ModuleBurnTime().setLogTimeMod(4).setWoodTimeMod(2) over FuelHandler burn times
        checkEq(WoodBurnerBlockEntity.fuelDuration(new ItemStack(Items.OAK_LOG)), 1200, problems, cases, "wood burner log");
        checkEq(WoodBurnerBlockEntity.fuelDuration(new ItemStack(Items.OAK_PLANKS)), 600, problems, cases, "wood burner planks");
        checkEq(WoodBurnerBlockEntity.fuelDuration(new ItemStack(Items.STICK)), 200, problems, cases, "wood burner stick");
        checkEq(WoodBurnerBlockEntity.fuelDuration(new ItemStack(Items.COAL)), 1600, problems, cases, "wood burner coal");
        checkEq(WoodBurnerBlockEntity.fuelDuration(new ItemStack(Items.CHARCOAL)), 1600, problems, cases, "wood burner charcoal");
        checkEq(WoodBurnerBlockEntity.fuelDuration(new ItemStack(Items.BLAZE_ROD)), 2400, problems, cases, "wood burner blaze rod");
        checkEq(WoodBurnerBlockEntity.fuelDuration(new ItemStack(Items.OAK_FENCE)), 300, problems, cases, "wood burner fence (no Wood ore name)");
        // chimney getPollutionMod (rampant mode off)
        check(HbmConfig.smokestackPollutionModifier(false) == 0.25D, problems, cases, "brick chimney pollution mod");
        check(HbmConfig.smokestackPollutionModifier(true) == 0.1D, problems, cases, "industrial chimney pollution mod");
        // CraftingManager: charger x16 from glowstone, steel block, copper coil torus
        check(server.getRecipeManager().byKey(AtomicLegacy.id("charger_16"))
                        .map(holder -> holder.value().getResultItem(server.registryAccess()).getCount() == 16).orElse(false),
                problems, cases, "charger x16 recipe");
        // AnvilRecipes: machine_stirling_steel needs gear_large meta 1
        boolean steelGear = false;
        for (AnvilConstructionRecipe recipe : HbmAnvilRecipes.construction()) {
            if (recipe.outputs().size() == 1 && recipe.outputs().get(0).stack().is(HbmBlocks.MACHINE_STIRLING_STEEL.get().asItem())) {
                for (AnvilIngredient ingredient : recipe.inputs()) {
                    if (ingredient.matches(new ItemStack(HbmItems.GEAR_LARGE_STEEL.get()))) steelGear = true;
                }
            }
        }
        check(steelGear, problems, cases, "stirling steel anvil recipe uses gear_large_steel");
    }

    // ---------------------------------------------------------------- generators

    /** TileEntityMachineCombustionEngine: toBurn = min(fill*10+tenth, setting*2), power += toBurn * E/10000 * eff. */
    private static void combustion(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level);
        CombustionEngineBlockEntity engine = place(level, pos, HbmBlocks.MACHINE_COMBUSTION_ENGINE.get().defaultBlockState(), CombustionEngineBlockEntity.class);
        if (engine == null) {
            check(false, problems, cases, "combustion engine block entity missing");
            return;
        }
        HbmFluidDefinition diesel = fluid("diesel");
        checkEq(engine.fuelTank().capacity(), 24_000, problems, cases, "combustion tank");
        engine.fuelTank().setType(diesel);
        engine.fuelTank().setAmount(1_000);
        engine.setItem(CombustionEngineBlockEntity.SLOT_PISTON, HbmItems.variantStack(HbmItems.PISTON_SET_ITEMS, "steel"));
        setField(engine, "enabled", true);
        setField(engine, "throttle", 10);
        CombustionEngineBlockEntity.tick(level, pos, level.getBlockState(pos), engine);
        long expected = (long) (20 * (diesel.combustibleHeatEnergy() / 10_000D) * 0.25D);
        checkEq(engine.power(), expected, problems, cases, "combustion power after 1 tick (diesel HIGH, steel 0.25, setting 10)");
        checkEq(engine.fuelTank().amount(), 998, problems, cases, "combustion fuel after 1 tick (20 tenths)");
        check(engine.pasteFluidSetting(fluid("water"), level, null, pos) && engine.fuelTank().type() == fluid("water"),
                problems, cases, "combustion tank accepts any configured type");
        engine.clearContent();
        cleanup(level, pos);
    }

    /** TileEntityMachineWoodBurner: 100 HE/t solid, liquid FT_Flammable * 2 / 2000, ash by ore name. */
    private static void woodBurner(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        Method ash = WoodBurnerBlockEntity.class.getDeclaredMethod("ashType", ItemStack.class);
        ash.setAccessible(true);
        checkStr(String.valueOf(ash.invoke(null, new ItemStack(Items.CHARCOAL))), "WOOD", problems, cases, "ash charcoal");
        checkStr(String.valueOf(ash.invoke(null, new ItemStack(Items.COAL))), "COAL", problems, cases, "ash coal");
        checkStr(String.valueOf(ash.invoke(null, new ItemStack(Items.OAK_SAPLING))), "WOOD", problems, cases, "ash sapling");
        checkStr(String.valueOf(ash.invoke(null, new ItemStack(Items.BLAZE_ROD))), "MISC", problems, cases, "ash blaze rod");
        BlockPos pos = pad(level);
        WoodBurnerBlockEntity burner = place(level, pos, HbmBlocks.MACHINE_WOOD_BURNER.get().defaultBlockState(), WoodBurnerBlockEntity.class);
        if (burner == null) {
            check(false, problems, cases, "wood burner block entity missing");
            return;
        }
        HbmFluidDefinition woodoil = fluid("woodoil");
        HbmFluidTank tank = (HbmFluidTank) field(burner, "tank");
        tank.setType(woodoil);
        tank.setAmount(100);
        setField(burner, "liquidBurn", true);
        setField(burner, "isOn", true);
        WoodBurnerBlockEntity.tick(level, pos, level.getBlockState(pos), burner);
        checkEq((long) field(burner, "energyStored"), (long) (int) (woodoil.flammableHeatEnergy() * 2 / 2_000L), problems, cases, "wood burner liquid HE/t");
        checkEq(tank.amount(), 98, problems, cases, "wood burner burns 2 mB/t");
        burner.clearContent();
        cleanup(level, pos);
    }

    /** TileEntityStirling: maxHeat 300/1500, buffer decays by 1 per tick without a cog. */
    private static void stirling(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level);
        StirlingGeneratorBlockEntity stirling = place(level, pos, HbmBlocks.MACHINE_STIRLING.get().defaultBlockState(), StirlingGeneratorBlockEntity.class);
        if (stirling == null) {
            check(false, problems, cases, "stirling block entity missing");
            return;
        }
        checkEq(stirling.maxHeat(), 300, problems, cases, "stirling max heat");
        stirling.setHasCog(false);
        setField(stirling, "powerBuffer", 10L);
        StirlingGeneratorBlockEntity.tick(level, pos, level.getBlockState(pos), stirling);
        checkEq((long) field(stirling, "powerBuffer"), 9, problems, cases, "stirling buffer decay without cog");
        cleanup(level, pos);
        pos = pad(level);
        StirlingGeneratorBlockEntity steel = place(level, pos, HbmBlocks.MACHINE_STIRLING_STEEL.get().defaultBlockState(), StirlingGeneratorBlockEntity.class);
        if (steel != null) checkEq(steel.maxHeat(), 1500, problems, cases, "steel stirling max heat");
        cleanup(level, pos);
    }

    /** TileEntitySteamEngine: steam 2000 / LDS 20, power = ops * 200 * 1.0 * 0.85. */
    private static void steamEngine(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level);
        SteamEngineBlockEntity engine = place(level, pos, HbmBlocks.MACHINE_STEAM_ENGINE.get().defaultBlockState(), SteamEngineBlockEntity.class);
        if (engine == null) {
            check(false, problems, cases, "steam engine block entity missing");
            return;
        }
        checkEq(engine.inputTank().capacity(), 2_000, problems, cases, "steam engine steam cap");
        checkEq(engine.outputTank().capacity(), 20, problems, cases, "steam engine lds cap");
        engine.inputTank().fill(fluid("steam"), 2_000, false);
        SteamEngineBlockEntity.tick(level, pos, level.getBlockState(pos), engine);
        checkEq((long) field(engine, "powerBuffer"), 3_400, problems, cases, "steam engine power (20 ops)");
        checkEq(engine.outputTank().amount(), 20, problems, cases, "steam engine spent steam");
        cleanup(level, pos);
    }

    /** TileEntitySolarBoiler: process = heat / 50, water 100, steam 10000, 1 water -> 100 steam. */
    private static void solarBoiler(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        SolarBoilerBlockEntity boiler = place(level, pos, HbmBlocks.MACHINE_SOLAR_BOILER.get().defaultBlockState(), SolarBoilerBlockEntity.class);
        if (boiler == null) {
            check(false, problems, cases, "solar boiler block entity missing");
            return;
        }
        boiler.inputTank().fill(fluid("water"), 100, false);
        boiler.addHeatInput(2_500);
        SolarBoilerBlockEntity.tick(level, pos, level.getBlockState(pos), boiler);
        checkEq(boiler.inputTank().amount(), 50, problems, cases, "solar boiler water used (2500 heat)");
        checkEq(boiler.outputTank().amount(), 5_000, problems, cases, "solar boiler steam made");
        checkEq(boiler.heat(), 50, problems, cases, "solar boiler display");
        cleanup(level, pos);
    }

    /** TileEntityCondenser 100/100, TowerSmall 1000, TowerLarge 10000, CondenserPowered 10 HE/mB with 5% tolerance. */
    private static void condensers(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level);
        SteamCondenserBlockEntity condenser = place(level, pos, HbmBlocks.MACHINE_CONDENSER.get().defaultBlockState(), SteamCondenserBlockEntity.class);
        if (condenser != null) {
            condenser.inputTank().fill(fluid("spentsteam"), 100, false);
            SteamCondenserBlockEntity.tick(level, pos, level.getBlockState(pos), condenser);
            checkEq(condenser.outputTank().amount(), 100, problems, cases, "condenser converts 100 mB");
            checkEq(condenser.waterTimer(), 20, problems, cases, "condenser water timer");
        } else check(false, problems, cases, "condenser block entity missing");
        cleanup(level, pos);

        pos = pad(level);
        CoolingTowerBlockEntity small = place(level, pos, HbmBlocks.MACHINE_TOWER_SMALL.get().defaultBlockState(), CoolingTowerBlockEntity.class);
        if (small != null) {
            CoolingTowerBlockEntity.tick(level, pos, level.getBlockState(pos), small);
            checkEq(small.inputTank().capacity(), 1_000, problems, cases, "small tower capacity");
        } else check(false, problems, cases, "small tower block entity missing");
        cleanup(level, pos);
        pos = pad(level);
        CoolingTowerBlockEntity large = place(level, pos, HbmBlocks.MACHINE_TOWER_LARGE.get().defaultBlockState(), CoolingTowerBlockEntity.class);
        if (large != null) {
            CoolingTowerBlockEntity.tick(level, pos, level.getBlockState(pos), large);
            checkEq(large.inputTank().capacity(), 10_000, problems, cases, "large tower capacity");
        } else check(false, problems, cases, "large tower block entity missing");
        cleanup(level, pos);

        pos = pad(level);
        PoweredSteamCondenserBlockEntity powered = place(level, pos, HbmBlocks.MACHINE_CONDENSER_POWERED.get().defaultBlockState(), PoweredSteamCondenserBlockEntity.class);
        if (powered != null) {
            powered.inputTank().fill(fluid("spentsteam"), 100, false);
            setField(powered, "power", 949L);
            PoweredSteamCondenserBlockEntity.tick(level, pos, level.getBlockState(pos), powered);
            checkEq(powered.outputTank().amount(), 0, problems, cases, "powered condenser refuses below 95%");
            setField(powered, "power", 950L);
            PoweredSteamCondenserBlockEntity.tick(level, pos, level.getBlockState(pos), powered);
            checkEq(powered.outputTank().amount(), 100, problems, cases, "powered condenser converts at 95%");
            checkEq((long) field(powered, "power"), 0, problems, cases, "powered condenser power clamped at 0");
        } else check(false, problems, cases, "powered condenser block entity missing");
        cleanup(level, pos);
    }

    /** TileEntityMachineLargeTurbine: power *= 0.95, ops = min(in, out, ceil((fill / req) / 5F)). */
    private static void largeTurbine(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level);
        LargeTurbineBlockEntity turbine = place(level, pos, HbmBlocks.MACHINE_LARGE_TURBINE.get().defaultBlockState(), LargeTurbineBlockEntity.class);
        if (turbine == null) {
            check(false, problems, cases, "large turbine block entity missing");
            return;
        }
        checkEq(turbine.inputTank().capacity(), 512_000, problems, cases, "large turbine input cap");
        checkEq(turbine.outputTank().capacity(), 10_240_000, problems, cases, "large turbine output cap");
        turbine.inputTank().fill(fluid("steam"), 600, false);
        LargeTurbineBlockEntity.tick(level, pos, level.getBlockState(pos), turbine);
        checkEq((long) field(turbine, "power"), 400, problems, cases, "large turbine 600 mB steam -> 2 ops");
        setField(turbine, "power", 0L);
        turbine.inputTank().setAmount(550);
        LargeTurbineBlockEntity.tick(level, pos, level.getBlockState(pos), turbine);
        checkEq((long) field(turbine, "power"), 200, problems, cases, "large turbine 550 mB steam -> ceil((550/100)/5F) = 1 op");
        setField(turbine, "power", 1_000L);
        turbine.inputTank().setAmount(0);
        LargeTurbineBlockEntity.tick(level, pos, level.getBlockState(pos), turbine);
        checkEq((long) field(turbine, "power"), 950, problems, cases, "large turbine power decay 0.95");
        turbine.clearContent();
        cleanup(level, pos);
    }

    /** TileEntityChungus: consumes 100% per tick, efficiency 0.85, tanks 1e9. */
    private static void leviathan(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        LeviathanTurbineBlockEntity turbine = place(level, pos, HbmBlocks.MACHINE_CHUNGUS.get().defaultBlockState(), LeviathanTurbineBlockEntity.class);
        if (turbine == null) {
            check(false, problems, cases, "leviathan block entity missing");
            return;
        }
        checkEq(turbine.inputTank().capacity(), 1_000_000_000, problems, cases, "leviathan input cap");
        turbine.inputTank().fill(fluid("steam"), 1_000, false);
        LeviathanTurbineBlockEntity.tick(level, pos, level.getBlockState(pos), turbine);
        checkEq(turbine.powerBuffer(), 1_700, problems, cases, "leviathan 1000 mB steam -> 10 ops * 200 * 0.85");
        checkEq(turbine.outputTank().amount(), 10, problems, cases, "leviathan spent steam");
        cleanup(level, pos);
    }

    /** TileEntityMachineIndustrialTurbine: 20% per tick, flywheel 0.5e8, target = min(max(spin, 0.05) * maxPower, flywheel). */
    private static void industrialTurbine(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level);
        IndustrialTurbineBlockEntity turbine = place(level, pos, HbmBlocks.MACHINE_INDUSTRIAL_TURBINE.get().defaultBlockState(), IndustrialTurbineBlockEntity.class);
        if (turbine == null) {
            check(false, problems, cases, "industrial turbine block entity missing");
            return;
        }
        checkEq(turbine.inputTank().capacity(), 750_000, problems, cases, "industrial turbine input cap");
        checkEq(turbine.outputTank().capacity(), 3_000_000, problems, cases, "industrial turbine output cap");
        turbine.inputTank().fill(fluid("steam"), 1_000, false);
        IndustrialTurbineBlockEntity.tick(level, pos, level.getBlockState(pos), turbine);
        checkEq(turbine.inputTank().amount(), 800, problems, cases, "industrial turbine consumes ceil(20%)");
        checkEq((long) field(turbine, "maxPower"), 300_000, problems, cases, "industrial turbine maxPower");
        checkEq(turbine.powerBuffer(), 400, problems, cases, "industrial turbine output limited by flywheel");
        cleanup(level, pos);
    }

    /** TileEntityMachineTurbofan: AERO burnValue = E / 1000 per mB, 1 + afterburner mB/t, 24000 mB tanks, 1M HE. */
    private static void turbofan(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level);
        LegacyMachineBlockEntity fan = place(level, pos, HbmBlocks.MACHINE_TURBOFAN.get().defaultBlockState(), LegacyMachineBlockEntity.class);
        if (fan == null || fan.tank(0) == null) {
            check(false, problems, cases, "turbofan block entity missing");
            return;
        }
        HbmFluidDefinition kerosene = fluid("kerosene");
        checkEq(fan.tank(0).capacity(), 24_000, problems, cases, "turbofan fuel cap");
        fan.tank(0).setType(kerosene);
        fan.tank(0).setAmount(1_000);
        LegacyMachineBlockEntity.tick(level, pos, level.getBlockState(pos), fan);
        checkEq(fan.tank(0).amount(), 999, problems, cases, "turbofan burns 1 mB/t without afterburner");
        checkEq((long) field(fan, "energy"), kerosene.combustibleHeatEnergy() / 1_000L, problems, cases, "turbofan HE/t from kerosene");
        fan.clearContent();
        cleanup(level, pos);
    }

    /** TileEntityMachineRadGen: one item per queue slot, fuels table. */
    private static void radgen(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level);
        LegacyMachineBlockEntity radgen = place(level, pos, HbmBlocks.MACHINE_RADGEN.get().defaultBlockState(), LegacyMachineBlockEntity.class);
        if (radgen == null) {
            check(false, problems, cases, "radgen block entity missing");
            return;
        }
        Method production = LegacyMachineBlockEntity.class.getDeclaredMethod("radGenProduction", ItemStack.class);
        Method duration = LegacyMachineBlockEntity.class.getDeclaredMethod("radGenDuration", ItemStack.class);
        production.setAccessible(true);
        duration.setAccessible(true);
        String[][] fuels = {
                {"nuclear_waste_short", "1500", "36000"}, {"nuclear_waste_short_tiny", "150", "3600"},
                {"nuclear_waste_long", "500", "144000"}, {"nuclear_waste_long_tiny", "50", "14400"},
                {"scrap_nuclear", "50", "6000"}, {"gem_rad", "25000", "36000"}};
        for (String[] fuel : fuels) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id(fuel[0])));
            checkEq((int) production.invoke(radgen, stack), Integer.parseInt(fuel[1]), problems, cases, "radgen " + fuel[0] + " HE/t");
            checkEq((int) duration.invoke(radgen, stack), Integer.parseInt(fuel[2]), problems, cases, "radgen " + fuel[0] + " ticks");
        }
        radgen.setItem(0, new ItemStack(HbmItems.NUCLEAR_WASTE_SHORT.get(), 5));
        LegacyMachineBlockEntity.tick(level, pos, level.getBlockState(pos), radgen);
        checkEq(radgen.getItem(0).getCount(), 4, problems, cases, "radgen takes one item per queue slot");
        checkEq((long) field(radgen, "energy"), 1_500, problems, cases, "radgen first tick output");
        radgen.clearContent();
        cleanup(level, pos);
    }

    /** TileEntityCharger.transferPower: everything is returned until usingTicks reaches delay (20). */
    private static void charger(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        ChargerBlockEntity charger = place(level, pos, HbmBlocks.CHARGER.get().defaultBlockState(), ChargerBlockEntity.class);
        if (charger == null) {
            check(false, problems, cases, "charger block entity missing");
            return;
        }
        checkEq(charger.receiveLeftover(1_000L), 1_000, problems, cases, "charger returns power while retracted");
        checkEq(charger.delay(), 20, problems, cases, "charger delay");
        cleanup(level, pos);
    }

    /** TileEntityTesla: 5000 HE per zap, 100000 HE buffer. */
    private static void tesla(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level);
        TeslaCoilBlockEntity tesla = place(level, pos, HbmBlocks.TESLA.get().defaultBlockState(), TeslaCoilBlockEntity.class);
        if (tesla == null) {
            check(false, problems, cases, "tesla block entity missing");
            return;
        }
        checkEq(TeslaCoilBlockEntity.MAX_POWER, 100_000, problems, cases, "tesla max power");
        setField(tesla, "power", 10_000L);
        TeslaCoilBlockEntity.tick(level, pos, level.getBlockState(pos), tesla);
        checkEq((long) field(tesla, "power"), 5_000, problems, cases, "tesla zap cost");
        cleanup(level, pos);
    }

    // ---------------------------------------------------------------- helpers

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElseThrow(() -> new IllegalStateException("missing fluid " + name));
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
        if (!condition) problems.add("Q3: " + message);
    }

    private static void checkEq(long actual, long expected, List<String> problems, int[] cases, String message) {
        check(actual == expected, problems, cases, message + ": " + actual + " expected " + expected);
    }

    private static void checkStr(String actual, String expected, List<String> problems, int[] cases, String message) {
        check(expected.equals(actual), problems, cases, message + ": '" + actual + "' expected '" + expected + "'");
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> T place(ServerLevel level, BlockPos pos, BlockState state, Class<T> type) {
        level.setBlock(pos, state, 2 | 16);
        BlockEntity be = level.getBlockEntity(pos);
        return type.isInstance(be) ? (T) be : null;
    }

    private static BlockPos pad(ServerLevel level) {
        int index = site++;
        BlockPos pos = new BlockPos(BASE_X + (index % 8) * SPACING, BASE_Y, BASE_Z + (index / 8) * SPACING);
        for (int cx = (pos.getX() - 12) >> 4; cx <= (pos.getX() + 12) >> 4; cx++) {
            for (int cz = (pos.getZ() - 12) >> 4; cz <= (pos.getZ() + 12) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
                FORCED.add(new int[] {cx, cz});
            }
        }
        for (int dx = -3; dx <= 3; dx++) for (int dy = -1; dy <= 4; dy++) for (int dz = -3; dz <= 3; dz++) {
            level.setBlock(pos.offset(dx, dy, dz), dy < 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        return pos;
    }

    private static void cleanup(ServerLevel level, BlockPos pos) {
        for (BlockPos cursor : BlockPos.betweenClosed(pos.offset(-3, 0, -3), pos.offset(3, 4, 3))) {
            BlockEntity be = level.getBlockEntity(cursor);
            if (be instanceof net.minecraft.world.Container container) container.clearContent();
            if (!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }

    @SuppressWarnings("unused")
    private static ResourceLocation id(String path) {
        return AtomicLegacy.id(path);
    }
}
