package com.atomiclegacy.event;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.tileentity.HbmStructureLoot;
import com.atomiclegacy.worldgen.structure.HbmLegacyNbtStructure;
import com.atomiclegacy.worldgen.structure.HbmLegacyNbtTemplate;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * [parity R3] World generation of the 1.7.10 reference (HbmWorldGen, HbmWorld,
 * NTMWorldGenerator, WorldConfig/StructureConfig/MobConfig defaults, ItemPool*):
 * registered ore features, config defaults, structure candidates, item pools
 * and a strict legacy conversion of every bundled structure NBT.
 */
final class ParityR3Diagnostics {
    private ParityR3Diagnostics() {
    }

    /** id -> {count or -rarity, vein size, min Y, max Y} from DungeonToolbox.generateOre(count, size, minY, variance). */
    private static final Map<String, int[]> ORES = new LinkedHashMap<>();

    static {
        ore("ore_uranium", 7, 5, 5, 20);
        ore("ore_thorium", 7, 5, 5, 25);
        ore("ore_titanium", 8, 6, 5, 30);
        ore("ore_sulfur", 5, 8, 5, 30);
        ore("ore_aluminium", 7, 6, 5, 40);
        ore("ore_fluorite", 6, 4, 5, 45);
        ore("ore_niter", 6, 6, 5, 30);
        ore("ore_tungsten", 10, 8, 5, 30);
        ore("ore_lead", 6, 9, 5, 30);
        ore("ore_beryllium", 6, 4, 5, 30);
        ore("ore_rare", 6, 5, 5, 20);
        ore("ore_lignite", 2, 24, 35, 25);
        ore("ore_asbestos", 2, 4, 16, 16);
        ore("ore_cinnabar", 1, 4, 8, 16);
        ore("ore_cobalt", 2, 4, 4, 8);
        ore("cluster_iron", 4, 6, 15, 45);
        ore("cluster_titanium", 2, 6, 15, 30);
        ore("cluster_aluminium", 3, 6, 15, 35);
        ore("cluster_copper", 4, 6, 15, 20);
        ore("stone_resource_limestone", 1, 16, 25, 30);
        ore("ore_coltan", 2, 4, 15, 40);
        ore("ore_tikite", 8, 6, 0, 127);
        ore("ore_alexandrite", -100, 3, 10, 5);
    }

    private static void ore(String id, int count, int size, int minY, int variance) {
        ORES.put(id, new int[] {count, size, minY, minY + variance - 1});
    }

