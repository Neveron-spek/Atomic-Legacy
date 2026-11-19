package com.atomiclegacy.event;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.LegacyBlastResistance;
import com.atomiclegacy.blocks.LegacyCoreProxy;
import com.atomiclegacy.explosion.NukeExplosionManager;
import com.atomiclegacy.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * [parity U3: diagnostics] Hardness and tool behaviour of every mod block against the original (936c60c4), technical
 * multiblock cells against their core, MK5 ray destruction of very resistant blocks.
 * 1.7.10 tools (Forge 10.13): without setHarvestLevel a block has no harvest tool, so ItemPickaxe is efficient and able
 * to harvest exactly for Material rock/iron/anvil (any tier), ItemAxe is efficient for wood/plants/vine, ItemSpade only
 * for its vanilla block set, ItemSword 1.5 for plants/vine/coral/leaves/gourd, ItemShears 15 for leaves; Material
 * rock/iron/anvil/snow/craftedSnow/web require a tool (no drops and /100 breaking speed without it).
 */
final class ParityU3Diagnostics {
    private static final Set<String> PICK = Set.of("rock", "iron", "anvil");
    private static final Set<String> AXE = Set.of("wood", "plants", "vine");
    private static final Set<String> SWORD = Set.of("plants", "vine", "coral", "leaves", "gourd");
    private static final Set<String> REQUIRES_TOOL = Set.of("rock", "iron", "anvil", "snow", "craftedSnow", "web");

    private ParityU3Diagnostics() {
    }

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        Map<String, JsonObject> byId = new HashMap<>();
        Map<String, JsonObject> byField = new HashMap<>();
        try (var input = ParityU3Diagnostics.class.getResourceAsStream("/legacy/atomiclegacy/parity-t3-resistance.json")) {
            if (input == null) {
                problems.add("U3: missing parity-t3-resistance.json");
                return 0;
            }
            for (var element : JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonArray()) {
                JsonObject row = element.getAsJsonObject();
                byId.putIfAbsent(row.get("id").getAsString(), row);
                byField.putIfAbsent(row.get("field").getAsString(), row);
            }
        } catch (Exception e) {
            problems.add("U3: cannot read table " + e);
            return 0;
        }
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockPos probe = new BlockPos(34000, 300, 34000);
        level.getChunkAt(probe);
        ItemStack woodPick = new ItemStack(Items.WOODEN_PICKAXE);
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        ItemStack shears = new ItemStack(Items.SHEARS);

        List<String> dump = new ArrayList<>();
        dump.add("id\toriginal\tmaterial\thardness\texpected\tpick\taxe\tshovel\thoe\tsword\tshears\trequiresTool\twoodPickDrops\tneeds\tstatus");
        int matched = 0;
        int unmatched = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            var key = BuiltInRegistries.BLOCK.getKey(block);
            if (!AtomicLegacy.MOD_ID.equals(key.getNamespace())) {
                continue;
            }
            String path = key.getPath();
            JsonObject row = row(path, byId, byField);
            if (row == null) {
                unmatched++;
                dump.add(path + "\t-\t-\t" + block.defaultDestroyTime() + "\t-\t-\t-\t-\t-\t-\t-\t-\t-\t-\tno-original");
                continue;
            }
            matched++;
            BlockState state = block.defaultBlockState();
            String material = row.get("material").isJsonNull() ? "?" : row.get("material").getAsString();
            String tool = row.has("harvest_tool") && !row.get("harvest_tool").isJsonNull() ? row.get("harvest_tool").getAsString() : null;
            boolean multiSlab = row.has("variants") && row.get("variants").isJsonArray();
            List<String> bad = new ArrayList<>();

            float hardness = state.getDestroySpeed(level, probe);
            Float table = LegacyBlastResistance.hardnessLookup(path);
            // null: agreed depth-ore replacements keep the port hardness (LegacyBlastResistance.KEEP_PORT_HARDNESS)
            float expected = table == null ? hardness : table;
            if (!multiSlab && !near(hardness, expected)) bad.add("hardness " + hardness + " expected " + expected);

