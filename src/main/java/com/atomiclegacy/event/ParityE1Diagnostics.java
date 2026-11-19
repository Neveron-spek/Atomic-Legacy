package com.atomiclegacy.event;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.blocks.SoyuzCapsuleBlock;
import com.atomiclegacy.config.HbmConfig;
import com.atomiclegacy.registry.HbmBlocks;
import com.atomiclegacy.worldgen.LegacyBiomeTraits;
import com.atomiclegacy.worldgen.structure.HbmLegacyStructureSelectionProbe;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [parity E1: diagnostics] World generation values of HbmWorldGen / NTMWorldGenerator (ore veins, scattered
 * feature rates, structure biome conditions, the rusty beach capsule) plus the mob weapon and block render
 * checks of the same batch.
 */
final class ParityE1Diagnostics {
    private ParityE1Diagnostics() {
    }

    /** name, veins per chunk, vein size, min Y, max Y inclusive (minHeight + variance - 1). */
    private static final Object[][] ORES = {
            {"ore_uranium", 7, 5, 5, 24}, {"ore_thorium", 7, 5, 5, 29}, {"ore_titanium", 8, 6, 5, 34},
            {"ore_sulfur", 5, 8, 5, 34}, {"ore_aluminium", 7, 6, 5, 44}, {"ore_copper", 12, 6, 5, 49},
            {"ore_fluorite", 6, 4, 5, 49}, {"ore_niter", 6, 6, 5, 34}, {"ore_tungsten", 10, 8, 5, 34},
            {"ore_lead", 6, 9, 5, 34}, {"ore_beryllium", 6, 4, 5, 34}, {"ore_rare", 6, 5, 5, 24},
            {"ore_lignite", 2, 24, 35, 59}, {"ore_asbestos", 2, 4, 16, 31}, {"ore_cinnabar", 1, 4, 8, 23},
            {"ore_cobalt", 2, 4, 4, 11}, {"cluster_iron", 4, 6, 15, 59}, {"cluster_titanium", 2, 6, 15, 44},
            {"cluster_aluminium", 3, 6, 15, 49}, {"cluster_copper", 4, 6, 15, 34},
            {"stone_resource_limestone", 1, 16, 25, 54}, {"ore_tikite", 8, 6, 0, 126},
            {"ore_coltan", 2, 4, 15, 54},
    };

    static int run(MinecraftServer server, List<String> problems) {
        int[] cases = {0};
        ServerLevel level = server.overworld();
        try {
            ores(problems, cases);
            rates(problems, cases);
            biomes(level, problems, cases);
            capsule(problems, cases);
        } catch (Exception exception) {
            problems.add("parity e1: diagnostics threw " + exception);
            AtomicLegacy.LOGGER.error("parity e1 diagnostics", exception);
        }
        try {
            cases[0] += ParityE1MobChecks.run(level, problems);
        } catch (Exception exception) {
            problems.add("parity e1: mob checks threw " + exception);
            AtomicLegacy.LOGGER.error("parity e1 mob checks", exception);
        }
        try {
            cases[0] += ParityE1RenderChecks.run(level, problems);
        } catch (Exception exception) {
            problems.add("parity e1: render checks threw " + exception);
            AtomicLegacy.LOGGER.error("parity e1 render checks", exception);
        }
        return cases[0];
    }