    /** ItemPool* of the reference: name:entries:total weight:sum(min):sum(max). */
    private static final String[] POOL_SHAPES = {
            "POOL_AMMO:10:69:68:68",
            "POOL_ANTENNA:23:101:25:56",
            "POOL_BLACK_PART:4:31:7:7",
            "POOL_BLACK_SLAB:1:10:1:1",
            "POOL_BLUEPRINTS:3:16:3:3",
            "POOL_EXPENSIVE:30:86:30:42",
            "POOL_FILING_CABINET:11:591:11:41",
            "POOL_GENERIC:30:119:45:104",
            "POOL_MACHINE_PARTS:17:60:23:80",
            "POOL_METEORITE_TREASURE:19:157:38:126",
            "POOL_METEOR_SAFE:9:9:9:9",
            "POOL_NUKE_FUEL:16:46:16:33",
            "POOL_NUKE_MISC:24:67:30:53",
            "POOL_NUKE_TRASH:11:36:15:32",
            "POOL_OFFICE_TRASH:15:45:15:48",
            "POOL_OIL_RIG:5:20:8:26",
            "POOL_PILE_BONES:3:17:3:3",
            "POOL_PILE_CAPS:3:24:12:12",
            "POOL_PILE_GEAR:11:340:16:32",
            "POOL_PILE_HIVE:23:197:37:65",
            "POOL_PILE_MAKESHIFT_GUN:1:10:1:1",
            "POOL_PILE_MAKESHIFT_PLATES:1:10:1:1",
            "POOL_PILE_MAKESHIFT_WIRE:1:10:1:1",
            "POOL_PILE_MAKESHIFT_WRENCH:1:10:1:1",
            "POOL_PILE_MECHANICAL:7:340:19:37",
            "POOL_PILE_MED_PILLS:4:40:4:4",
            "POOL_PILE_MED_SYRINGE:3:20:3:3",
            "POOL_PILE_NUKE_STORAGE:3:70:3:3",
            "POOL_PILE_OF_GARBAGE:23:410:5:56",
            "POOL_PILE_SUPPLIES:7:160:18:30",
            "POOL_RED_PEDESTAL:25:160:25:25",
            "POOL_REPAIR_MATERIALS:14:39:39:123",
            "POOL_RTG:2:41:1:2",
            "POOL_SAT_LUNAR:7:29:104:104",
            "POOL_SAT_MINER:26:189:59:59",
            "POOL_SILO:12:51:12:14",
            "POOL_SNACKS:5:40:5:5",
            "POOL_SODA:7:47:7:7",
            "POOL_SOLID_FUEL:6:8:6:20",
            "POOL_SPACESHIP:14:55:35:109",
            "POOL_SUPPLIES:8:58:10:27",
            "POOL_VAULT_LAB:16:94:16:19",
            "POOL_VAULT_LOCKERS:26:81:27:58",
            "POOL_VAULT_REINFORCED:12:12:68:128",
            "POOL_VAULT_RUSTY:13:15:97:162",
            "POOL_VAULT_STANDARD:11:11:41:73",
            "POOL_VAULT_UNBREAKABLE:6:6:29:55",
            "POOL_VERTIBIRD:22:128:23:55",
            "POOL_WEAPONS:10:551:10:10"
    };

    static int run(MinecraftServer server, List<String> problems) {
        int cases = 0;
        cases += checkOres(server, problems);
        cases += checkBiomeModifiers(server, problems);
        cases += checkConfigDefaults(problems);
        cases += checkStructures(problems);
        cases += checkPools(problems);
        cases += checkStructureNbt(server, problems);
        return cases;
    }

    private static int checkOres(MinecraftServer server, List<String> problems) {
        Registry<PlacedFeature> placed = server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        int cases = 0;
        for (Map.Entry<String, int[]> e : ORES.entrySet()) {
            cases++;
            String id = e.getKey();
            int[] want = e.getValue();
            PlacedFeature feature = placed.get(rl(id));
            if (feature == null) {
                problems.add("R3 ore " + id + ": placed feature missing");
                continue;
            }
            ConfiguredFeature<?, ?> configured = feature.feature().value();
            if (!(configured.config() instanceof OreConfiguration ore)) {
                problems.add("R3 ore " + id + ": not an ore configuration");
                continue;
            }
            if (ore.size != want[1]) {
                problems.add("R3 ore " + id + ": vein size " + ore.size + " expected " + want[1]);
            }
            Integer count = null;
            Integer rarity = null;
            String height = null;
            for (PlacementModifier modifier : feature.placement()) {
                CompoundTag tag = encode(modifier);
                String type = tag.getString("type");
                if (type.equals("minecraft:count")) {
                    count = tag.getInt("count");
                } else if (type.equals("minecraft:rarity_filter")) {
                    rarity = tag.getInt("chance");
                } else if (type.equals("minecraft:height_range")) {
                    CompoundTag h = tag.getCompound("height");
                    height = h.getString("type") + " " + anchor(h.getCompound("min_inclusive")) + ".." + anchor(h.getCompound("max_inclusive"));
                }
            }
            if (want[0] > 0 && (count == null || count != want[0])) {
                problems.add("R3 ore " + id + ": veins per chunk " + count + " expected " + want[0]);
            }
            if (want[0] < 0 && (rarity == null || rarity != -want[0])) {
                problems.add("R3 ore " + id + ": rarity " + rarity + " expected 1/" + (-want[0]));
            }
            String wantHeight = "minecraft:uniform abs" + want[2] + "..abs" + want[3];
            if (!wantHeight.equals(height)) {
                problems.add("R3 ore " + id + ": height " + height + " expected " + wantHeight);
            }
        }
        // Gneiss ores: SchistStratumFeature carries the HbmWorldGen table (copper = copperSpawn * 3).
        cases++;
        if (!BuiltInRegistries.BLOCK.containsKey(rl("ore_gneiss_copper"))
                || !HbmBlocks.SCHIST_ORES.contains(HbmBlocks.ORE_GNEISS_COPPER)) {
            problems.add("R3 ore_gneiss_copper is not registered with the schist ores");
        }
        return cases;
    }

