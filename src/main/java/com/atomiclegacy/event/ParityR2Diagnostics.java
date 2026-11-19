package com.atomiclegacy.event;

import java.util.ArrayList;
import java.util.List;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.ConveyorBlock;
import com.atomiclegacy.blocks.CraneMachineBlock;
import com.atomiclegacy.blocks.HbmRailBlock;
import com.atomiclegacy.blocks.RadioTorchBlock;
import com.atomiclegacy.entity.ConveyorMovingItem;
import com.atomiclegacy.pneumatic.PneumaticNetworkManager;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmEntityTypes;
import com.atomiclegacy.rtty.HbmRttySystem;
import com.atomiclegacy.tileentity.MassStorageBlockEntity;
import com.atomiclegacy.tileentity.PneumaticTubeBlockEntity;
import com.atomiclegacy.tileentity.RadioTorchBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * [parity R2] Dedicated-server checks for the logistics batch against the pinned 1.7.10 sources: ModBlocks
 * hardness/resistance, RailGeneric/RailBooster, CraftingManager rail recipes, TileEntityMassStorage, the RTTY
 * sender/receiver/logic chain (RTTYSystem one-tick delay), TileEntityPneumoTube/PneumaticNetwork transport and
 * filters, the conveyor moving-item speed (0.0625 per tick, x3 express) and the crane screwdriver rules.
 * Block cases use pads near x = 25000, y = 200; the moving-item case uses the spawn chunks at y = 250.
 * Not covered: GUIs, rendering, sounds, drones, minecarts and trains (not ported).
 */
final class ParityR2Diagnostics {

    private ParityR2Diagnostics() {
    }

