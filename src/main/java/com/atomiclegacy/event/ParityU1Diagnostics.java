package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.LargeMachineBlock;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidNetUser;
import com.atomiclegacy.fluid.HbmFluidNetworks;
import com.atomiclegacy.fluid.HbmFluidTank;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.rtty.RttyInfo;
import com.atomiclegacy.tileentity.FluidBarrelBlockEntity;
import com.atomiclegacy.tileentity.FluidPipeBlockEntity;
import com.atomiclegacy.tileentity.FluidPumpBlockEntity;
import com.atomiclegacy.tileentity.FluidTankBlockEntity;
import com.atomiclegacy.tileentity.NetworkSeparatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity U1: diagnostics] FluidNetMK2 port nodes of the converted machines (port count of getPorts(), tanks,
 * pressure, priority and speeds), duct-less mini nets and pass-through machines, fluid_pump as one net, the
 * network separator keeping two power nets apart, barrel ROR values and the removed port-only content.
 */
final class ParityU1Diagnostics {
    private static final int BASE_X = 32_000;
    private static final int BASE_Y = 120;
    private static final int BASE_Z = 0;
    private static int site;
    private static final List<int[]> FORCED = new ArrayList<>();

    private ParityU1Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        site = 0;
        try {
            machinePorts(level, problems, cases);
            miniNet(level, problems, cases);
            passThrough(level, problems, cases);
            convertedMachine(level, problems, cases);
            fluidPump(level, problems, cases);
            separator(level, problems, cases);
            barrelRor(level, problems, cases);
            removedContent(problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity U1 diagnostics crashed", e);
            problems.add("U1 diagnostics crashed: " + e);
        } finally {
            for (int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
            FORCED.clear();
        }
        return cases[0];
    }

    private static void check(boolean ok, List<String> problems, int[] cases, String what) {
        cases[0]++;
        if (!ok) problems.add("U1: " + what);
    }

    private static void eq(long actual, long expected, List<String> problems, int[] cases, String what) {
        check(actual == expected, problems, cases, what + " expected " + expected + " got " + actual);
    }

    private static BlockPos site(ServerLevel level) {
        int index = site++;
        BlockPos pos = new BlockPos(BASE_X, BASE_Y, BASE_Z + index * 24);
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                level.setChunkForced(x, z, true);
                level.getChunk(x, z);
                FORCED.add(new int[]{x, z});
            }
        }
        for (int dx = -10; dx <= 10; dx++) {
            for (int dy = -3; dy <= 13; dy++) {
                for (int dz = -10; dz <= 10; dz++) {
                    level.setBlock(pos.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2 | 16);
                }
            }
        }
        return pos;
    }

    private static HbmFluidDefinition water() {
        return HbmFluids.byName("water").orElseThrow();
    }

    /// machine ports ///

    /** block id, connections of the original getPorts() (TilePortShapes), sending and receiving tank counts */
    private record MachineCase(String id, int ports, int sending, int receiving) {
    }

    private static final MachineCase[] MACHINES = {
            new MachineCase("machine_large_turbine", 3, 1, 1),
            new MachineCase("machine_industrial_turbine", 6, 1, 1),
            new MachineCase("machine_chungus", 3, 1, 1),
            new MachineCase("machine_condenser", 6, 1, 1),
            new MachineCase("machine_condenser_powered", 6, 1, 1),
            new MachineCase("machine_tower_small", 4, 1, 1),
            new MachineCase("machine_tower_large", 12, 1, 1),
            new MachineCase("machine_steam_engine", 3, 1, 1),
            new MachineCase("machine_solar_boiler", 2, 1, 1),
            new MachineCase("machine_boiler", 3, 1, 1),
            new MachineCase("machine_deuterium_extractor", 6, 1, 1),
            new MachineCase("machine_liquefactor", 6, 1, 0),
            new MachineCase("machine_intake", 8, 1, 0),
            new MachineCase("machine_catalytic_cracker", 8, 3, 2),
            new MachineCase("machine_catalytic_reformer", 6, 3, 1),
            new MachineCase("machine_coker", 8, 1, 1),
            new MachineCase("machine_fraction_tower", 4, 2, 1),
            new MachineCase("machine_hydrotreater", 8, 2, 2),
            new MachineCase("machine_refinery", 8, 4, 1),
            new MachineCase("machine_vacuum_distill", 8, 4, 1),
            new MachineCase("machine_mixer", 6, 1, 2),
            new MachineCase("machine_mining_laser", 4, 1, 0),
            new MachineCase("machine_well", 5, 2, 0),
            new MachineCase("machine_pumpjack", 4, 2, 0),
            new MachineCase("machine_fracking_tower", 5, 2, 1),
            new MachineCase("machine_ore_slopper", 8, 1, 1),
            new MachineCase("machine_storage_drum", 6, 2, 0),
            new MachineCase("watz", 10, 2, 1),
            new MachineCase("machine_hephaestus", 8, 1, 1),
            new MachineCase("machine_blast_furnace", 7, 1, 1),
            new MachineCase("machine_turbinegas", 5, 4, 3), // [parity V1: smoke] steam + smoke, smoke_leaded, smoke_poison
            new MachineCase("furnace_combination", 24, 4, 0),
            new MachineCase("refueler", 1, 0, 1),
            new MachineCase("machine_zirnox", 4, 1, 2),
            new MachineCase("icf", 6, 2, 1),
            new MachineCase("pump_electric", 4, 1, 0),
            new MachineCase("pump_steam", 4, 2, 1),
            new MachineCase("machine_combustion_engine", 4, 3, 1),
            new MachineCase("machine_diesel", 6, 3, 1),
            new MachineCase("heater_oilburner", 4, 3, 1),
            new MachineCase("heater_heatex", 4, 1, 1),
            new MachineCase("heater_firebox", 12, 3, 0),
            new MachineCase("heater_oven", 12, 3, 0),
            new MachineCase("chimney_brick", 4, 0, 3),
            new MachineCase("chimney_industrial", 4, 0, 3),
            new MachineCase("machine_fluidtank", 4, 1, 1),
            new MachineCase("barrel_steel", 6, 1, 1),
            new MachineCase("machine_bat9000", 8, 1, 1),
            new MachineCase("machine_bigasstank", 2, 1, 1),
            new MachineCase("machine_compressor", 0, 1, 1),
            new MachineCase("fluid_pump", 2, 1, 1),
            new MachineCase("flow_control_pump", 2, 1, 1),
    };

    private static BlockEntity placeCore(ServerLevel level, BlockPos pos, String id) {
        Block block = BuiltInRegistries.BLOCK.get(AtomicLegacy.id(id));
        if (block == Blocks.AIR) return null;
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(LargeMachineBlock.FACING)) state = state.setValue(LargeMachineBlock.FACING, Direction.NORTH);
        else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        level.setBlock(pos, state, 2 | 16);
        return level.getBlockEntity(pos);
    }

    private static void machinePorts(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        for (MachineCase machine : MACHINES) {
            BlockEntity entity = placeCore(level, o, machine.id());
            check(entity instanceof HbmFluidNetUser, problems, cases, machine.id() + " is a FluidNetMK2 user");
            if (!(entity instanceof HbmFluidNetUser user)) continue;
            HbmFluidDefinition type = firstType(user);
            List<HbmFluidNetUser.FluidPort> ports = user.fluidPorts(type);
            if (machine.ports() > 0) {
                eq(ports == null ? -1 : ports.size(), machine.ports(), problems, cases, machine.id() + " port connections");
                if (ports != null) {
                    for (HbmFluidNetUser.FluidPort port : ports) {
                        check(port.pos().distManhattan(o) <= 14, problems, cases, machine.id() + " port near the core " + port);
                    }
                }
            }
            eq(user.fluidSendingTanks().length, machine.sending(), problems, cases, machine.id() + " sending tanks");
            eq(user.fluidReceivingTanks().length, machine.receiving(), problems, cases, machine.id() + " receiving tanks");
            level.setBlock(o, Blocks.AIR.defaultBlockState(), 2 | 16);
        }

        // pressures and speeds of the originals
        BlockEntity vacuum = placeCore(level, o, "machine_vacuum_distill");
        if (vacuum instanceof HbmFluidNetUser user) {
            eq(user.fluidReceivingTanks()[0].pressure(), 2, problems, cases, "vacuum distill oil tank withPressure(2)");
        }
        level.setBlock(o, Blocks.AIR.defaultBlockState(), 2 | 16);
        BlockEntity hydro = placeCore(level, o, "machine_hydrotreater");
        if (hydro instanceof HbmFluidNetUser user) {
            eq(user.fluidReceivingTanks()[1].pressure(), 1, problems, cases, "hydrotreater hydrogen tank withPressure(1)");
        }
        level.setBlock(o, Blocks.AIR.defaultBlockState(), 2 | 16);
        BlockEntity zirnox = placeCore(level, o, "machine_zirnox");
        if (zirnox instanceof HbmFluidNetUser user) {
            eq(user.fluidReceiverSpeed(water(), 0), 1_000_000, problems, cases, "zirnox receiver speed");
            eq(user.fluidProviderSpeed(water(), 0), 1_000_000, problems, cases, "zirnox provider speed");
        }
        level.setBlock(o, Blocks.AIR.defaultBlockState(), 2 | 16);
        BlockEntity blast = placeCore(level, o, "machine_blast_furnace");
        if (blast instanceof HbmFluidNetUser user) {
            eq(user.fluidProviderSpeed(water(), 0), 8, problems, cases, "blast furnace provider speed max(fill * 50 / max, 8)");
        }
        level.setBlock(o, Blocks.AIR.defaultBlockState(), 2 | 16);
        BlockEntity chimney = placeCore(level, o, "chimney_brick");
        if (chimney instanceof HbmFluidNetUser user) {
            eq(user.fluidDemand(HbmFluids.byName("smoke").orElseThrow(), 0), 1_000_000, problems, cases, "chimney getDemand");
        }
        level.setBlock(o, Blocks.AIR.defaultBlockState(), 2 | 16);
    }

    private static HbmFluidDefinition firstType(HbmFluidNetUser user) {
        for (HbmFluidTank tank : user.fluidSendingTanks()) if (!tank.type().isNone()) return tank.type();
        for (HbmFluidTank tank : user.fluidReceivingTanks()) if (!tank.type().isNone()) return tank.type();
        return water();
    }

    /// nets ///

    private static FluidBarrelBlockEntity barrel(ServerLevel level, BlockPos pos, int mode, int amount) {
        level.setBlock(pos, HbmBlocks.BARREL_STEEL.get().defaultBlockState(), 3);
        FluidBarrelBlockEntity barrel = (FluidBarrelBlockEntity) level.getBlockEntity(pos);
        for (int i = 0; i < mode; i++) barrel.cycleMode();
        barrel.tank().setType(water());
        barrel.tank().setAmount(amount);
        HbmFluidNetworks.subscribe(level, barrel);
        return barrel;
    }

    private static FluidPipeBlockEntity pipe(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, HbmBlocks.FLUID_DUCT_NEO.get().defaultBlockState(), 3);
        FluidPipeBlockEntity pipe = (FluidPipeBlockEntity) level.getBlockEntity(pos);
        pipe.setType(water());
        return pipe;
    }

    private static void update(ServerLevel level, int times) {
        for (int i = 0; i < times; i++) HbmFluidNetworks.updateNow(level);
    }

    /** two barrels touching each other: their port nodes form one net without any duct */
    private static void miniNet(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        FluidBarrelBlockEntity source = barrel(level, o, 2, 1000);
        FluidBarrelBlockEntity sink = barrel(level, o.east(), 0, 0);
        update(level, 2);
        check(sink.tank().amount() > 0, problems, cases, "adjacent barrels share a port net (" + sink.tank().amount() + ")");
        eq(source.tank().amount() + sink.tank().amount(), 1000, problems, cases, "mini net conserves fluid");
    }

    /** a disabled barrel between two ducts joins them into one net: the sink is fed straight from the source */
    private static void passThrough(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        FluidBarrelBlockEntity source = barrel(level, o.west(2), 2, 1000);
        pipe(level, o.west());
        FluidBarrelBlockEntity middle = barrel(level, o, 3, 0);
        pipe(level, o.east());
        FluidBarrelBlockEntity sink = barrel(level, o.east(2), 0, 0);
        update(level, 2);
        eq(middle.tank().amount(), 0, problems, cases, "disabled barrel buffers nothing");
        check(sink.tank().amount() > 0, problems, cases, "machine node merges both duct groups (" + sink.tank().amount() + ")");
        eq(source.tank().amount() + sink.tank().amount(), 1000, problems, cases, "pass-through net conserves fluid");
    }

    /** a converted machine is fed by the net instead of pulling: deuterium extractor next to a water barrel */
    private static void convertedMachine(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        BlockEntity entity = placeCore(level, o, "machine_deuterium_extractor");
        if (!(entity instanceof HbmFluidNetUser extractor)) {
            check(false, problems, cases, "deuterium extractor placed");
            return;
        }
        HbmFluidNetworks.subscribe(level, extractor);
        FluidBarrelBlockEntity source = barrel(level, o.east(), 2, 1000);
        update(level, 2);
        long received = extractor.fluidReceivingTanks()[0].amount();
        check(received > 0, problems, cases, "deuterium extractor receives water through its port node (" + received + ")");
        eq(source.tank().amount() + received, 1000, problems, cases, "extractor transfer conserves fluid");
        level.setBlock(o, Blocks.AIR.defaultBlockState(), 3);
    }

    /** fluid_pump: PortDef.make(x, y, z, dir, dir.getOpposite()) - both sides are one net */
    private static void fluidPump(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        BlockEntity entity = placeCore(level, o, "fluid_pump");
        if (!(entity instanceof FluidPumpBlockEntity pump)) {
            check(false, problems, cases, "fluid_pump placed");
            return;
        }
        Direction axis = pump.portAxis();
        pump.tank().setType(water());
        HbmFluidNetworks.subscribe(level, pump);
        pipe(level, o.relative(axis));
        pipe(level, o.relative(axis.getOpposite()));
        barrel(level, o.relative(axis, 2), 2, 1000);
        FluidBarrelBlockEntity sink = barrel(level, o.relative(axis.getOpposite(), 2), 0, 0);
        update(level, 1);
        List<String> left = HbmFluidNetworks.debugInfo(level, o.relative(axis));
        List<String> right = HbmFluidNetworks.debugInfo(level, o.relative(axis.getOpposite()));
        check(!left.isEmpty() && left.equals(right) && left.get(0).equals("Links: 5"), problems, cases,
                "fluid_pump sides are one net " + left + " / " + right);
        check(sink.tank().amount() > 100, problems, cases, "fluid_pump does not limit a shared net to its buffer (" + sink.tank().amount() + ")");
    }

    /** network separator: in and out ports are separate nodes; power moves only through the buffer */
    private static void separator(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        NetworkSeparatorBlockEntity first = placeSeparator(level, o);
        BlockPos secondCore = o.west(4);
        NetworkSeparatorBlockEntity second = placeSeparator(level, secondCore);
        if (first == null || second == null) {
            check(false, problems, cases, "network_separator placed");
            return;
        }
        check(first.outDirection() == Direction.WEST && first.inDirection() == Direction.EAST, problems, cases,
                "separator ports rot(DOWN) out / rot(UP) in");
        level.setBlock(o.west(2), HbmBlocks.RED_CABLE.get().defaultBlockState(), 3);
        level.setBlock(o.east(2), HbmBlocks.RED_CABLE.get().defaultBlockState(), 3);
        CompoundTag tag = new CompoundTag();
        tag.putLong("power", 500);
        tag.putLong("configuredLimit", 1000);
        tag.putLong("actualLimit", 1000);
        tag.putByte("p", (byte) PowerEndpoint.ConnectionPriority.NORMAL.ordinal());
        first.loadCustomOnly(tag, level.registryAccess());
        for (int i = 0; i < 4; i++) {
            NetworkSeparatorBlockEntity.serverTick(level, o, level.getBlockState(o), first);
            NetworkSeparatorBlockEntity.serverTick(level, secondCore, level.getBlockState(secondCore), second);
            PowerNetworkManager.updateNow(level);
        }
        List<PowerEndpoint> outNet = PowerNetworkManager.componentEndpoints(level, first.outPort().getPowerPos());
        List<PowerEndpoint> inNet = PowerNetworkManager.componentEndpoints(level, first.inPort().getPowerPos());
        check(outNet.contains(first.outPort()) && outNet.contains(second.inPort()), problems, cases,
                "out port and the next separator's in port share the cable net");
        check(!outNet.contains(first.inPort()) && !inNet.contains(first.outPort()), problems, cases,
                "the separator does not merge its two nets");
        check(second.getPower() > 0 && first.getPower() < 500, problems, cases,
                "power crosses into the next net (" + first.getPower() + " -> " + second.getPower() + ")");
        eq(first.getPower() + second.getPower(), 500, problems, cases, "separator chain conserves power");
        // ROR functions
        first.runRttyFunction(RttyInfo.PREFIX_FUNCTION + "setpriority", new String[]{"2"});
        check(first.priority == PowerEndpoint.ConnectionPriority.HIGH, problems, cases, "setpriority 2 -> HIGH");
        first.runRttyFunction(RttyInfo.PREFIX_FUNCTION + "setlimit", new String[]{"5000"});
        eq(first.configuredLimit, 5000, problems, cases, "setlimit");
        check(first.provideRttyValue(RttyInfo.PREFIX_VALUE + "deltatick") != null, problems, cases, "VAL:deltatick");
    }

    private static NetworkSeparatorBlockEntity placeSeparator(ServerLevel level, BlockPos pos) {
        BlockState state = HbmBlocks.NETWORK_SEPARATOR.get().defaultBlockState().setValue(LargeMachineBlock.FACING, Direction.NORTH);
        level.setBlock(pos, state, 3);
        HbmBlocks.NETWORK_SEPARATOR.get().setPlacedBy(level, pos, level.getBlockState(pos), null, net.minecraft.world.item.ItemStack.EMPTY);
        return level.getBlockEntity(pos) instanceof NetworkSeparatorBlockEntity separator ? separator : null;
    }

    /** TileEntityBarrel IRORValueProvider / IRORInteractive */
    private static void barrelRor(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        FluidTankBlockEntity barrel = barrel(level, o, 0, 500);
        check("WATER".equals(barrel.provideRttyValue(RttyInfo.PREFIX_VALUE + "type")), problems, cases, "barrel VAL:type");
        check("500".equals(barrel.provideRttyValue(RttyInfo.PREFIX_VALUE + "fill")), problems, cases, "barrel VAL:fill");
        String percent = barrel.provideRttyValue(RttyInfo.PREFIX_VALUE + "fillpercent");
        check(("" + (500 * 100 / barrel.tank().capacity())).equals(percent), problems, cases, "barrel VAL:fillpercent " + percent);
        barrel.runRttyFunction(RttyInfo.PREFIX_FUNCTION + "setmode", new String[]{"2"});
        check(barrel.mode() == FluidTankBlockEntity.Mode.SEND, problems, cases, "setmode 2");
        barrel.runRttyFunction(RttyInfo.PREFIX_FUNCTION + "setmode", new String[]{"2", "1"});
        check(barrel.mode() == FluidTankBlockEntity.Mode.BUFFER, problems, cases, "setmode same mode uses the fallback");
        eq(barrel.rttyFunctionInfo().length, 5, problems, cases, "barrel getFunctionInfo entries");
    }

    /** port-only ducts and the c:buckets/* buckets are gone; concrete_liquid is never registered in 1.7.10 */
    private static void removedContent(List<String> problems, int[] cases) {
        for (String id : new String[]{"fluid_duct_mk2", "fluid_duct_solid", "fluid_duct_solid_sealed", "fluid_diesel",
                "fluid_gasoline", "fluid_biofuel", "concrete_liquid"}) {
            check(!BuiltInRegistries.BLOCK.containsKey(AtomicLegacy.id(id)), problems, cases, "no block " + id);
        }
        for (String id : new String[]{"diesel_bucket", "diesel_crack_bucket", "diesel_reform_bucket", "diesel_crack_reform_bucket",
                "gasoline_bucket", "gasoline_leaded_bucket", "biofuel_bucket", "fluid_duct_mk2", "fluid_duct_solid_sealed"}) {
            check(!BuiltInRegistries.ITEM.containsKey(AtomicLegacy.id(id)), problems, cases, "no item " + id);
        }
        check(BuiltInRegistries.BLOCK.containsKey(AtomicLegacy.id("network_separator")), problems, cases, "network_separator registered");
    }
}
