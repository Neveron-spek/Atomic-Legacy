package com.atomiclegacy.event;

import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.RadioTorchBlock;
import com.atomiclegacy.blocks.RbmkComponentBlock;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.rtty.HbmRttySystem;
import com.atomiclegacy.rtty.RttyFunctionException;
import com.atomiclegacy.rtty.RttyInteractive;
import com.atomiclegacy.tileentity.RadioTorchBlockEntity;
import com.atomiclegacy.tileentity.RbmkComponentBlockEntity;
import com.atomiclegacy.tileentity.RbmkPanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * [parity T2] RBMK control periphery and Redstone-over-Radio: IRORInteractive parsing, RTTYSystem queue sums,
 * IROR values/functions of TileEntityRBMKRod/Control/ControlManual/Boiler/Heater, proxy-only top dummies,
 * console AZ-5, the RoR panels (gauge, keypad, lever, indicator, numitron, graph, terminal) and their recipes,
 * hardness and resistance. The pad is at x = 30000. Not covered: GUI drawing, sounds, particles, rendering.
 */
final class ParityT2Diagnostics {
    private static final int BASE_X = 30_000;
    private static final int BASE_Y = 120;
    private static final int BASE_Z = 30_400;
    private static final List<int[]> FORCED = new ArrayList<>();

