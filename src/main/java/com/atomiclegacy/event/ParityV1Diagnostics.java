package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidNetUser;
import com.atomiclegacy.fluid.HbmFluidNetworks;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.rtty.RttyInfo;
import com.atomiclegacy.rtty.RttyInteractive;
import com.atomiclegacy.rtty.RttyProxyLayouts;
import com.atomiclegacy.rtty.RttyValueProvider;
import com.atomiclegacy.tileentity.AssemblyFactoryBlockEntity;
import com.atomiclegacy.tileentity.FluidBarrelBlockEntity;
import com.atomiclegacy.tileentity.LegacyMachineBlockEntity;
import com.atomiclegacy.tileentity.MachineDummyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * [parity V1: diagnostics] FluidNetMK2 port nodes of the machines converted in V1 (connection count of the original
 * getPorts(), sending / receiving tanks, priority; every port lies in the machine and connects out of it), transfers
 * through the net (crystallizer, factory coolant delegate, gas turbine smoke, pyro oven exhaust node), ROR proxy
 * cells of the barrel multiblocks / machine_orbus and the function-less machine_boiler_off.
 */
final class ParityV1Diagnostics {
    private static final int BASE_X = 35_000;
    private static final int BASE_Y = 200;
    private static int site;
    private static final List<int[]> FORCED = new ArrayList<>();

    private ParityV1Diagnostics() {
    }

    /** block id, connections of the original getPorts(), sending / receiving tanks, LOW priority */
    private record MachineCase(String id, int connections, int sending, int receiving, boolean low) {
    }