    private static String anchor(CompoundTag tag) {
        if (tag.contains("absolute")) return "abs" + tag.getInt("absolute");
        if (tag.contains("above_bottom")) return "bottom+" + tag.getInt("above_bottom");
        if (tag.contains("below_top")) return "top-" + tag.getInt("below_top");
        return tag.toString();
    }

    private static CompoundTag encode(PlacementModifier modifier) {
        Tag tag = PlacementModifier.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, modifier).result().orElse(new CompoundTag());
        return tag instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    private static int checkBiomeModifiers(MinecraftServer server, List<String> problems) {
        Registry<BiomeModifier> modifiers = server.registryAccess().registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        int cases = 0;
        for (String id : ORES.keySet()) {
            cases++;
            BiomeModifier modifier = modifiers.get(rl("add_" + id));
            if (modifier == null || modifier.getClass().getSimpleName().contains("None")) {
                problems.add("R3 biome modifier add_" + id + " missing or disabled");
            }
        }
        // Invented ore-vein forms of the oil deposits stay disabled: oil comes from MapGenBubble/OilBubbleFeature.
        for (String id : List.of("ore_oil", "ore_oil_sand", "ore_deepslate_oil")) {
            cases++;
            BiomeModifier modifier = modifiers.get(rl("add_" + id));
            if (modifier != null && !modifier.getClass().getSimpleName().contains("None")) {
                problems.add("R3 biome modifier add_" + id + " must stay neoforge:none");
            }
        }
        for (String id : List.of("legacy_surface_worldgen", "schist_stratum", "nether_ore_legacy", "oil_bubble",
                "oil_sand_bubble", "bedrock_oil_deposit", "bedrock_ore_deposit_overworld", "bedrock_ore_deposit_nether",
                "meteorite", "glyphid_hive", "chlorine_geyser", "radiation_hotspot", "sulfur_cave", "asbestos_cave",
                "resource_layer_hematite", "resource_layer_bauxite", "resource_layer_malachite", "coltan_deposit",
                "australium_region", "lantern_behemoth")) {
            cases++;
            BiomeModifier modifier = modifiers.get(rl("add_" + id));
            if (modifier == null || modifier.getClass().getSimpleName().contains("None")) {
                problems.add("R3 biome modifier add_" + id + " missing or disabled");
            }
        }
        return cases;
    }

