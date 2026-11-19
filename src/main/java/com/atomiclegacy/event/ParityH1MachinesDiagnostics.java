package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.EnergyCableBlock;
import com.atomiclegacy.inventory.recipes.BreederReactorRecipe;
import com.atomiclegacy.items.LegacyVariantItem;
import com.atomiclegacy.power.PowerEndpoint;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmItems;
import com.atomiclegacy.registry.HbmRecipeTypes;
import com.atomiclegacy.tileentity.BreederReactorBlockEntity;
import com.atomiclegacy.tileentity.IndustrialGeneratorBlockEntity;
import com.atomiclegacy.tileentity.LegacyBatteryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * [parity h1 2026-11-19: diagnostics] machine_industrial_generator (MachineIGenerator / TileEntityMachineIGenerator /
 * RenderIGenerator), machine_reactor (TileEntityMachineReactorBreeding slot rules and canProcess) and the
 * legacy MachineBattery blocks joining cable networks (agreed deviation: TilePortShapes.solderer bug fixed).
 * Counted into checked_parity_h1_cases together with the older ParityH1Diagnostics.
 */
final class ParityH1MachinesDiagnostics {
    private static final BlockPos BASE = new BlockPos(11_000, 200, 11_000);

    private ParityH1MachinesDiagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        int cx = BASE.getX() >> 4;
        int cz = BASE.getZ() >> 4;
        try {
            level.setChunkForced(cx, cz, true);
            level.getChunk(cx, cz);
            for (int dx = -1; dx < 12; dx++) {
                level.setBlock(BASE.offset(dx, -1, 0), Blocks.STONE.defaultBlockState(), 2 | 16);
            }
            industrialGenerator(level, problems, cases);
            breeder(level, problems, cases);
            batteries(level, problems, cases);
        } catch (Exception exception) {
            problems.add("parity h1m: diagnostics threw " + exception);
            AtomicLegacy.LOGGER.error("parity h1m diagnostics", exception);
        } finally {
            for (int dx = -1; dx < 12; dx++) {
                for (int dy = -1; dy <= 3; dy++) {
                    level.setBlock(BASE.offset(dx, dy, 0), Blocks.AIR.defaultBlockState(), 2 | 16);
                }
            }
            for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(BASE).inflate(16, 8, 8))) entity.discard();
            level.setChunkForced(cx, cz, false);
        }
        return cases[0];
    }

    private static void industrialGenerator(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos pos = BASE;
        BlockState state = HbmBlocks.MACHINE_INDUSTRIAL_GENERATOR.get().defaultBlockState();
        level.setBlock(pos, state, 3);
        BlockEntity be = level.getBlockEntity(pos);
        check(problems, cases, be instanceof IndustrialGeneratorBlockEntity && !(be instanceof MenuProvider)
                        && !(be instanceof net.minecraft.world.Container),
                "industrial generator: inventory-less TileEntityMachineIGenerator without GUI");
        check(problems, cases, state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED, "industrial generator: RenderIGenerator (BER)");
        check(problems, cases, state.getDestroySpeed(level, pos) == 5.0F
                        && HbmBlocks.MACHINE_INDUSTRIAL_GENERATOR.get().getExplosionResistance() == 6.0F,
                "industrial generator: hardness 5, resistance 10*3/5");
        check(problems, cases, "machine".equals(tab("machine_industrial_generator")), "industrial generator: machine tab");
    }

    private static void breeder(ServerLevel level, List<String> problems, int[] cases) throws ReflectiveOperationException {
        BlockPos pos = BASE.offset(3, 0, 0);
        level.setBlock(pos, HbmBlocks.MACHINE_REACTOR.get().defaultBlockState(), 2 | 16);
        if (!(level.getBlockEntity(pos) instanceof BreederReactorBlockEntity breeder)) {
            check(problems, cases, false, "breeder block entity");
            return;
        }
        // isItemValidForSlot: i == 0 (any item); canExtractItem: i == 1
        ItemStack dirt = new ItemStack(Items.DIRT);
        check(problems, cases, breeder.canPlaceItem(0, dirt) && !breeder.canPlaceItem(1, dirt)
                        && breeder.canPlaceItemThroughFace(0, dirt, null)
                        && breeder.canTakeItemThroughFace(1, dirt, Direction.UP)
                        && !breeder.canTakeItemThroughFace(0, dirt, Direction.UP),
                "breeder slot rules");
        var progress = BreederReactorBlockEntity.class.getDeclaredField("progress");
        progress.setAccessible(true);
        progress.setFloat(breeder, 0.5F);
        breeder.setItem(0, LegacyVariantItem.stackFor(HbmItems.ROD.get(), "co"));
        breeder.removeItem(0, 1);
        breeder.setItem(0, LegacyVariantItem.stackFor(HbmItems.ROD.get(), "co"));
        check(problems, cases, breeder.progress() == 0.5F, "breeder: slot changes keep progress (" + breeder.progress() + ")");
        var recipe = level.getRecipeManager().getRecipeFor(HbmRecipeTypes.BREEDER_REACTOR.get(),
                new BreederReactorRecipe.Input(breeder.getItem(0)), level);
        if (recipe.isEmpty()) {
            check(problems, cases, false, "breeder: rod co recipe");
            return;
        }
        var flux = BreederReactorBlockEntity.class.getDeclaredField("flux");
        flux.setAccessible(true);
        var canProcess = BreederReactorBlockEntity.class.getDeclaredMethod("canProcess", BreederReactorRecipe.class);
        canProcess.setAccessible(true);
        BreederReactorRecipe value = recipe.get().value();
        flux.setInt(breeder, value.flux());
        ItemStack out = value.result().copy();
        out.setCount(63);
        breeder.setItem(1, out);
        boolean at63 = (boolean) canProcess.invoke(breeder, value);
        breeder.getItem(1).setCount(64);
        boolean at64 = (boolean) canProcess.invoke(breeder, value);
        flux.setInt(breeder, value.flux() - 1);
        breeder.getItem(1).setCount(1);
        boolean lowFlux = (boolean) canProcess.invoke(breeder, value);
        check(problems, cases, value.flux() == 100 && at63 && !at64 && !lowFlux,
                "breeder canProcess: flux >= recipe flux (100), output < max stack");
        breeder.clearContent();
    }

    private static void batteries(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos source = BASE.offset(6, 0, 0);
        BlockPos cable = source.east();
        BlockPos sink = source.east(2);
        level.setBlock(source, HbmBlocks.MACHINE_BATTERY.get().defaultBlockState(), 3);
        level.setBlock(sink, HbmBlocks.MACHINE_BATTERY.get().defaultBlockState(), 3);
        level.setBlock(cable, HbmBlocks.RED_CABLE.get().defaultBlockState(), 3);
        EnergyCableBlock.refreshConnections(level, cable);
        BlockState cableState = level.getBlockState(cable);
        check(problems, cases, cableState.getValue(EnergyCableBlock.WEST) && cableState.getValue(EnergyCableBlock.EAST),
                "cable draws sleeves to both legacy batteries");
        if (!(level.getBlockEntity(source) instanceof LegacyBatteryBlockEntity out)
                || !(level.getBlockEntity(sink) instanceof LegacyBatteryBlockEntity in)) {
            check(problems, cases, false, "legacy battery block entities");
            return;
        }
        out.setPower(600_000L);
        out.cycleLowMode();
        out.cycleLowMode(); // redLow 2 = output
        check(problems, cases, out.relevantMode() == LegacyBatteryBlockEntity.MODE_OUTPUT && in.relevantMode() == LegacyBatteryBlockEntity.MODE_INPUT,
                "modes: output / input");
        check(problems, cases, out.getAvailableOutput() == 1_000_000L / 600L && out.getRequestedInput() == 0L
                        && in.getRequestedInput() == 1_000_000L / 200L && in.getAvailableOutput() == 0L,
                "provider speed max/600, receiver speed max/200");
        for (int i = 0; i < 4; i++) {
            LegacyBatteryBlockEntity.tick(level, source, level.getBlockState(source), out);
            LegacyBatteryBlockEntity.tick(level, sink, level.getBlockState(sink), in);
            PowerNetworkManager.updateNow(level);
        }
        check(problems, cases, in.getPower() > 0L && out.getPower() < 600_000L && in.getPower() + out.getPower() == 600_000L,
                "power flows over the cable (" + out.getPower() + " -> " + in.getPower() + ")");
        // redstone switches the sink to redHigh (default 2 = output): it no longer receives
        level.setBlock(sink.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        check(problems, cases, in.relevantMode() == LegacyBatteryBlockEntity.MODE_OUTPUT && in.getRequestedInput() == 0L,
                "redstone selects redHigh");
        level.setBlock(sink.above(), Blocks.AIR.defaultBlockState(), 3);
        in.cyclePriority();
        check(problems, cases, in.getPowerPriority() == PowerEndpoint.ConnectionPriority.NORMAL, "GUI priority is the network priority");
    }

    private static String tab(String id) {
        try (var stream = com.atomiclegacy.registry.HbmCreativeTabs.class.getResourceAsStream("creative-tabs.properties")) {
            var properties = new java.util.Properties();
            properties.load(java.util.Objects.requireNonNull(stream));
            return properties.getProperty(id);
        } catch (java.io.IOException exception) {
            return null;
        }
    }

    private static void check(List<String> problems, int[] cases, boolean ok, String message) {
        cases[0]++;
        if (!ok) problems.add("parity h1m: " + message);
    }
}
