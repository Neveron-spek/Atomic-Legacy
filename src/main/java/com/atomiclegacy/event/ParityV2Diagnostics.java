package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.PipeAnchorBlock;
import com.atomiclegacy.blocks.PneumaticTubeBlock;
import com.atomiclegacy.blocks.PneumaticTubePaintableBlock;
import com.atomiclegacy.inventory.container.PneumaticStorageMenu;
import com.atomiclegacy.pneumatic.PneumaticNetworkManager;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.registry.HbmSoundTypes;
import com.atomiclegacy.tileentity.LegacyMachineBlockEntity;
import com.atomiclegacy.tileentity.PipeAnchorBlockEntity;
import com.atomiclegacy.tileentity.PneumaticStorageBlockEntity;
import com.atomiclegacy.tileentity.PneumaticTubeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * [parity V2] Dedicated-server checks for the pneumatic batch against the pinned 1.7.10 sources: PneumoPipeAnchor /
 * TileEntityPneumaticPipeAnchor (one IPneumaticConnector side, linked nodes, wrench network types, no fluid),
 * PneumoTubePaintableBlock (full cube, paint, hand drill, defuser, copy tool), the storage endpoints (importer,
 * exporter request modes, access terminal clicks, air availability) and TileEntityMachineAutocrafter.
 * Pads near x = 36000, y = 200. Not covered: rendering, GUIs, sounds.
 */
final class ParityV2Diagnostics {
    private static final int BASE_X = 36_000;
    private static final int BASE_Y = 200;
    private static final int BASE_Z = 36_000;
    private static int site;
    private static final List<int[]> FORCED = new ArrayList<>();

