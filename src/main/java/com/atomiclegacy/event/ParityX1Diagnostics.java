package com.atomiclegacy.event;

import com.google.gson.JsonObject;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.CustomMachineBlock;
import com.atomiclegacy.blocks.LegacyVariantBlock;
import com.atomiclegacy.config.CustomMachineConfigJSON;
import com.atomiclegacy.config.CustomMachineConfigJSON.MachineConfiguration;
import com.atomiclegacy.fluid.HbmFluidDefinition;
import com.atomiclegacy.fluid.HbmFluidNetworks;
import com.atomiclegacy.inventory.recipes.CustomMachineRecipes;
import com.atomiclegacy.inventory.recipes.CustomMachineRecipes.CustomMachineRecipe;
import com.atomiclegacy.items.CustomMachineStructureItem;
import com.atomiclegacy.power.PowerNetworkManager;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.registry.HbmFluids;
import com.atomiclegacy.tileentity.CustomMachineBlockEntity;
import com.atomiclegacy.tileentity.CustomMachinePortBlockEntity;
import com.atomiclegacy.tileentity.FluidBarrelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * [parity X1: diagnostics] Custom machines: the default hbmCustomMachines.json (machine configuration and recipes, file
 * round trip), the core crafting recipe, structure building / checking in both axis orientations, cm_port proxies,
 * power and FluidNetMK2 ports, a full paper press cycle, templates, breaking the structure, drops, the invalid
 * controller and the CM structure wand output. Site x ~ 39000.
 */