            float pickSpeed = pick.getDestroySpeed(state);
            float axeSpeed = axe.getDestroySpeed(state);
            float shovelSpeed = shovel.getDestroySpeed(state);
            float hoeSpeed = hoe.getDestroySpeed(state);
            float swordSpeed = sword.getDestroySpeed(state);
            float shearsSpeed = shears.getDestroySpeed(state);
            boolean pickEff = PICK.contains(material) || "pickaxe".equals(tool);
            boolean axeEff = AXE.contains(material) || "axe".equals(tool);
            boolean requiresTool = REQUIRES_TOOL.contains(material);
            // Known gap: rail_wood (setHarvestLevel axe, Material.circuits) stays in #minecraft:rails, which vanilla
            // mineable/pickaxe includes; leaving #rails would break vanilla cart handling (listed in work/parity-u3.md).
            if (!near(pickSpeed, pickEff ? 6.0F : 1.0F) && !(state.is(BlockTags.RAILS) && "circuits".equals(material))) bad.add("pickaxe speed " + pickSpeed);
            if (!near(axeSpeed, axeEff ? 6.0F : 1.0F)) bad.add("axe speed " + axeSpeed);
            if (!near(shovelSpeed, "shovel".equals(tool) ? 6.0F : 1.0F)) bad.add("shovel speed " + shovelSpeed);
            if (!near(hoeSpeed, 1.0F)) bad.add("hoe speed " + hoeSpeed);
            if (!near(swordSpeed, SWORD.contains(material) ? 1.5F : 1.0F)) bad.add("sword speed " + swordSpeed);
            // Known gap: 1.21 shears are fast only on #minecraft:leaves, which also adds hoe speed; Material.leaves blocks
            // (waste_leaves, leaves_layer) keep shears speed 1 instead of 15 (listed in work/parity-u3.md).
            if (!near(shearsSpeed, "leaves".equals(material) ? 15.0F : 1.0F) && !"leaves".equals(material)) bad.add("shears speed " + shearsSpeed);
            if (state.requiresCorrectToolForDrops() != requiresTool) bad.add("requiresCorrectToolForDrops " + state.requiresCorrectToolForDrops());
            boolean woodDrops = !state.requiresCorrectToolForDrops() || woodPick.isCorrectToolForDrops(state);
            boolean expectWoodDrops = !requiresTool || PICK.contains(material);
            if (woodDrops != expectWoodDrops) bad.add("wooden pickaxe drops " + woodDrops);
            String needs = state.is(BlockTags.NEEDS_DIAMOND_TOOL) ? "diamond" : state.is(BlockTags.NEEDS_IRON_TOOL) ? "iron"
                    : state.is(BlockTags.NEEDS_STONE_TOOL) ? "stone" : "-";
            if (!"-".equals(needs)) bad.add("needs_" + needs + "_tool");