    private static int checkConfigDefaults(List<String> problems) {
        Map<String, Supplier<Object>> actual = new LinkedHashMap<>();
        Map<String, Object> want = new LinkedHashMap<>();
        def(actual, want, "gasBubbleSpawn", HbmConfig.GAS_BUBBLE_SPAWN_RATE, 12);
        def(actual, want, "explosiveBubbleSpawn", HbmConfig.EXPLOSIVE_GAS_BUBBLE_SPAWN_RATE, 0);
        def(actual, want, "oilSpawn", HbmConfig.OIL_DEPOSIT_SPAWN_RATE, 100);
        def(actual, want, "oilBubbleMin", HbmConfig.OIL_DEPOSIT_MIN_RADIUS, 8);
        def(actual, want, "oilBubbleMax", HbmConfig.OIL_DEPOSIT_MAX_RADIUS, 16);
        def(actual, want, "sandBubbleSpawn", HbmConfig.OIL_SAND_DEPOSIT_SPAWN_RATE, 200);
        def(actual, want, "sandBubbleMin", HbmConfig.OIL_SAND_DEPOSIT_MIN_RADIUS, 16);
        def(actual, want, "sandBubbleMax", HbmConfig.OIL_SAND_DEPOSIT_MAX_RADIUS, 48);
        def(actual, want, "bedrockOilSpawn", HbmConfig.BEDROCK_OIL_DEPOSIT_SPAWN_RATE, 200);
        def(actual, want, "bedrockOreChance", HbmConfig.BEDROCK_ORE_SPAWN_RATE, 10);
        def(actual, want, "bedrockGlowstoneSpawn", HbmConfig.BEDROCK_ORE_NETHER_GLOWSTONE_WEIGHT, 100);
        def(actual, want, "bedrockPhosphorusSpawn", HbmConfig.BEDROCK_ORE_NETHER_PHOSPHORUS_WEIGHT, 50);
        def(actual, want, "bedrockQuartzSpawn", HbmConfig.BEDROCK_ORE_NETHER_QUARTZ_WEIGHT, 100);
        def(actual, want, "meteoriteSpawn", HbmConfig.METEORITE_SPAWN, 200);
        def(actual, want, "geyserChlorine", HbmConfig.CHLORINE_GEYSER_SPAWN_RATE, 3000);
        def(actual, want, "radfreq", HbmConfig.RADIATION_HOTSPOT_SPAWN_RATE, 5000);
        def(actual, want, "hiveSpawn", HbmConfig.GLYPHID_HIVE_SPAWN, 256);
        def(actual, want, "antennaStructure", HbmConfig.ANTENNA_STRUCTURE_SPAWN_RATE, 250);
        def(actual, want, "atomStructure", HbmConfig.ATOM_STRUCTURE_SPAWN_RATE, 500);
        def(actual, want, "dungeonStructure", HbmConfig.LIBRARY_DUNGEON_SPAWN_RATE, 64);
        def(actual, want, "dudStructure", HbmConfig.DUD_STRUCTURE_SPAWN_RATE, 500);
        def(actual, want, "spaceshipStructure", HbmConfig.SPACESHIP_STRUCTURE_SPAWN_RATE, 1000);
        def(actual, want, "barrelStructure", HbmConfig.WASTE_TANK_STRUCTURE_SPAWN_RATE, 5000);
        def(actual, want, "broadcaster", HbmConfig.BROADCASTER_SPAWN_RATE, 5000);
        def(actual, want, "minefreq", HbmConfig.LANDMINE_SPAWN_RATE, 64);
        def(actual, want, "vaultfreq", HbmConfig.VAULT_SPAWN_RATE, 2500);
        def(actual, want, "capsuleStructure", HbmConfig.SOYUZ_CAPSULE_SPAWN_RATE, 100);
        def(actual, want, "arcticStructure", HbmConfig.ARCTIC_VAULT_SPAWN_RATE, 500);
        def(actual, want, "jungleStructure", HbmConfig.JUNGLE_DUNGEON_SPAWN_RATE, 2000);
        def(actual, want, "pyramidStructure", HbmConfig.ANCIENT_TOMB_SPAWN_CHANCE, 4000);
        def(actual, want, "pinkLog (rand 1000)", HbmConfig.PINK_TREE_SPAWN_RATE, 1000);
        def(actual, want, "stoneKeyhole (rand 4)", HbmConfig.STONE_KEYHOLE_SPAWN_RATE, 4);
        def(actual, want, "structureMinChunks", HbmConfig.HBM_STRUCTURE_MIN_CHUNKS, 4);
        def(actual, want, "structureMaxChunks", HbmConfig.HBM_STRUCTURE_MAX_CHUNKS, 16);
        def(actual, want, "spireSpawnWeight", HbmConfig.HBM_STRUCTURE_SPIRE_WEIGHT, 2);
        def(actual, want, "featuresSpawnWeight", HbmConfig.HBM_STRUCTURE_FEATURES_WEIGHT, 50);
        def(actual, want, "bunkerSpawnWeight", HbmConfig.HBM_STRUCTURE_BUNKER_WEIGHT, 6);
        def(actual, want, "vertibirdSpawnWeight", HbmConfig.HBM_STRUCTURE_VERTIBIRD_WEIGHT, 6);
        def(actual, want, "crashedVertibirdSpawnWeight", HbmConfig.HBM_STRUCTURE_CRASHED_VERTIBIRD_WEIGHT, 10);
        def(actual, want, "aircraftCarrierSpawnWeight", HbmConfig.HBM_STRUCTURE_AIRCRAFT_CARRIER_WEIGHT, 3);
        def(actual, want, "oilRigSpawnWeight", HbmConfig.HBM_STRUCTURE_OIL_RIG_WEIGHT, 5);
        def(actual, want, "lighthouseSpawnWeight", HbmConfig.HBM_STRUCTURE_LIGHTHOUSE_WEIGHT, 1);
        def(actual, want, "beachedPatrolSpawnWeight", HbmConfig.HBM_STRUCTURE_BEACHED_PATROL_WEIGHT, 15);
        def(actual, want, "dishSpawnWeight", HbmConfig.HBM_STRUCTURE_DISH_WEIGHT, 10);
        def(actual, want, "forestChemSpawnWeight", HbmConfig.HBM_STRUCTURE_FOREST_CHEM_WEIGHT, 30);
        def(actual, want, "plane1SpawnWeight", HbmConfig.HBM_STRUCTURE_PLANE1_WEIGHT, 25);
        def(actual, want, "plane2SpawnWeight", HbmConfig.HBM_STRUCTURE_PLANE2_WEIGHT, 25);
        def(actual, want, "desertShack1SpawnWeight", HbmConfig.HBM_STRUCTURE_DESERT_SHACK1_WEIGHT, 18);
        def(actual, want, "desertShack2SpawnWeight", HbmConfig.HBM_STRUCTURE_DESERT_SHACK2_WEIGHT, 20);
        def(actual, want, "desertShack3SpawnWeight", HbmConfig.HBM_STRUCTURE_DESERT_SHACK3_WEIGHT, 22);
        def(actual, want, "laboratorySpawnWeight", HbmConfig.HBM_STRUCTURE_LABORATORY_WEIGHT, 20);
        def(actual, want, "forestPostSpawnWeight", HbmConfig.HBM_STRUCTURE_FOREST_POST_WEIGHT, 30);
        def(actual, want, "ruinASpawnWeight", HbmConfig.HBM_STRUCTURE_RUIN_A_WEIGHT, 10);
        def(actual, want, "ruinBSpawnWeight", HbmConfig.HBM_STRUCTURE_RUIN_B_WEIGHT, 12);
        def(actual, want, "ruinJSpawnWeight", HbmConfig.HBM_STRUCTURE_RUIN_J_WEIGHT, 12);
        def(actual, want, "radioSpawnWeight", HbmConfig.HBM_STRUCTURE_RADIO_WEIGHT, 25);
        def(actual, want, "factorySpawnWeight", HbmConfig.HBM_STRUCTURE_FACTORY_WEIGHT, 40);
        def(actual, want, "plainsNullWeight", HbmConfig.HBM_STRUCTURE_PLAINS_NULL_WEIGHT, 20);
        def(actual, want, "oceanNullWeight", HbmConfig.HBM_STRUCTURE_OCEAN_NULL_WEIGHT, 35);
        def(actual, want, "craneSpawnWeight", HbmConfig.HBM_STRUCTURE_CRANE_WEIGHT, 20);
        def(actual, want, "broadcastingTowerSpawnWeight", HbmConfig.HBM_STRUCTURE_BROADCASTING_TOWER_WEIGHT, 25);
        def(actual, want, "towerBaseSpawnWeight", HbmConfig.HBM_STRUCTURE_TOWER_BASE_WEIGHT, 30);
        def(actual, want, "waterPumpSpawnWeight", HbmConfig.HBM_STRUCTURE_WATER_PUMP_WEIGHT, 15);
        def(actual, want, "deadDishSmallSpawnWeight", HbmConfig.HBM_STRUCTURE_DEAD_DISH_SMALL_WEIGHT, 15);
        def(actual, want, "falloutShelterHouseSpawnWeight", HbmConfig.HBM_STRUCTURE_FALLOUT_SHELTER_HOUSE_WEIGHT, 15);
        def(actual, want, "abandonedArcticStationSpawnWeight", HbmConfig.HBM_STRUCTURE_ABANDONED_ARCTIC_STATION_WEIGHT, 5);
        def(actual, want, "militaryBaseSpawnWeight", HbmConfig.HBM_STRUCTURE_MILITARY_BASE_WEIGHT, 1);
        def(actual, want, "submarineSpawnWeight", HbmConfig.HBM_STRUCTURE_SUBMARINE_WEIGHT, 2);
        def(actual, want, "meteorDungeon (SpawnCondition default 1)", HbmConfig.HBM_STRUCTURE_METEOR_DUNGEON_WEIGHT, 1);
        int cases = 0;
        for (Map.Entry<String, Object> e : want.entrySet()) {
            cases++;
            Object value = actual.get(e.getKey()).get();
            if (!e.getValue().equals(value)) {
                problems.add("R3 config default " + e.getKey() + " = " + value + " expected " + e.getValue());
            }
        }
        return cases;
    }

