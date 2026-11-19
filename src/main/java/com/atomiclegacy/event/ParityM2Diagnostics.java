package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.PileGraphiteBlock;
import com.atomiclegacy.blocks.PwrBlock;
import com.atomiclegacy.inventory.recipes.BreederReactorRecipe;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.items.PileRodMk2Item;
import com.atomiclegacy.items.PlateFuelItem;
import com.atomiclegacy.items.PwrFuelItem;
import com.atomiclegacy.items.ZirnoxRodItem;
import com.atomiclegacy.radiation.ChunkRadiationData;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.tileentity.BreederReactorBlockEntity;
import com.atomiclegacy.tileentity.PileGraphiteBlockEntity;
import com.atomiclegacy.tileentity.PileMk2CoreBlockEntity;
import com.atomiclegacy.tileentity.PwrControllerBlockEntity;
import com.atomiclegacy.tileentity.ResearchReactorBlockEntity;
import com.atomiclegacy.tileentity.ZirnoxReactorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * [parity M2] Dedicated-server checks of the fission reactors other than the RBMK (PWR, ZIRNOX, Chicago Pile, research
 * and breeding reactor) against the pinned 1.7.10 sources (commit 936c60c). Expected numbers were produced by
 * work/tmp-m2/sim_m2.py, an independent re-implementation of TileEntityPWRController, TileEntityReactorZirnox,
 * TileEntityReactorResearch, TileEntityMachineReactorBreeding, ItemPileRodMK2 and TileEntityPileFuel with Java
 * long/int/float semantics. Reactors are built on pads at x = 15000 and ticked directly. Explosions are not triggered;
 * meltdown thresholds are checked as values. Not covered: GUI drawing, sounds, particles, fluid networks.
 */
final class ParityM2Diagnostics {

    private ParityM2Diagnostics() {
    }

    private static final int BASE_X = 15_000;
    private static final int BASE_Z = 15_000;
    private static final int SPACING = 48;
    private static int site;
    private static final List<int[]> FORCED = new ArrayList<>();

    /** id|heatEmission|label|yield|f(0)|f(100)|f(10000) from ItemPWRFuel.EnumPWRFuel and util.function.Function */
    private static final String[] EXPECTED_PWR = {
            "meu|5.0|log10(x / 2,500.0 + 1.0) * 600.0|1000000000|0.0|10.220003579268223|419.3820026016113",
            "heu233|7.5|sqrt(x) * 25.000|1000000000|0.0|249.7550221068531|2499.9975005011497",
            "heu235|7.5|sqrt(x) * 22.500|1000000000|0.0|224.77951989616778|2249.997750451035",
            "men|7.5|log10(x / 2,500.0 + 1.0) * 675.0|1000000000|0.0|11.497504026676749|471.80475292681274",
            "hen237|7.5|sqrt(x) * 27.500|1000000000|0.0|274.7305243175384|2749.9972505512646",
            "mox|7.5|log10(x / 2,500.0 + 1.0) * 600.0|1000000000|0.0|10.220003579268223|419.3820026016113",
            "mep|7.5|log10(x / 2,500.0 + 1.0) * 675.0|1000000000|0.0|11.497504026676749|471.80475292681274",
            "hep239|10.0|sqrt(x) * 22.500|1000000000|0.0|224.77951989616778|2249.997750451035",
            "hep241|10.0|sqrt(x) * 25.000|1000000000|0.0|249.7550221068531|2499.9975005011497",
            "mea|7.5|log10(x / 2,500.0 + 1.0) * 750.0|1000000000|0.0|12.775004474085277|524.2275032520141",
            "hea242|10.0|sqrt(x) * 25.000|1000000000|0.0|249.7550221068531|2499.9975005011497",
            "hes326|12.5|sqrt(x) * 27.500|1000000000|0.0|274.7305243175384|2749.9972505512646",
            "hes327|12.5|sqrt(x) * 30.000|1000000000|0.0|299.70602652822373|2999.9970006013796",
            "bfb_am_mix|2.5|sqrt(x) * 15.000|1000000000|0.0|149.85301326411187|1499.9985003006898",
            "bfb_pu241|2.5|sqrt(x) * 15.000|1000000000|0.0|149.85301326411187|1499.9985003006898"};