    private ParityV2Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        site = 0;
        try {
            properties(server, problems, cases);
            anchors(level, problems, cases);
            paintable(level, problems, cases);
            storage(level, problems, cases);
            autocrafter(level, problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity V2 diagnostics crashed", e);
            problems.add("V2 diagnostics crashed: " + e);
        } finally {
            for (int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
            FORCED.clear();
        }
        return cases[0];
    }

    /// ModBlocks 1889 / 1933, CraftingManager 583 ///
    private static void properties(MinecraftServer server, List<String> problems, int[] cases) {
        Block anchor = HbmBlocks.PIPE_ANCHOR_PNEUMATIC.get();
        check(anchor instanceof PipeAnchorBlock a && a.pneumatic(), problems, cases, "pipe_anchor_pneumatic is a pneumatic PipeAnchorBlock");
        check(anchor.defaultDestroyTime() == 5.0F, problems, cases, "pipe_anchor_pneumatic hardness " + anchor.defaultDestroyTime());
        check(ParityT3Diagnostics.resistanceMatches(anchor), problems, cases, "pipe_anchor_pneumatic resistance " + anchor.getExplosionResistance());
        check(anchor.defaultBlockState().getSoundType() == HbmSoundTypes.PIPE, problems, cases, "pipe_anchor_pneumatic step sound pipe");
        check(anchor.defaultBlockState().requiresCorrectToolForDrops(), problems, cases, "pipe_anchor_pneumatic Material.iron needs a tool");
        Block paintable = HbmBlocks.PNEUMATIC_TUBE_PAINTABLE.get();
        check(paintable instanceof PneumaticTubePaintableBlock, problems, cases, "pneumatic_tube_paintable is PneumoTubePaintableBlock");
        check(paintable.defaultDestroyTime() == 5.0F && ParityT3Diagnostics.resistanceMatches(paintable), problems, cases,
                "pneumatic_tube_paintable 5 / 10");
        check(paintable.defaultBlockState().canOcclude(), problems, cases, "pneumatic_tube_paintable is an opaque cube");
        var holder = server.getRecipeManager().byKey(AtomicLegacy.id("pipe_anchor_pneumatic"));
        check(holder.isPresent() && holder.get().value().getResultItem(server.registryAccess()).getCount() == 2
                        && holder.get().value().getResultItem(server.registryAccess()).is(anchor.asItem()),
                problems, cases, "recipe pipe_anchor_pneumatic: P/P/S welded copper + rubber -> 2");
    }

    /// TileEntityPneumaticPipeAnchor + PneumaticNetwork.send over a linked pair ///
    private static void anchors(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos chestA = pad(level);
        BlockPos tubeA = chestA.east();
        BlockPos anchorA = tubeA.east();
        BlockPos anchorB = anchorA.east(8);
        BlockPos tubeB = anchorB.east();
        BlockPos chestB = tubeB.east();
        BlockPos sideTube = anchorA.north();
        level.setBlock(chestA, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(chestB, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(tubeA, HbmBlocks.PNEUMATIC_TUBE.get().defaultBlockState(), 3);
        level.setBlock(tubeB, HbmBlocks.PNEUMATIC_TUBE.get().defaultBlockState(), 3);
        level.setBlock(sideTube, HbmBlocks.PNEUMATIC_TUBE.get().defaultBlockState(), 3);
        // meta = clicked side; getOppositeDir() is the connected side: A connects west, B connects east
        level.setBlock(anchorA, HbmBlocks.PIPE_ANCHOR_PNEUMATIC.get().defaultBlockState().setValue(PipeAnchorBlock.FACING, Direction.EAST), 3);
        level.setBlock(anchorB, HbmBlocks.PIPE_ANCHOR_PNEUMATIC.get().defaultBlockState().setValue(PipeAnchorBlock.FACING, Direction.WEST), 3);
        if (!(level.getBlockEntity(tubeA) instanceof PneumaticTubeBlockEntity compressor)
                || !(level.getBlockEntity(tubeB) instanceof PneumaticTubeBlockEntity endpoint)
                || !(level.getBlockEntity(anchorA) instanceof PipeAnchorBlockEntity first)
                || !(level.getBlockEntity(anchorB) instanceof PipeAnchorBlockEntity second)
                || !(level.getBlockEntity(chestA) instanceof ChestBlockEntity source)
                || !(level.getBlockEntity(chestB) instanceof ChestBlockEntity dest)) {
            check(false, problems, cases, "anchor test blocks missing");
            return;
        }
        check(first.isPneumaticAnchor() && first.anchorKind() == PipeAnchorBlock.Kind.PNEUMATIC, problems, cases, "pneumatic anchor kind");
        check(!first.canConnect(PneumaticTubeBlockEntity.air()) && first.fluidHandler(Direction.WEST) == null
                && first.networkLinks().isEmpty(), problems, cases, "pneumatic anchor is no fluid connector");
        check(level.getBlockState(tubeA).getValue(PneumaticTubeBlock.EAST), problems, cases, "tube connects to the anchor's connector side");
        check(!level.getBlockState(sideTube).getValue(PneumaticTubeBlock.SOUTH), problems, cases, "tube on another anchor side does not connect");
        BlockState fluidAnchor = HbmBlocks.PIPE_ANCHOR.get().defaultBlockState();
        check(fluidAnchor.getBlock() instanceof PipeAnchorBlock f && f.kind() == PipeAnchorBlock.Kind.FLUID
                        && HbmBlocks.PIPE_ANCHOR_EXHAUST.get() instanceof PipeAnchorBlock e && e.kind() == PipeAnchorBlock.Kind.EXHAUST,
                problems, cases, "wrench network types FLUID / EXHAUST / PNEUMATIC differ");

        compressor.cycleDirection(false);
        endpoint.cycleDirection(true);
        source.setItem(0, new ItemStack(Items.STONE, 64));
        PneumaticNetworkManager.addReceiver(level, tubeB, dest, chestB, Direction.EAST, endpoint);
        check(!PneumaticNetworkManager.send(level, tubeA, source, chestA, compressor, Direction.EAST, 0, 0, 10, 0),
                problems, cases, "unlinked anchors: no transfer");
        first.addLink(anchorB);
        second.addLink(anchorA);
        PneumaticNetworkManager.addReceiver(level, tubeB, dest, chestB, Direction.EAST, endpoint);
        checkEq(PneumaticNetworkManager.receiverCount(level, tubeA), 1, problems, cases, "linked anchors share the endpoint's net");
        check(PneumaticNetworkManager.send(level, tubeA, source, chestA, compressor, Direction.EAST, 0, 0, 25, 0),
                problems, cases, "compressor sends through the anchor link");
        checkEq(dest.getItem(0).getCount(), 64, problems, cases, "64 stone arrive through the link");
        checkEq(PneumaticNetworkManager.receiverCount(level, sideTube), 0, problems, cases, "side tube is a separate net");

        source.setItem(0, new ItemStack(Items.DIRT, 16));
        level.setBlock(anchorB, Blocks.AIR.defaultBlockState(), 3);
        check(first.anchorLinks().isEmpty(), problems, cases, "breaking an anchor removes the link (disconnectAll)");
        PneumaticNetworkManager.addReceiver(level, tubeB, dest, chestB, Direction.EAST, endpoint);
        check(!PneumaticNetworkManager.send(level, tubeA, source, chestA, compressor, Direction.EAST, 0, 0, 25, 0),
                problems, cases, "broken link: no transfer");
        checkEq(source.getItem(0).getCount(), 16, problems, cases, "dirt stays in the source after the link broke");
    }

    /// PneumoTubePaintableBlock ///
    private static void paintable(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos chestA = pad(level);
        BlockPos tubeA = chestA.east();
        BlockPos tubeB = tubeA.east();
        BlockPos chestB = tubeB.east();
        level.setBlock(chestA, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(chestB, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(tubeA, HbmBlocks.PNEUMATIC_TUBE_PAINTABLE.get().defaultBlockState(), 3);
        level.setBlock(tubeB, HbmBlocks.PNEUMATIC_TUBE.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(tubeA) instanceof PneumaticTubeBlockEntity painted)
                || !(level.getBlockEntity(tubeB) instanceof PneumaticTubeBlockEntity endpoint)
                || !(level.getBlockEntity(chestA) instanceof ChestBlockEntity source)
                || !(level.getBlockEntity(chestB) instanceof ChestBlockEntity dest)) {
            check(false, problems, cases, "paintable test blocks missing");
            return;
        }
        BlockState state = level.getBlockState(tubeA);
        check(state.getShape(level, tubeA, CollisionContext.empty()).equals(Shapes.block())
                && state.getCollisionShape(level, tubeA).equals(Shapes.block()), problems, cases, "paintable tube is a full cube");
        check(level.getBlockState(tubeB).getValue(PneumaticTubeBlock.WEST), problems, cases, "tube connects to the paintable tube");

        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.setPos(tubeA.getX() + 0.5D, tubeA.getY() + 2, tubeA.getZ() + 0.5D);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(tubeA), Direction.UP, tubeA, false);
        check(!PneumaticTubePaintableBlock.allowedPaint(Blocks.GRASS_BLOCK, state.getBlock())
                        && !PneumaticTubePaintableBlock.allowedPaint(state.getBlock(), state.getBlock())
                        && !PneumaticTubePaintableBlock.allowedPaint(Blocks.GLASS, state.getBlock())
                        && PneumaticTubePaintableBlock.allowedPaint(Blocks.STONE_BRICKS, state.getBlock()),
                problems, cases, "BlockCablePaintable.allowedPaint: no grass, no self, only normal blocks");
        use(level, player, new ItemStack(Items.STONE_BRICKS), state, hit);
        check(painted.paintBlock() == Blocks.STONE_BRICKS, problems, cases, "a block item paints the tube");
        ItemStack dirt = new ItemStack(Items.DIRT);
        use(level, player, dirt, level.getBlockState(tubeA), hit);
        check(painted.paintBlock() == Blocks.STONE_BRICKS && dirt.getCount() == 1, problems, cases, "existing paint is kept, item not used");
        ItemStack defuser = new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id("defuser")));
        use(level, player, defuser, level.getBlockState(tubeA), hit);
        check(painted.overlayHidden() && defuser.getDamageValue() == 1, problems, cases, "defuser toggles metadata (overlay) and is damaged");

        BlockPos copyPos = tubeA.below(2);
        level.setBlock(copyPos, HbmBlocks.PNEUMATIC_TUBE_PAINTABLE.get().defaultBlockState(), 3);
        if (level.getBlockEntity(copyPos) instanceof PneumaticTubeBlockEntity copy) {
            copy.pasteSettings(painted.getSettings(level, tubeA), 0, level, player, copyPos);
            check(copy.paintBlock() == Blocks.STONE_BRICKS, problems, cases, "copy tool pastes the paint block");
        }
        ItemStack drill = new ItemStack(BuiltInRegistries.ITEM.get(AtomicLegacy.id("hand_drill")));
        use(level, player, drill, level.getBlockState(tubeA), hit);
        check(painted.paintBlock() == null && drill.getDamageValue() == 0, problems, cases, "hand drill removes the paint without damage");

        painted.cycleDirection(false);
        endpoint.cycleDirection(true);
        check(painted.insertionDir() == Direction.WEST && endpoint.ejectionDir() == Direction.EAST, problems, cases,
                "paintable tube keeps the screwdriver side cycling");
        source.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        PneumaticNetworkManager.addReceiver(level, tubeB, dest, chestB, Direction.EAST, endpoint);
        check(PneumaticNetworkManager.send(level, tubeA, source, chestA, painted, Direction.EAST, 0, 0, 10, 0)
                && dest.getItem(0).getCount() == 32, problems, cases, "paintable compressor transfers like a pneumatic tube");
    }

    private static void use(ServerLevel level, FakePlayer player, ItemStack stack, BlockState state, BlockHitResult hit) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        state.useItemOn(stack, level, player, InteractionHand.MAIN_HAND, hit);
    }