    private static void def(Map<String, Supplier<Object>> actual, Map<String, Object> want, String key,
                            ModConfigSpec.IntValue value, int expected) {
        actual.put(key, value::getDefault);
        want.put(key, expected);
    }

    private static int checkStructures(List<String> problems) {
        List<String> names = HbmLegacyNbtStructure.listStructureNames();
        List<String> expected = List.of("spire", "features", "bunker", "vertibird", "crashed_vertibird",
                "beached_patrol", "aircraft_carrier", "oil_rig", "lighthouse", "dish", "water_pump", "forestchem",
                "labolatory", "forest_post", "radio", "factory", "crane", "broadcaster_tower", "plane1", "plane2",
                "desert_shack_1", "desert_shack_2", "desert_shack_3", "dead_dish_small", "ruin1", "ruin2", "ruin3",
                "ruin4", "ruin5", "ruin6", "ruin7", "ruin8", "ruin9", "ruin10", "tower_base",
                "falloutshelter_house", "abandoned_arcticstation", "military_base", "submarine", "meteor_dungeon");
        int cases = 0;
        for (String name : expected) {
            cases++;
            if (!names.contains(name)) {
                problems.add("R3 NTMWorldGenerator structure " + name + " is not a weighted candidate");
            }
        }
        return cases;
    }

