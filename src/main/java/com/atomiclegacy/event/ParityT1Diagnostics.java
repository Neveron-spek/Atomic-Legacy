package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.FluidDuctBlock;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidNetMK2Transfer;
import com.atomiclegacy.fluid.HbmFluidNetUser;
import com.atomiclegacy.fluid.HbmFluidNetworks;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.tileentity.FlowControlPumpBlockEntity;
import com.atomiclegacy.tileentity.FluidBarrelBlockEntity;
import com.atomiclegacy.tileentity.FluidPipeBlockEntity;
import com.atomiclegacy.tileentity.FluidTankBlockEntity;
import com.atomiclegacy.tileentity.PipeAnchorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity T1: diagnostics] FluidNetMK2 transfer (shares, priorities, pressures, scapegoat), valves and counter,
 * flow_control_pump, net split on pipe removal, exhaust anchor, removed port-only fluid blocks.
 * Block site at x 29000 (no entities).
 */
final class ParityT1Diagnostics {
    private static final int BASE_X = 29_000;
    private static final int BASE_Y = 120;
    private static final int BASE_Z = 0;
    private static int site;
    private static final List<int[]> FORCED = new ArrayList<>();

    private ParityT1Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        site = 0;
        try {
            algorithm(problems, cases);
            shares(level, problems, cases);
            priorities(level, problems, cases);
            pressure(level, problems, cases);
            valveAndCounter(level, problems, cases);
            flowControlPump(level, problems, cases);
            breakPipe(level, problems, cases);
            exhaustAnchor(level, problems, cases);
            removedBlocks(problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity T1 diagnostics crashed", e);
            problems.add("T1 diagnostics crashed: " + e);
        } finally {
            for (int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
            FORCED.clear();
        }
        return cases[0];
    }

    private static void check(boolean ok, List<String> problems, int[] cases, String what) {
        cases[0]++;
        if (!ok) problems.add("T1: " + what);
    }

    private static void eq(long actual, long expected, List<String> problems, int[] cases, String what) {
        check(actual == expected, problems, cases, what + " = " + actual + " (expected " + expected + ")");
    }

    /// pure FluidNetMK2#update ///

    private static final class Fake implements HbmFluidNetMK2Transfer.Endpoint {
        final long[] stored = new long[6];
        final long[] space = new long[6];
        final PowerEndpoint.ConnectionPriority priority;
        final boolean provider;
        final boolean receiver;
        final int pressure;

        Fake(boolean provider, boolean receiver, int pressure, long stored, long space, PowerEndpoint.ConnectionPriority priority) {
            this.provider = provider;
            this.receiver = receiver;
            this.pressure = pressure;
            this.stored[pressure] = stored;
            this.space[pressure] = space;
            this.priority = priority;
        }

        @Override public boolean isProvider() { return provider; }
        @Override public boolean isReceiver() { return receiver; }
        @Override public int[] providingPressureRange() { return new int[]{pressure, pressure}; }
        @Override public int[] receivingPressureRange() { return new int[]{pressure, pressure}; }
        @Override public long fluidAvailable(int p) { return stored[p]; }
        @Override public long providerSpeed(int p) { return HbmFluidNetUser.DEFAULT_SPEED; }
        @Override public void useUpFluid(int p, long amount) { stored[p] -= Math.min(amount, stored[p]); }
        @Override public long demand(int p) { return space[p]; }
        @Override public long receiverSpeed(int p) { return HbmFluidNetUser.DEFAULT_SPEED; }
        @Override public PowerEndpoint.ConnectionPriority priority() { return priority; }

        @Override
        public long transferFluid(int p, long amount) {
            long accepted = Math.min(amount, space[p]);
            space[p] -= accepted;
            stored[p] += 0;
            received += accepted;
            return amount - accepted;
        }

        long received;
    }

    private static void algorithm(List<String> problems, int[] cases) {
        PowerEndpoint.ConnectionPriority normal = PowerEndpoint.ConnectionPriority.NORMAL;
        // three receivers 1/1/1 sharing 100: (long) 33.33 each, the leftover 1 is never sent
        Fake p = new Fake(true, false, 0, 100, 0, normal);
        Fake a = new Fake(false, true, 0, 0, 1000, normal);
        Fake b = new Fake(false, true, 0, 0, 1000, normal);
        Fake c = new Fake(false, true, 0, 0, 1000, normal);
        long tracker = HbmFluidNetMK2Transfer.update(List.of(p), List.of(a, b, c));
        eq(a.received + b.received + c.received, 99, problems, cases, "MK2 truncated shares 3x33");
        eq(tracker, 99, problems, cases, "MK2 fluidTracker");
        eq(p.stored[0], 1, problems, cases, "MK2 provider keeps the rounding rest");
        // HIGHEST before NORMAL; cumulative received[p] quirk: second tier sees available - received
        Fake p2 = new Fake(true, false, 0, 1000, 0, normal);
        Fake hi = new Fake(false, true, 0, 0, 300, PowerEndpoint.ConnectionPriority.HIGHEST);
        Fake lo = new Fake(false, true, 0, 0, 5000, PowerEndpoint.ConnectionPriority.LOW);
        HbmFluidNetMK2Transfer.update(List.of(p2), List.of(hi, lo));
        eq(hi.received, 300, problems, cases, "MK2 HIGHEST tier served first");
        eq(lo.received, 700, problems, cases, "MK2 LOW tier gets the remainder");
        // pressures are separate pools
        Fake pp = new Fake(true, false, 1, 500, 0, normal);
        Fake r0 = new Fake(false, true, 0, 0, 500, normal);
        Fake r1 = new Fake(false, true, 1, 0, 200, normal);
        HbmFluidNetMK2Transfer.update(List.of(pp), List.of(r0, r1));
        eq(r0.received, 0, problems, cases, "MK2 pressure 1 never reaches a pressure 0 receiver");
        eq(r1.received, 200, problems, cases, "MK2 pressure 1 receiver");
        // providers debited by weight 600/900 and 300/900 of 450
        Fake q1 = new Fake(true, false, 0, 600, 0, normal);
        Fake q2 = new Fake(true, false, 0, 300, 0, normal);
        Fake sink = new Fake(false, true, 0, 0, 450, normal);
        HbmFluidNetMK2Transfer.update(List.of(q1, q2), List.of(sink));
        eq(q1.stored[0], 300, problems, cases, "MK2 provider share 600/900");
        eq(q2.stored[0], 150, problems, cases, "MK2 provider share 300/900");
        // no provider or no receiver: nothing happens
        eq(HbmFluidNetMK2Transfer.update(List.of(), List.of(sink)), 0, problems, cases, "MK2 empty provider set");
    }

    /// world sites ///

    private static BlockPos site(ServerLevel level) {
        int index = site++;
        BlockPos pos = new BlockPos(BASE_X, BASE_Y, BASE_Z + index * 16);
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        for (int x = cx - 1; x <= cx + 1; x++) {
            level.setChunkForced(x, cz, true);
            level.getChunk(x, cz);
            FORCED.add(new int[]{x, cz});
        }
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -2; dy <= 3; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    level.setBlock(pos.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2 | 16);
                }
            }
        }
        return pos;
    }

    private static HbmFluidDefinition water() {
        return HbmFluids.byName("water").orElseThrow();
    }

    private static FluidPipeBlockEntity pipe(ServerLevel level, BlockPos pos, Block block, HbmFluidDefinition type) {
        level.setBlock(pos, block.defaultBlockState(), 3);
        FluidPipeBlockEntity pipe = (FluidPipeBlockEntity) level.getBlockEntity(pos);
        pipe.setType(type);
        return pipe;
    }

    /** steel barrel with mode index (0 receive, 1 buffer, 2 send) and contents */
    private static FluidBarrelBlockEntity barrel(ServerLevel level, BlockPos pos, int mode, int amount, int pressure) {
        level.setBlock(pos, HbmBlocks.BARREL_STEEL.get().defaultBlockState(), 3);
        FluidBarrelBlockEntity barrel = (FluidBarrelBlockEntity) level.getBlockEntity(pos);
        for (int i = 0; i < mode; i++) barrel.cycleMode();
        barrel.tank().setType(water());
        barrel.tank().setPressure(pressure);
        barrel.tank().setAmount(amount);
        HbmFluidNetworks.subscribe(level, barrel); // [parity U1: fluid ports] stands in for the first barrel tick
        return barrel;
    }

    private static void update(ServerLevel level, int times) {
        for (int i = 0; i < times; i++) {
            HbmFluidNetworks.updateNow(level);
            for (BlockEntityTick tick : TICKS) tick.run();
        }
    }

    private interface BlockEntityTick {
        void run();
    }

    private static final List<BlockEntityTick> TICKS = new ArrayList<>();

    private static void shares(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        FluidBarrelBlockEntity source = barrel(level, o, 2, 900, 0);
        int cap = source.tank().capacity();
        for (int i = 1; i <= 3; i++) pipe(level, o.east(i), HbmBlocks.FLUID_DUCT_NEO.get(), water());
        FluidBarrelBlockEntity r1 = barrel(level, o.east(2).north(), 0, cap - 1000, 0);
        FluidBarrelBlockEntity r2 = barrel(level, o.east(4), 0, cap - 2000, 0);
        update(level, 1);
        // demand 1000 + 2000, available 900: (long) 900*1/3 = 300 and 600
        eq(r1.tank().amount() - (cap - 1000), 300, problems, cases, "net share 1000/3000 of 900");
        eq(r2.tank().amount() - (cap - 2000), 600, problems, cases, "net share 2000/3000 of 900");
        eq(source.tank().amount(), 0, problems, cases, "net provider emptied in one tick");
        eq(HbmFluidNetworks.lastTransfer(level, o.east(1), water()), 900, problems, cases, "net fluidTracker of the tick");
    }

    private static void priorities(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        FluidBarrelBlockEntity source = barrel(level, o, 2, 1000, 0);
        int cap = source.tank().capacity();
        for (int i = 1; i <= 3; i++) pipe(level, o.east(i), HbmBlocks.FLUID_DUCT_NEO.get(), water());
        FluidBarrelBlockEntity buffer = barrel(level, o.east(2).north(), 1, 0, 0); // mode 1: LOW priority
        FluidBarrelBlockEntity normal = barrel(level, o.east(4), 0, cap - 600, 0);
        check(buffer.fluidPriority() == PowerEndpoint.ConnectionPriority.LOW, problems, cases, "barrel mode 1 LOW priority");
        update(level, 1);
        eq(normal.tank().amount(), cap, problems, cases, "NORMAL receiver filled before LOW");
        eq(buffer.tank().amount(), 400, problems, cases, "LOW buffer barrel gets the remainder");
    }

    private static void pressure(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        FluidBarrelBlockEntity source = barrel(level, o, 2, 1000, 2);
        for (int i = 1; i <= 3; i++) pipe(level, o.east(i), HbmBlocks.FLUID_DUCT_NEO.get(), water());
        FluidBarrelBlockEntity atZero = barrel(level, o.east(2).north(), 0, 0, 0);
        FluidBarrelBlockEntity atTwo = barrel(level, o.east(4), 0, 0, 2);
        update(level, 1);
        eq(atZero.tank().amount(), 0, problems, cases, "2 PU fluid not delivered to a 0 PU tank");
        eq(atTwo.tank().amount(), 1000, problems, cases, "2 PU fluid delivered to a 2 PU tank");
        eq(source.tank().amount(), 0, problems, cases, "2 PU provider emptied");
    }

    private static void valveAndCounter(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        FluidBarrelBlockEntity source = barrel(level, o, 2, 500, 0);
        pipe(level, o.east(1), HbmBlocks.FLUID_DUCT_NEO.get(), water());
        pipe(level, o.east(2), HbmBlocks.FLUID_VALVE.get(), water());
        pipe(level, o.east(3), HbmBlocks.FLUID_COUNTER_VALVE.get(), water());
        FluidBarrelBlockEntity sink = barrel(level, o.east(4), 0, 0, 0);
        FluidDuctBlock valve = (FluidDuctBlock) HbmBlocks.FLUID_VALVE.get();
        BlockState valveState = level.getBlockState(o.east(2));
        boolean initiallyOpen = valveState.getValue(FluidDuctBlock.OPEN);
        ((FluidDuctBlock) HbmBlocks.FLUID_COUNTER_VALVE.get()).setOpen(level, o.east(3), true, false);
        valve.setOpen(level, o.east(2), false, false);
        update(level, 1);
        eq(sink.tank().amount(), 0, problems, cases, "closed fluid_valve splits the net");
        valve.setOpen(level, o.east(2), true, false);
        FluidPipeBlockEntity counter = (FluidPipeBlockEntity) level.getBlockEntity(o.east(3));
        update(level, 1);
        FluidPipeBlockEntity.serverTick(level, o.east(3), level.getBlockState(o.east(3)), counter);
        eq(sink.tank().amount(), 500, problems, cases, "open fluid_valve joins the net");
        eq(counter.counter(), 500, problems, cases, "fluid_counter_valve counts fluidTracker");
        check(!initiallyOpen, problems, cases, "fluid_valve placed closed (metadata 0)");
    }

    private static void flowControlPump(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        // pump core at o facing NORTH: in port at o.east() (rot UP = clockwise), out port at o.west()
        level.setBlock(o, HbmBlocks.FLOW_CONTROL_PUMP.get().defaultBlockState()
                .setValue(com.atomiclegacy.blocks.FlowControlPumpBlock.FACING, Direction.NORTH), 3);
        HbmBlocks.FLOW_CONTROL_PUMP.get().setPlacedBy(level, o, level.getBlockState(o), null, net.minecraft.world.item.ItemStack.EMPTY);
        FlowControlPumpBlockEntity pump = (FlowControlPumpBlockEntity) level.getBlockEntity(o);
        check(pump.inDirection() == Direction.EAST && pump.outDirection() == Direction.WEST, problems, cases,
                "flow_control_pump ports rot(UP)/rot(DOWN)");
        pump.setType(water());
        pipe(level, o.east(2), HbmBlocks.FLUID_DUCT_NEO.get(), water());
        FluidBarrelBlockEntity source = barrel(level, o.east(3), 2, 5000, 0);
        pipe(level, o.west(2), HbmBlocks.FLUID_DUCT_NEO.get(), water());
        FluidBarrelBlockEntity sink = barrel(level, o.west(3), 0, 0, 0);
        TICKS.add(() -> FlowControlPumpBlockEntity.serverTick(level, o, level.getBlockState(o), pump));
        try {
            update(level, 3);
            check(sink.tank().amount() > 0 && sink.tank().amount() <= 300, problems, cases,
                    "flow_control_pump moves at most its 100 mB buffer per tick (" + sink.tank().amount() + ")");
            eq(source.tank().amount() + sink.tank().amount() + pump.tank.amount(), 5000, problems, cases, "flow_control_pump conserves fluid");
            check(pump.tank.amount() <= 100, problems, cases, "flow_control_pump buffer 100");
            int before = sink.tank().amount();
            pump.redstone = true;
            check(pump.fluidReceiverSpeed(water(), 0) == 0 && pump.fluidProviderSpeed(water(), 0) == 0, problems, cases,
                    "flow_control_pump disabled by redstone");
            HbmFluidNetworks.updateNow(level);
            eq(sink.tank().amount(), before, problems, cases, "redstone stops the pump");
            pump.redstone = false;
            net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
            data.putInt("capacity", 20_000);
            data.putByte("pressure", (byte) 7);
            data.putByte("priority", (byte) 4);
            pump.receiveControl(fakeNear(level, o), data);
            eq(pump.bufferSize, 10_000, problems, cases, "GUIPump capacity clamp 0..10,000");
            eq(pump.tank.pressure(), 5, problems, cases, "GUIPump pressure clamp 0..5");
            check(pump.priority == PowerEndpoint.ConnectionPriority.HIGHEST, problems, cases, "GUIPump priority");
            check(pump.bufferMismatch() && pump.fluidReceiverSpeed(water(), 5) == 0, problems, cases,
                    "buffer mismatch blocks receiving until the tank is resized");
        } finally {
            TICKS.clear();
        }
    }

    private static net.minecraft.world.entity.player.Player fakeNear(ServerLevel level, BlockPos pos) {
        net.minecraft.world.entity.player.Player player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        return player;
    }

    private static void breakPipe(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        FluidBarrelBlockEntity source = barrel(level, o, 2, 3000, 0);
        for (int i = 1; i <= 3; i++) pipe(level, o.east(i), HbmBlocks.FLUID_DUCT_NEO.get(), water());
        FluidBarrelBlockEntity sink = barrel(level, o.east(4), 0, 0, 0);
        level.removeBlock(o.east(2), false);
        update(level, 1);
        eq(sink.tank().amount(), 0, problems, cases, "removed pipe splits the net");
        pipe(level, o.east(2), HbmBlocks.FLUID_DUCT_NEO.get(), water());
        update(level, 1);
        eq(sink.tank().amount(), 3000, problems, cases, "replaced pipe rejoins the net");
        eq(source.tank().amount(), 0, problems, cases, "provider drained after rejoin");
        pipe(level, o.east(2), HbmBlocks.FLUID_DUCT_NEO.get(), HbmFluids.byName("oil").orElseThrow());
        source.tank().setAmount(100);
        update(level, 1);
        eq(sink.tank().amount(), 3000, problems, cases, "pipe of another fluid type separates the nets");
    }

    private static void exhaustAnchor(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = site(level);
        level.setBlock(o, HbmBlocks.PIPE_ANCHOR_EXHAUST.get().defaultBlockState(), 3);
        check(level.getBlockEntity(o) instanceof PipeAnchorBlockEntity anchor && anchor.isExhaustPipe()
                        && anchor.canConnect(HbmFluids.byName("smoke").orElseThrow())
                        && anchor.canConnect(HbmFluids.byName("smoke_leaded").orElseThrow())
                        && anchor.canConnect(HbmFluids.byName("smoke_poison").orElseThrow())
                        && !anchor.canConnect(water()),
                problems, cases, "pipe_anchor_exhaust carries SMOKE, SMOKE_LEADED, SMOKE_POISON only");
        level.setBlock(o.east(), HbmBlocks.PIPE_ANCHOR.get().defaultBlockState(), 3);
        check(level.getBlockEntity(o.east()) instanceof PipeAnchorBlockEntity anchor && !anchor.isExhaustPipe(),
                problems, cases, "pipe_anchor is a fluid anchor");
    }

    private static void removedBlocks(List<String> problems, int[] cases) {
        for (String id : new String[]{"mustard_gas", "phosgene_gas", "fluid_oil", "fluid_blood", "fluid_mercury"}) {
            check(!BuiltInRegistries.BLOCK.containsKey(AtomicLegacy.id(id)), problems, cases, "port-only block removed: " + id);
        }
        for (String id : new String[]{"oil_bucket", "blood_bucket", "watz_heavy_bucket", "hydrazine_bucket"}) {
            check(!BuiltInRegistries.ITEM.containsKey(AtomicLegacy.id(id)), problems, cases, "port-only bucket removed: " + id);
        }
        for (String id : new String[]{"bucket_mud", "bucket_acid", "bucket_toxic", "bucket_schrabidic_acid", "bucket_sulfuric_acid"}) {
            check(BuiltInRegistries.ITEM.containsKey(AtomicLegacy.id(id)), problems, cases, "original bucket kept: " + id);
        }
        for (String id : new String[]{"mud_block", "acid_block", "toxic_block", "schrabidic_block", "corium_block",
                "volcanic_lava_block", "rad_lava_block", "sulfuric_acid_block", "flow_control_pump", "pipe_anchor_exhaust"}) {
            ResourceLocation key = AtomicLegacy.id(id);
            check(BuiltInRegistries.BLOCK.containsKey(key), problems, cases, "block present: " + id);
        }
    }
}