    /** ItemZirnoxRod.EnumZirnoxType: id|maxLife|heat|breeding */
    private static final List<String> EXPECTED_ZIRNOX = List.of(
            "natural_uranium_fuel|250000|30|false", "uranium_fuel|200000|50|false", "th232_fuel|20000|0|true",
            "thorium_fuel|200000|40|false", "mox_fuel|165000|75|false", "plutonium_fuel|175000|65|false",
            "u233_fuel|150000|100|false", "u235_fuel|165000|85|false", "les_fuel|150000|150|false",
            "lithium_fuel|20000|0|true", "zfb_mox_fuel|50000|35|false");

    /** ItemPileRodMK2.EnumPileRod: id|getReactivity(100)|heatPerNeutron|life|turnsInto */
    private static final String[] EXPECTED_PILE_RODS = {
            "ra226be|1.0|0.0|0|pile_rod_ra226be", "po210be|1.0|0.0|0|pile_rod_po210be", "zr|0.0|0.0|0|pile_rod_zr",
            "nu|9.990200884274124|0.25|25000|pile_rod_pu239", "pu239|9.990200884274124|0.5|500|pile_rod_rgp",
            "rgp|9.990200884274124|0.5|1000|pile_rod_waste", "waste|9.990200884274124|1.5|0|pile_rod_waste",
            "thorium|9.990200884274124|0.25|35000|pile_rod_thorium_fuel",
            "thorium_fuel|9.990200884274124|0.5|2000|pile_rod_waste"};