    private static final int BASE_X = 25_000;
    private static final int BASE_Y = 200;
    private static final int BASE_Z = 25_000;
    private static int site;
    private static final List<int[]> FORCED = new ArrayList<>();

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        site = 0;
        try {
            properties(problems, cases);
            recipes(server, problems, cases);
            rails(problems, cases);
            massStorage(level, problems, cases);
            rtty(level, problems, cases);
            pneumaticTube(level, problems, cases);
            conveyor(level, problems, cases);
            craneScrewdriver(level, problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity R2 diagnostics crashed", e);
            problems.add("R2 diagnostics crashed: " + e);
        } finally {
            for (int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
            FORCED.clear();
        }
        return cases[0];
    }

    /// ModBlocks setHardness / setResistance ///
    private static void properties(List<String> problems, int[] cases) {
        Object[][] rows = {
                {HbmBlocks.CONVEYOR.get(), 2F, 2F}, {HbmBlocks.CONVEYOR_EXPRESS.get(), 2F, 2F},
                {HbmBlocks.CONVEYOR_DOUBLE.get(), 2F, 2F}, {HbmBlocks.CONVEYOR_TRIPLE.get(), 2F, 2F},
                {HbmBlocks.CONVEYOR_CHUTE.get(), 2F, 2F}, {HbmBlocks.CONVEYOR_LIFT.get(), 2F, 2F},
                {HbmBlocks.CRANE_EXTRACTOR.get(), 5F, 10F}, {HbmBlocks.CRANE_INSERTER.get(), 5F, 10F},
                {HbmBlocks.CRANE_GRABBER.get(), 5F, 10F}, {HbmBlocks.CRANE_ROUTER.get(), 5F, 10F},
                {HbmBlocks.CRANE_BOXER.get(), 5F, 10F}, {HbmBlocks.CRANE_UNBOXER.get(), 5F, 10F},
                {HbmBlocks.CRANE_SPLITTER.get(), 5F, 10F}, {HbmBlocks.CRANE_PARTITIONER.get(), 5F, 10F},
                {HbmBlocks.DRONE_WAYPOINT.get(), 0.1F, 10F}, {HbmBlocks.DRONE_WAYPOINT_REQUEST.get(), 0.1F, 10F},
                {HbmBlocks.DRONE_CRATE.get(), 0.1F, 10F}, {HbmBlocks.DRONE_DOCK.get(), 0.1F, 10F},
                {HbmBlocks.DRONE_CRATE_PROVIDER.get(), 0.1F, 10F}, {HbmBlocks.DRONE_CRATE_REQUESTER.get(), 0.1F, 10F},
                {HbmBlocks.PNEUMATIC_TUBE.get(), 2F, 10F}, {HbmBlocks.PNEUMATIC_TUBE_PAINTABLE.get(), 5F, 10F},
                {HbmBlocks.PNEUMATIC_STORAGE_ACCESS.get(), 5F, 10F}, {HbmBlocks.PNEUMATIC_STORAGE_CLUTTER.get(), 5F, 10F},
                {HbmBlocks.PNEUMATIC_STORAGE_MONO.get(), 5F, 10F}, {HbmBlocks.PNEUMATIC_STORAGE_IMPORTER.get(), 5F, 10F},
                {HbmBlocks.PNEUMATIC_STORAGE_EXPORTER.get(), 5F, 10F},
                {HbmBlocks.RADIO_TORCH_SENDER.get(), 0.1F, 10F}, {HbmBlocks.RADIO_TORCH_RECEIVER.get(), 0.1F, 10F},
                {HbmBlocks.RADIO_TORCH_COUNTER.get(), 0.1F, 10F}, {HbmBlocks.RADIO_TORCH_LOGIC.get(), 0.1F, 10F},
                {HbmBlocks.RADIO_TORCH_READER.get(), 0.1F, 10F}, {HbmBlocks.RADIO_TORCH_CONTROLLER.get(), 0.1F, 10F},
                {HbmBlocks.RADIO_TORCH_RELAY.get(), 0.1F, 10F},
                {HbmBlocks.RAIL_WOOD.get(), 5F, 10F}, {HbmBlocks.RAIL_NARROW.get(), 5F, 10F},
                {HbmBlocks.RAIL_HIGHSPEED.get(), 5F, 10F}, {HbmBlocks.RAIL_BOOSTER.get(), 5F, 10F},
                {HbmBlocks.MASS_STORAGE.get(), 5F, 10F}, {HbmBlocks.MACHINE_CONVEYOR_PRESS.get(), 5F, 10F},
        };
        for (Object[] row : rows) {
            Block block = (Block) row[0];
            String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
            check(block.defaultDestroyTime() == (float) row[1], problems, cases, id + " hardness " + block.defaultDestroyTime());
            check(ParityT3Diagnostics.resistanceMatches(block), problems, cases, id + " resistance " + block.getExplosionResistance()); // [parity T3: expected = original r*3/5]
        }
        // Material.circuits: no tool required for the RTTY torches and drone waypoints
        for (Block block : new Block[] {HbmBlocks.RADIO_TORCH_SENDER.get(), HbmBlocks.RADIO_TORCH_RELAY.get(), HbmBlocks.DRONE_WAYPOINT.get()}) {
            check(!block.defaultBlockState().requiresCorrectToolForDrops(), problems, cases,
                    BuiltInRegistries.BLOCK.getKey(block).getPath() + " must not require a tool (Material.circuits)");
        }
        check(HbmBlocks.RADIO_TORCH_RELAY.get() instanceof RadioTorchBlock relay && relay.kind() == RadioTorchBlock.Kind.RELAY,
                problems, cases, "radio_torch_relay registered as RadioTorchRelay");
    }

    /// CraftingManager 487-490 ///
    private static void recipes(MinecraftServer server, List<String> problems, int[] cases) {
        var manager = server.getRecipeManager();
        Object[][] rows = {{"rail_wood", 16}, {"rail_narrow", 64}, {"rail_highspeed", 16}, {"rail_booster", 6}};
        for (Object[] row : rows) {
            var holder = manager.byKey(AtomicLegacy.id((String) row[0]));
            int count = holder.map(h -> h.value().getResultItem(server.registryAccess()).getCount()).orElse(-1);
            checkEq(count, (int) row[1], problems, cases, "recipe " + row[0] + " output count");
        }
        var booster = manager.byKey(AtomicLegacy.id("rail_booster"));
        check(booster.isPresent() && booster.get().value().getIngredients().stream()
                        .anyMatch(ing -> ing.test(stack("coil_copper"))) && booster.get().value().getIngredients().stream()
                        .anyMatch(ing -> ing.test(stack("ingot_red_copper"))),
                problems, cases, "rail_booster uses copper coils and a red copper (Mingrade) ingot");
    }

    /// RailGeneric speeds (0.4 base), RailBooster x1.15, isFlexibleRail always true ///
    private static void rails(List<String> problems, int[] cases) {
        check(((HbmRailBlock) HbmBlocks.RAIL_WOOD.get()).maxSpeed() == 0.2F, problems, cases, "rail_wood max speed 0.2");
        check(((HbmRailBlock) HbmBlocks.RAIL_HIGHSPEED.get()).maxSpeed() == 1.0F, problems, cases, "rail_highspeed max speed 1.0");
        check(((HbmRailBlock) HbmBlocks.RAIL_BOOSTER.get()).maxSpeed() == 1.0F, problems, cases, "rail_booster max speed 1.0");
        check(((HbmRailBlock) HbmBlocks.RAIL_NARROW.get()).maxSpeed() == 0.4F, problems, cases, "rail_narrow max speed 0.4");
        check(((HbmRailBlock) HbmBlocks.RAIL_HIGHSPEED.get()).isFlexibleRail(HbmBlocks.RAIL_HIGHSPEED.get().defaultBlockState(), null, BlockPos.ZERO), problems, cases,
                "rail_highspeed stays flexible (RailGeneric.isFlexibleRail = !isPowered())");
    }

    /// TileEntityMassStorage: quickExtract needs a full stack and output on; provide fills slot 2; RTTY values ///
    private static void massStorage(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        level.setBlock(pos, HbmBlocks.MASS_STORAGE_IRON.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(pos) instanceof MassStorageBlockEntity storage)) {
            check(false, problems, cases, "mass storage block entity missing");
            return;
        }
        storage.setItem(1, new ItemStack(Items.COBBLESTONE));
        ItemStack in = new ItemStack(Items.COBBLESTONE, 40);
        check(storage.quickInsert(in) && in.isEmpty(), problems, cases, "quickInsert accepts a stack that fits");
        checkEq(storage.stockpile(), 40, problems, cases, "stockpile after quickInsert");
        check(storage.quickExtract().isEmpty(), problems, cases, "quickExtract returns nothing while output is off");
        storage.toggleOutput();
        check(storage.quickExtract().isEmpty(), problems, cases, "quickExtract returns nothing below a full stack (40 < 64)");
        storage.provide(false);
        checkEq(storage.getItem(2).getCount(), 1, problems, cases, "provide one fills the output slot");
        checkEq(storage.stockpile(), 39, problems, cases, "stockpile after provide one");
        check("39".equals(storage.provideRttyValue("VAL:fill")), problems, cases, "RTTY fill value");
        check("0".equals(storage.provideRttyValue("VAL:fillpercent")), problems, cases, "RTTY fillpercent value (39 * 100 / 10000)");
    }