    /// TileEntityPneumoStorage* + StackCache ///
    private static void storage(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos clutterPos = pad(level);
        BlockPos importerPos = clutterPos.east();
        BlockPos exporterPos = importerPos.east();
        BlockPos accessPos = exporterPos.east();
        level.setBlock(clutterPos, HbmBlocks.PNEUMATIC_STORAGE_CLUTTER.get().defaultBlockState(), 3);
        level.setBlock(importerPos, HbmBlocks.PNEUMATIC_STORAGE_IMPORTER.get().defaultBlockState(), 3);
        level.setBlock(exporterPos, HbmBlocks.PNEUMATIC_STORAGE_EXPORTER.get().defaultBlockState(), 3);
        level.setBlock(accessPos, HbmBlocks.PNEUMATIC_STORAGE_ACCESS.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(clutterPos) instanceof PneumaticStorageBlockEntity clutter)
                || !(level.getBlockEntity(importerPos) instanceof PneumaticStorageBlockEntity importer)
                || !(level.getBlockEntity(exporterPos) instanceof PneumaticStorageBlockEntity exporter)
                || !(level.getBlockEntity(accessPos) instanceof PneumaticStorageBlockEntity access)) {
            check(false, problems, cases, "storage test blocks missing");
            return;
        }
        clutter.airTank().setAmount(4_000);
        checkEq(PneumaticNetworkManager.available(level, exporterPos, new ItemStack(Items.STONE)), 0, problems, cases, "empty system");