    private static int checkPools(List<String> problems) {
        Map<String, int[]> shapes = HbmStructureLoot.diagnosticPoolShapes();
        int cases = 0;
        for (String line : POOL_SHAPES) {
            cases++;
            String[] parts = line.split(":");
            int[] actual = shapes.get(parts[0]);
            if (actual == null) {
                problems.add("R3 item pool " + parts[0] + " missing");
                continue;
            }
            for (int i = 0; i < 4; i++) {
                if (actual[i] != Integer.parseInt(parts[i + 1])) {
                    problems.add("R3 item pool " + parts[0] + " shape " + java.util.Arrays.toString(actual) + " expected " + line);
                    break;
                }
            }
        }
        if (shapes.size() != POOL_SHAPES.length) {
            problems.add("R3 item pools: " + shapes.size() + " registered, reference has " + POOL_SHAPES.length);
        }
        for (String problem : HbmStructureLoot.diagnosticEntryProblems()) {
            cases++;
            if (!KNOWN_UNPORTED_POOL_ITEMS.stream().anyMatch(problem::contains)) {
                problems.add("R3 " + problem);
            }
        }
        return cases;
    }

    /** Pool items whose 1.7.10 item is not registered by the port yet (reported in work/parity-r3.md). */
    private static final List<String> KNOWN_UNPORTED_POOL_ITEMS = List.of(); // [parity S4: gun kits, ammo_container] all registered

