package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.BatteryPackItem;
import com.atomiclegacy.items.LegacyBatteryTooltips;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.power.PowerNetMK2Transfer;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.tileentity.CableDiodeBlockEntity;
import com.atomiclegacy.tileentity.DieselGeneratorBlockEntity;
import com.atomiclegacy.tileentity.EnergyConverterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity L1] Energy system checks against 1.7.10 commit 936c60c: PowerNetMK2.update arithmetic (priorities with
 * the cumulative toTransfer quirk, truncation, scapegoat leftover), passive capacitor/diode blocks, battery item
 * capacities and rates, diesel generator output, HE&lt;-&gt;RF converter rates, block properties and recipes.
 * World pads are placed around x = 11000. Not covered: GUI drawing, sounds, pylon wiring, multiblock fluid ports.
 */
final class ParityL1Diagnostics {
    private static final int BASE_X = 11_000;
    private static final int BASE_Y = 200;
    private static final int BASE_Z = 11_000;
    private static final int SPACING = 32;
    private static int site;
    private static final List<int[]> FORCED = new ArrayList<>();

    private ParityL1Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        site = 0;
        try {
            properties(problems, cases);
            network(problems, cases);
            batteries(problems, cases);
            diesel(level, problems, cases);
            converters(level, problems, cases);
            recipes(server, problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity L1 diagnostics crashed", e);
            problems.add("L1 diagnostics crashed: " + e);
        } finally {
            for (int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
            FORCED.clear();
        }
        return cases[0];
    }

    /// ModBlocks.java 1832-1882, 2035-2036 (hardness 5, resistance 10); capacitor and diode have no TileEntity behaviour ///
    private static void properties(List<String> problems, int[] cases) {
        Block[] blocks = {
                HbmBlocks.MACHINE_CONVERTER_HE_RF.get(), HbmBlocks.MACHINE_CONVERTER_RF_HE.get(), HbmBlocks.MACHINE_DIESEL.get(),
                HbmBlocks.MACHINE_BATTERY_SOCKET.get(), HbmBlocks.MACHINE_BATTERY_REDD.get(), HbmBlocks.RED_CABLE.get(),
                HbmBlocks.CABLE_SWITCH.get(), HbmBlocks.CABLE_DIODE.get(), HbmBlocks.CAPACITOR_COPPER.get(), HbmBlocks.CAPACITOR_BUS.get()
        };
        for (Block block : blocks) {
            String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
            check(block.defaultDestroyTime() == 5.0F, problems, cases, id + " hardness " + block.defaultDestroyTime());
            check(ParityT3Diagnostics.resistanceMatches(block), problems, cases, id + " resistance " + block.getExplosionResistance()); // [parity T3: expected = original r*3/5]
        }
        check(!(HbmBlocks.CAPACITOR_COPPER.get() instanceof EntityBlock), problems, cases, "capacitor_copper must not have a block entity");
        check(!(HbmBlocks.CAPACITOR_BUS.get() instanceof EntityBlock), problems, cases, "capacitor_bus must not have a block entity");
        BlockEntity diode = ((EntityBlock) HbmBlocks.CABLE_DIODE.get()).newBlockEntity(BlockPos.ZERO, HbmBlocks.CABLE_DIODE.get().defaultBlockState());
        check(diode instanceof CableDiodeBlockEntity d && d.getPowerConnectorPositions(null).size() == 6
                && !d.requiresDirectedPowerRouting(null) && d.getPowerFlowLimit(null) == Long.MAX_VALUE,
                problems, cases, "cable_diode must be a plain 6-side conductor (TileEntityDiode extends TileEntityCableBaseNT)");
    }

    /// api.hbm.energymk2.PowerNetMK2#update ///
    private static void network(List<String> problems, int[] cases) {
        PowerEndpoint.ConnectionPriority L = PowerEndpoint.ConnectionPriority.LOW, N = PowerEndpoint.ConnectionPriority.NORMAL;
        // proportional split inside one tier
        {
            Fake src = new Fake(1000, 0, N), a = new Fake(0, 600, N), b = new Fake(0, 600, N);
            long moved = PowerNetMK2Transfer.transfer(List.of(src, a, b));
            checkEq(a.received, 500, problems, cases, "net split a");
            checkEq(b.received, 500, problems, cases, "net split b");
            checkEq(src.used, 1000, problems, cases, "net split provider debit");
            checkEq(moved, 1000, problems, cases, "net split moved");
        }
        // HIGH first, then NORMAL; LOW gets nothing because toTransfer -= cumulative energyUsed
        {
            Fake src = new Fake(1000, 0, N), h = new Fake(0, 300, PowerEndpoint.ConnectionPriority.HIGH),
                    n = new Fake(0, 1000, N), l = new Fake(0, 1000, L);
            PowerNetMK2Transfer.transfer(List.of(src, h, n, l));
            checkEq(h.received, 300, problems, cases, "net priority HIGH");
            checkEq(n.received, 700, problems, cases, "net priority NORMAL");
            checkEq(l.received, 0, problems, cases, "net priority LOW");
        }
        // empty tiers also subtract the cumulative energyUsed: 1000 - 100 - 100 - 100 = 700 for LOWEST
        {
            Fake src = new Fake(1000, 0, N), h = new Fake(0, 100, PowerEndpoint.ConnectionPriority.HIGH),
                    lo = new Fake(0, 1000, PowerEndpoint.ConnectionPriority.LOWEST);
            PowerNetMK2Transfer.transfer(List.of(src, h, lo));
            checkEq(h.received, 100, problems, cases, "net quirk HIGH");
            checkEq(lo.received, 700, problems, cases, "net quirk LOWEST (cumulative subtraction)");
            checkEq(src.used, 800, problems, cases, "net quirk provider debit");
        }
        // truncation: 3 x (long) (10 / 3) = 9 delivered and debited
        {
            Fake src = new Fake(10, 0, N), a = new Fake(0, 10, N), b = new Fake(0, 10, N), c = new Fake(0, 10, N);
            PowerNetMK2Transfer.transfer(List.of(src, a, b, c));
            checkEq(a.received + b.received + c.received, 9, problems, cases, "net truncation delivered");
            checkEq(src.used, 9, problems, cases, "net truncation debit");
        }
        // provider weights 3/10 and 7/10 of 5 -> 1 + 3, leftover 1 from a random provider (scapegoat)
        {
            Fake p1 = new Fake(3, 0, N), p2 = new Fake(7, 0, N), r = new Fake(0, 5, N);
            PowerNetMK2Transfer.transfer(List.of(p1, p2, r));
            checkEq(r.received, 5, problems, cases, "net scapegoat receiver");
            checkEq(p1.used + p2.used, 5, problems, cases, "net scapegoat total debit");
            check(p1.used >= 1 && p2.used >= 3, problems, cases, "net scapegoat weights " + p1.used + "/" + p2.used);
        }
        // a buffer battery is provider and receiver in the same net
        {
            Fake buffer = new Fake(100, 100, L), sink = new Fake(0, 50, N);
            PowerNetMK2Transfer.transfer(List.of(buffer, sink));
            checkEq(sink.received, 50, problems, cases, "net buffer sink");
            // toTransfer 100: NORMAL sink 50 -> 50 left for the LOW buffer; providers debited 100
            checkEq(buffer.received, 50, problems, cases, "net buffer self-feed");
            checkEq(buffer.used, 100, problems, cases, "net buffer debit");
        }
    }

    /// ItemBatteryPack.EnumBatteryPack, ItemBatterySC.EnumBatterySC, ModItems 3703-3708 / 4392 ///
    private static void batteries(List<String> problems, int[] cases) {
        Object[][] packs = {
                {"battery_redstone", 100L, false}, {"battery_lead", 1_000L, false}, {"battery_lithium", 10_000L, false},
                {"battery_sodium", 50_000L, false}, {"battery_schrabidium", 250_000L, false}
        };
        for (Object[] row : packs) {
            ItemStack stack = LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, (String) row[0]);
            long rate = (long) row[1];
            checkEq(BatteryPackItem.capacity(stack), rate * 20 * 60 * 15, problems, cases, row[0] + " capacity");
            checkEq(BatteryPackItem.chargeRate(stack), rate * 10, problems, cases, row[0] + " charge rate");
            checkEq(BatteryPackItem.dischargeRate(stack), rate, problems, cases, row[0] + " discharge rate");
        }
        ItemStack quantum = LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK, "battery_quantum");
        checkEq(BatteryPackItem.capacity(quantum), 1_000_000L * 20 * 60 * 60, problems, cases, "battery_quantum capacity");
        checkEq(BatteryPackItem.chargeRate(quantum), 10_000_000L, problems, cases, "battery_quantum charge rate");
        Object[][] caps = {
                {HbmItems.CAPACITOR_COPPER, 1_000L}, {HbmItems.CAPACITOR_GOLD, 10_000L}, {HbmItems.CAPACITOR_NIOBIUM, 100_000L},
                {HbmItems.CAPACITOR_TANTALUM, 500_000L}, {HbmItems.CAPACITOR_BISMUTH, 2_500_000L}, {HbmItems.CAPACITOR_SPARK, 10_000_000L}
        };
        for (Object[] row : caps) {
            @SuppressWarnings("unchecked")
            ItemStack stack = new ItemStack(((java.util.function.Supplier<net.minecraft.world.item.Item>) row[0]).get());
            long rate = (long) row[1];
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            checkEq(BatteryPackItem.capacity(stack), rate * 20 * 30, problems, cases, id + " capacity");
            checkEq(BatteryPackItem.chargeRate(stack), rate, problems, cases, id + " charge rate");
            checkEq(BatteryPackItem.dischargeRate(stack), rate, problems, cases, id + " discharge rate");
        }
        ItemStack potato = new ItemStack(HbmItems.BATTERY_POTATO.get());
        checkEq(BatteryPackItem.capacity(potato), 1_000L, problems, cases, "battery_potato capacity");
        checkEq(BatteryPackItem.chargeRate(potato), 0L, problems, cases, "battery_potato charge rate");
        checkEq(BatteryPackItem.dischargeRate(potato), 100L, problems, cases, "battery_potato discharge rate");
        ItemStack cube = new ItemStack(HbmItems.CUBE_POWER.get());
        checkEq(BatteryPackItem.capacity(cube), 1_000_000_000_000_000_000L, problems, cases, "cube_power capacity");
        checkEq(BatteryPackItem.dischargeRate(cube), 1_000_000_000_000_000L, problems, cases, "cube_power discharge rate");
        String[][] sc = {{"waste", "150"}, {"ra226", "200"}, {"tc99", "500"}, {"co60", "750"}, {"pu238", "1000"},
                {"po210", "1250"}, {"au198", "1500"}, {"pb209", "2000"}, {"am241", "2500"}};
        for (String[] row : sc) {
            ItemStack stack = LegacyVariantItem.stackFor(HbmItems.BATTERY_SC, row[0]);
            checkEq(BatteryPackItem.dischargeRate(stack), Long.parseLong(row[1]), problems, cases, "battery_sc " + row[0] + " discharge");
        }
        // tooltip formatting: BobMathUtil.getShortNumber and the ItemBatteryPack lines
        checkStr(LegacyBatteryTooltips.shortNumber(1_800_000L), "1.8M", problems, cases, "getShortNumber 1.8M");
        checkStr(LegacyBatteryTooltips.shortNumber(1_250L), "1.25k", problems, cases, "getShortNumber 1.25k");
        List<Component> lines = new ArrayList<>();
        LegacyBatteryTooltips.batteryPack(lines, 900_000L, 1_800_000L, 1_000L, 100L);
        checkEq(lines.size(), 5, problems, cases, "battery pack tooltip lines");
        checkStr(lines.get(0).getString(), "Energy stored: 900.0k/1.8MHE (50.0%)", problems, cases, "battery pack tooltip energy");
        checkStr(lines.get(3).getString(), "Time for full charge: 1.5min", problems, cases, "battery pack tooltip charge time");
        checkStr(lines.get(4).getString(), "Charge lasts for: 15.0min", problems, cases, "battery pack tooltip lasts");
    }

    /// TileEntityMachineDiesel: 1 mB/t, combustion / 1000 * efficiency, maxPower 50000, fuelCap 16000, isOn ///
    private static void diesel(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        BlockState state = HbmBlocks.MACHINE_DIESEL.get().defaultBlockState();
        DieselGeneratorBlockEntity diesel = place(level, pos, state, DieselGeneratorBlockEntity.class);
        if (diesel == null) {
            check(false, problems, cases, "diesel block entity missing");
            return;
        }
        checkEq(diesel.fuelTank().capacity(), 16_000, problems, cases, "diesel fuelCap");
        // DIESEL combustion energy comes from Fluids.registerCalculatedFuel (overrides the 500_000 literal)
        long dieselHe = (long) (HbmFluids.byName("diesel").orElseThrow().combustibleHeatEnergy() / 1000L * 0.75D);
        checkEq(DieselGeneratorBlockEntity.heFromFuel(HbmFluids.byName("diesel").orElseThrow()), dieselHe, problems, cases, "diesel HE/t from DIESEL (HIGH 0.75)");
        checkEq(DieselGeneratorBlockEntity.heFromFuel(HbmFluids.byName("kerosene").orElseThrow()),
                (long) (HbmFluids.byName("kerosene").orElseThrow().combustibleHeatEnergy() / 1000L * 0.1D), problems, cases, "diesel accepts AERO fuel at 0.1");
        diesel.fuelTank().fill(diesel.fuelTank().type(), 1_000, false);
        for (int i = 0; i < 20; i++) DieselGeneratorBlockEntity.tick(level, pos, level.getBlockState(pos), diesel);
        checkEq(diesel.power(), 0, problems, cases, "diesel off by default (isOn=false)");
        diesel.toggleOn();
        for (int i = 0; i < 20; i++) DieselGeneratorBlockEntity.tick(level, pos, level.getBlockState(pos), diesel);
        checkEq(diesel.fuelTank().amount(), 980, problems, cases, "diesel fuel after 20 ticks");
        checkEq(diesel.power(), Math.min(50_000L, 20 * dieselHe), problems, cases, "diesel power after 20 ticks");
        checkEq(DieselGeneratorBlockEntity.SLOT_COUNT, 4, problems, cases, "diesel slot count");
    }

    /// TileEntityConverterHeRf / RfHe with default config (5 HE -> 1 RF, 2 RF -> 5 HE, decay 0) ///
    private static void converters(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        EnergyConverterBlockEntity heRf = place(level, pos, HbmBlocks.MACHINE_CONVERTER_HE_RF.get().defaultBlockState(), EnergyConverterBlockEntity.class);
        EnergyConverterBlockEntity rfHe = place(level, pos.east(4), HbmBlocks.MACHINE_CONVERTER_RF_HE.get().defaultBlockState(), EnergyConverterBlockEntity.class);
        if (heRf == null || rfHe == null) {
            check(false, problems, cases, "converter block entities missing");
            return;
        }
        checkEq(heRf.getRequestedInput(), 5_000_000L, problems, cases, "he_rf receiver demand (maxPower 5M)");
        check(heRf.getPowerPriority() == PowerEndpoint.ConnectionPriority.LOW, problems, cases, "he_rf priority LOW");
        heRf.applyPower(0L, 5_003L);
        heRf.serverTick(level);
        checkEq(heRf.rf(), 1_000, problems, cases, "he_rf 5003 HE -> 1000 RF");
        checkEq(heRf.power(), 3, problems, cases, "he_rf HE remainder");
        checkEq(heRf.energyStorage().receiveEnergy(100, false), 0, problems, cases, "he_rf refuses RF input");
        checkEq(rfHe.energyStorage().receiveEnergy(2_000_000, false), 1_000_000, problems, cases, "rf_he RF input capped at 1M");
        rfHe.serverTick(level);
        checkEq(rfHe.power(), 2_500_000L, problems, cases, "rf_he 1M RF -> 2.5M HE");
        checkEq(rfHe.rf(), 0, problems, cases, "rf_he RF consumed");
        checkEq(rfHe.energyStorage().extractEnergy(10, false), 0, problems, cases, "rf_he refuses RF extraction");
    }

    /// CraftingManager 252-253 ///
    private static void recipes(MinecraftServer server, List<String> problems, int[] cases) {
        for (String id : new String[] {"machine_converter_he_rf", "machine_converter_rf_he"}) {
            RecipeHolder<?> holder = server.getRecipeManager().byKey(AtomicLegacy.id(id)).orElse(null);
            check(holder != null, problems, cases, "recipe " + id + " loaded");
        }
        check(server.getRecipeManager().byKey(AtomicLegacy.id("cable_diode")).isEmpty(), problems, cases, "cable_diode has no recipe in 1.7.10");
        ResourceLocation unused = ResourceLocation.withDefaultNamespace("air");
        check(BuiltInRegistries.BLOCK.get(unused) == Blocks.AIR, problems, cases, "registry sanity");
    }

    private static final class Fake implements PowerEndpoint {
        private final long out;
        private final long in;
        private final ConnectionPriority priority;
        private long used;
        private long received;

        private Fake(long out, long in, ConnectionPriority priority) {
            this.out = out;
            this.in = in;
            this.priority = priority;
        }

        @Override public BlockPos getPowerPos() { return BlockPos.ZERO; }
        @Override public long getAvailableOutput() { return out; }
        @Override public long getRequestedInput() { return in; }
        @Override public ConnectionPriority getPowerPriority() { return priority; }
        @Override public void applyPower(long usedOutput, long receivedInput) { used += usedOutput; received += receivedInput; }
        @Override public Component getPowerStatus() { return Component.empty(); }
    }

    private static BlockPos pad(ServerLevel level) {
        int index = site++;
        BlockPos pos = new BlockPos(BASE_X + (index % 8) * SPACING, BASE_Y, BASE_Z + (index / 8) * SPACING);
        for (int cx = (pos.getX() - 8) >> 4; cx <= (pos.getX() + 8) >> 4; cx++) {
            for (int cz = (pos.getZ() - 8) >> 4; cz <= (pos.getZ() + 8) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
                FORCED.add(new int[] {cx, cz});
            }
        }
        for (int dx = -3; dx <= 6; dx++) for (int dy = -2; dy <= 4; dy++) for (int dz = -3; dz <= 3; dz++) {
            level.setBlock(pos.offset(dx, dy, dz), dy < 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        return pos;
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> T place(ServerLevel level, BlockPos pos, BlockState state, Class<T> type) {
        level.setBlock(pos, state, 2 | 16);
        BlockEntity be = level.getBlockEntity(pos);
        return type.isInstance(be) ? (T) be : null;
    }

    private static void check(boolean condition, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (!condition) problems.add("L1: " + message);
    }

    private static void checkEq(long actual, long expected, List<String> problems, int[] cases, String message) {
        check(actual == expected, problems, cases, message + ": " + actual + " expected " + expected);
    }

    private static void checkStr(String actual, String expected, List<String> problems, int[] cases, String message) {
        check(expected.equals(actual), problems, cases, message + ": '" + actual + "' expected '" + expected + "'");
    }
}