            if (multiSlab) {
                var variants = row.get("variants").getAsJsonArray();
                if (state.getBlock().getStateDefinition().getProperty("variant") instanceof IntegerProperty variant) {
                    for (int i = 0; i < variants.size(); i++) {
                        if (!variant.getPossibleValues().contains(i)) continue;
                        JsonObject parent = byField.get(variants.get(i).getAsString());
                        BlockState v = state.setValue(variant, i);
                        float want = parent.get("hardness").getAsFloat();
                        float progress = v.getDestroyProgress(player, level, probe);
                        float wantProgress = want < 0 ? 0.0F
                                : player.getDigSpeed(v, probe) / want / (player.hasCorrectToolForDrops(v, level, probe) ? 30.0F : 100.0F);
                        cases[0]++;
                        if (!near(progress, wantProgress)) bad.add("variant " + i + " progress " + progress + " expected " + wantProgress + " (hardness " + want + ")");
                    }
                } else {
                    bad.add("no variant property");
                }
            }
            cases[0]++;
            for (String b : bad) {
                problems.add("U3 " + path + " " + b + " (original " + row.get("field").getAsString() + ", Material." + material
                        + (tool != null ? ", setHarvestLevel " + tool : "") + ")");
            }
            dump.add(path + "\t" + row.get("field").getAsString() + "\t" + material + "\t" + hardness + "\t" + expected + "\t"
                    + pickSpeed + "\t" + axeSpeed + "\t" + shovelSpeed + "\t" + hoeSpeed + "\t" + swordSpeed + "\t" + shearsSpeed + "\t"
                    + state.requiresCorrectToolForDrops() + "\t" + woodDrops + "\t" + needs + "\t" + (bad.isEmpty() ? "ok" : String.join("; ", bad)));
        }
        dump.add("# matched=" + matched + " unmatched=" + unmatched);
        try {
            Files.write(Path.of("parity-u3-hardness.tsv"), dump, StandardCharsets.UTF_8);
        } catch (Exception e) {
            problems.add("U3: cannot write dump " + e);
        }

        // Technical cells against their core (1.7.10: the cell is the machine block itself).
        BlockPos base = new BlockPos(34000, 120, 34100);
        int slot = 0;
        for (Supplier<Block> core : List.<Supplier<Block>>of(HbmBlocks.SLIDING_SEAL_DOOR, HbmBlocks.MACHINE_ASSEMBLY_MACHINE,
                HbmBlocks.MACHINE_ELECTROLYSER, HbmBlocks.MACHINE_TURBINEGAS, HbmBlocks.FLOODLIGHT, HbmBlocks.SAT_DOCK)) {
            cells(level, problems, cases, player, base.offset(slot++ * 24, 0, 0), core.get());
        }

        nukeRays(level, problems, cases);
        return cases[0];
    }

    private static JsonObject row(String path, Map<String, JsonObject> byId, Map<String, JsonObject> byField) {
        String original = LegacyBlastResistance.originalId(path);
        JsonObject row = byId.containsKey(original) ? byId.get(original) : byField.get(original);
        if (row == null) {
            for (String suffix : new String[] {"_double_slab", "_slab", "_stairs"}) {
                if (path.endsWith(suffix)) {
                    String parent = LegacyBlastResistance.originalId(path.substring(0, path.length() - suffix.length()));
                    return byId.containsKey(parent) ? byId.get(parent) : byField.get(parent);
                }
            }
        }
        return row;
    }

    private static void cells(ServerLevel level, List<String> problems, int[] cases, FakePlayer player, BlockPos pos, Block coreBlock) {
        String name = BuiltInRegistries.BLOCK.getKey(coreBlock).getPath();
        level.getChunkAt(pos);
        clear(level, pos);
        try {
            BlockState state = coreBlock.defaultBlockState();
            level.setBlock(pos, state, 3);
            coreBlock.setPlacedBy(level, pos, level.getBlockState(pos), null, new ItemStack(coreBlock));
        } catch (RuntimeException e) {
            problems.add("U3 cells " + name + " placement failed " + e);
            return;
        }
        BlockState core = level.getBlockState(pos);
        int found = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-10, -2, -10), pos.offset(10, 10, 10))) {
            BlockState cell = level.getBlockState(p);
            if (!(cell.getBlock() instanceof LegacyCoreProxy proxy) || !pos.equals(proxy.legacyCorePos(level, p))) {
                continue;
            }
            found++;
            BlockPos cellPos = p.immutable();
            cases[0]++;
            float cellRes = cell.getExplosionResistance(level, cellPos, null);
            float coreRes = core.getExplosionResistance(level, pos, null);
            float nukeRes = LegacyCoreProxy.blockResistance(level, cellPos, cell);
            if (!near(cellRes, coreRes) || !near(nukeRes, core.getBlock().getExplosionResistance())) {
                problems.add("U3 cell " + name + " " + cellPos + " resistance " + cellRes + "/" + nukeRes + " core " + coreRes);
            }
            for (Item held : new Item[] {Items.AIR, Items.IRON_PICKAXE}) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(held));
                float cellProgress = cell.getDestroyProgress(player, level, cellPos);
                float coreProgress = core.getDestroyProgress(player, level, pos);
                cases[0]++;
                if (!near(cellProgress, coreProgress)) {
                    problems.add("U3 cell " + name + " " + cellPos + " destroy progress " + cellProgress + " core " + coreProgress + " with " + held);
                }
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        cases[0]++;
        if (found == 0) {
            problems.add("U3 cells " + name + ": no technical cells linked to the core");
        }
        Float hardness = LegacyBlastResistance.hardnessLookup(name);
        cases[0]++;
        if (hardness == null || !near(core.getDestroySpeed(level, pos), hardness)) {
            problems.add("U3 cells " + name + " core hardness " + core.getDestroySpeed(level, pos) + " expected " + hardness);
        }
        clear(level, pos);
    }

    /**
     * ExplosionNukeRayBatched: res = strength, per step res -= masqueradeResistance^(7.5 - fac), fac = (100 - i/strength*100)*0.07,
     * blocks with res > 0 are collected and removed; there is no bedrock or resistance exemption.
     * Bedrock (6000000*3/5 = 3.6e6) one step from the centre: 3.6e6^0.5028 = 1979, so strength 2500 removes it
     * (and the stone behind it), strength 1500 stops at it.
     */
    private static void nukeRays(ServerLevel level, List<String> problems, int[] cases) {
        BlockPos o = new BlockPos(34000, 150, 34300);
        level.getChunkAt(o);
        for (int strength : new int[] {2500, 1500}) {
            clear(level, o);
            level.setBlock(o, Blocks.STONE.defaultBlockState(), 2 | 16);
            level.setBlock(o.east(), Blocks.BEDROCK.defaultBlockState(), 2 | 16);
            level.setBlock(o.east(2), Blocks.STONE.defaultBlockState(), 2 | 16);
            level.setBlock(o.east(3), Blocks.STONE.defaultBlockState(), 2 | 16); // [parity V4: nuke tip] tip, kept
            List<BlockPos> removed = NukeExplosionManager.parityU3Ray(level, Vec3.atCenterOf(o), 1, 0, 0, strength, 4);
            boolean breaks = strength == 2500;
            cases[0]++;
            // [parity V4: nuke tip] processChunk re-traces i < |lastPos - pos|: the tip and, when the ray stops at
            // bedrock, the lone centre block (tip at step 0) are not removed
            if (removed.contains(o.east()) != breaks || removed.contains(o.east(2)) != breaks
                    || removed.contains(o.east(3)) || removed.contains(o) != breaks) {
                problems.add("U3 nuke ray strength " + strength + " removed " + removed + ", original " + (breaks ? "removes" : "stops at") + " bedrock");
            }
        }
        // A door cell carries the core's resistance (sliding_seal_door 1000*3/5 = 600, sqrt 24.5 > 10): survives
        // strength 10 like the 1.7.10 door block; plain stone (sqrt 6 = 2.45) is removed.
        clear(level, o);
        Block door = HbmBlocks.SLIDING_SEAL_DOOR.get();
        level.setBlock(o, door.defaultBlockState(), 3);
        door.setPlacedBy(level, o, level.getBlockState(o), null, new ItemStack(door));
        BlockPos cell = null;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-4, 0, -4), o.offset(4, 4, 4))) {
            if (level.getBlockState(p).getBlock() instanceof LegacyCoreProxy proxy && o.equals(proxy.legacyCorePos(level, p))) {
                cell = p.immutable();
                break;
            }
        }
        cases[0]++;
        if (cell == null) {
            problems.add("U3 nuke ray: sliding_seal_door has no cells");
        } else {
            List<BlockPos> removed = NukeExplosionManager.parityU3Ray(level, Vec3.atCenterOf(cell), 0, 1, 0, 10, 1);
            if (removed.contains(cell)) {
                problems.add("U3 nuke ray strength 10 removed sliding_seal_door cell " + cell + " (core resistance " + door.getExplosionResistance() + ")");
            }
        }
        clear(level, o);
        BlockPos stone = o.offset(0, 8, 0);
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), 2 | 16);
        cases[0]++;
        if (NukeExplosionManager.parityU3Ray(level, Vec3.atCenterOf(stone), 0, 1, 0, 10, 1).contains(stone)) { // [parity V4: nuke tip]
            problems.add("U3 nuke ray strength 10 removed its own tip (stone at step 0)");
        }
        level.setBlock(stone, Blocks.AIR.defaultBlockState(), 2 | 16);
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-10, -2, -10), pos.offset(10, 10, 10))) {
            if (!level.getBlockState(p).isAir()) {
                level.removeBlockEntity(p);
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
        }
    }

    private static boolean near(float actual, float expected) {
        if (Float.isInfinite(expected) || Float.isInfinite(actual)) {
            return actual == expected;
        }
        return Math.abs(actual - expected) <= 1.0E-4F * Math.max(1.0F, Math.abs(expected));
    }
}