    private static int checkStructureNbt(MinecraftServer server, List<String> problems) {
        Set<String> knownPools = HbmStructureLoot.diagnosticKnownPoolNames();
        Map<ResourceLocation, Resource> files = server.getResourceManager().listResources("structure",
                id -> id.getNamespace().equals("atomiclegacy") && id.getPath().endsWith(".nbt"));
        int cases = 0;
        Set<String> badStates = new TreeSet<>();
        Set<String> badPools = new TreeSet<>();
        for (Map.Entry<ResourceLocation, Resource> file : files.entrySet()) {
            cases++;
            CompoundTag root;
            try (InputStream stream = file.getValue().open()) {
                root = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            } catch (Exception e) {
                problems.add("R3 structure NBT unreadable " + file.getKey() + ": " + e.getMessage());
                continue;
            }
            ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
            for (int i = 0; i < palette.size(); i++) {
                CompoundTag entry = palette.getCompound(i);
                String name = entry.getString("Name");
                int meta = 0;
                if (entry.contains("Properties", Tag.TAG_COMPOUND)
                        && entry.getCompound("Properties").contains("meta", Tag.TAG_STRING)) {
                    try {
                        meta = Integer.parseInt(entry.getCompound("Properties").getString("meta"));
                    } catch (NumberFormatException ignored) {
                        meta = 0;
                    }
                }
                cases++;
                strict(file.getKey(), name, meta, badStates);
            }
            ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < blocks.size(); i++) {
                CompoundTag block = blocks.getCompound(i);
                if (!block.contains("nbt", Tag.TAG_COMPOUND)) continue;
                CompoundTag nbt = block.getCompound("nbt");
                // wand_loot (BlockWandLoot) stores pool + min/max; jigsaw blocks also use a "pool" key.
                if (nbt.contains("pool", Tag.TAG_STRING) && nbt.contains("max", Tag.TAG_ANY_NUMERIC)) {
                    cases++;
                    String pool = nbt.getString("pool");
                    if (!pool.isEmpty() && !knownPools.contains(pool)) {
                        badPools.add(file.getKey().getPath() + " -> " + pool);
                    }
                    if (nbt.contains("block", Tag.TAG_STRING)) {
                        strict(file.getKey(), nbt.getString("block"), nbt.getInt("meta"), badStates);
                    }
                }
            }
            // The lenient loader used by world generation must accept the whole template too.
            try {
                String path = file.getKey().getPath();
                HbmLegacyNbtTemplate.get(path.substring("structure/".length(), path.length() - ".nbt".length()));
            } catch (RuntimeException e) {
                problems.add("R3 structure NBT " + file.getKey() + " fails to load: " + e.getMessage());
            }
        }
        if (files.size() < 81) {
            problems.add("R3 structure NBT files: " + files.size() + " (reference 83, of which repeater_radio is unused)");
        }
        for (String bad : badStates) problems.add("R3 strict legacy state " + bad);
        for (String bad : badPools) problems.add("R3 unknown loot pool in structure " + bad);
        return cases;
    }

    private static void strict(ResourceLocation file, String name, int meta, Set<String> bad) {
        try {
            BlockState state = HbmLegacyNbtTemplate.stateFromLegacyIdStrict(name, meta);
            if (state == null) bad.add(file.getPath() + " " + name + ":" + meta + " -> null");
        } catch (RuntimeException e) {
            bad.add(file.getPath() + " " + name + ":" + meta + " -> " + e.getMessage());
        }
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("atomiclegacy", path);
    }
}