    private static JsonObject json(String path) throws Exception {
        try (InputStream in = ParityE1Diagnostics.class.getResourceAsStream("/data/atomiclegacy/worldgen/" + path + ".json")) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static void ores(List<String> problems, int[] cases) throws Exception {
        for (Object[] ore : ORES) {
            String name = (String) ore[0];
            cases[0]++;
            JsonObject configured = json("configured_feature/" + name);
            JsonObject placed = json("placed_feature/" + name);
            if (configured == null || placed == null) {
                problems.add("parity e1: missing worldgen json for " + name);
                continue;
            }
            int size = configured.getAsJsonObject("config").get("size").getAsInt();
            int count = -1;
            int min = Integer.MIN_VALUE;
            int max = Integer.MIN_VALUE;
            for (JsonElement element : placed.getAsJsonArray("placement")) {
                JsonObject modifier = element.getAsJsonObject();
                String type = modifier.get("type").getAsString();
                if (type.equals("minecraft:count")) {
                    count = modifier.get("count").getAsInt();
                } else if (type.equals("minecraft:height_range")) {
                    JsonObject height = modifier.getAsJsonObject("height");
                    min = height.getAsJsonObject("min_inclusive").get("absolute").getAsInt();
                    max = height.getAsJsonObject("max_inclusive").get("absolute").getAsInt();
                }
            }
            if (count != (int) ore[1] || size != (int) ore[2] || min != (int) ore[3] || max != (int) ore[4]) {
                problems.add("parity e1: " + name + " count/size/min/max " + count + "/" + size + "/" + min + "/" + max
                        + " expected " + ore[1] + "/" + ore[2] + "/" + ore[3] + "/" + ore[4]);
            }
        }
    }

    private static void rate(List<String> problems, int[] cases, String name, int actual, int expected) {
        cases[0]++;
        if (actual != expected) {
            problems.add("parity e1: " + name + " default " + actual + " expected " + expected);
        }
    }

    private static void rates(List<String> problems, int[] cases) {
        rate(problems, cases, "antennaSpawn", HbmConfig.ANTENNA_STRUCTURE_SPAWN_RATE.getDefault(), 250);
        rate(problems, cases, "atomSpawn", HbmConfig.ATOM_STRUCTURE_SPAWN_RATE.getDefault(), 500);
        rate(problems, cases, "dungeonSpawn", HbmConfig.LIBRARY_DUNGEON_SPAWN_RATE.getDefault(), 64);
        rate(problems, cases, "dudSpawn", HbmConfig.DUD_STRUCTURE_SPAWN_RATE.getDefault(), 500);
        rate(problems, cases, "spaceshipSpawn", HbmConfig.SPACESHIP_STRUCTURE_SPAWN_RATE.getDefault(), 1000);
        rate(problems, cases, "barrelSpawn", HbmConfig.WASTE_TANK_STRUCTURE_SPAWN_RATE.getDefault(), 5000);
        rate(problems, cases, "broadcasterSpawn", HbmConfig.BROADCASTER_SPAWN_RATE.getDefault(), 5000);
        rate(problems, cases, "landmineSpawn", HbmConfig.LANDMINE_SPAWN_RATE.getDefault(), 64);
        rate(problems, cases, "radHotspotSpawn", HbmConfig.RADIATION_HOTSPOT_SPAWN_RATE.getDefault(), 5000);
        rate(problems, cases, "vaultSpawn", HbmConfig.VAULT_SPAWN_RATE.getDefault(), 2500);
        rate(problems, cases, "geyserChlorineSpawn", HbmConfig.CHLORINE_GEYSER_SPAWN_RATE.getDefault(), 3000);
        rate(problems, cases, "capsuleSpawn", HbmConfig.SOYUZ_CAPSULE_SPAWN_RATE.getDefault(), 100);
        rate(problems, cases, "arcticVaultSpawn", HbmConfig.ARCTIC_VAULT_SPAWN_RATE.getDefault(), 500);
        rate(problems, cases, "jungleDungeonSpawn", HbmConfig.JUNGLE_DUNGEON_SPAWN_RATE.getDefault(), 2000);
        rate(problems, cases, "pyramidSpawn", HbmConfig.ANCIENT_TOMB_SPAWN_CHANCE.getDefault(), 4000);
        rate(problems, cases, "pinkTree", HbmConfig.PINK_TREE_SPAWN_RATE.getDefault(), 1000);
        rate(problems, cases, "stoneKeyhole", HbmConfig.STONE_KEYHOLE_SPAWN_RATE.getDefault(), 4);
        rate(problems, cases, "gasBubble", HbmConfig.GAS_BUBBLE_SPAWN_RATE.getDefault(), 12);
        rate(problems, cases, "explosiveBubble", HbmConfig.EXPLOSIVE_GAS_BUBBLE_SPAWN_RATE.getDefault(), 0);
        rate(problems, cases, "oilSpawn", HbmConfig.OIL_DEPOSIT_SPAWN_RATE.getDefault(), 100);
        rate(problems, cases, "oilMinSize", HbmConfig.OIL_DEPOSIT_MIN_RADIUS.getDefault(), 8);
        rate(problems, cases, "oilMaxSize", HbmConfig.OIL_DEPOSIT_MAX_RADIUS.getDefault(), 16);
        rate(problems, cases, "sandOilSpawn", HbmConfig.OIL_SAND_DEPOSIT_SPAWN_RATE.getDefault(), 200);
        rate(problems, cases, "sandOilMinSize", HbmConfig.OIL_SAND_DEPOSIT_MIN_RADIUS.getDefault(), 16);
        rate(problems, cases, "sandOilMaxSize", HbmConfig.OIL_SAND_DEPOSIT_MAX_RADIUS.getDefault(), 48);
        rate(problems, cases, "bedrockOilSpawn", HbmConfig.BEDROCK_OIL_DEPOSIT_SPAWN_RATE.getDefault(), 200);
        rate(problems, cases, "meteoriteSpawn", HbmConfig.METEORITE_SPAWN.getDefault(), 200);
        rate(problems, cases, "hiveSpawn", HbmConfig.GLYPHID_HIVE_SPAWN.getDefault(), 256);
        rate(problems, cases, "structureMinChunks", HbmConfig.HBM_STRUCTURE_MIN_CHUNKS.getDefault(), 4);
        rate(problems, cases, "structureMaxChunks", HbmConfig.HBM_STRUCTURE_MAX_CHUNKS.getDefault(), 16);
    }

    @SafeVarargs
    private static void spawn(ServerLevel level, List<String> problems, int[] cases, String structure, boolean expected,
                              ResourceKey<Biome>... biomes) {
        for (ResourceKey<Biome> key : biomes) {
            cases[0]++;
            Holder<Biome> biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
            Boolean actual = HbmLegacyStructureSelectionProbe.canSpawn(structure, biome);
            if (actual == null || actual != expected) {
                problems.add("parity e1: structure " + structure + " in " + key.location() + " canSpawn=" + actual
                        + " expected " + expected);
            }
        }
    }

    private static void biomes(ServerLevel level, List<String> problems, int[] cases) {
        // spire: heightVariation <= 0.05 && !water
        spawn(level, problems, cases, "spire", true, Biomes.PLAINS, Biomes.DESERT, Biomes.SAVANNA, Biomes.BEACH, Biomes.SNOWY_PLAINS);
        spawn(level, problems, cases, "spire", false, Biomes.FOREST, Biomes.TAIGA, Biomes.OCEAN, Biomes.RIVER, Biomes.WINDSWEPT_HILLS);
        // isFlatBiome: heightVariation <= 0.2 && !water && SPARSE
        for (String flat : new String[] {"labolatory", "radio", "broadcaster_tower"}) {
            spawn(level, problems, cases, flat, true, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WOODED_BADLANDS);
            spawn(level, problems, cases, flat, false, Biomes.PLAINS, Biomes.DESERT, Biomes.SNOWY_PLAINS, Biomes.WINDSWEPT_FOREST);
        }
        // SANDY (desert, mesa); the 1.7.10 beach is BEACH only
        for (String sandy : new String[] {"vertibird", "crashed_vertibird", "desert_shack_1", "desert_shack_2", "desert_shack_3", "dead_dish_small"}) {
            spawn(level, problems, cases, sandy, true, Biomes.DESERT, Biomes.BADLANDS, Biomes.WOODED_BADLANDS);
            spawn(level, problems, cases, sandy, false, Biomes.BEACH, Biomes.PLAINS, Biomes.SAVANNA);
        }
        // PLAINS (plains, savanna, savanna plateau)
        spawn(level, problems, cases, "dish", true, Biomes.PLAINS, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU);
        spawn(level, problems, cases, "dish", false, Biomes.DESERT, Biomes.FOREST, Biomes.SNOWY_PLAINS);
        spawn(level, problems, cases, "falloutshelter_house", true, Biomes.PLAINS, Biomes.SAVANNA);
        spawn(level, problems, cases, "water_pump", true, Biomes.PLAINS, Biomes.SWAMP, Biomes.SAVANNA);
        spawn(level, problems, cases, "water_pump", false, Biomes.FOREST, Biomes.DESERT);
        // heightVariation <= 0.2 / 0.3 && !water
        spawn(level, problems, cases, "factory", true, Biomes.PLAINS, Biomes.FOREST, Biomes.TAIGA, Biomes.JUNGLE, Biomes.SWAMP);
        spawn(level, problems, cases, "factory", false, Biomes.MUSHROOM_FIELDS, Biomes.WINDSWEPT_HILLS, Biomes.STONY_SHORE, Biomes.OCEAN);
        spawn(level, problems, cases, "crane", false, Biomes.MUSHROOM_FIELDS, Biomes.RIVER);
        for (String gentle : new String[] {"forestchem", "forest_post", "plane1", "plane2"}) {
            spawn(level, problems, cases, gentle, true, Biomes.MUSHROOM_FIELDS, Biomes.FOREST, Biomes.DESERT);
            spawn(level, problems, cases, gentle, false, Biomes.WINDSWEPT_HILLS, Biomes.STONY_SHORE, Biomes.OCEAN);
        }
        spawn(level, problems, cases, "tower_base", true, Biomes.FOREST, Biomes.PLAINS, Biomes.MUSHROOM_FIELDS);
        spawn(level, problems, cases, "tower_base", false, Biomes.DESERT, Biomes.BADLANDS, Biomes.WINDSWEPT_HILLS, Biomes.RIVER);
        // SNOWY includes the frozen ocean / river
        for (String snowy : new String[] {"abandoned_arcticstation", "military_base"}) {
            spawn(level, problems, cases, snowy, true, Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.FROZEN_OCEAN, Biomes.SNOWY_BEACH);
            spawn(level, problems, cases, snowy, false, Biomes.PLAINS, Biomes.TAIGA, Biomes.OCEAN);
        }
        // ocean structures
        spawn(level, problems, cases, "aircraft_carrier", true, Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.FROZEN_OCEAN);
        spawn(level, problems, cases, "oil_rig", true, Biomes.OCEAN, Biomes.FROZEN_OCEAN);
        spawn(level, problems, cases, "oil_rig", false, Biomes.DEEP_OCEAN, Biomes.BEACH);
        spawn(level, problems, cases, "lighthouse", true, Biomes.OCEAN, Biomes.BEACH, Biomes.STONY_SHORE);
        spawn(level, problems, cases, "beached_patrol", true, Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE);
        spawn(level, problems, cases, "beached_patrol", false, Biomes.OCEAN, Biomes.PLAINS);
        spawn(level, problems, cases, "submarine", true, Biomes.OCEAN, Biomes.DEEP_OCEAN);
        spawn(level, problems, cases, "submarine", false, Biomes.FROZEN_OCEAN, Biomes.WARM_OCEAN);
        // ruins: !water && canSpawnLightningBolt
        spawn(level, problems, cases, "ruin1", true, Biomes.PLAINS, Biomes.FOREST, Biomes.JUNGLE);
        spawn(level, problems, cases, "ruin1", false, Biomes.DESERT, Biomes.SAVANNA, Biomes.SNOWY_PLAINS, Biomes.OCEAN);
        // meteor dungeon: rootHeight >= 0; features / bunker: !water
        spawn(level, problems, cases, "meteor_dungeon", true, Biomes.PLAINS, Biomes.BEACH, Biomes.WINDSWEPT_HILLS);
        spawn(level, problems, cases, "meteor_dungeon", false, Biomes.SWAMP, Biomes.OCEAN, Biomes.RIVER);
        spawn(level, problems, cases, "features", true, Biomes.PLAINS, Biomes.DESERT, Biomes.WINDSWEPT_HILLS);
        spawn(level, problems, cases, "bunker", false, Biomes.OCEAN, Biomes.RIVER);

        cases[0]++;
        Holder<Biome> grove = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.GROVE);
        Holder<Biome> birch = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.BIRCH_FOREST);
        if (LegacyBiomeTraits.isForestClass(grove) || !LegacyBiomeTraits.isForestClass(birch)) {
            problems.add("parity e1: BiomeGenForest class mapping (foxglove) is wrong");
        }
    }

    private static void capsule(List<String> problems, int[] cases) {
        cases[0]++;
        BlockState state = HbmBlocks.SOYUZ_CAPSULE.get().defaultBlockState();
        if (!state.hasProperty(SoyuzCapsuleBlock.RUSTY) || state.getValue(SoyuzCapsuleBlock.RUSTY)) {
            problems.add("parity e1: soyuz_capsule must have rusty=false by default (meta 3 only from world generation)");
        }
        cases[0]++;
        if (ParityE1Diagnostics.class.getResource("/assets/atomiclegacy/textures/models/soyuz_capsule/soyuz_lander_rust.png") == null) {
            problems.add("parity e1: soyuz_lander_rust.png missing");
        }
    }
}