final class ParityX1Diagnostics {
    private ParityX1Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        try {
            config(problems, cases);
            recipes(problems, cases);
            coreRecipe(server, level, problems, cases);
            machine(level, problems, cases);
            east(level, problems, cases);
            invalid(level, problems, cases);
            wand(level, problems, cases);
        } catch (Exception e) {
            problems.add("X1 custom machine threw " + e);
            AtomicLegacy.LOGGER.error("X1 diagnostics", e);
        }
        return cases[0];
    }

    private static void check(boolean ok, List<String> problems, int[] cases, String what) {
        cases[0]++;
        if (!ok) problems.add("X1 " + what);
    }

    private static void eq(Object actual, Object expected, List<String> problems, int[] cases, String what) {
        check(String.valueOf(actual).equals(String.valueOf(expected)), problems, cases, what + ": " + actual + " != " + expected);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(AtomicLegacy.id(id));
    }

    private static HbmFluidDefinition water() {
        return HbmFluids.byName("water").orElseThrow();
    }

    /// configuration ///

    private static void checkPaperPress(MachineConfiguration c, List<String> problems, int[] cases, String where) {
        eq(c.recipeKey, "paperPress", problems, cases, where + " recipeKey");
        eq(c.localizedName, "Paper Press", problems, cases, where + " localizedName");
        eq(c.localization.get("de_DE"), "Papierpresse", problems, cases, where + " de_DE");
        eq(c.fluidInCount + "/" + c.fluidInCap + "/" + c.itemInCount + "/" + c.fluidOutCount + "/" + c.fluidOutCap + "/" + c.itemOutCount,
                "1/1000/1/0/0/1", problems, cases, where + " counts");
        eq(c.generatorMode + "/" + c.maxPollutionCap + "/" + c.fluxMode + "/" + c.recipeSpeedMult + "/" + c.recipeConsumptionMult + "/" + c.maxPower + "/" + c.maxHeat,
                "false/100/false/1.0/1.0/10000/0", problems, cases, where + " values");
        eq(c.components.size(), 25, problems, cases, where + " component count");
        int sheets = 0, blocks = 0, ports = 0;
        for (CustomMachineConfigJSON.ComponentDefinition comp : c.components) {
            if (comp.block == HbmBlocks.CM_SHEET.get()) sheets++;
            if (comp.block == HbmBlocks.CM_BLOCK.get()) blocks++;
            if (comp.block == HbmBlocks.CM_PORT.get()) ports++;
            check(comp.allowedMetas.size() == 1 && comp.allowedMetas.contains(0), problems, cases, where + " metas " + comp.x + "," + comp.y + "," + comp.z);
        }
        eq(sheets + "/" + blocks + "/" + ports, "7/16/2", problems, cases, where + " sheet/block/port components");
    }

    private static void config(List<String> problems, int[] cases) throws Exception {
        MachineConfiguration paper = CustomMachineConfigJSON.customMachines.get("paperPress");
        check(paper != null && CustomMachineConfigJSON.niceList.indexOf(paper) == 0, problems, cases, "default paperPress configuration is loaded first");
        if (paper != null) checkPaperPress(paper, problems, cases, "loaded");

        File temp = Files.createTempFile("hbmCustomMachines", ".json").toFile();
        temp.deleteOnExit();
        CustomMachineConfigJSON.writeDefault(temp);
        String text = Files.readString(temp.toPath(), StandardCharsets.UTF_8);
        check(text.contains("\"recipeParts\": [\"I\",[\"dict\",\"c:ingots/steel\"],\"P\",[\"dict\",\"c:plates/steel\"],\"C\",[\"item\",\"atomiclegacy:circuit_basic\"]"),
                problems, cases, "default recipeParts are written inline like the original");
        check(text.contains("{\"block\":\"atomiclegacy:cm_port\",\"x\":0,\"y\":-1,\"z\":0,\"metas\":[0]}"), problems, cases, "default components are written inline");

        List<MachineConfiguration> before = new ArrayList<>(CustomMachineConfigJSON.niceList);
        var machines = new java.util.HashMap<>(CustomMachineConfigJSON.customMachines);
        var core = new java.util.LinkedHashMap<>(CustomMachineConfigJSON.coreRecipes);
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(temp.toPath()), StandardCharsets.UTF_8)) {
            CustomMachineConfigJSON.customMachines.clear();
            CustomMachineConfigJSON.niceList.clear();
            CustomMachineConfigJSON.coreRecipes.clear();
            CustomMachineConfigJSON.readConfig(CustomMachineConfigJSON.gson.fromJson(reader, JsonObject.class));
            MachineConfiguration read = CustomMachineConfigJSON.customMachines.get("paperPress");
            check(read != null, problems, cases, "written default parses back");
            if (read != null) checkPaperPress(read, problems, cases, "round trip");
            eq(CustomMachineConfigJSON.coreRecipes.size(), 1, problems, cases, "round trip core recipe");
        } finally {
            CustomMachineConfigJSON.customMachines.clear();
            CustomMachineConfigJSON.customMachines.putAll(machines);
            CustomMachineConfigJSON.niceList.clear();
            CustomMachineConfigJSON.niceList.addAll(before);
            CustomMachineConfigJSON.coreRecipes.clear();
            CustomMachineConfigJSON.coreRecipes.putAll(core);
        }

        if (paper != null) eq(CustomMachineConfigJSON.stackFor(paper).getHoverName().getString(), "Paper Press", problems, cases, "controller item name");
        eq(new ItemStack(HbmBlocks.CUSTOM_MACHINE.get()).getHoverName().getString(), "INVALID MACHINE CONTROLLER", problems, cases, "controller without configuration");
    }

    /// recipes ///

    private static void checkRecipe(List<CustomMachineRecipe> list, List<String> problems, int[] cases, String where) {
        check(list != null && list.size() == 1, problems, cases, where + " paperPress recipe list");
        if (list == null || list.isEmpty()) return;
        CustomMachineRecipe r = list.get(0);
        eq(r.inputFluids.length + ":" + r.inputFluids[0].type().name() + ":" + r.inputFluids[0].amount() + ":" + r.inputFluids[0].pressure(), "1:water:250:0", problems, cases, where + " input fluid");
        eq(r.inputItems.length + ":" + r.inputItems[0].name() + ":" + r.inputItems[0].stacksize(), "1:atomiclegacy:powder_sawdust:1", problems, cases, where + " input item");
        eq(r.outputFluids.length, 0, problems, cases, where + " output fluids");
        eq(r.outputItems.length + ":" + BuiltInRegistries.ITEM.getKey(r.outputItems[0].stack().getItem()) + ":" + r.outputItems[0].stack().getCount() + ":" + r.outputItems[0].chance(),
                "1:minecraft:paper:3:1.0", problems, cases, where + " output");
        eq(r.duration + "/" + r.consumptionPerTick + "/" + r.pollutionType + "/" + r.pollutionAmount + "/" + r.radiationAmount + "/" + r.flux + "/" + r.heat,
                "60/10/SOOT/0.03/0.0/0/0", problems, cases, where + " values");
    }

    private static void recipes(List<String> problems, int[] cases) throws Exception {
        checkRecipe(CustomMachineRecipes.recipes.get("paperPress"), problems, cases, "loaded");
        File temp = Files.createTempFile("_hbmCustomMachines", ".json").toFile();
        temp.deleteOnExit();
        CustomMachineRecipes.writeTemplateFile(temp);
        String text = Files.readString(temp.toPath(), StandardCharsets.UTF_8);
        check(text.contains("[\"WATER\",250]") && text.contains("[\"minecraft:paper\",3,1.0]")
                && text.contains("\"pollutionAmount\": 0.029999999329447746"), problems, cases, "template uses the SerializableRecipe formats");
        var saved = new java.util.HashMap<>(CustomMachineRecipes.recipes);
        try {
            CustomMachineRecipes.recipes.clear();
            CustomMachineRecipes.readRecipeFile(temp);
            checkRecipe(CustomMachineRecipes.recipes.get("paperPress"), problems, cases, "template round trip");
        } finally {
            CustomMachineRecipes.recipes.clear();
            CustomMachineRecipes.recipes.putAll(saved);
        }
    }

    private static void coreRecipe(MinecraftServer server, ServerLevel level, List<String> problems, int[] cases) {
        ItemStack i = new ItemStack(item("ingot_steel"));
        ItemStack p = new ItemStack(item("plate_steel"));
        ItemStack c = new ItemStack(item("circuit_basic"));
        CraftingInput input = CraftingInput.of(3, 3, List.of(i, p, i, p, c, p, i, p, i));
        var match = server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        check(match.isPresent(), problems, cases, "IPI/PCP/IPI crafts a controller");
        if (match.isPresent()) {
            ItemStack out = match.get().value().assemble(input, level.registryAccess());
            MachineConfiguration conf = CustomMachineConfigJSON.configFor(out);
            check(out.is(HbmBlocks.CUSTOM_MACHINE.get().asItem()) && out.getCount() == 1 && conf != null && conf.unlocalizedName.equals("paperPress"),
                    problems, cases, "controller recipe result " + out);
        }
        CraftingInput wrong = CraftingInput.of(3, 3, List.of(i, p, i, p, i, p, i, p, i));
        check(server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, wrong, level).map(h -> !h.value().assemble(wrong, level.registryAccess()).is(HbmBlocks.CUSTOM_MACHINE.get().asItem())).orElse(true),
                problems, cases, "controller recipe needs the circuit");
    }

    /// world ///

    private static void clear(ServerLevel level, BlockPos center, int r) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -3, -r), center.offset(r, 4, r))) {
            if (!level.getBlockState(p).isAir()) {
                level.removeBlockEntity(p);
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
        }
    }

    private static CustomMachineBlockEntity place(ServerLevel level, BlockPos pos, Direction facing, String type) {
        level.setBlock(pos, HbmBlocks.CUSTOM_MACHINE.get().defaultBlockState().setValue(CustomMachineBlock.FACING, facing), 3);
        CustomMachineBlockEntity tile = (CustomMachineBlockEntity) level.getBlockEntity(pos);
        if (type != null) {
            tile.machineType = type;
            tile.init();
        }
        return tile;
    }

    private static void tick(ServerLevel level, CustomMachineBlockEntity tile, int times) {
        for (int t = 0; t < times; t++) CustomMachineBlockEntity.serverTick(level, tile.getBlockPos(), tile.getBlockState(), tile);
    }

    private static Block blockAt(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock();
    }

    private static void machine(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = new BlockPos(39000, 120, 39000);
        level.getChunkAt(o);
        clear(level, o, 5);
        CustomMachineBlockEntity tile = place(level, o, Direction.NORTH, "paperPress");
        check(tile.config != null, problems, cases, "controller initialised from the configuration");
        check(!tile.checkStructure(), problems, cases, "empty site is no valid structure");

        tile.buildStructure();
        // NORTH: component (x, y, z) at core + (x, y, z)
        eq(blockAt(level, o.offset(1, 0, 2)), HbmBlocks.CM_SHEET.get(), problems, cases, "north sheet at +1,0,+2");
        eq(blockAt(level, o.offset(-1, 1, 0)), HbmBlocks.CM_BLOCK.get(), problems, cases, "north block at -1,+1,0");
        eq(blockAt(level, o.offset(0, 0, 1)), Blocks.AIR, problems, cases, "north centre stays hollow");
        eq(blockAt(level, o.below()), HbmBlocks.CM_PORT.get(), problems, cases, "north lower port");
        eq(level.getBlockState(o.below()).getValue(LegacyVariantBlock.VARIANT), 0, problems, cases, "port meta 0 (steel)");
        check(tile.checkStructure(), problems, cases, "built structure validates");
        eq(tile.ports.size(), 3, problems, cases, "two cm_port proxies plus the controller are ports");
        for (BlockPos port : List.of(o.below(), o.above())) {
            check(level.getBlockEntity(port) instanceof CustomMachinePortBlockEntity proxy && proxy.hasPos() && proxy.getCorePos().equals(o),
                    problems, cases, "cm_port " + port.subtract(o) + " caches the controller");
            check(CustomMachineBlockEntity.findForPort(level, port) == tile, problems, cases, "cm_port " + port.subtract(o) + " resolves the controller");
        }

        // a wrong meta breaks the structure
        level.setBlock(o.offset(1, 0, 2), HbmBlocks.CM_SHEET.get().defaultBlockState().setValue(LegacyVariantBlock.VARIANT, 1), 3);
        check(!tile.checkStructure(), problems, cases, "alloy sheet where steel (meta 0) is required");
        level.setBlock(o.offset(1, 0, 2), HbmBlocks.CM_SHEET.get().defaultBlockState(), 3);
        check(tile.checkStructure(), problems, cases, "structure valid again");

        tick(level, tile, 1);
        check(tile.getPowerConnectorPositions(level).contains(o.below(2)) && tile.getPowerConnectorPositions(level).contains(o.above(2))
                && tile.getPowerConnectorPositions(level).contains(o.north()), problems, cases, "power ports at both cm_ports and the controller");
        check(PowerNetworkManager.endpointAt(level, o.below()) == tile, problems, cases, "cables at a cm_port reach the controller");
        check(tile.fluidPorts(water()).size() == 18, problems, cases, "fluid node: 3 ports x 6 sides (" + tile.fluidPorts(water()).size() + ")");
        eq(tile.getRequestedInput(), 10000, problems, cases, "receiver demand up to maxPower");
        eq(tile.getAvailableOutput(), 0, problems, cases, "no provision outside generator mode");

        // FluidNetMK2 through the lower port
        tile.inputTanks[0].setType(water());
        level.setBlock(o.below(2), HbmBlocks.BARREL_STEEL.get().defaultBlockState(), 3);
        FluidBarrelBlockEntity barrel = (FluidBarrelBlockEntity) level.getBlockEntity(o.below(2));
        for (int i = 0; i < 2; i++) barrel.cycleMode();
        barrel.tank().setType(water());
        barrel.tank().setAmount(600);
        HbmFluidNetworks.subscribe(level, barrel);
        HbmFluidNetworks.subscribe(level, tile);
        for (int i = 0; i < 2; i++) HbmFluidNetworks.updateNow(level);
        int received = tile.inputTanks[0].amount();
        check(received > 0, problems, cases, "water reaches the input tank through cm_port (" + received + ")");
        eq(received + barrel.tank().amount(), 600, problems, cases, "fluid transfer conserves water");
        level.removeBlockEntity(o.below(2));
        level.setBlock(o.below(2), Blocks.AIR.defaultBlockState(), 3);

        // one paper press cycle
        tile.inputTanks[0].setAmount(1000);
        tile.setItem(4, new ItemStack(item("powder_sawdust"), 2));
        tile.power = 10000;
        tick(level, tile, 59);
        eq(tile.getItem(16).getCount(), 0, problems, cases, "no paper before 60 ticks");
        eq(tile.progress, 59, problems, cases, "progress after 59 ticks");
        tick(level, tile, 1);
        check(tile.getItem(16).is(Items.PAPER) && tile.getItem(16).getCount() == 3, problems, cases, "60 ticks make 3 paper (" + tile.getItem(16) + ")");
        eq(tile.inputTanks[0].amount(), 750, problems, cases, "250 mB water used");
        eq(tile.getItem(4).getCount(), 1, problems, cases, "one sawdust used");
        eq(tile.power, 10000 - 60 * 10, problems, cases, "10 HE/t for 60 ticks");
        eq(tile.progress, 0, problems, cases, "progress reset");
        check(tile.smoke.capacity() == 100 && tile.smoke.amount() <= 100 && (level.getGameTime() % 20 != 0 || tile.smoke.amount() > 0), problems, cases,
                "SOOT goes into the 100 mB smoke buffer (maxPollutionCap) (" + tile.smoke.amount() + ")");

        // template slot
        tile.setItem(10, new ItemStack(Items.PAPER));
        check(!tile.canPlaceItem(4, new ItemStack(item("powder_sawdust"))), problems, cases, "template rejects other items (exact)");
        check(tile.canPlaceItem(4, new ItemStack(Items.PAPER)), problems, cases, "template accepts its item");
        tile.setItem(10, ItemStack.EMPTY);
        check(!tile.canPlaceItem(0, new ItemStack(Items.PAPER)) && !tile.canTakeItemThroughFace(4, ItemStack.EMPTY, Direction.UP)
                && tile.canTakeItemThroughFace(16, ItemStack.EMPTY, Direction.UP), problems, cases, "automation slots");

        // no power -> no progress
        tile.power = 5;
        tick(level, tile, 3);
        eq(tile.progress, 0, problems, cases, "below 10 HE nothing runs");
        tile.power = 10000;
        tick(level, tile, 5);
        eq(tile.progress, 5, problems, cases, "running again");

        // break one casing
        level.setBlock(o.offset(-1, -1, 1), Blocks.AIR.defaultBlockState(), 3);
        tile.structureCheckDelay = 0;
        tick(level, tile, 1);
        check(!tile.structureOK && tile.progress == 0, problems, cases, "a missing casing stops the machine");

        // drops
        BlockState state = level.getBlockState(o);
        List<ItemStack> drops = Block.getDrops(state, level, o, tile);
        check(drops.size() == 1 && CustomMachineConfigJSON.configFor(drops.get(0)) == tile.config, problems, cases, "drops its configured controller " + drops);

        clear(level, o, 5);
    }

    /** EAST: component (x, y, z) at core + (-z, y, x) (the swapped EW formula) */
    private static void east(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = new BlockPos(39020, 120, 39000);
        level.getChunkAt(o);
        clear(level, o, 5);
        CustomMachineBlockEntity tile = place(level, o, Direction.EAST, "paperPress");
        tile.buildStructure();
        eq(blockAt(level, o.offset(-2, 0, 1)), HbmBlocks.CM_SHEET.get(), problems, cases, "east sheet for component +1,0,+2");
        eq(blockAt(level, o.offset(-1, 0, 0)), Blocks.AIR, problems, cases, "east centre stays hollow");
        check(tile.checkStructure(), problems, cases, "east structure validates");
        clear(level, o, 5);

        o = new BlockPos(39040, 120, 39000);
        level.getChunkAt(o);
        clear(level, o, 5);
        tile = place(level, o, Direction.SOUTH, "paperPress");
        tile.buildStructure();
        eq(blockAt(level, o.offset(-1, 0, -2)), HbmBlocks.CM_SHEET.get(), problems, cases, "south sheet for component +1,0,+2");
        check(tile.checkStructure(), problems, cases, "south structure validates");
        clear(level, o, 5);
    }

    private static void invalid(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = new BlockPos(39060, 120, 39000);
        level.getChunkAt(o);
        clear(level, o, 2);
        CustomMachineBlockEntity tile = place(level, o, Direction.NORTH, null);
        tick(level, tile, 1);
        check(level.getBlockState(o).isAir(), problems, cases, "a controller without configuration breaks itself");
        clear(level, o, 2);
    }

    private static void wand(ServerLevel level, List<String> problems, int[] cases) throws Exception {
        BlockPos o = new BlockPos(39080, 120, 39000);
        level.getChunkAt(o);
        clear(level, o, 3);
        level.setBlock(o, HbmBlocks.CUSTOM_MACHINE_ANCHOR.get().defaultBlockState()
                .setValue(com.atomiclegacy.blocks.CustomMachineAnchorBlock.FACING, Direction.SOUTH), 3);
        level.setBlock(o.offset(1, 0, 2), HbmBlocks.CM_BLOCK.get().defaultBlockState().setValue(LegacyVariantBlock.VARIANT, 2), 3);
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("anchorX", o.getX()); nbt.putInt("anchorY", o.getY()); nbt.putInt("anchorZ", o.getZ());
        nbt.putInt("x1", o.getX() - 2); nbt.putInt("y1", o.getY()); nbt.putInt("z1", o.getZ());
        nbt.putInt("x2", o.getX() + 2); nbt.putInt("y2", o.getY()); nbt.putInt("z2", o.getZ() + 2);
        File temp = Files.createTempFile("CMstructureOutput", ".txt").toFile();
        temp.deleteOnExit();
        CustomMachineStructureItem.writeToFile(temp, nbt, level);
        String text = Files.readString(temp.toPath(), StandardCharsets.UTF_8);
        // SOUTH: compX = anchorX - x, compZ = anchorZ - z
        check(text.contains("{\"block\":\"atomiclegacy:cm_block\",\"x\":-1,\"y\":0,\"z\":-2,\"metas\":[2]}"), problems, cases, "CMstructureOutput component " + text.replace('\n', ' '));
        check(!text.contains("custom_machine_anchor"), problems, cases, "the anchor itself is not written");
        clear(level, o, 3);
    }
}