    private static final MachineCase[] MACHINES = {
            new MachineCase("machine_crystallizer", 8, 0, 1, false),
            new MachineCase("machine_gascent", 6, 0, 1, false),
            new MachineCase("machine_electrolyser", 8, 2, 2, false),
            new MachineCase("machine_assembly_machine", 12, 1, 1, false),
            new MachineCase("machine_chemical_plant", 12, 3, 3, false),
            new MachineCase("machine_assembly_factory", 16, 4, 4, false),
            new MachineCase("machine_chemical_factory", 30, 12, 12, false),
            new MachineCase("machine_arc_welder", 10, 0, 1, false),
            new MachineCase("machine_soldering_station", 8, 0, 1, false),
            new MachineCase("machine_cyclotron", 12, 2, 1, false),
            new MachineCase("machine_drain", 3, 0, 1, true),
            new MachineCase("machine_excavator", 4, 0, 1, false),
            new MachineCase("machine_flare", 4, 0, 1, true),
            new MachineCase("machine_icf_press", 6, 0, 2, false),
            new MachineCase("machine_rockmill", 8, 1, 1, false),
            new MachineCase("machine_silex", 2, 0, 1, false),
            new MachineCase("machine_solidifier", 6, 0, 1, false),
            new MachineCase("machine_strand_caster", 4, 1, 1, false),
            new MachineCase("machine_supercomputer", 5, 1, 1, false),
            new MachineCase("machine_wood_burner", 2, 0, 1, false),
            new MachineCase("pile_vent", 1, 0, 1, false),
            new MachineCase("launch_pad", 8, 0, 2, false),
            new MachineCase("launch_pad_large", 8, 0, 2, false),
            new MachineCase("compact_launcher", 8, 0, 2, false),
            new MachineCase("launch_table", 36, 0, 2, false),
            new MachineCase("launchpad_lambda", 5, 0, 2, false),
            new MachineCase("launchpad_soyuz", 12, 0, 2, false),
            new MachineCase("dfc_emitter", 6, 0, 1, false),
            new MachineCase("dfc_injector", 6, 0, 2, false),
            new MachineCase("dfc_receiver", 6, 0, 1, false),
            new MachineCase("drone_crate", 6, 0, 0, false),
            new MachineCase("turret_fritz", 8, 0, 1, false),
            new MachineCase("fusion_boiler", 4, 1, 1, false),
            new MachineCase("fusion_breeder", 5, 1, 1, false),
            new MachineCase("fusion_klystron", 3, 0, 1, false),
            new MachineCase("fusion_mhdt", 3, 1, 1, false),
            new MachineCase("fusion_plasma_forge", 10, 0, 1, false),
            new MachineCase("fusion_torus", 26, 2, 4, false),
            new MachineCase("pa_source", 10, 1, 1, false),
            new MachineCase("pa_rfc", 6, 1, 1, false),
            new MachineCase("pa_quadrupole", 4, 1, 1, false),
            new MachineCase("pa_dipole", 8, 1, 1, false),
            new MachineCase("pa_detector", 5, 1, 1, false),
            new MachineCase("machine_annihilator", 3, 0, 1, true),
            new MachineCase("machine_autosaw", 6, 0, 1, false),
            new MachineCase("machine_thresher", 2, 0, 1, false),
            new MachineCase("machine_precass", 12, 1, 1, false),
            new MachineCase("machine_radiolysis", 4, 2, 1, false),
            new MachineCase("machine_orbus", 8, 1, 1, false),
            new MachineCase("machine_pyrooven", 5, 2, 1, false),
            new MachineCase("machine_turbinegas", 5, 4, 3, false),
    };

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        site = 0;
        try {
            Player fake = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            for (MachineCase machine : MACHINES) machinePorts(level, fake, machine, problems, cases);
            crystallizerTransfer(level, fake, problems, cases);
            factoryCoolant(level, fake, problems, cases);
            turbineSmoke(level, fake, problems, cases);
            pyroOvenNodes(level, fake, problems, cases);
            orbusRor(level, fake, problems, cases);
            tankProxies(level, fake, problems, cases);
            boilers(level, problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity V1 diagnostics crashed", e);
            problems.add("V1 diagnostics crashed: " + e);
        } finally {
            for (int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
            FORCED.clear();
        }
        return cases[0];
    }

    private static void check(boolean ok, List<String> problems, int[] cases, String what) {
        cases[0]++;
        if (!ok) problems.add("V1: " + what);
    }

    private static void eq(long actual, long expected, List<String> problems, int[] cases, String what) {
        check(actual == expected, problems, cases, what + " expected " + expected + " got " + actual);
    }

    private static BlockPos site(ServerLevel level) {
        BlockPos pos = new BlockPos(BASE_X, BASE_Y, (site++) * 48);
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                level.setChunkForced(x, z, true);
                level.getChunk(x, z);
                FORCED.add(new int[]{x, z});
            }
        }
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-14, -4, -14), pos.offset(14, 12, 14))) {
            if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        return pos;
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-14, -4, -14), pos.offset(14, 12, 14))) {
            if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }

    /** Places the block like a player facing EAST (U2 placement) and returns the core block entity. */
    private static BlockEntity place(ServerLevel level, Player fake, BlockPos pos, String id) {
        Block block = BuiltInRegistries.BLOCK.get(AtomicLegacy.id(id));
        if (block == Blocks.AIR) return null;
        BlockState state = block.defaultBlockState();
        for (Property<?> property : state.getProperties()) {
            if (property instanceof DirectionProperty direction && property.getName().equals("facing")
                    && direction.getPossibleValues().contains(Direction.EAST)) {
                state = state.setValue(direction, Direction.EAST);
            }
        }
        level.setBlock(pos, state, 3);
        block.setPlacedBy(level, pos, level.getBlockState(pos), fake, new ItemStack(block));
        BlockEntity core = level.getBlockEntity(pos);
        if (core instanceof MachineDummyBlockEntity dummy) core = dummy.core();
        return core;
    }

    private static boolean partOf(ServerLevel level, BlockPos pos, BlockPos core) {
        if (pos.equals(core)) return true;
        BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof MachineDummyBlockEntity dummy && core.equals(dummy.getCorePos());
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElseThrow();
    }

    private static HbmFluidDefinition typeOf(HbmFluidNetUser user) {
        for (HbmFluidTank tank : user.fluidReceivingTanks()) if (!tank.type().isNone()) return tank.type();
        for (HbmFluidTank tank : user.fluidSendingTanks()) if (!tank.type().isNone()) return tank.type();
        return fluid("water");
    }

    private static void update(ServerLevel level, int times) {
        for (int i = 0; i < times; i++) HbmFluidNetworks.updateNow(level);
    }

    /// port tables ///

    private static void machinePorts(ServerLevel level, Player fake, MachineCase machine, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        BlockEntity core = place(level, fake, o, machine.id());
        check(core instanceof HbmFluidNetUser user && user.isFluidNetUser(), problems, cases, machine.id() + " is a FluidNetMK2 user (" + core + ")");
        if (!(core instanceof HbmFluidNetUser user) || !user.isFluidNetUser()) {
            clear(level, o);
            return;
        }
        List<HbmFluidNetUser.FluidPort> ports = user.fluidPorts(typeOf(user));
        eq(ports == null ? -1 : ports.size(), machine.connections(), problems, cases, machine.id() + " port connections");
        eq(user.fluidSendingTanks().length, machine.sending(), problems, cases, machine.id() + " sending tanks");
        eq(user.fluidReceivingTanks().length, machine.receiving(), problems, cases, machine.id() + " receiving tanks");
        check((user.fluidPriority() == PowerEndpoint.ConnectionPriority.LOW) == machine.low(), problems, cases,
                machine.id() + " priority " + user.fluidPriority());
        if (ports != null) {
            BlockPos corePos = core.getBlockPos();
            Direction facing = RttyProxyLayouts.facing(core.getBlockState());
            for (HbmFluidNetUser.FluidPort port : ports) {
                // original geometry: MachineChemicalFactory is 5x5x3 but getFluidPorts() also puts nodes at y + 3 and
                // rot * 3 (outside the block, joined by ducts placed there); TilePortShapes.around on the gas
                // centrifuge connects UP into its own upper part
                BlockPos rel = port.pos().subtract(corePos);
                if (machine.id().equals("machine_chemical_factory") && (rel.getY() == 3 || Math.abs(rel.getX()) == 3 || Math.abs(rel.getZ()) == 3)) continue;
                if (machine.id().equals("machine_gascent") && port.dir() == Direction.UP) continue;
                check(partOf(level, port.pos(), corePos), problems, cases, machine.id() + " port " + port.pos().subtract(corePos)
                        + " (facing " + facing + ") is a part of the machine");
                check(!partOf(level, port.connection(), corePos), problems, cases, machine.id() + " port " + port.pos().subtract(corePos)
                        + " -> " + port.dir() + " (facing " + facing + ") connects out of the machine");
            }
        }
        clear(level, o);
    }

    /// transfers ///

    private static FluidBarrelBlockEntity barrel(ServerLevel level, BlockPos pos, int mode, HbmFluidDefinition type, int amount) {
        level.setBlock(pos, HbmBlocks.BARREL_STEEL.get().defaultBlockState(), 3);
        FluidBarrelBlockEntity barrel = (FluidBarrelBlockEntity) level.getBlockEntity(pos);
        for (int i = 0; i < mode; i++) barrel.cycleMode();
        barrel.tank().setType(type);
        barrel.tank().setAmount(amount);
        HbmFluidNetworks.subscribe(level, barrel);
        return barrel;
    }

    /** a free connection of the user's ports (air, outside the machine) */
    private static HbmFluidNetUser.FluidPort freePort(ServerLevel level, HbmFluidNetUser user, HbmFluidDefinition type, BlockPos core) {
        List<HbmFluidNetUser.FluidPort> ports = user.fluidPorts(type);
        if (ports == null) return null;
        for (HbmFluidNetUser.FluidPort port : ports) {
            if (level.getBlockState(port.connection()).isAir() && !partOf(level, port.connection(), core)) return port;
        }
        return null;
    }

    private static void crystallizerTransfer(ServerLevel level, Player fake, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        BlockEntity core = place(level, fake, o, "machine_crystallizer");
        if (!(core instanceof HbmFluidNetUser user)) {
            check(false, problems, cases, "crystallizer placed");
            return;
        }
        HbmFluidDefinition peroxide = fluid("peroxide");
        HbmFluidNetUser.FluidPort port = freePort(level, user, peroxide, core.getBlockPos());
        check(port != null, problems, cases, "crystallizer has a free port connection");
        if (port != null) {
            FluidBarrelBlockEntity source = barrel(level, port.connection(), 2, peroxide, 1000);
            HbmFluidNetworks.subscribe(level, user);
            update(level, 2);
            int received = user.fluidReceivingTanks()[0].amount();
            check(received > 0, problems, cases, "crystallizer is fed through its port node (" + received + ")");
            eq(source.tank().amount() + received, 1000, problems, cases, "crystallizer transfer conserves fluid");
        }
        clear(level, o);
    }

    /** DelegateAssemblyFactoy: water at a coolant port fills only the coolant tank, not the module tanks */
    private static void factoryCoolant(ServerLevel level, Player fake, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        BlockEntity core = place(level, fake, o, "machine_assembly_factory");
        if (!(core instanceof AssemblyFactoryBlockEntity factory)) {
            check(false, problems, cases, "assembly factory placed");
            return;
        }
        HbmFluidDefinition water = fluid("water");
        HbmFluidNetUser coolant = factory.coolantPorts();
        eq(coolant.fluidPorts(water).size(), 4, problems, cases, "assembly factory coolant port connections");
        check(coolant.fluidNetOwner() == factory, problems, cases, "coolant delegate is owned by the factory");
        for (HbmFluidTank tank : factory.fluidReceivingTanks()) tank.setType(water);
        HbmFluidNetUser.FluidPort port = freePort(level, coolant, water, core.getBlockPos());
        check(port != null, problems, cases, "assembly factory has a free coolant connection");
        if (port != null) {
            FluidBarrelBlockEntity source = barrel(level, port.connection(), 2, water, 1000);
            HbmFluidNetworks.subscribe(level, factory);
            HbmFluidNetworks.subscribe(level, coolant);
            update(level, 2);
            int coolantFill = factory.waterTank().amount();
            int moduleFill = 0;
            for (HbmFluidTank tank : factory.fluidReceivingTanks()) moduleFill += tank.amount();
            check(coolantFill > 0, problems, cases, "coolant port fills the water tank (" + coolantFill + ")");
            eq(moduleFill, 0, problems, cases, "coolant port does not reach the module input tanks");
            eq(source.tank().amount() + coolantFill, 1000, problems, cases, "coolant transfer conserves fluid");
        }
        clear(level, o);
    }

    /** TileEntityMachineTurbineGas: smoke tanks are sending tanks of the main ports */
    private static void turbineSmoke(ServerLevel level, Player fake, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        BlockEntity core = place(level, fake, o, "machine_turbinegas");
        if (!(core instanceof HbmFluidNetUser user)) {
            check(false, problems, cases, "gas turbine placed");
            return;
        }
        HbmFluidTank[] sending = user.fluidSendingTanks();
        String[] types = {"smoke", "smoke_leaded", "smoke_poison"};
        for (int i = 0; i < 3 && i + 1 < sending.length; i++) {
            check(sending[i + 1].type() == fluid(types[i]), problems, cases, "gas turbine sending tank " + (i + 1) + " is " + types[i]);
            eq(sending[i + 1].capacity(), 150, problems, cases, "gas turbine " + types[i] + " buffer");
        }
        if (sending.length < 2) return;
        HbmFluidDefinition smoke = fluid("smoke");
        sending[1].setAmount(100);
        HbmFluidNetUser.FluidPort port = freePort(level, user, smoke, core.getBlockPos());
        check(port != null, problems, cases, "gas turbine has a free port connection");
        if (port != null) {
            FluidBarrelBlockEntity sink = barrel(level, port.connection(), 0, smoke, 0);
            HbmFluidNetworks.subscribe(level, user);
            update(level, 2);
            eq(sink.tank().amount(), 100, problems, cases, "gas turbine smoke leaves through the port net");
        }
        clear(level, o);
    }

    /** TileEntityMachinePyroOven: getPorts() and moduleSmokePorts are separate nodes */
    private static void pyroOvenNodes(ServerLevel level, Player fake, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        BlockEntity core = place(level, fake, o, "machine_pyrooven");
        if (!(core instanceof LegacyMachineBlockEntity oven)) {
            check(false, problems, cases, "pyro oven placed");
            return;
        }
        HbmFluidDefinition smoke = fluid("smoke");
        HbmFluidNetUser exhaust = oven.pyroSmokePorts();
        List<HbmFluidNetUser.FluidPort> exhaustPorts = exhaust.fluidPorts(smoke);
        eq(exhaustPorts == null ? -1 : exhaustPorts.size(), 1, problems, cases, "pyro oven smoke port");
        eq(exhaust.fluidSendingTanks().length, 3, problems, cases, "pyro oven smoke port tanks");
        if (exhaustPorts != null && !exhaustPorts.isEmpty()) {
            HbmFluidNetUser.FluidPort port = exhaustPorts.get(0);
            check(oven.fluidPorts(smoke).stream().noneMatch(p -> p.pos().equals(port.pos())), problems, cases, "exhaust is not one of the side ports");
            check(partOf(level, port.pos(), core.getBlockPos()), problems, cases, "pyro oven exhaust port is part of the machine");
            oven.fluidSendingTanks()[1].setType(smoke); // the port types the smoke buffers on the first fill
            oven.fluidSendingTanks()[1].setAmount(30);
            FluidBarrelBlockEntity sink = barrel(level, port.connection(), 0, smoke, 0);
            HbmFluidNetworks.subscribe(level, oven);
            HbmFluidNetworks.subscribe(level, exhaust);
            update(level, 2);
            eq(sink.tank().amount(), 30, problems, cases, "pyro oven smoke leaves through the exhaust node");
        }
        clear(level, o);
    }

    /// ROR ///

    private static void orbusRor(ServerLevel level, Player fake, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        BlockEntity core = place(level, fake, o, "machine_orbus");
        if (!(core instanceof LegacyMachineBlockEntity orbus)) {
            check(false, problems, cases, "orbus placed");
            return;
        }
        String[] expected = {"VAL:type", "VAL:fill", "VAL:fillpercent", "FUN:setmode!mode (0-3)", "FUN:setmode!mode:fallback (0-3)"};
        check(Arrays.equals(orbus.rttyFunctionInfo(), expected), problems, cases, "orbus getFunctionInfo " + Arrays.toString(orbus.rttyFunctionInfo()));
        check(orbus.rttyProvidesValues() && orbus.rttyInteractive(), problems, cases, "orbus is IRORValueProvider + IRORInteractive");
        HbmFluidTank tank = orbus.fluidReceivingTanks()[0];
        tank.setType(fluid("water"));
        tank.setAmount(128_000);
        check("WATER".equals(orbus.provideRttyValue("VAL:type")), problems, cases, "orbus VAL:type " + orbus.provideRttyValue("VAL:type"));
        check("25".equals(orbus.provideRttyValue("VAL:fillpercent")), problems, cases, "orbus VAL:fillpercent " + orbus.provideRttyValue("VAL:fillpercent"));
        orbus.runRttyFunction("FUN:setmode", new String[]{"2"});
        eq(orbus.fluidReceiverSpeed(tank.type(), 0), 0, problems, cases, "orbus mode 2 receiver speed");
        eq(orbus.fluidProviderSpeed(tank.type(), 0), 1_280, problems, cases, "orbus mode 2 provider speed fill/100");
        orbus.runRttyFunction("FUN:setmode", new String[]{"2", "1"});
        check(orbus.fluidPriority() == PowerEndpoint.ConnectionPriority.LOW, problems, cases, "orbus setmode fallback to 1 (LOW)");
        proxies(level, core, "machine_orbus", problems, cases);
        clear(level, o);
    }

    private static void tankProxies(ServerLevel level, Player fake, List<String> problems, int[] cases) {
        for (String id : new String[]{"machine_fluidtank", "machine_bat9000", "machine_bigasstank"}) {
            BlockPos o = site(level);
            BlockEntity core = place(level, fake, o, id);
            check(core instanceof RttyValueProvider, problems, cases, id + " ROR core");
            if (core != null) proxies(level, core, id, problems, cases);
            clear(level, o);
        }
    }

    private static void proxies(ServerLevel level, BlockEntity core, String id, List<String> problems, int[] cases) {
        check(RttyProxyLayouts.hasLayout(id), problems, cases, id + " has a proxy layout");
        BlockPos corePos = core.getBlockPos();
        BlockPos[] cells = RttyProxyLayouts.proxyCells(id, corePos, RttyProxyLayouts.facing(core.getBlockState()));
        if (cells == null) return;
        for (BlockPos cell : cells) {
            if (cell.equals(corePos)) continue;
            check(partOf(level, cell, corePos), problems, cases, id + " proxy cell " + cell.subtract(corePos) + " is a dummy");
            check(RttyProxyLayouts.isProxy(core, cell), problems, cases, id + " proxy cell forwards");
        }
        List<BlockPos> cellList = Arrays.asList(cells);
        for (BlockPos p : BlockPos.betweenClosed(corePos.offset(-8, -2, -8), corePos.offset(8, 10, 8))) {
            if (!cellList.contains(p) && partOf(level, p, corePos) && !p.equals(corePos)) {
                check(!RttyProxyLayouts.isProxy(core, p.immutable()), problems, cases, id + " plain dummy " + p.subtract(corePos) + " is no proxy");
                break;
            }
        }
    }

    /// boilers ///

    private static void boilers(ServerLevel level, List<String> problems, int[] cases) {
        check(!BuiltInRegistries.BLOCK.containsKey(AtomicLegacy.id("machine_boiler_electric_off")), problems, cases,
                "machine_boiler_electric_off is not registered (MainRegistry.ignoreMappings)");
        check(!BuiltInRegistries.ITEM.containsKey(AtomicLegacy.id("machine_boiler_electric_off")), problems, cases,
                "machine_boiler_electric_off has no item");
        check(!BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(AtomicLegacy.id("small_boiler"))
                && !BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(AtomicLegacy.id("small_electric_boiler")), problems, cases,
                "the small boiler block entities are removed");
        BlockPos o = site(level);
        Block boiler = HbmBlocks.MACHINE_BOILER_OFF.get();
        level.setBlock(o, boiler.defaultBlockState(), 3);
        check(level.getBlockEntity(o) == null, problems, cases, "machine_boiler_off has no tile entity");
        check(!(boiler instanceof net.minecraft.world.level.block.EntityBlock), problems, cases, "machine_boiler_off is a plain block");
        check(boiler.defaultBlockState().getProperties().size() == 1, problems, cases, "machine_boiler_off has only its facing");
        net.minecraft.world.level.storage.loot.LootTable table = level.getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, AtomicLegacy.id("blocks/machine_boiler_off")));
        net.minecraft.world.level.storage.loot.LootParams params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, o.getCenter())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, ItemStack.EMPTY)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, boiler.defaultBlockState())
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
        for (int i = 0; i < 8; i++) {
            List<ItemStack> drops = table.getRandomItems(params);
            boolean ok = drops.size() == 1 && BuiltInRegistries.ITEM.getKey(drops.get(0).getItem()).getPath().equals("scraps_steel")
                    && drops.get(0).getCount() >= 3 && drops.get(0).getCount() <= 6;
            check(ok, problems, cases, "machine_boiler_off drops 3-6 steel scraps " + drops);
        }
        clear(level, o);
    }
}