        importer.setItem(0, new ItemStack(Items.STONE, 10));
        tick(level, importer);
        checkEq(importer.getItem(0).getCount(), 10, problems, cases, "importer waits one tick after setInventorySlotContents");
        tick(level, importer);
        check(importer.getItem(0).isEmpty() && clutter.getItem(0).is(Items.STONE) && clutter.getItem(0).getCount() == 10,
                problems, cases, "importer moves the stack into the clutter storage");

        ItemStack namedFilter = new ItemStack(Items.STONE, 4);
        namedFilter.set(DataComponents.CUSTOM_NAME, Component.literal("filter"));
        exporter.setExporterFilter(0, namedFilter);
        exporter.requestNow();
        ItemStack out = exporter.getItem(PneumaticStorageBlockEntity.EXPORTER_OUTPUT_START);
        check(out.is(Items.STONE) && out.getCount() == 4 && !out.has(DataComponents.CUSTOM_NAME), problems, cases,
                "exporter pulls the NBT-less item of the filter, filter count 4: " + out);
        checkEq(clutter.getItem(0).getCount(), 6, problems, cases, "clutter keeps 6 stone");

        exporter.cycleRequestMode();
        exporter.cycleRequestMode(); // MODE_FULL_REQUEST
        exporter.setExporterFilter(1, new ItemStack(Items.DIRT, 1));
        exporter.requestNow();
        checkEq(exporter.getItem(PneumaticStorageBlockEntity.EXPORTER_OUTPUT_START).getCount(), 4, problems, cases,
                "full request: a missing item blocks every slot");
        exporter.setExporterFilter(1, ItemStack.EMPTY);
        exporter.requestNow();
        checkEq(exporter.getItem(PneumaticStorageBlockEntity.EXPORTER_OUTPUT_START).getCount(), 8, problems, cases,
                "full request pulls all filters at once");

        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.setPos(accessPos.getX() + 0.5D, accessPos.getY() + 1, accessPos.getZ() + 0.5D);
        PneumaticStorageMenu menu = new PneumaticStorageMenu(0, player.getInventory(), access);
        menu.handleAccessClick(player, 1, new ItemStack(Items.STONE));
        check(menu.getCarried().is(Items.STONE) && menu.getCarried().getCount() == 1, problems, cases, "access right click takes one");
        menu.handleAccessClick(player, 0, new ItemStack(Items.STONE));
        checkEq(menu.getCarried().getCount(), 2, problems, cases, "access left click adds the rest (2 stone left)");
        menu.handleAccessClick(player, 0, ItemStack.EMPTY);
        check(menu.getCarried().isEmpty() && clutter.getItem(0).getCount() == 2, problems, cases,
                "access left click on an empty grid slot deposits the held stack");
        menu.handleAccessClick(player, 2, new ItemStack(Items.STONE));
        int inInventory = player.getInventory().countItem(Items.STONE);
        checkEq(inInventory, 2, problems, cases, "access shift click moves the stack into the inventory");
        player.getInventory().clearContent();