    /// RTTY: sender on a redstone block -> channel -> receiver (one tick later via RTTYSystem queue) ///
    private static void rtty(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        level.setBlock(pos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        level.setBlock(pos, HbmBlocks.RADIO_TORCH_SENDER.get().defaultBlockState().setValue(RadioTorchBlock.FACING, Direction.UP), 3);
        BlockPos rec = pos.east(3);
        level.setBlock(rec.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(rec, HbmBlocks.RADIO_TORCH_RECEIVER.get().defaultBlockState().setValue(RadioTorchBlock.FACING, Direction.UP), 3);
        BlockPos logicPos = pos.east(5);
        level.setBlock(logicPos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(logicPos, HbmBlocks.RADIO_TORCH_LOGIC.get().defaultBlockState().setValue(RadioTorchBlock.FACING, Direction.UP), 3);
        if (!(level.getBlockEntity(pos) instanceof RadioTorchBlockEntity sender)
                || !(level.getBlockEntity(rec) instanceof RadioTorchBlockEntity receiver)
                || !(level.getBlockEntity(logicPos) instanceof RadioTorchBlockEntity logic)) {
            check(false, problems, cases, "RTTY block entities missing");
            return;
        }
        String channel = "r2diag";
        sender.setChannel(channel);
        receiver.setChannel(channel);
        logic.setChannel(channel);
        Player fake = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        fake.setPos(logicPos.getX() + 0.5D, logicPos.getY(), logicPos.getZ() + 0.5D);
        net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
        data.putString("m3", "10");
        data.putInt("c3", 3); // signal GREATER THAN 10 -> state 3
        logic.receiveControl(fake, data);

        RadioTorchBlockEntity.tick(level, pos, level.getBlockState(pos), sender);
        checkEq(sender.signal(), 15, problems, cases, "sender reads the redstone block (15)");
        RadioTorchBlockEntity.tick(level, rec, level.getBlockState(rec), receiver);
        checkEq(receiver.signal(), 0, problems, cases, "receiver sees nothing before the queue is published");
        HbmRttySystem.onLevelTick(new LevelTickEvent.Post(() -> true, level));
        RadioTorchBlockEntity.tick(level, rec, level.getBlockState(rec), receiver);
        checkEq(receiver.signal(), 15, problems, cases, "receiver output after one tick");
        checkEq(level.getBlockState(rec).getSignal(level, rec, Direction.UP), 15, problems, cases, "receiver redstone power");
        RadioTorchBlockEntity.tick(level, logicPos, level.getBlockState(logicPos), logic);
        checkEq(logic.signal(), 3, problems, cases, "logic receiver: 15 > 10 maps to state 3");
    }

    /// TileEntityPneumoTube + PneumaticNetwork.send: 64-mass transfer, range by pressure, whitelist filter ///
    private static void pneumaticTube(ServerLevel level, List<String> problems, int[] cases) {
        checkEq(PneumaticTubeBlockEntity.getRangeFromPressure(1), 10, problems, cases, "range at 1 PU");
        checkEq(PneumaticTubeBlockEntity.getRangeFromPressure(3), 100, problems, cases, "range at 3 PU");
        checkEq(PneumaticTubeBlockEntity.getRangeFromPressure(5), 1_000, problems, cases, "range at 5 PU");

        BlockPos pos = pad(level);
        BlockPos chestA = pos.west();
        BlockPos tubeA = pos;
        BlockPos tubeB = pos.east();
        BlockPos chestB = pos.east(2);
        level.setBlock(chestA, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(chestB, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(tubeA, HbmBlocks.PNEUMATIC_TUBE.get().defaultBlockState(), 3);
        level.setBlock(tubeB, HbmBlocks.PNEUMATIC_TUBE.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(tubeA) instanceof PneumaticTubeBlockEntity compressor)
                || !(level.getBlockEntity(tubeB) instanceof PneumaticTubeBlockEntity endpoint)
                || !(level.getBlockEntity(chestA) instanceof ChestBlockEntity source)
                || !(level.getBlockEntity(chestB) instanceof ChestBlockEntity dest)) {
            check(false, problems, cases, "pneumatic tube test blocks missing");
            return;
        }
        compressor.cycleDirection(false);
        endpoint.cycleDirection(true);
        check(compressor.insertionDir() == Direction.WEST, problems, cases, "screwdriver cycles insertion to the adjacent inventory");
        check(endpoint.ejectionDir() == Direction.EAST, problems, cases, "sneaking screwdriver cycles ejection to the adjacent inventory");
        source.setItem(0, new ItemStack(Items.STONE, 64));
        source.setItem(1, new ItemStack(Items.ENDER_PEARL, 16));
        PneumaticNetworkManager.addReceiver(level, tubeB, dest, chestB, Direction.EAST, endpoint);
        boolean sent = PneumaticNetworkManager.send(level, tubeA, source, chestA, compressor, Direction.EAST,
                0, 0, PneumaticTubeBlockEntity.getRangeFromPressure(1), 0);
        check(sent, problems, cases, "compressor sends to the registered endpoint");
        checkEq(dest.getItem(0).getCount(), 64, problems, cases, "64 mass per transfer: a full stack of stone");
        checkEq(source.getItem(1).getCount(), 16, problems, cases, "transfer stops after 64 mass");
        sent = PneumaticNetworkManager.send(level, tubeA, source, chestA, compressor, Direction.EAST, 0, 0, 10, 0);
        check(sent, problems, cases, "second send moves ender pearls");
        int pearls = 0;
        for (int i = 0; i < dest.getContainerSize(); i++) if (dest.getItem(i).is(Items.ENDER_PEARL)) pearls += dest.getItem(i).getCount();
        checkEq(pearls, 16, problems, cases, "ender pearls (mass 4 each) move 16 at once");
        source.setItem(0, new ItemStack(Items.DIRT, 8));
        compressor.handleButton(0); // whitelist with an empty filter blocks everything
        check(!PneumaticNetworkManager.send(level, tubeA, source, chestA, compressor, Direction.EAST, 0, 0, 10, 0),
                problems, cases, "empty whitelist blocks the transfer");
        compressor.handleButton(0);
        check(!PneumaticNetworkManager.send(level, tubeA, source, chestA, compressor, Direction.EAST, 0, 0, 0, 0),
                problems, cases, "out of range (0 PU) blocks the transfer");
    }

    /// EntityMovingItem on BlockConveyor: 0.0625 blocks per tick after the 5-tick grace period ///
    private static void conveyor(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos pos = new BlockPos(spawn.getX() + 2, 250, spawn.getZ() - 6);
        for (int dx = -1; dx <= 6; dx++) for (int dy = -1; dy <= 2; dy++) level.setBlock(pos.offset(dx, dy, 0), Blocks.AIR.defaultBlockState(), 2 | 16);
        for (int dx = 0; dx < 5; dx++) {
            level.setBlock(pos.offset(dx, -1, 0), Blocks.STONE.defaultBlockState(), 2 | 16);
            level.setBlock(pos.offset(dx, 0, 0), HbmBlocks.CONVEYOR.get().defaultBlockState().setValue(ConveyorBlock.FACING, Direction.WEST), 2 | 16);
        }
        ConveyorMovingItem item = new ConveyorMovingItem(level, new ItemStack(Items.IRON_INGOT));
        item.setPos(pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D);
        level.addFreshEntity(item);
        Vec3 start = item.position();
        for (int t = 0; t < 21; t++) {
            item.tickCount++; // ServerLevel.tickNonPassenger increments tickCount before tick()
            item.tick();
        }
        double moved = Math.sqrt(Math.pow(item.getX() - start.x, 2) + Math.pow(item.getZ() - start.z, 2));
        check(Math.abs(moved - 16 * 0.0625D) < 0.02D, problems, cases, "conveyor moved " + moved + " in 16 moving ticks, expected 1.0");
        item.discard();
        for (int dx = 0; dx < 5; dx++) {
            level.setBlock(pos.offset(dx, 0, 0), Blocks.AIR.defaultBlockState(), 2 | 16);
            level.setBlock(pos.offset(dx, -1, 0), Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }

    /// CraneRouter / CranePartitioner are not IToolable ///
    private static void craneScrewdriver(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        level.setBlock(pos, HbmBlocks.CRANE_ROUTER.get().defaultBlockState(), 3);
        Player fake = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        ItemStack driver = new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id("screwdriver")));
        check(!CraneMachineBlock.configureWithScrewdriver(level, pos, fake, driver, InteractionHand.MAIN_HAND, Direction.UP),
                problems, cases, "screwdriver does nothing on crane_router");
        level.setBlock(pos, HbmBlocks.CRANE_PARTITIONER.get().defaultBlockState(), 3);
        check(!CraneMachineBlock.configureWithScrewdriver(level, pos, fake, driver, InteractionHand.MAIN_HAND, Direction.UP),
                problems, cases, "screwdriver does nothing on crane_partitioner");
    }

    private static void check(boolean condition, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (!condition) problems.add("R2: " + message);
    }

    private static void checkEq(long actual, long expected, List<String> problems, int[] cases, String message) {
        check(actual == expected, problems, cases, message + ": " + actual + " expected " + expected);
    }

    private static ItemStack stack(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id(path)));
    }

    private static BlockPos pad(ServerLevel level) {
        int index = site++;
        BlockPos pos = new BlockPos(BASE_X + (index % 8) * 32, BASE_Y, BASE_Z + (index / 8) * 32);
        for (int cx = (pos.getX() - 8) >> 4; cx <= (pos.getX() + 8) >> 4; cx++) {
            for (int cz = (pos.getZ() - 8) >> 4; cz <= (pos.getZ() + 8) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
                FORCED.add(new int[] {cx, cz});
            }
        }
        for (int dx = -4; dx <= 8; dx++) for (int dy = -8; dy <= 6; dy++) for (int dz = -4; dz <= 4; dz++) {
            level.setBlock(pos.offset(dx, dy, dz), dy < -6 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        return pos;
    }
}