    /** ItemPlateFuel in ModItems: id|lifeTime|react(flux 100)|life after one reaction */
    private static final String[] EXPECTED_PLATES = {
            "plate_fuel_u233|2200000|50|100", "plate_fuel_u235|2200000|40|100", "plate_fuel_mox|2400000|50|100",
            "plate_fuel_pu239|2000000|49|100", "plate_fuel_sa326|2000000|80|100", "plate_fuel_ra226be|1300000|30|30",
            "plate_fuel_pu238be|1000000|50|50"};

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        site = 0;
        try {
            pwrFuelTable(problems, cases);
            zirnoxFuelTable(problems, cases);
            pileRodTable(problems, cases);
            plateTable(problems, cases);
            breederRecipes(server, problems, cases);
            pwrReactor(level, problems, cases);
            zirnoxReactor(level, problems, cases);
            researchAndBreeder(level, problems, cases);
            graphitePile(level, problems, cases);
        } catch (RuntimeException | ReflectiveOperationException e) {
            AtomicLegacy.LOGGER.error("Parity M2 diagnostics crashed", e);
            problems.add("M2 diagnostics crashed: " + e);
        } finally {
            for (int[] chunk : FORCED) {
                ChunkRadiationData.get(level).setRadiation(new BlockPos(chunk[0] << 4, 64, chunk[1] << 4), 0.0D);
                level.setChunkForced(chunk[0], chunk[1], false);
            }
            FORCED.clear();
        }
        return cases[0];
    }

    // ---------------------------------------------------------------- tables

    private static void pwrFuelTable(List<String> problems, int[] cases) {
        check(PwrFuelItem.FUELS.size() == EXPECTED_PWR.length, problems, cases, "PWR fuel count " + PwrFuelItem.FUELS.size());
        for (int i = 0; i < Math.min(EXPECTED_PWR.length, PwrFuelItem.FUELS.size()); i++) {
            String[] e = EXPECTED_PWR[i].split("\\|");
            PwrFuelItem.Fuel fuel = PwrFuelItem.FUELS.get(i);
            String actual = fuel.id() + "|" + fuel.heatEmission() + "|" + fuel.function().label() + "|" + (long) fuel.yield();
            check(actual.equals(e[0] + "|" + e[1] + "|" + e[2] + "|" + e[3]), problems, cases, "PWR fuel " + actual);
            near(problems, cases, fuel.function().output(0), Double.parseDouble(e[4]), "PWR " + e[0] + " f(0)");
            near(problems, cases, fuel.function().output(100), Double.parseDouble(e[5]), "PWR " + e[0] + " f(100)");
            near(problems, cases, fuel.function().output(10000), Double.parseDouble(e[6]), "PWR " + e[0] + " f(10000)");
        }
        ItemStack fuel = PwrFuelItem.stackForFuel(HbmItems.PWR_FUEL.get(), 0);
        check(fuel.getMaxStackSize() == 64, problems, cases, "pwr_fuel stack size " + fuel.getMaxStackSize());
    }

    private static void zirnoxFuelTable(List<String> problems, int[] cases) {
        List<String> actual = new ArrayList<>();
        for (ZirnoxRodItem.Fuel f : ZirnoxRodItem.FUELS) actual.add(f.id() + "|" + f.maxLife() + "|" + f.heat() + "|" + f.breeding());
        check(actual.equals(EXPECTED_ZIRNOX), problems, cases, "ZIRNOX fuel table " + actual);
        cases[0] += EXPECTED_ZIRNOX.size() - 1;
    }

    private static void pileRodTable(List<String> problems, int[] cases) {
        for (String line : EXPECTED_PILE_RODS) {
            String[] e = line.split("\\|");
            Item item = BuiltInRegistries.ITEM.get(AtomicLegacy.id("pile_rod_" + e[0]));
            ItemStack stack = new ItemStack(item);
            check(item instanceof PileRodMk2Item rod && (long) rod.kind().life() == Long.parseLong(e[3]),
                    problems, cases, "pile rod life " + e[0]);
            near(problems, cases, PileRodMk2Item.reactivity(stack, 100.0D), Double.parseDouble(e[1]), "pile rod reactivity " + e[0]);
            near(problems, cases, PileRodMk2Item.heatPerNeutron(stack), Double.parseDouble(e[2]), "pile rod heat " + e[0]);
            if (item instanceof PileRodMk2Item rod && rod.kind().life() > 0) {
                ItemStack spent = PileRodMk2Item.react(stack.copy(), rod.kind().life());
                check(BuiltInRegistries.ITEM.getKey(spent.getItem()).getPath().equals(e[4]), problems, cases,
                        "pile rod " + e[0] + " turns into " + BuiltInRegistries.ITEM.getKey(spent.getItem()));
            }
        }
        check(PileMk2CoreBlockEntity.MAX_HEAT == 800, problems, cases, "TileEntityPileCore.MAX_HEAT 800");
    }

    private static void plateTable(List<String> problems, int[] cases) {
        for (String line : EXPECTED_PLATES) {
            String[] e = line.split("\\|");
            Item item = BuiltInRegistries.ITEM.get(AtomicLegacy.id(e[0]));
            if (!(item instanceof PlateFuelItem plate)) {
                problems.add("M2: missing plate fuel " + e[0]);
                continue;
            }
            ItemStack stack = new ItemStack(item);
            int out = plate.react(null, stack, 100);
            check(plate.lifeTime() == Integer.parseInt(e[1]) && out == Integer.parseInt(e[2])
                    && PlateFuelItem.life(stack) == Integer.parseInt(e[3]), problems, cases,
                    "plate " + e[0] + " life " + plate.lifeTime() + " out " + out + " used " + PlateFuelItem.life(stack));
        }
    }

    private static void breederRecipes(MinecraftServer server, List<String> problems, int[] cases) {
        // BreederRecipes.registerDefaults: setRecipe(in, out, flux) for rod / rod_dual (x2) / rod_quad (x3) + sword
        String[][] rods = {{"lithium", "tritium", "200"}, {"co", "co60", "100"}, {"ra226", "ac227", "300"},
                {"th232", "thf", "500"}, {"u235", "np237", "300"}, {"np237", "pu238", "200"}, {"pu238", "pu239", "1000"},
                {"u238", "rgp", "300"}, {"uranium", "rgp", "200"}, {"rgp", "waste", "200"}};
        List<String> expected = new ArrayList<>();
        for (String[] r : rods) {
            int flux = Integer.parseInt(r[2]);
            expected.add("rod@" + r[0] + ">rod@" + r[1] + "|" + flux);
            expected.add("rod_dual@" + r[0] + ">rod_dual@" + r[1] + "|" + flux * 2);
            expected.add("rod_quad@" + r[0] + ">rod_quad@" + r[1] + "|" + flux * 3);
        }
        expected.add("meteorite_sword_etched>meteorite_sword_bred|1000");
        List<String> actual = new ArrayList<>();
        for (RecipeHolder<BreederReactorRecipe> h : server.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.BREEDER_REACTOR.get())) {
            BreederReactorRecipe r = h.value();
            ItemStack[] inputs = r.ingredient().getItems();
            String in = inputs.length == 0 ? "-" : describe(inputs[0]);
            actual.add(in + ">" + describe(r.result()) + "|" + r.flux());
        }
        compare(expected, actual, problems, cases, "breeder recipes");
    }

    // ---------------------------------------------------------------- PWR

    private static void pwrReactor(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos origin = pad(level, 80, 3);
        // casing box x 0..4, y 0..3, z 0..2; interior row y1: fuel, control, fuel; y2: source, heatex, channel
        for (int x = 0; x <= 4; x++) for (int y = 0; y <= 3; y++) for (int z = 0; z <= 2; z++) {
            level.setBlock(origin.offset(x, y, z), HbmBlocks.PWR_CASING.get().defaultBlockState(), 2 | 16);
        }
        level.setBlock(origin.offset(1, 1, 1), HbmBlocks.PWR_FUELROD.get().defaultBlockState(), 2 | 16);
        level.setBlock(origin.offset(2, 1, 1), HbmBlocks.PWR_CONTROL.get().defaultBlockState(), 2 | 16);
        level.setBlock(origin.offset(3, 1, 1), HbmBlocks.PWR_FUELROD.get().defaultBlockState(), 2 | 16);
        level.setBlock(origin.offset(1, 2, 1), HbmBlocks.PWR_NEUTRON_SOURCE.get().defaultBlockState(), 2 | 16);
        level.setBlock(origin.offset(2, 2, 1), HbmBlocks.PWR_HEATEX.get().defaultBlockState(), 2 | 16);
        level.setBlock(origin.offset(3, 2, 1), HbmBlocks.PWR_CHANNEL.get().defaultBlockState(), 2 | 16);
        level.setBlock(origin.offset(0, 1, 1), HbmBlocks.PWR_REFLECTOR.get().defaultBlockState(), 2 | 16);
        level.setBlock(origin.offset(4, 1, 1), HbmBlocks.PWR_REFLECTOR.get().defaultBlockState(), 2 | 16);
        level.setBlock(origin.offset(2, 3, 1), HbmBlocks.PWR_PORT.get().defaultBlockState(), 2 | 16);
        BlockPos controllerPos = origin.offset(2, 1, 0);
        BlockState controllerState = HbmBlocks.PWR_CONTROLLER.get().defaultBlockState().setValue(PwrBlock.FACING, Direction.NORTH);
        level.setBlock(controllerPos, controllerState, 2 | 16);
        if (!(level.getBlockEntity(controllerPos) instanceof PwrControllerBlockEntity pwr)) {
            problems.add("M2: PWR controller block entity missing");
            return;
        }
        PwrControllerBlockEntity.AssemblyResult result = pwr.assemble();
        check(result.status() == PwrControllerBlockEntity.AssemblyStatus.SUCCESS, problems, cases, "PWR assembly " + result.status());
        if (result.status() != PwrControllerBlockEntity.AssemblyStatus.SUCCESS) {
            cleanup(level, origin, 3);
            return;
        }
        check(getInt(pwr, "rodCount") == 2 && getInt(pwr, "connections") == 2 && getInt(pwr, "connectionsControlled") == 1
                        && getInt(pwr, "sourceCount") == 1 && getInt(pwr, "heatexCount") == 1 && getInt(pwr, "channelCount") == 1
                        && getLong(pwr, "coreHeatCapacity") == 10_000_000L,
                problems, cases, "PWR setup rods " + getInt(pwr, "rodCount") + " conn " + getInt(pwr, "connections") + "/"
                        + getInt(pwr, "connectionsControlled") + " cap " + getLong(pwr, "coreHeatCapacity"));
        ItemStack fuel = PwrFuelItem.stackForFuel(HbmItems.PWR_FUEL.get(), 0);
        fuel.setCount(2);
        pwr.setItem(PwrControllerBlockEntity.SLOT_INPUT, fuel);
        pwr.coolantTank().setType(HbmFluids.byName("coolant").orElseThrow());
        pwr.coolantTank().setAmount(128_000);
        set(pwr, "coreHeat", 3_000_000L);
        set(pwr, "hullHeat", 1_000_000L);
        pwr.setRodTarget(0.0D);
        for (int i = 0; i < 40; i++) PwrControllerBlockEntity.tick(level, controllerPos, controllerState, pwr);
        // sim_m2.py pwr_run(40)
        long core = getLong(pwr, "coreHeat");
        long hull = getLong(pwr, "hullHeat");
        check(Math.abs(core - 801_321L) <= 2 && Math.abs(hull - 694_601L) <= 2, problems, cases,
                "PWR heat core " + core + " (801321) hull " + hull + " (694601)");
        near(problems, cases, getDouble(pwr, "flux"), 20.0D, "PWR flux");
        near(problems, cases, getDouble(pwr, "progress"), 17.808292968834934D, "PWR fuel progress");
        near(problems, cases, getDouble(pwr, "rodLevel"), 60.0D, "PWR rod level after 40 ticks");
        check(getInt(pwr, "amountLoaded") == 2 && pwr.coolantTank().amount() == 119_992 && pwr.hotCoolantTank().amount() == 8_008
                        && "coolant_hot".equals(pwr.hotCoolantTank().type().name()),
                problems, cases, "PWR loaded " + getInt(pwr, "amountLoaded") + " coolant " + pwr.coolantTank().amount()
                        + " hot " + pwr.hotCoolantTank().amount() + " " + pwr.hotCoolantTank().type().name());
        // meltdown threshold: coreHeat > coreHeatCapacity (checked as a value; the explosion is not triggered)
        check(core < getLong(pwr, "coreHeatCapacity") && level.getBlockState(controllerPos).is(HbmBlocks.PWR_CONTROLLER.get()),
                problems, cases, "PWR below meltdown threshold");
        pwr.clearContent();
        pwr.disassemble();
        cleanup(level, origin, 3);
    }

    // ---------------------------------------------------------------- ZIRNOX

    private static void zirnoxReactor(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level, 80, 4);
        BlockState state = HbmBlocks.MACHINE_ZIRNOX.get().defaultBlockState();
        level.setBlock(pos, state, 2 | 16);
        if (!(level.getBlockEntity(pos) instanceof ZirnoxReactorBlockEntity zirnox)) {
            problems.add("M2: ZIRNOX block entity missing");
            return;
        }
        for (int slot : new int[] {0, 1, 7, 8}) {
            zirnox.setItem(slot, LegacyVariantItem.stackFor(HbmItems.ROD_ZIRNOX.get(), "uranium_fuel"));
        }
        zirnox.waterTank().setType(HbmFluids.byName("water").orElseThrow());
        zirnox.waterTank().setAmount(32_000);
        zirnox.carbonDioxideTank().setType(HbmFluids.byName("carbondioxide").orElseThrow());
        zirnox.carbonDioxideTank().setAmount(16_000);
        zirnox.toggleActive();
        for (int i = 0; i < 600; i++) ZirnoxReactorBlockEntity.tick(level, pos, level.getBlockState(pos), zirnox);
        // sim_m2.py zirnox_run(600, 50) on slots 0, 1, 7, 8
        int life = ZirnoxRodItem.life(zirnox.getItem(0));
        check(zirnox.heat() == 12_658 && zirnox.pressure() == 45_258 && zirnox.steamTank().amount() == 2_780
                        && zirnox.waterTank().amount() == 29_220 && life == 1_800,
                problems, cases, "ZIRNOX heat " + zirnox.heat() + " (12658) pressure " + zirnox.pressure() + " (45258) steam "
                        + zirnox.steamTank().amount() + " (2780) water " + zirnox.waterTank().amount() + " (29220) life " + life + " (1800)");
        check(level.getBlockState(pos).is(HbmBlocks.MACHINE_ZIRNOX.get()), problems, cases, "ZIRNOX below meltdown threshold");
        // vent: receiveControl("vent") removes 1000 mB CO2
        zirnox.ventCarbonDioxide();
        check(zirnox.carbonDioxideTank().amount() == 15_000, problems, cases, "ZIRNOX vent " + zirnox.carbonDioxideTank().amount());
        zirnox.clearContent();
        cleanup(level, pos, 4);
    }

    // ---------------------------------------------------------------- research + breeder

    private static void researchAndBreeder(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level, 80, 4);
        BlockState reactorState = HbmBlocks.MACHINE_REACTOR_SMALL.get().defaultBlockState();
        level.setBlock(pos, reactorState, 2 | 16);
        BlockPos breederPos = pos.east();
        BlockState breederState = HbmBlocks.MACHINE_REACTOR.get().defaultBlockState();
        level.setBlock(breederPos, breederState, 2 | 16);
        if (!(level.getBlockEntity(pos) instanceof ResearchReactorBlockEntity reactor)
                || !(level.getBlockEntity(breederPos) instanceof BreederReactorBlockEntity breeder)) {
            problems.add("M2: research or breeding reactor block entity missing");
            return;
        }
        reactor.setItem(0, new ItemStack(HbmItems.PLATE_FUEL_PU238BE.get()));
        for (int slot : new int[] {1, 5, 6, 10, 11}) reactor.setItem(slot, new ItemStack(HbmItems.PLATE_FUEL_U233.get()));
        reactor.setTargetLevel(1.0D);
        breeder.setItem(0, LegacyVariantItem.stackFor(HbmItems.ROD.get(), "co"));
        for (int i = 0; i < 60; i++) {
            ResearchReactorBlockEntity.tick(level, pos, level.getBlockState(pos), reactor);
            BreederReactorBlockEntity.tick(level, breederPos, level.getBlockState(breederPos), breeder);
        }
        // sim_m2.py research_run(60) with pu238be in slot 0 and u233 in 1, 5, 6, 10, 11
        int[] lives = new int[12];
        for (int s : new int[] {0, 1, 5, 6, 10, 11}) lives[s] = PlateFuelItem.life(reactor.getItem(s));
        check(reactor.totalFlux() == 341 && reactor.heat() == 33_836 && lives[0] == 3_000 && lives[1] == 5_154
                        && lives[5] == 7_432 && lives[6] == 7_456 && lives[10] == 5_121 && lives[11] == 5_155,
                problems, cases, "research reactor flux " + reactor.totalFlux() + " (341) heat " + reactor.heat()
                        + " (33836) lives " + lives[0] + "/" + lives[1] + "/" + lives[5] + "/" + lives[6] + "/" + lives[10] + "/" + lives[11]);
        check(getInt(breeder, "flux") == 341 && Math.abs(breeder.progress() - 0.35749977827072144F) < 1e-7F,
                problems, cases, "breeder flux " + getInt(breeder, "flux") + " progress " + breeder.progress() + " (0.35749978)");
        // breeder ignores RBMK and any non-research neighbour (getInteractions only reads reactor_research)
        check(BreederReactorBlockEntity.researchReactorFlux(level, breederPos.east()) == 0, problems, cases, "breeder only reads adjacent research reactors");
        reactor.clearContent();
        breeder.clearContent();
        cleanup(level, pos, 4);
    }

    // ---------------------------------------------------------------- legacy graphite pile

    private static void graphitePile(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = pad(level, 80, 2);
        BlockState state = PileGraphiteBlock.stateFor(PileGraphiteBlock.Kind.FUEL, Direction.Axis.Y, false, false);
        level.setBlock(pos, state, 2 | 16);
        if (!(level.getBlockEntity(pos) instanceof PileGraphiteBlockEntity pile)) {
            problems.add("M2: graphite fuel block entity missing");
            return;
        }
        set(pile, "heat", 500);
        pile.receiveNeutrons(400);
        PileGraphiteBlockEntity.tick(level, pos, level.getBlockState(pos), pile);
        // TileEntityPileFuel: heat -= heat * 0.05 (compound int), reaction = neutrons * (1 - heat / 1000 * 0.5)
        check(getInt(pile, "heat") == 780 && getInt(pile, "progress") == 305, problems, cases,
                "graphite fuel heat " + getInt(pile, "heat") + " (780) progress " + getInt(pile, "progress") + " (305)");
        cleanup(level, pos, 2);
    }

    // ---------------------------------------------------------------- helpers

    private static String describe(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        String variant = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("variant");
        return variant.isEmpty() ? id : id + "@" + variant;
    }

    private static void check(boolean ok, List<String> problems, int[] cases, String what) {
        cases[0]++;
        if (!ok) problems.add("M2: " + what);
    }

    private static void near(List<String> problems, int[] cases, double actual, double expected, String what) {
        cases[0]++;
        double tolerance = Math.max(1e-9D, Math.abs(expected) * 1e-9D);
        if (Math.abs(actual - expected) > tolerance) {
            problems.add(String.format(Locale.ROOT, "M2: %s = %s, expected %s", what, actual, expected));
        }
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
        if (!missing.isEmpty() || !extra.isEmpty()) problems.add("M2: " + what + " differ: missing " + missing + ", extra " + extra);
    }

    private static Field field(Object target, String name) throws NoSuchFieldException {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field f = type.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static int getInt(Object target, String name) throws ReflectiveOperationException {
        return ((Number) field(target, name).get(target)).intValue();
    }

    private static long getLong(Object target, String name) throws ReflectiveOperationException {
        return ((Number) field(target, name).get(target)).longValue();
    }

    private static double getDouble(Object target, String name) throws ReflectiveOperationException {
        return ((Number) field(target, name).get(target)).doubleValue();
    }

    private static void set(Object target, String name, Object value) throws ReflectiveOperationException {
        field(target, name).set(target, value);
    }

    /** Clears a pad; chunks within two chunks are force-loaded (the PWR checks chunkExists at +-2 chunks). */
    private static BlockPos pad(ServerLevel level, int y, int radius) {
        int index = site++;
        BlockPos pos = new BlockPos(BASE_X + index * SPACING, y, BASE_Z);
        for (int cx = (pos.getX() >> 4) - 2; cx <= (pos.getX() >> 4) + 2; cx++) {
            for (int cz = (pos.getZ() >> 4) - 2; cz <= (pos.getZ() >> 4) + 2; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
                FORCED.add(new int[] {cx, cz});
            }
        }
        for (int dx = -radius - 1; dx <= radius + 5; dx++) for (int dy = -2; dy <= 8; dy++) for (int dz = -radius - 1; dz <= radius + 3; dz++) {
            BlockPos p = pos.offset(dx, dy, dz);
            level.setBlock(p, dy < 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        return pos;
    }

    private static void cleanup(ServerLevel level, BlockPos pos, int radius) {
        for (int pass = 0; pass < 2; pass++) {
            for (BlockPos cursor : BlockPos.betweenClosed(pos.offset(-radius - 1, 0, -radius - 1), pos.offset(radius + 5, 8, radius + 3))) {
                BlockEntity be = level.getBlockEntity(cursor);
                if (be instanceof net.minecraft.world.Container container) container.clearContent();
                if (!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
        }
    }

    @SuppressWarnings("unused")
    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(AtomicLegacy.id(path));
    }
}