        clutter.setItem(0, new ItemStack(Items.STONE, 5));
        clutter.airTank().setAmount(0);
        checkEq(PneumaticNetworkManager.available(level, exporterPos, new ItemStack(Items.STONE)), 0, problems, cases,
                "a storage without compressed air is unavailable");
        clutter.airTank().setAmount(4_000);
        tick(level, clutter);
        checkEq(clutter.airTank().amount(), 4_000 - (4_000 * 9 / 4_000 + 1), problems, cases, "air consumption ceil(fill*9/max)+1");
        BlockPos far = clutterPos.east(11);
        check(!clutter.pneumaticAvailableFrom(far) && clutter.pneumaticAvailableFrom(clutterPos.east(10)), problems, cases,
                "1 PU storage range 10");
        check(HbmFluids.byName("air").isPresent(), problems, cases, "air fluid registered");
    }

    /// TileEntityMachineAutocrafter ///
    private static void autocrafter(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = pad(level);
        level.setBlock(pos, HbmBlocks.MACHINE_AUTOCRAFTER.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity crafter)) {
            check(false, problems, cases, "autocrafter missing");
            return;
        }
        crafter.setAutocrafterTemplate(0, new ItemStack(Items.OAK_LOG));
        check(crafter.autocrafterRecipeCount() >= 1 && crafter.getItem(9).is(Items.OAK_PLANKS) && crafter.getItem(9).getCount() == 4,
                problems, cases, "template oak log: preview 4 oak planks, recipes " + crafter.autocrafterRecipeCount());
        check(crafter.canPlaceItem(10, new ItemStack(Items.OAK_LOG)) && !crafter.canPlaceItem(11, new ItemStack(Items.OAK_LOG))
                        && !crafter.canPlaceItem(10, new ItemStack(Items.OAK_LOG, 5)),
                problems, cases, "isItemValidForSlot: filter slot only, at most 4");
        crafter.setItem(10, new ItemStack(Items.OAK_LOG, 2));
        crafter.menuData().set(0, 150);
        LegacyMachineBlockEntity.tick(level, pos, level.getBlockState(pos), crafter);
        check(crafter.getItem(19).is(Items.OAK_PLANKS) && crafter.getItem(19).getCount() == 4 && crafter.getItem(10).getCount() == 1,
                problems, cases, "one craft per tick: " + crafter.getItem(19) + " / " + crafter.getItem(10));
        checkEq(crafter.energyStored(), 50, problems, cases, "consumption 100 HE per craft");
        LegacyMachineBlockEntity.tick(level, pos, level.getBlockState(pos), crafter);
        checkEq(crafter.getItem(19).getCount(), 4, problems, cases, "no craft below 100 HE");
        checkEq(crafter.energyCapacity(), 10_000, problems, cases, "maxPower = consumption * 100");
    }

    private static void tick(ServerLevel level, PneumaticStorageBlockEntity storage) {
        PneumaticStorageBlockEntity.tick(level, storage.getBlockPos(), storage.getBlockState(), storage);
    }

    private static void check(boolean condition, List<String> problems, int[] cases, String message) {
        cases[0]++;
        if (!condition) problems.add("V2: " + message);
    }

    private static void checkEq(long actual, long expected, List<String> problems, int[] cases, String message) {
        check(actual == expected, problems, cases, message + ": " + actual + " expected " + expected);
    }

    private static BlockPos pad(ServerLevel level) {
        int index = site++;
        BlockPos pos = new BlockPos(BASE_X + (index % 8) * 32, BASE_Y, BASE_Z + (index / 8) * 32);
        for (int cx = (pos.getX() - 8) >> 4; cx <= (pos.getX() + 24) >> 4; cx++) {
            for (int cz = (pos.getZ() - 8) >> 4; cz <= (pos.getZ() + 8) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
                FORCED.add(new int[] {cx, cz});
            }
        }
        for (int dx = -4; dx <= 16; dx++) for (int dy = -8; dy <= 6; dy++) for (int dz = -4; dz <= 4; dz++) {
            level.setBlock(pos.offset(dx, dy, dz), dy < -6 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        return pos;
    }
}