    private ParityT2Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        try {
            parsing(problems, cases);
            queue(level, problems, cases);
            properties(problems, cases);
            recipes(server, problems, cases);
            BlockPos pos = pad(level);
            columns(level, pos, problems, cases);
            panels(level, pos.offset(0, 0, 6), problems, cases);
        } catch (RuntimeException e) {
            AtomicLegacy.LOGGER.error("Parity T2 diagnostics crashed", e);
            problems.add("T2 diagnostics crashed: " + e);
        } finally {
            for (int[] chunk : FORCED) level.setChunkForced(chunk[0], chunk[1], false);
            FORCED.clear();
        }
        return cases[0];
    }

    /// IRORInteractive.getCommand/getParams/parseInt/parseLong/parseBool ///
    private static void parsing(List<String> problems, int[] cases) {
        check("setrods".equals(RttyInteractive.command("SetRods!50")), problems, cases, "command is lower-cased");
        check(Arrays.equals(RttyInteractive.params("setrods!50"), new String[] {"50"}), problems, cases, "single parameter");
        check(RttyInteractive.params("clear!").length == 0, problems, cases, "trailing separator yields no parameters (String.split)");
        check(Arrays.equals(RttyInteractive.params("write!a:b:"), new String[] {"a", "b"}), problems, cases, "trailing empty parameters dropped");
        check(Arrays.equals(RttyInteractive.params("a!b!"), new String[] {"b"}), problems, cases, "trailing name separator dropped");
        check(throwsFormat(() -> RttyInteractive.command("!")), problems, cases, "lone separator throws");
        check(throwsFormat(() -> RttyInteractive.command("a!b!c")), problems, cases, "two separators throw");
        checkEq(RttyInteractive.parseInt("2.5"), 3, problems, cases, "parseInt rounds decimals");
        checkEq(RttyInteractive.parseInt("Infinity"), -1, problems, cases, "parseInt casts Math.round(Infinity) to int like 1.7.10");
        check(throwsFormat(() -> RttyInteractive.parseInt("101", 0, 100)), problems, cases, "parseInt range");
        checkEq(RttyInteractive.parseLong("12.4", 0, 100), 12, problems, cases, "parseLong");
        check(RttyInteractive.parseBool("ON") && !RttyInteractive.parseBool("0"), problems, cases, "parseBool");
        check(throwsFormat(() -> RttyInteractive.parseBool("yes")), problems, cases, "parseBool rejects other words");
    }

    private static boolean throwsFormat(Runnable action) {
        try {
            action.run();
            return false;
        } catch (RttyFunctionException expected) {
            return true;
        }
    }

    /// RTTYSystem.broadcast sums numeric signals of one tick with long arithmetic ///
    private static void queue(ServerLevel level, List<String> problems, int[] cases) {
        HbmRttySystem.broadcast(level, "t2sum", 40);
        HbmRttySystem.broadcast(level, "t2sum", "2");
        HbmRttySystem.broadcast(level, "t2wrap", Long.MAX_VALUE);
        HbmRttySystem.broadcast(level, "t2wrap", 1);
        HbmRttySystem.broadcast(level, "t2text", "1.5");
        HbmRttySystem.broadcast(level, "t2text", "2");
        publish(level);
        check("42".equals(signal(level, "t2sum")), problems, cases, "numeric signals are summed: " + signal(level, "t2sum"));
        check(Long.toString(Long.MIN_VALUE).equals(signal(level, "t2wrap")), problems, cases, "long sum wraps: " + signal(level, "t2wrap"));
        check("2".equals(signal(level, "t2text")), problems, cases, "non-long signal is replaced");
    }

    /// RBMKBase / RBMKMiniPanelBase 3/30, autoloader/loader/inlet/outlet 50/60; resistance stored as 1.7.10 effective r*3/5 = 18/36 [parity T3] ///
    private static void properties(List<String> problems, int[] cases) {
        Object[][] rows = {
                {HbmBlocks.RBMK_BLANK.get(), 3F, 18F}, {HbmBlocks.RBMK_ROD.get(), 3F, 18F},
                {HbmBlocks.RBMK_CONTROL.get(), 3F, 18F}, {HbmBlocks.RBMK_BOILER.get(), 3F, 18F},
                {HbmBlocks.RBMK_CONSOLE.get(), 3F, 18F}, {HbmBlocks.RBMK_CRANE_CONSOLE.get(), 3F, 18F},
                {HbmBlocks.RBMK_DISPLAY.get(), 3F, 18F}, {HbmBlocks.RBMK_DISPLAY_BLANK.get(), 3F, 18F},
                {HbmBlocks.RBMK_GAUGE.get(), 3F, 18F}, {HbmBlocks.RBMK_GRAPH.get(), 3F, 18F},
                {HbmBlocks.RBMK_INDICATOR.get(), 3F, 18F}, {HbmBlocks.RBMK_KEY_PAD.get(), 3F, 18F},
                {HbmBlocks.RBMK_LEVER.get(), 3F, 18F}, {HbmBlocks.RBMK_NUMITRON.get(), 3F, 18F},
                {HbmBlocks.RBMK_TERMINAL.get(), 3F, 18F}, {HbmBlocks.RBMK_AUTOLOADER.get(), 50F, 36F},
                {HbmBlocks.RBMK_LOADER.get(), 50F, 36F}, {HbmBlocks.RBMK_STEAM_INLET.get(), 50F, 36F},
        };
        for (Object[] row : rows) {
            Block block = (Block) row[0];
            String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
            check(block.defaultDestroyTime() == (float) row[1], problems, cases, name + " hardness " + block.defaultDestroyTime());
            check(block.getExplosionResistance() == (float) row[2], problems, cases, name + " resistance " + block.getExplosionResistance());
            check(!block.defaultBlockState().hasAnalogOutputSignal(), problems, cases, name + " has no comparator output");
        }
    }

    /// CraftingManager lines 775-783 ///
    private static void recipes(MinecraftServer server, List<String> problems, int[] cases) {
        String[][] rows = {
                {"rbmk_display_blank", "8", "B|D", "B=#c:ingots/boron", "D=atomiclegacy:concrete_asbestos"},
                {"rbmk_display", "1", "C|B", "C=atomiclegacy:crt_display", "B=atomiclegacy:rbmk_display_blank"},
                {"rbmk_key_pad", "1", "R|C|B", "R=atomiclegacy:radio_torch_sender", "C=atomiclegacy:circuit_vacuum_tube", "B=atomiclegacy:rbmk_display_blank"},
                {"rbmk_gauge", "1", "R|C|B", "R=atomiclegacy:radio_torch_receiver", "C=atomiclegacy:circuit_vacuum_tube", "B=atomiclegacy:rbmk_display_blank"},
                {"rbmk_numitron", "1", " R |CCC| B ", "R=atomiclegacy:radio_torch_receiver", "C=atomiclegacy:circuit_numitron", "B=atomiclegacy:rbmk_display_blank"},
                {"rbmk_graph", "1", "R|C|B", "R=atomiclegacy:radio_torch_receiver", "C=atomiclegacy:crt_display", "B=atomiclegacy:rbmk_display_blank"},
                {"rbmk_lever", "1", "R|C|B", "R=atomiclegacy:radio_torch_sender", "C=#c:ingots/copper", "B=atomiclegacy:rbmk_display_blank"},
                {"rbmk_indicator", "1", "R|C|B", "R=atomiclegacy:radio_torch_receiver", "C=atomiclegacy:coil_tungsten", "B=atomiclegacy:rbmk_display_blank"},
                {"rbmk_terminal", "1", "R |CD|B ", "R=atomiclegacy:radio_torch_sender", "C=atomiclegacy:circuit_analog", "D=atomiclegacy:crt_display", "B=atomiclegacy:rbmk_display_blank"},
        };
        for (String[] row : rows) {
            RecipeHolder<?> holder = server.getRecipeManager().byKey(AtomicLegacy.id(row[0])).orElse(null);
            if (!(holder != null && holder.value() instanceof ShapedRecipe shaped)) {
                check(false, problems, cases, "recipe " + row[0] + " missing or not shaped");
                continue;
            }
            ItemStack result = shaped.getResultItem(server.registryAccess());
            check(BuiltInRegistries.ITEM.getKey(result.getItem()).getPath().equals(row[0]) && result.getCount() == Integer.parseInt(row[1]),
                    problems, cases, "recipe " + row[0] + " result");
            String[] pattern = row[2].split("\\|", -1);
            int width = pattern[0].length();
            check(shaped.getWidth() == width && shaped.getHeight() == pattern.length, problems, cases, "recipe " + row[0] + " size");
            for (int y = 0; y < pattern.length && shaped.getWidth() == width; y++) {
                for (int x = 0; x < width; x++) {
                    char c = pattern[y].charAt(x);
                    net.minecraft.world.item.crafting.Ingredient ingredient = shaped.getIngredients().get(y * width + x);
                    if (c == ' ') {
                        check(ingredient.isEmpty(), problems, cases, "recipe " + row[0] + " empty cell");
                        continue;
                    }
                    String expected = null;
                    for (int k = 3; k < row.length; k++) if (row[k].charAt(0) == c) expected = row[k].substring(2);
                    check(expected != null && matches(ingredient, expected), problems, cases, "recipe " + row[0] + " cell " + c + " = " + expected);
                }
            }
        }
    }

    private static boolean matches(net.minecraft.world.item.crafting.Ingredient ingredient, String expected) {
        if (expected.startsWith("#")) {
            net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag = net.minecraft.tags.TagKey.create(
                    net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.parse(expected.substring(1)));
            ItemStack[] items = ingredient.getItems();
            if (items.length == 0) return false;
            for (ItemStack stack : items) if (!stack.is(tag)) return false;
            return true;
        }
        ItemStack[] items = ingredient.getItems();
        return items.length == 1 && BuiltInRegistries.ITEM.getKey(items[0].getItem()).toString().equals(expected);
    }

    /// TileEntityRBMKRod/Control/ControlManual/Boiler/Heater IROR, top proxies, readers/controllers, AZ-5 ///
    private static void columns(ServerLevel level, BlockPos pos, List<String> problems, int[] cases) {
        RbmkComponentBlockEntity rod = column(level, pos, HbmBlocks.RBMK_ROD.get());
        RbmkComponentBlockEntity manual = column(level, pos.east(), HbmBlocks.RBMK_CONTROL.get());
        RbmkComponentBlockEntity auto = column(level, pos.east(2), HbmBlocks.RBMK_CONTROL_AUTO.get());
        RbmkComponentBlockEntity boiler = column(level, pos.east(3), HbmBlocks.RBMK_BOILER.get());
        RbmkComponentBlockEntity heater = column(level, pos.east(4), HbmBlocks.RBMK_HEATER.get());
        RbmkComponentBlockEntity blank = column(level, pos.west(), HbmBlocks.RBMK_BLANK.get());
        if (rod == null || manual == null || auto == null || boiler == null || heater == null || blank == null) {
            check(false, problems, cases, "RBMK test columns missing");
            return;
        }
        check(Arrays.equals(rod.rttyFunctionInfo(), new String[] {"VAL:columnheat", "VAL:rodheat", "VAL:depletion", "VAL:xenon", "VAL:fastflux", "VAL:slowflux", "VAL:flux"}),
                problems, cases, "rod function info");
        check(Arrays.equals(manual.rttyFunctionInfo(), new String[] {"VAL:extraction", "FUN:setrods!percent", "FUN:extendrods!percent"}), problems, cases, "manual control function info");
        check(Arrays.equals(auto.rttyFunctionInfo(), new String[] {"VAL:extraction"}), problems, cases, "auto control function info");
        check(Arrays.equals(boiler.rttyFunctionInfo(), new String[] {"VAL:feed", "VAL:steam", "VAL:consumption"}), problems, cases, "boiler function info");
        check(Arrays.equals(heater.rttyFunctionInfo(), new String[] {"VAL:in", "VAL:out"}), problems, cases, "heater function info");
        check("0".equals(rod.provideRttyValue("VAL:columnheat")), problems, cases, "rod columnheat (int) heat");
        check(rod.provideRttyValue("VAL:rodheat") == null, problems, cases, "rodheat is null without a rod");
        check("0".equals(rod.provideRttyValue("VAL:flux")), problems, cases, "rod flux");
        check("0".equals(manual.provideRttyValue("VAL:extraction")), problems, cases, "control extraction");
        check("0".equals(boiler.provideRttyValue("VAL:feed")) && "0".equals(boiler.provideRttyValue("VAL:consumption")), problems, cases, "boiler values");
        check("0".equals(heater.provideRttyValue("VAL:in")), problems, cases, "heater in");
        check(blank.rttyFunctionInfo().length == 0 && !blank.rttyProvidesValues() && !blank.rttyInteractive(), problems, cases, "blank column is no ROR component");
        check(manual.rttyInteractive() && !auto.rttyInteractive() && !rod.rttyInteractive(), problems, cases, "only manual control rods are interactive");

        manual.runRttyFunction("FUN:setrods", new String[] {"50"});
        check(Math.abs(manual.targetControlLevel() - 0.5D) < 1e-9, problems, cases, "setrods!50 -> target 0.5");
        manual.runRttyFunction("FUN:extendrods", new String[] {"-20"});
        check(Math.abs(manual.targetControlLevel() - 0.3D) < 1e-9, problems, cases, "extendrods!-20 -> target 0.3");
        check(throwsFormat(() -> manual.runRttyFunction("FUN:setrods", new String[] {"101"})), problems, cases, "setrods rejects 101");
        auto.runRttyFunction("FUN:setrods", new String[] {"50"});

        int height = RbmkComponentBlock.columnHeight(level);
        BlockPos top = pos.east().above(height - 1);
        BlockPos lower = pos.east().above(1);
        check(manual.hasRorProxyAt(top) && !manual.hasRorProxyAt(lower), problems, cases, "only the top dummy is a proxy");
        check(!auto.hasRorProxyAt(pos.east(2).above(height - 1)), problems, cases, "automatic control rods have no proxy");

        // RadioTorchController on the top proxy of the manual rod
        BlockPos ctrlPos = top.north();
        level.setBlock(ctrlPos, HbmBlocks.RADIO_TORCH_CONTROLLER.get().defaultBlockState().setValue(RadioTorchBlock.FACING, Direction.NORTH), 3);
        BlockPos readPos = pos.above(height - 1).north();
        level.setBlock(readPos, HbmBlocks.RADIO_TORCH_READER.get().defaultBlockState().setValue(RadioTorchBlock.FACING, Direction.NORTH), 3);
        check(level.getBlockState(ctrlPos).canSurvive(level, ctrlPos), problems, cases, "controller may stand on the top proxy");
        check(level.getBlockState(readPos).canSurvive(level, readPos), problems, cases, "reader may stand on the rod's top proxy");
        check(!level.getBlockState(ctrlPos).canSurvive(level, ctrlPos.below()), problems, cases, "controller may not stand on a lower dummy");
        BlockPos autoSide = pos.east(2).north();
        check(!HbmBlocks.RADIO_TORCH_CONTROLLER.get().defaultBlockState().setValue(RadioTorchBlock.FACING, Direction.NORTH).canSurvive(level, autoSide),
                problems, cases, "controller may not stand on an automatic control rod");
        check(HbmBlocks.RADIO_TORCH_CONTROLLER.get().defaultBlockState().setValue(RadioTorchBlock.FACING, Direction.NORTH).canSurvive(level, pos.east().north()),
                problems, cases, "controller may stand on a manual control rod core");
        check(!HbmBlocks.RADIO_TORCH_READER.get().defaultBlockState().setValue(RadioTorchBlock.FACING, Direction.NORTH).canSurvive(level, blank.getBlockPos().north()),
                problems, cases, "reader cannot stand on a blank column");
        if (level.getBlockEntity(ctrlPos) instanceof RadioTorchBlockEntity controller
                && level.getBlockEntity(readPos) instanceof RadioTorchBlockEntity reader) {
            controller.setChannel("t2rods");
            HbmRttySystem.broadcast(level, "t2rods", "setrods!70");
            publish(level);
            RadioTorchBlockEntity.tick(level, ctrlPos, level.getBlockState(ctrlPos), controller);
            check(Math.abs(manual.targetControlLevel() - 0.7D) < 1e-9, problems, cases, "controller runs setrods!70 through the proxy: " + manual.targetControlLevel());
            reader.setReaderEntry(0, "t2heat", "columnheat");
            RadioTorchBlockEntity.tick(level, readPos, level.getBlockState(readPos), reader);
            publish(level);
            check("0".equals(signal(level, "t2heat")), problems, cases, "reader broadcasts columnheat: " + signal(level, "t2heat"));
        } else {
            check(false, problems, cases, "radio torch block entities missing");
        }

        // AZ-5 from the console
        BlockPos consolePos = pos.offset(0, 0, -4);
        level.setBlock(consolePos, HbmBlocks.RBMK_CONSOLE.get().defaultBlockState(), 3);
        HbmBlocks.RBMK_CONSOLE.get().setPlacedBy(level, consolePos, level.getBlockState(consolePos), null, ItemStack.EMPTY);
        if (level.getBlockEntity(consolePos) instanceof RbmkComponentBlockEntity console) {
            console.setConsoleTarget(pos);
            // scanConsole runs every 10 game ticks in tickConsole; call it directly
            invokeScan(console, level);
            int center = RbmkComponentBlockEntity.CONSOLE_COLUMN_COUNT / 2;
            check(console.consoleKind(center) == RbmkComponentBlock.Kind.FUEL_ROD.ordinal(), problems, cases, "console sees the rod at its target: " + console.consoleKind(center));
            check(console.consoleKind(center + 1) == RbmkComponentBlock.Kind.CONTROL.ordinal(), problems, cases, "console sees the control rod east of the target");
            console.applyConsoleControl(com.atomiclegacy.network.RbmkConsoleControlPayload.ACTION_AZ5, 0, new int[0]);
            check(manual.targetControlLevel() == 0.0D, problems, cases, "AZ-5 sets manual control rods to 0");
        } else {
            check(false, problems, cases, "console missing");
        }
    }

    /// TileEntityRBMKGauge/KeyPad/Lever/Indicator/Numitron/Graph/Terminal ///
    private static void panels(ServerLevel level, BlockPos pos, List<String> problems, int[] cases) {
        Player fake = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        fake.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        RbmkPanelBlockEntity gauge = panel(level, pos, HbmBlocks.RBMK_GAUGE.get());
        RbmkPanelBlockEntity keypad = panel(level, pos.east(), HbmBlocks.RBMK_KEY_PAD.get());
        RbmkPanelBlockEntity lever = panel(level, pos.east(2), HbmBlocks.RBMK_LEVER.get());
        RbmkPanelBlockEntity indicator = panel(level, pos.east(3), HbmBlocks.RBMK_INDICATOR.get());
        RbmkPanelBlockEntity numitron = panel(level, pos.east(4), HbmBlocks.RBMK_NUMITRON.get());
        RbmkPanelBlockEntity graph = panel(level, pos.east(5), HbmBlocks.RBMK_GRAPH.get());
        RbmkPanelBlockEntity terminal = panel(level, pos.east(6), HbmBlocks.RBMK_TERMINAL.get());
        if (gauge == null || keypad == null || lever == null || indicator == null || numitron == null || graph == null || terminal == null) {
            check(false, problems, cases, "panel block entities missing (not RbmkPanelBlockEntity)");
            return;
        }
        level.setBlock(pos.west(), HbmBlocks.RBMK_DISPLAY.get().defaultBlockState(), 3);
        check(level.getBlockEntity(pos.west()) instanceof RbmkComponentBlockEntity display && !(display instanceof RbmkPanelBlockEntity),
                problems, cases, "rbmk_display keeps the base entity");
        // defaults of the unit constructors
        check(gauge.gauges[0].color == 0x800000 && gauge.gauges[3].color == 0x000080 && "Gauge 1".equals(gauge.gauges[0].label)
                && gauge.gauges[0].max == 100 && !gauge.gauges[0].active, problems, cases, "gauge defaults");
        check(keypad.keys[2].color == 0x0080ff && "Button 4".equals(keypad.keys[3].label), problems, cases, "keypad defaults");
        check(indicator.indicators[1].color == 0xffff00 && "Indicator 6".equals(indicator.indicators[5].label), problems, cases, "indicator defaults");
        check(numitron.displays[0].activeDigits == 0b01111111 && numitron.displays[0].shortenNumber && numitron.displays[0].leadingZeroes, problems, cases, "numitron defaults");
        check("Lever 2".equals(lever.levers[1].label) && "Graph 1".equals(graph.graphs[0].label), problems, cases, "lever/graph defaults");

        CompoundTag data = new CompoundTag();
        data.putByte("active", (byte) 1);
        data.putByte("polling", (byte) 0);
        data.putInt("color0", 0x1234567);
        data.putString("label0", "Heat");
        data.putString("rtty0", "t2val");
        data.putInt("min0", 0);
        data.putInt("max0", 50);
        gauge.receiveControl(fake, data);
        check(gauge.gauges[0].active && !gauge.gauges[1].active && gauge.gauges[0].color == 0xffffff && gauge.gauges[0].max == 50
                && gauge.gauges[1].max == 0, problems, cases, "gauge receiveControl (colour clamp, missing max -> 0)");
        CompoundTag ind = new CompoundTag();
        ind.putByte("active", (byte) 3);
        ind.putString("rtty0", "t2val");
        ind.putString("rtty1", "t2val");
        ind.putInt("min0", 10);
        ind.putInt("max0", 50);
        ind.putInt("min1", 50);
        ind.putInt("max1", 10);
        indicator.receiveControl(fake, ind);
        CompoundTag num = new CompoundTag();
        num.putByte("active", (byte) 1);
        num.putString("rtty0", "t2val");
        numitron.receiveControl(fake, num);
        check(!numitron.displays[0].shortenNumber && !numitron.displays[0].leadingZeroes, problems, cases, "numitron flags come from the GUI bit masks");
        CompoundTag gr = new CompoundTag();
        gr.putByte("active", (byte) 1);
        gr.putString("rtty0", "t2val");
        gr.putLong("min0", 90);
        gr.putLong("max0", 10);
        graph.receiveControl(fake, gr);
        check(graph.graphs[0].minBound && graph.graphs[0].min == 10 && graph.graphs[0].max == 90, problems, cases, "graph swaps min/max");

        HbmRttySystem.broadcast(level, "t2val", "42");
        publish(level);
        gauge.gauges[0].update();
        indicator.indicators[0].update();
        indicator.indicators[1].update();
        numitron.displays[0].update();
        graph.graphs[0].update();
        checkEq(gauge.gauges[0].value, 42, problems, cases, "gauge reads the channel");
        check(indicator.indicators[0].light && !indicator.indicators[1].light, problems, cases, "indicator bounds and inverted bounds");
        checkEq(numitron.displays[0].value, 42, problems, cases, "numitron reads the channel");
        checkEq(graph.graphs[0].values[29], 42, problems, cases, "graph pushes the value");

        CompoundTag key = new CompoundTag();
        key.putByte("active", (byte) 1);
        key.putString("rtty0", "t2key");
        key.putString("cmd0", "hello");
        keypad.receiveControl(fake, key);
        keypad.keys[0].click();
        check(keypad.keys[0].isPressed && keypad.keys[0].clickTimer == 7, problems, cases, "keypad press");
        publish(level);
        check("hello".equals(signal(level, "t2key")), problems, cases, "keypad sends its command");

        CompoundTag lv = new CompoundTag();
        lv.putByte("active", (byte) 1);
        lv.putString("rtty0", "t2lever");
        lv.putString("cmdOn0", "on");
        lv.putString("cmdOff0", "off");
        lever.receiveControl(fake, lv);
        lever.levers[0].click();
        for (int i = 0; i < 9; i++) lever.levers[0].update();
        check(lever.levers[0].flipProgress < 1F, problems, cases, "lever still moving after 9 ticks");
        lever.levers[0].update();
        publish(level);
        check(lever.levers[0].flipProgress == 1F && "on".equals(signal(level, "t2lever")), problems, cases, "lever sends commandOn after 10 ticks");

        terminal.eval("send x");
        check("Cannot send - no channel set".equals(terminal.history[0]), problems, cases, "terminal without channel");
        terminal.eval("chan t2term");
        terminal.eval("send abc");
        check("Sent signal on t2term".equals(terminal.history[0]) && "send abc".equals(terminal.history[1]), problems, cases, "terminal send history");
        terminal.eval("horse");
        check("Horse.".equals(terminal.history[0]), problems, cases, "terminal horse");
        publish(level);
        check("abc".equals(signal(level, "t2term")), problems, cases, "terminal broadcasts");
        terminal.runRttyFunction("FUN:set3", new String[] {"a", "b"});
        check("a b".equals(terminal.history[2]), problems, cases, "terminal set<line#> joins parameters with spaces");
        terminal.runRttyFunction("FUN:clear", new String[0]);
        check("".equals(terminal.history[0]) && "".equals(terminal.history[16]), problems, cases, "terminal clear");
        check(terminal.rttyInteractive() && !gauge.rttyInteractive() && !gauge.rttyProvidesValues(), problems, cases, "only the terminal is interactive");
        check(terminal.rttyFunctionInfo().length == 4 && "FUN:set<line#>!text".equals(terminal.rttyFunctionInfo()[2]), problems, cases, "terminal function info");
        terminal.eval("start ping");
        terminal.tickPanel(level);
        publish(level);
        check("ping".equals(signal(level, "t2term")), problems, cases, "terminal repeats its signal every tick");

        // NBT round trip of the panel state
        CompoundTag saved = gauge.saveWithoutMetadata(level.registryAccess());
        check(saved.getBoolean("active0") && "t2val".equals(saved.getString("rtty0")) && saved.getLong("value0") == 42, problems, cases, "gauge NBT keys of 1.7.10");
        CompoundTag termSaved = terminal.saveWithoutMetadata(level.registryAccess());
        check("t2term".equals(termSaved.getString("channel")) && " ".equals(termSaved.getString("history5")), problems, cases, "terminal NBT keeps \" \" for empty lines");
    }

    /* ------------------------------------------------------------ helpers */

    private static BlockPos pad(ServerLevel level) {
        BlockPos pos = new BlockPos(BASE_X, BASE_Y, BASE_Z);
        for (int cx = (pos.getX() - 16) >> 4; cx <= (pos.getX() + 16) >> 4; cx++) {
            for (int cz = (pos.getZ() - 16) >> 4; cz <= (pos.getZ() + 16) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
                FORCED.add(new int[] {cx, cz});
            }
        }
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-8, -1, -8), pos.offset(12, 8, 10))) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof RbmkComponentBlockEntity rbmk) rbmk.clearContent();
            level.setBlock(p, p.getY() < pos.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        return pos;
    }

    private static RbmkComponentBlockEntity column(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), 3);
        block.setPlacedBy(level, pos, level.getBlockState(pos), null, ItemStack.EMPTY);
        return level.getBlockEntity(pos) instanceof RbmkComponentBlockEntity rbmk ? rbmk : null;
    }

    private static RbmkPanelBlockEntity panel(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), 3);
        return level.getBlockEntity(pos) instanceof RbmkPanelBlockEntity panel ? panel : null;
    }

    private static void invokeScan(RbmkComponentBlockEntity console, ServerLevel level) {
        try {
            java.lang.reflect.Method scan = RbmkComponentBlockEntity.class.getDeclaredMethod("scanConsole", net.minecraft.world.level.Level.class);
            scan.setAccessible(true);
            scan.invoke(console, level);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
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
        if (!condition) problems.add("T2: " + message);
    }

    private static void checkEq(long actual, long expected, List<String> problems, int[] cases, String message) {
        check(actual == expected, problems, cases, message + ": " + actual + " expected " + expected);
    }
}
